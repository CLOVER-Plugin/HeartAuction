package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.manager.GambleFlowerManager;

public class FlowerBetCommand implements CommandExecutor {
    private final GambleFlowerManager gm;
    public FlowerBetCommand(GambleFlowerManager gm) { this.gm = gm; }

    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("플레이어만"); return true; }
        if (args.length != 1) { p.sendMessage("/꽃도박 <꽃개수>"); return true; }
        try { int guess = Integer.parseInt(args[0]); gm.run(p, guess); }
        catch (Exception e) { p.sendMessage("숫자만!"); }
        return true;
    }
}