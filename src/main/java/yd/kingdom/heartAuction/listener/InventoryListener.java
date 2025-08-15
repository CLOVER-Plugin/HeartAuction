package yd.kingdom.heartAuction.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import yd.kingdom.heartAuction.manager.ShopManager;

public class InventoryListener implements Listener {
    private final ShopManager shop;
    public InventoryListener(ShopManager shop) { this.shop = shop; }

    @EventHandler
    public void onClick(InventoryClickEvent e) { shop.handleClick(e); }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (shop.isShopNpc(e.getRightClicked())) {
            e.setCancelled(true);
            shop.open(e.getPlayer());
        }
    }
}