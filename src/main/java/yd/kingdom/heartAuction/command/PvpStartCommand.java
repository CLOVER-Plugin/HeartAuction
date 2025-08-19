package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yd.kingdom.heartAuction.manager.GameManager;

public class PvpStartCommand implements CommandExecutor {
    private final GameManager game;
    
    public PvpStartCommand(GameManager game) { 
        this.game = game; 
    }
    
    @Override 
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("heart.admin")) {
            sender.sendMessage("§c권한이 없습니다!");
            return true;
        }
        
        game.startPvpImmediately();
        sender.sendMessage("§aPVP가 즉시 시작되었습니다!");
        return true;
    }
}
