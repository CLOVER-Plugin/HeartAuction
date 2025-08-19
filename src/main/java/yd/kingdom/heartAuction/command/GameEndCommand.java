package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yd.kingdom.heartAuction.manager.GameManager;

public class GameEndCommand implements CommandExecutor {
    private final GameManager game;
    
    public GameEndCommand(GameManager game) { 
        this.game = game; 
    }
    
    @Override 
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("heart.admin")) {
            sender.sendMessage("§c권한이 없습니다!");
            return true;
        }
        
        game.endGame();
        sender.sendMessage("§a게임이 종료되었습니다!");
        return true;
    }
}
