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
        bet = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "bet.yml"));
    }

    public void run(Player p, int guess) {
        var c = plugin.getConfig();
        World w = Bukkit.getWorld(c.getString("flower-gamble.center.world"));
        int cx = c.getInt("flower-gamble.center.x");
        int cy = c.getInt("flower-gamble.center.y");
        int cz = c.getInt("flower-gamble.center.z");
        int size = c.getInt("flower-gamble.size", 5);

        Location chestLoc = new Location(
                Bukkit.getWorld(c.getString("flower-gamble.chest.world")),
                c.getInt("flower-gamble.chest.x"),
                c.getInt("flower-gamble.chest.y"),
                c.getInt("flower-gamble.chest.z")
        );
        Chest chest = (Chest) chestLoc.getBlock().getState();

        // 상자 다이아 총합
        int diamonds = chest.getInventory().all(Material.DIAMOND).values()
                .stream().mapToInt(i -> i.getAmount()).sum();
        chest.getInventory().remove(Material.DIAMOND);
        if (diamonds <= 0) { p.sendMessage("§c상자에 다이아가 없습니다."); return; }

        // 식생 생성
        Set<Material> flowers = flowerSet();
        int minX = cx - size / 2, minZ = cz - size / 2;
        int count = 0;
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
            // 기존 식생 제거 후 새로 배치
            w.getBlockAt(minX + x, cy + 1, minZ + z).setType(Material.AIR);
            double r = Math.random();
            if (r < 0.2) {
                Material m = flowers.stream().skip((int) (flowers.size() * Math.random()))
                        .findFirst().orElse(Material.DANDELION);
                w.getBlockAt(minX + x, cy + 1, minZ + z).setType(m);
                count++;
            } else if (r < 0.9) {
                w.getBlockAt(minX + x, cy + 1, minZ + z).setType(Material.SHORT_GRASS);
            }
        }

        // 성공/실패 분기
        if (guess != count) {
            p.sendMessage("§c실패! 입력: " + guess + "개 " + " / 실제 꽃: " + count + "개");
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    clearVegetation(w, minX, cy, minZ, size), 20L * 5);
            return;
        }

        // 성공: 보상 지급 후 '수령하면' 제거 (슬롯 비워지면 정리, 최대 60초 대기)
        double mult = bet.getDouble("flower." + count, bet.getDouble("flower.default", 0.0));
        int payout = (int) Math.floor(diamonds * mult);
        depositDiamonds(chest, payout); // 0번 슬롯부터 채우기
        p.sendMessage("§a정답! 꽃 " + count + "개 §7/ 배율 " + mult + " / 지급 " + payout + "개");
        waitAndClearAfterLoot(chest, w, minX, cy, minZ, size);
    }

    private void depositDiamonds(Chest chest, int amount) {
        var inv = chest.getBlockInventory();

        // 1) 기존 다이아 스택 보충
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != Material.DIAMOND) continue;
            int space = it.getMaxStackSize() - it.getAmount();
            if (space <= 0) continue;
            int add = Math.min(space, amount);
            it.setAmount(it.getAmount() + add);
            amount -= add;
        }

        // 2) 빈 슬롯부터 새 스택 채우기 (0번 슬롯부터)
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) continue;
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            inv.setItem(i, new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }

        // 3) 남으면 바닥에 드랍(안전장치)
        while (amount > 0) {
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            chest.getWorld().dropItem(chest.getLocation().add(0.5, 1.0, 0.5), new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }
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
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
            w.getBlockAt(minX + x, cy + 1, minZ + z).setType(Material.AIR);
        }
    }

    private void waitAndClearAfterLoot(Chest chest, World w, int minX, int cy, int minZ, int size) {
        new BukkitRunnable() {
            int seconds = 0;
            @Override public void run() {
                boolean hasDiamonds = false;
                for (ItemStack it : chest.getBlockInventory().getContents()) {
                    if (it != null && it.getType() == Material.DIAMOND && it.getAmount() > 0) {
                        hasDiamonds = true; break;
                    }
                }
                if (!hasDiamonds || seconds >= 60) { // 보상 수령 완료 또는 60초 타임아웃
                    clearVegetation(w, minX, cy, minZ, size);
                    cancel();
                }
                seconds++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}