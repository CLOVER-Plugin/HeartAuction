package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.util.Locations;

public class SpawnCommand implements CommandExecutor {
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("플레이어만"); return true; }
        p.teleport(Locations.spawn());
        p.sendMessage("§a스폰으로 이동했습니다.");
        return true;
    }
}