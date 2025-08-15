package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.manager.MissionManager;

public class MissionForfeitCommand implements CommandExecutor {
    private final MissionManager missions;
    public MissionForfeitCommand(MissionManager m) { this.missions=m; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("플레이어만"); return true; }
        missions.forfeitAndNew(p); return true;
    }
}