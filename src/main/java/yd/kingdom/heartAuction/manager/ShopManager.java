package yd.kingdom.heartAuction.manager;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Items;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ShopManager {
    private final HeartAuction plugin;
    private final Map<Material, Integer> prices = new HashMap<>();
    private final Map<Integer, Material> layout = new HashMap<>();

    public ShopManager(HeartAuction plugin) {
        this.plugin = plugin; load();
        load();
    }

    private void load() {
        File f = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration yc = YamlConfiguration.loadConfiguration(f);
        for (String k : yc.getConfigurationSection("prices").getKeys(false)) {
            prices.put(Material.valueOf(k), yc.getInt("prices."+k));
        }
        for (String k : yc.getConfigurationSection("layout").getKeys(false)) {
            layout.put(Integer.parseInt(k), Material.valueOf(yc.getString("layout."+k)));
        }
    }

    public boolean isShopNpc(Entity e) {
        // 스코어보드 태그로 식별(태그가 붙어있다면 그대로 사용)
        if (e.getScoreboardTags().contains("HEART_SHOP_NPC")) return true;

        // 커스텀 이름이 config의 shop-npc.name 과 같으면 상점 NPC로 인식
        String target = ChatColor.stripColor(ChatColor.translateAlternateColorCodes(
                '&', java.util.Objects.requireNonNullElse(plugin.getConfig().getString("shop-npc.name"), "아이템 상점")
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
            if (m==Material.SPLASH_POTION) {
                it = new ItemStack(Material.SPLASH_POTION);
                PotionMeta meta = (PotionMeta) it.getItemMeta();
                meta.clearCustomEffects();
                meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, true, true), true); // 치유 II
                meta.setDisplayName("§d즉시 치유(투척)");
                it.setItemMeta(meta);
            } else {
                it = new ItemStack(m);
            }
            int price = prices.getOrDefault(m, 9999);
            it = Items.withLore(it, "§7가격: §b다이아 "+price+"개");
            inv.setItem(slot, it);
        }
        return inv;
    }

    public void handleClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals("§8아이템 상점")) return;
        e.setCancelled(true);
        if (e.getCurrentItem()==null) return;
        Player p = (Player) e.getWhoClicked();
        Material m = e.getCurrentItem().getType();
        Integer price = prices.get(m);
        if (price==null) return;
        if (!Items.take(p, Material.DIAMOND, price)) { p.sendMessage("§c다이아가 부족합니다!"); return; }
        ItemStack buy = e.getCurrentItem().clone(); buy.setAmount(1);
        p.getInventory().addItem(buy);
        p.sendMessage("§a구매 완료: §f"+m+" §7(다이아 "+price+")");
    }
}