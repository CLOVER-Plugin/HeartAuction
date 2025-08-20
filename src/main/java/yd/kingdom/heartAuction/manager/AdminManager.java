package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import yd.kingdom.heartAuction.HeartAuction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminManager {
    private final HeartAuction plugin;
    private final Set<UUID> admins = new HashSet<>();

    public AdminManager(HeartAuction plugin) {
        this.plugin = plugin;
        FileConfiguration c = plugin.getConfig();
        if (c.isList("admins")) {
            for (String s : c.getStringList("admins")) {
                try { admins.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
        }
    }

    public boolean toggle(String name) {
        OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(name);
        if (op == null) op = Bukkit.getOfflinePlayer(name);
        UUID id = op.getUniqueId();
        boolean now;
        if (admins.contains(id)) { admins.remove(id); now = false; }
        else { admins.add(id); now = true; }
        save();
        return now;
    }

    public boolean isAdmin(UUID id) { return admins.contains(id); }

    private void save() {
        plugin.getConfig().set("admins", admins.stream().map(UUID::toString).toList());
        plugin.saveConfig();
    }
}