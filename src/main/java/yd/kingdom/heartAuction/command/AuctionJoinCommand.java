package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.manager.AuctionManager;

public class AuctionJoinCommand implements CommandExecutor {
    private final AuctionManager auction;
    public AuctionJoinCommand(AuctionManager a) { this.auction = a; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("플레이어만"); return true; }
        auction.join(p);
        return true;
    }
}