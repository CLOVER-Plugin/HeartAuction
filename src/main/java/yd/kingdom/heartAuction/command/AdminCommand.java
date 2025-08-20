package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import yd.kingdom.heartAuction.manager.AdminManager;

public class AdminCommand implements CommandExecutor {
    private final AdminManager admins;
    public AdminCommand(AdminManager admins) { this.admins = admins; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length!=1) { sender.sendMessage("/운영자 <닉네임>"); return true; }
        boolean set = admins.toggle(args[0]);
        sender.sendMessage("§a"+args[0]+" §7운영자="+set);
        return true;
    }
}