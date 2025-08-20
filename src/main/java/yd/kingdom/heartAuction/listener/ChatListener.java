package yd.kingdom.heartAuction.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.manager.AuctionManager;

public class ChatListener implements Listener {
    private final AuctionManager auction;
    public ChatListener(AuctionManager auction) { this.auction = auction; }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        if (auction.isExpectingBid(e.getPlayer().getUniqueId())) {
            String msg = e.getMessage();
            e.setCancelled(true); // 비공개
            // 메인 스레드에서 처리
            Bukkit.getScheduler().runTask(HeartAuction.get(), () -> auction.submitBid(e.getPlayer(), msg));
        }
    }
}