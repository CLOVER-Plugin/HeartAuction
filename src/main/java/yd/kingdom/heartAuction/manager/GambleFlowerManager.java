package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import yd.kingdom.heartAuction.HeartAuction;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GambleFlowerManager {
    private final HeartAuction plugin;
    private final YamlConfiguration bet;

    public GambleFlowerManager(HeartAuction plugin) {
        this.plugin = plugin;
        this.bet = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "bet.yml"));
    }

    /** 인자 없이 실행: 상자 다이아(배팅액) × flower.<꽃개수> 배율 지급 */
    public void run(Player p) {
        var c = plugin.getConfig();

        String wName = c.getString("flower-gamble.center.world");
        World w = Bukkit.getWorld(wName);
        if (w == null) { p.sendMessage("§cflower-gamble.center.world 월드를 찾을 수 없습니다: " + wName); return; }

        int cx = c.getInt("flower-gamble.center.x");
        int cy = c.getInt("flower-gamble.center.y");
        int cz = c.getInt("flower-gamble.center.z");
        int size = c.getInt("flower-gamble.size", 5);

        String cwName = c.getString("flower-gamble.chest.world");
        World cw = Bukkit.getWorld(cwName);
        if (cw == null) { p.sendMessage("§cflower-gamble.chest.world 월드를 찾을 수 없습니다: " + cwName); return; }

        Location chestLoc = new Location(cw,
                c.getInt("flower-gamble.chest.x"),
                c.getInt("flower-gamble.chest.y"),
                c.getInt("flower-gamble.chest.z"));

        var state = chestLoc.getBlock().getState();
        if (!(state instanceof Chest chest)) {
            p.sendMessage("§c설정된 위치에 §e상자§c가 없습니다. config.yml의 flower-gamble.chest를 확인하세요.");
            return;
        }

        int stake = chest.getBlockInventory().all(Material.DIAMOND).values()
                .stream().mapToInt(i -> i.getAmount()).sum();
        if (stake <= 0) { p.sendMessage("§c상자에 배팅할 다이아를 먼저 넣어주세요."); return; }

        // 식생 생성 & 꽃 개수 집계
        Set<Material> flowers = flowerSet();
        int minX = cx - size / 2, minZ = cz - size / 2;
        int count = 0;
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
            // 기존 식생 제거
            w.getBlockAt(minX + x, cy + 1, minZ + z).setType(Material.AIR);

            // 1칸당 확률: 10% 꽃 / 60% 잔디 / 30% 빈칸
            double r = Math.random();
            if (r < 0.045) {
                Material m = pickRandom(flowers);
                w.getBlockAt(minX + x, cy + 1, minZ + z).setType(m);
                count++;
            } else if (r < 0.70) {
                w.getBlockAt(minX + x, cy + 1, minZ + z).setType(Material.SHORT_GRASS);
            } // else: 그대로 AIR (빈칸)
        }

        // 배율 적용 → 배팅액 제거 → 지급
        double mult = bet.getDouble("flower." + count, bet.getDouble("flower.default", 0.0));
        int payout = (int) Math.floor(stake * mult);
        chest.getBlockInventory().remove(Material.DIAMOND);
        depositDiamonds(chest, payout);

        p.sendMessage("§a꽃도박 결과: 꽃 §e" + count + "개§a / 배율 §e" + mult + "§a / 지급 §b" + payout + "개");

        // ✅ 결과 출력 후 1초 뒤 판 리셋
        Bukkit.getScheduler().runTaskLater(plugin, () ->
                clearVegetation(w, minX, cy, minZ, size), 20L);
    }

    private Material pickRandom(Set<Material> set) {
        int idx = (int) (Math.random() * set.size()), i = 0;
        for (Material m : set) if (i++ == idx) return m;
        return Material.DANDELION;
    }

    private Set<Material> flowerSet() {
        return new HashSet<>(Arrays.asList(
                Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
                Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
                Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
                Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY
        ));
    }

    private void clearVegetation(World w, int minX, int cy, int minZ, int size) {
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++)
            w.getBlockAt(minX + x, cy + 1, minZ + z).setType(Material.AIR);
    }

    /** 기존 스택 보충 → 빈칸 채우기 → 남으면 드랍 */
    private void depositDiamonds(Chest chest, int amount) {
        var inv = chest.getBlockInventory();
        // 기존 스택 보충
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != Material.DIAMOND) continue;
            int space = it.getMaxStackSize() - it.getAmount();
            if (space <= 0) continue;
            int add = Math.min(space, amount);
            it.setAmount(it.getAmount() + add);
            amount -= add;
        }
        // 빈 슬롯부터 채우기
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) continue;
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            inv.setItem(i, new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }
        // 남으면 드랍
        while (amount > 0) {
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            chest.getWorld().dropItem(chest.getLocation().add(0.5, 1.0, 0.5), new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }
    }
}