package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.HeartAuction;

import java.io.File;
import java.util.List;

public class GambleEggManager {
    private final HeartAuction plugin;
    private final YamlConfiguration bet;

    public GambleEggManager(HeartAuction plugin) {
        this.plugin = plugin;
        bet = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "bet.yml"));
    }

    public void run(Player p, int guess) {
        var c = plugin.getConfig();
        World w = Bukkit.getWorld(c.getString("egg-gamble.pit-origin.world"));
        int ox = c.getInt("egg-gamble.pit-origin.x");
        int oy = c.getInt("egg-gamble.pit-origin.y");
        int oz = c.getInt("egg-gamble.pit-origin.z");
        int sx = c.getInt("egg-gamble.size-x",3);
        int sy = c.getInt("egg-gamble.size-y",5);
        int sz = c.getInt("egg-gamble.size-z",3);

        Location chestLoc = new Location(Bukkit.getWorld(c.getString("egg-gamble.chest.world")), c.getInt("egg-gamble.chest.x"), c.getInt("egg-gamble.chest.y"), c.getInt("egg-gamble.chest.z"));
        Chest chest = (Chest) chestLoc.getBlock().getState();
        int diamonds = chest.getInventory().all(Material.DIAMOND).values().stream().mapToInt(i->i.getAmount()).sum();
        chest.getInventory().remove(Material.DIAMOND);
        if (diamonds<=0) { p.sendMessage("§c상자에 다이아가 없습니다."); return; }

        // 달걀 10개 지급
        p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EGG, 10));
        p.sendMessage("§a달걀 10개를 모두 던지세요! 15초 후 결과를 계산합니다.");

        // 15초 후 집계
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int babies = countBabies(w, ox, oy, oz, sx, sy, sz);
            double mult = bet.getDouble("chicken."+babies, bet.getDouble("chicken.default", 0.0));
            int payout = (int)Math.floor(diamonds * mult);
            chest.getBlockInventory().setItem(c.getInt("egg-gamble.payout-chest-slot",13), new org.bukkit.inventory.ItemStack(Material.DIAMOND, payout));
            p.sendMessage("§a새끼 닭: "+babies+"마리 §7/ 배율 "+mult+" / 지급 "+payout);

            // 정리
            clearEntities(w, ox, oy, oz, sx, sy, sz);
        }, 20L*15);
    }

    private int countBabies(World w, int ox, int oy, int oz, int sx, int sy, int sz) {
        List<Entity> ents = w.getNearbyEntities(new Location(w,ox,oy,oz), sx, sy, sz).stream().toList();
        int c=0; for (Entity e : ents) if (e.getType()== EntityType.CHICKEN) {
            Chicken ch = (Chicken)e; if (!ch.isAdult()) c++;
        }
        return c;
    }

    private void clearEntities(World w, int ox, int oy, int oz, int sx, int sy, int sz) {
        for (Entity e : w.getNearbyEntities(new Location(w,ox,oy,oz), sx, sy, sz)) {
            if (e.getType()==EntityType.CHICKEN) e.remove();
        }
    }
}