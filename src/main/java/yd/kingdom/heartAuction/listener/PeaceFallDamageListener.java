package yd.kingdom.heartAuction.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import yd.kingdom.heartAuction.manager.GameManager;

public class PeaceFallDamageListener implements Listener {
    private final GameManager game;

    public PeaceFallDamageListener(GameManager game) {
        this.game = game;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!game.isPeaceTime()) return;                 // 평화시간에만 적용
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);                        // 낙하 피해 무효화
        }
    }
}