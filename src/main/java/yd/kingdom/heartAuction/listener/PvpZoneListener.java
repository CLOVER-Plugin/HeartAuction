package yd.kingdom.heartAuction.listener;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import yd.kingdom.heartAuction.manager.PvpZoneManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PvpZoneListener implements Listener {
    private final PvpZoneManager zone;
    private final Set<UUID> toSpectate = new HashSet<>();

    public PvpZoneListener(PvpZoneManager zone, org.bukkit.plugin.Plugin plugin) {
        this.zone = zone;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (zone.isOutside(e.getEntity().getLocation())) {
            toSpectate.add(e.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (toSpectate.remove(e.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("HeartAuction"), () ->
                    e.getPlayer().setGameMode(GameMode.SPECTATOR));
        }
    }
}