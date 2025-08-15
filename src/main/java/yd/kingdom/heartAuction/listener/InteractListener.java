package yd.kingdom.heartAuction.listener;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.manager.MissionManager;
import yd.kingdom.heartAuction.util.Items;

public class InteractListener implements Listener {
    private final HeartAuction plugin;
    private final MissionManager missions;
    public InteractListener(HeartAuction plugin, MissionManager missions) {
        this.plugin = plugin; this.missions = missions;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getItem() == null) return;
        Player p = e.getPlayer();

        // 체력 증가 아이템 사용
        if (Items.isHealthItem(e.getItem())) {
            e.setCancelled(true);
            Items.consumeOne(e.getItem());
            var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr == null) return;
            double now = attr.getBaseValue();
            double cap = 100.0; // 50칸
            if (now >= cap) { p.sendMessage("§c최대 체력 한도(50칸)에 도달했습니다."); return; }
            attr.setBaseValue(Math.min(now + 2.0, cap));
            p.sendMessage("§a최대 체력이 1칸 증가했습니다!");
        }
    }
}