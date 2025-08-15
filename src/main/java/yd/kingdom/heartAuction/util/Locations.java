package yd.kingdom.heartAuction.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import yd.kingdom.heartAuction.HeartAuction;

public class Locations {
    public static Location spawn() {
        HeartAuction plugin = JavaPlugin.getPlugin(HeartAuction.class);
        FileConfiguration c = plugin.getConfig();

        String worldName = c.getString("spawn.world");
        if (worldName == null) worldName = "world";
        World w = Bukkit.getWorld(worldName);

        double x = c.getDouble("spawn.x");
        double y = c.getDouble("spawn.y");
        double z = c.getDouble("spawn.z");
        float yaw = (float) c.getDouble("spawn.yaw");
        float pitch = (float) c.getDouble("spawn.pitch");

        return new Location(w, x, y, z, yaw, pitch);
    }
}