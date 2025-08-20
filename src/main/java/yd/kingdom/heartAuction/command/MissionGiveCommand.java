package yd.kingdom.heartAuction.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.manager.MissionManager;

public class MissionGiveCommand implements CommandExecutor {
    private final MissionManager missions;
    public MissionGiveCommand(MissionManager m) { this.missions=m; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Player t = args.length>=1? Bukkit.getPlayer(args[0]) : (sender instanceof Player ? (Player)sender : null);
        if (t==null) { sender.sendMessage("대상 플레이어 지정"); return true; }
        missions.giveRandom(t); return true;
    }
}