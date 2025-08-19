package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Tasker;

import java.util.concurrent.atomic.AtomicBoolean;

public class GameManager {
    private final HeartAuction plugin;
    private final PvpZoneManager pvp;
    private final AuctionManager auction;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean pvpStarted = new AtomicBoolean(false);

    public GameManager(HeartAuction plugin, PvpZoneManager pvp, AuctionManager auction) {
        this.plugin = plugin; this.pvp = pvp; this.auction = auction;
    }

    public void startGame() {
        if (!started.compareAndSet(false, true)) return;
        int peaceMin = plugin.getConfig().getInt("peace-minutes", 30);

        // 1) 운영자 제외 모든 유저 최대체력 2칸(4.0)으로
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.admins().isAdmin(p.getUniqueId())) continue;
            var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) attr.setBaseValue(4.0);
            p.setHealth(Math.min(4.0, p.getHealth()));
        }

        // 2) 평화시간: PVP OFF, 인벤세이브 ON
        for (World w : Bukkit.getWorlds()) {
            w.setPVP(false);
            w.setGameRuleValue("keepInventory", "true");
        }
        Bukkit.broadcastMessage("§a[안내] 지금부터 " + peaceMin + "분간 평화시간입니다! PVP 불가, 인벤세이브 ON");

        // 3) 평화시간 중 경매 스케줄 시작
        auction.beginPeaceAuctions(peaceMin * 60);

        // 4) 평화시간 종료 스케줄 -> PVP 시작/텔레포트/수축 시작
        Tasker.runLater(() -> {
            startPvpPhase();
        }, peaceMin * 20L * 60L);
    }

    public void startPvpImmediately() {
        if (!started.get()) {
            Bukkit.broadcastMessage("§c[오류] 게임이 시작되지 않았습니다!");
            return;
        }
        
        if (pvpStarted.get()) {
            Bukkit.broadcastMessage("§c[오류] PVP가 이미 시작되었습니다!");
            return;
        }
        
        // 경매 중단
        auction.stopRepeat();
        
        // 즉시 PVP 시작
        startPvpPhase();
    }

    private void startPvpPhase() {
        if (!pvpStarted.compareAndSet(false, true)) return;
        
        Bukkit.broadcastMessage("§c[알림] 평화시간 종료! 이제 PVP가 시작됩니다.");
        for (World w : Bukkit.getWorlds()) {
            w.setPVP(true);
            w.setGameRuleValue("keepInventory", "false");
        }
        // PVP 존으로 TP
        pvp.teleportAllIntoZone();
        // 흰색 콘크리트 벽으로 경기장 축소 시작
        pvp.startShrinking();
    }

    public void endGame() {
        if (!started.get()) {
            Bukkit.broadcastMessage("§c[오류] 게임이 시작되지 않았습니다!");
            return;
        }
        
        // 게임 상태 초기화
        started.set(false);
        pvpStarted.set(false);
        
        // 경매 중단
        auction.stopRepeat();
        
        // PVP 존 정리
        pvp.shutdown();
        
        // PVP OFF, 인벤세이브 OFF로 복원
        for (World w : Bukkit.getWorlds()) {
            w.setPVP(false);
            w.setGameRuleValue("keepInventory", "false");
        }
        
        // 모든 플레이어 체력 복원
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.admins().isAdmin(p.getUniqueId())) continue;
            var attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr != null) attr.setBaseValue(20.0);
            p.setHealth(20.0);
        }
        
        Bukkit.broadcastMessage("§a[알림] 게임이 종료되었습니다. 모든 설정이 초기화되었습니다.");
    }

    public boolean isGameStarted() {
        return started.get();
    }

    public boolean isPvpStarted() {
        return pvpStarted.get();
    }
}