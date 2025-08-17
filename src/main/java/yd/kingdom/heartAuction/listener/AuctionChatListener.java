package yd.kingdom.heartAuction.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.manager.AuctionManager;

public class AuctionChatListener implements Listener {
    private final AuctionManager auction;

    public AuctionChatListener(AuctionManager auction) {
        this.auction = auction;
        Bukkit.getPluginManager().registerEvents(this, HeartAuction.getPlugin(HeartAuction.class));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (!auction.isExpectingBid(e.getPlayer().getUniqueId())) return;
        String msg = e.getMessage();
        e.setCancelled(true); // 입찰 비공개
        Bukkit.getScheduler().runTask(HeartAuction.getPlugin(HeartAuction.class),
                () -> auction.submitBid(e.getPlayer(), msg));
    }
}