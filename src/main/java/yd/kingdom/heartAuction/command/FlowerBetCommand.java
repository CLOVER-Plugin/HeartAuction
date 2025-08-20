package yd.kingdom.heartAuction.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.manager.GambleFlowerManager;

public class FlowerBetCommand implements CommandExecutor {
    private final GambleFlowerManager gm;
    public FlowerBetCommand(GambleFlowerManager gm) { this.gm = gm; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        // 인자 없이 바로 실행
        gm.run(p);
        return true;
    }
}