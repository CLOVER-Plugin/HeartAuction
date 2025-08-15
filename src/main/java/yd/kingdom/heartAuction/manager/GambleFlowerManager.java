package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
        Location chestLoc = new Location(Bukkit.getWorld(c.getString("flower-gamble.chest.world")), c.getInt("flower-gamble.chest.x"), c.getInt("flower-gamble.chest.y"), c.getInt("flower-gamble.chest.z"));
        Chest chest = (Chest) chestLoc.getBlock().getState();

        // 상자 다이아 총합을 기록 후 0으로(꺼내기)
        int diamonds = chest.getInventory().all(Material.DIAMOND).values().stream().mapToInt(i->i.getAmount()).sum();
        chest.getInventory().remove(Material.DIAMOND);
        if (diamonds<=0) { p.sendMessage("§c상자에 다이아가 없습니다."); return; }

        // 중앙 잔디에 뼛가루 효과 시뮬 (간단화: 랜덤 꽃 스폰)
        // 실제 뼛가루 상호작용을 흉내내는 대신, 5x5 안에 임의로 꽃/잔디를 배치 후 카운트
        Set<Material> flowers = flowerSet();
        int minX = cx - size/2, minZ = cz - size/2;
        int count=0;
        for (int x=0; x<size; x++) for (int z=0; z<size; z++) {
            Block b = w.getBlockAt(minX+x, cy, minZ+z);
            // 기존 식생 제거
            w.getBlockAt(minX+x, cy+1, minZ+z).setType(Material.AIR);
            // 간단히 40% 확률로 꽃, 30% 잔디, 30% 비움
            double r = Math.random();
            if (r < 0.4) {
                Material m = flowers.stream().skip((int)(flowers.size()*Math.random())).findFirst().orElse(Material.DANDELION);
                w.getBlockAt(minX+x, cy+1, minZ+z).setType(m);
                count++;
            } else if (r < 0.7) {
                w.getBlockAt(minX+x, cy+1, minZ+z).setType(Material.SHORT_GRASS);
            }
        }

        // 결과 계산
        double mult = bet.getDouble("flower."+count, bet.getDouble("flower.default", 0.0));
        int payout = (int)Math.floor(diamonds * mult);
        int slot = c.getInt("flower-gamble.payout-chest-slot", 13);
        chest.getBlockInventory().setItem(slot, new org.bukkit.inventory.ItemStack(Material.DIAMOND, payout));
        p.sendMessage("§a꽃 개수: "+count+"개 §7/ 배율 "+mult+" / 지급 "+payout);

        // 플레이어가 수거하면 식생 제거
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int x=0; x<size; x++) for (int z=0; z<size; z++) {
                w.getBlockAt(minX+x, cy+1, minZ+z).setType(Material.AIR);
            }
        }, 20L*10);
    }

    private Set<Material> flowerSet() {
        return new HashSet<>(Arrays.asList(
                Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
                Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
                Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
                Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY
        ));
    }
}