package yd.kingdom.heartAuction.manager;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Items;

import java.io.File;
import java.util.*;

public class ShopManager {
    private final HeartAuction plugin;
    private final Map<Material, Integer> prices = new HashMap<>();
    private final Map<Integer, Material> layout = new HashMap<>();

    // === YAML에서 읽어오는 커스텀 전투 수치(기본값 포함) ===
    private int leatherArmor = 1;
    private int goldenArmor = 2;
    private int chainmailArmor = 3;
    private int woodenSwordAtk = 1;
    private int goldenSwordAtk = 2;
    private int ironSwordAtk   = 3;

    public ShopManager(HeartAuction plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        File f = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration yc = YamlConfiguration.loadConfiguration(f);

        // 가격/레이아웃
        for (String k : yc.getConfigurationSection("prices").getKeys(false)) {
            prices.put(Material.valueOf(k), yc.getInt("prices."+k));
        }
        for (String k : yc.getConfigurationSection("layout").getKeys(false)) {
            layout.put(Integer.parseInt(k), Material.valueOf(yc.getString("layout."+k)));
        }

        // 커스텀 전투 수치
        if (yc.isConfigurationSection("custom-attributes.armor")) {
            leatherArmor   = yc.getInt("custom-attributes.armor.leather", leatherArmor);
            goldenArmor    = yc.getInt("custom-attributes.armor.golden", goldenArmor);
            chainmailArmor = yc.getInt("custom-attributes.armor.chainmail", chainmailArmor);
        }
        if (yc.isConfigurationSection("custom-attributes.swords")) {
            woodenSwordAtk = yc.getInt("custom-attributes.swords.wooden", woodenSwordAtk);
            goldenSwordAtk = yc.getInt("custom-attributes.swords.golden", goldenSwordAtk);
            ironSwordAtk   = yc.getInt("custom-attributes.swords.iron", ironSwordAtk);
        }
    }

    public boolean isShopNpc(Entity e) {
        // 스코어보드 태그 우선
        if (e.getScoreboardTags().contains("HEART_SHOP_NPC")) return true;

        // 커스텀 이름 매칭
        String target = ChatColor.stripColor(ChatColor.translateAlternateColorCodes(
                '&', Objects.requireNonNullElse(plugin.getConfig().getString("shop-npc.name"), "아이템 상점")
        ));
        String name = e.getCustomName();
        return name != null && ChatColor.stripColor(name).equalsIgnoreCase(target);
    }

    public void open(Player p) { p.openInventory(build()); }

    private Inventory build() {
        Inventory inv = Bukkit.createInventory(null, 54, "§8아이템 상점");
        for (var entry : layout.entrySet()) {
            int slot = entry.getKey();
            Material m = entry.getValue();
            ItemStack it;

            if (m == Material.SPLASH_POTION) {
                it = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) it.getItemMeta();
                meta.clearCustomEffects();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, true, true), true); // 치유 II
                meta.setDisplayName("§d즉시 치유(투척)");
                it.setItemMeta(meta);
            } else {
                it = new ItemStack(m);
            }

            // ▼ 커스텀 전투 수치 적용(미리보기에도 적용)
            it = applyCustomAttributes(it);

            int price = prices.getOrDefault(m, 9999);
            it = Items.withLore(it, "§7가격: §b다이아 " + price + "개");
            inv.setItem(slot, it);
        }
        return inv;
    }

    public void handleClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§8아이템 상점")) return;
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        Material m = e.getCurrentItem().getType();
        Integer price = prices.get(m);
        if (price == null) return;

        if (!Items.take(p, Material.DIAMOND, price)) {
            p.sendMessage("§c다이아가 부족합니다!");
            return;
        }
        ItemStack buy = e.getCurrentItem().clone();
        buy.setAmount(1);

        // ▼ 실제 지급 아이템에도 커스텀 전투 수치 확정 적용
        buy = stripPriceLore(buy);
        buy = applyCustomAttributes(buy);

        p.getInventory().addItem(buy);
        p.sendMessage("§a구매 완료: §f" + m + " §7(다이아 " + price + ")");
    }

    // 가격 로어 제거(복제한 GUI 아이템에서)
    private ItemStack stripPriceLore(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;
        meta.setLore(null);
        it.setItemMeta(meta);
        return it;
    }

    // === 커스텀 전투 수치 적용 ===
    private ItemStack applyCustomAttributes(ItemStack it) {
        if (it == null) return null;
        Material mat = it.getType();
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return it;

        String name = mat.name();

        // ▷ 갑옷(가죽/금/사슬) — 부위당 방어력 고정
        if (isArmorOf(name, "LEATHER_")) {
            it.setItemMeta(applyArmor(meta, slotOfArmor(name), leatherArmor));
            return it;
        }
        if (isArmorOf(name, "GOLDEN_")) {
            it.setItemMeta(applyArmor(meta, slotOfArmor(name), goldenArmor));
            return it;
        }
        if (isArmorOf(name, "CHAINMAIL_")) {
            it.setItemMeta(applyArmor(meta, slotOfArmor(name), chainmailArmor));
            return it;
        }

        // ▷ 검(나무/금/철) — 공격력 고정
        if (mat == Material.WOODEN_SWORD) {
            it.setItemMeta(applyAttack(meta, woodenSwordAtk));
            return it;
        }
        if (mat == Material.GOLDEN_SWORD) {
            it.setItemMeta(applyAttack(meta, goldenSwordAtk));
            return it;
        }
        if (mat == Material.IRON_SWORD) {
            it.setItemMeta(applyAttack(meta, ironSwordAtk));
            return it;
        }

        // 그 외 아이템은 변경 없음
        return it;
    }

    private boolean isArmorOf(String materialName, String prefix) {
        if (!materialName.startsWith(prefix)) return false;
        return materialName.endsWith("_HELMET")
                || materialName.endsWith("_CHESTPLATE")
                || materialName.endsWith("_LEGGINGS")
                || materialName.endsWith("_BOOTS");
    }

    private EquipmentSlot slotOfArmor(String materialName) {
        if (materialName.endsWith("_HELMET"))     return EquipmentSlot.HEAD;
        if (materialName.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (materialName.endsWith("_LEGGINGS"))   return EquipmentSlot.LEGS;
        if (materialName.endsWith("_BOOTS"))      return EquipmentSlot.FEET;
        return EquipmentSlot.CHEST; // fallback
    }

    private ItemMeta applyArmor(ItemMeta meta, EquipmentSlot slot, int armorAmount) {
        meta.setAttributeModifiers(null); // 기존(바닐라/이전) 수치 초기화

        // 1.21+ 권장 생성자: (NamespacedKey, amount, Operation, EquipmentSlotGroup)
        AttributeModifier mod = new AttributeModifier(
                new NamespacedKey(plugin, "armor_" + slot.name().toLowerCase()),
                armorAmount,
                AttributeModifier.Operation.ADD_NUMBER,
                toGroup(slot) // EquipmentSlot -> EquipmentSlotGroup 매핑
        );

        meta.addAttributeModifier(Attribute.GENERIC_ARMOR, mod);
        return meta;
    }

    private ItemMeta applyAttack(ItemMeta meta, int attackAmount) {
        meta.setAttributeModifiers(null);

        AttributeModifier mod = new AttributeModifier(
                new NamespacedKey(plugin, "attack_mainhand"),
                attackAmount,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.MAINHAND
        );

        meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE, mod);
        return meta;
    }

    private EquipmentSlotGroup toGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD  -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS  -> EquipmentSlotGroup.LEGS;
            case FEET  -> EquipmentSlotGroup.FEET;
            default    -> EquipmentSlotGroup.ARMOR;
        };
    }

    public Set<Material> getRegisteredItems() {
        return new HashSet<>(layout.values());
    }
}