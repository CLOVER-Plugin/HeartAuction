package yd.kingdom.heartAuction.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class DiamondOreGuard implements Listener {
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Material t = e.getBlock().getType();
        if (t == Material.DIAMOND_ORE || t == Material.DEEPSLATE_DIAMOND_ORE) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c다이아몬드 원석 채굴은 금지되어 있습니다.");
        }
    }
}