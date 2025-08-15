package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Tasker;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class PvpZoneManager {
    private final HeartAuction plugin;
    private final Random rnd = new Random();

    private World world; private double cx, cy, cz; private int radius;
    private int shrinkEvery; // sec
    private int damageCheckTicks; // ticks

    private final AtomicInteger currentSafe = new AtomicInteger();
    private int shrinkTaskId = -1;
    private int damageTaskId = -1;

    public PvpZoneManager(HeartAuction plugin) {
        this.plugin = plugin; reload();
    }

    public void reload() {
        var c = plugin.getConfig();
        world = Bukkit.getWorld(c.getString("pvp-zone.world", "world"));
        cx = c.getDouble("pvp-zone.center-x");
        cy = c.getDouble("pvp-zone.center-y");
        cz = c.getDouble("pvp-zone.center-z");
        radius = c.getInt("pvp-zone.radius", 50);
        shrinkEvery = c.getInt("pvp-zone.shrink-every-seconds", 60);
        damageCheckTicks = c.getInt("pvp-zone.damage-check-ticks", 10);
        currentSafe.set(radius);
    }

    public void teleportAllIntoZone() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.admins().isAdmin(p.getUniqueId())) continue; // 운영자 제외
            Location loc = pickRandomInside();
            p.teleport(loc);
            p.sendTitle("§cPVP 시작!", "§f존 밖으로 나가면 피해를 입습니다", 10, 60, 10);
        }
    }

    private Location pickRandomInside() {
        double r = radius * Math.sqrt(rnd.nextDouble());
        double t = rnd.nextDouble() * Math.PI * 2;
        double x = cx + r * Math.cos(t);
        double z = cz + r * Math.sin(t);
        int y = world.getHighestBlockYAt((int)Math.floor(x), (int)Math.floor(z)) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public void startShrinking() {
        stopTasks();
        currentSafe.set(radius);

        // 외곽 파티클/축소 타이머 (분당 1블럭)
        shrinkTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.updateAndGet(v -> Math.max(0, v - 1));
            // 테두리 파티클을 몇 초간 강하게 보여주기
            drawRing(world, cx, cy, cz, safe + 0.5, 100); // 100 틱 = 5초 정도
            Bukkit.broadcastMessage("§7[존] 안전 반경: §e" + safe + "§7블럭");
            if (safe <= 0) stopTasks();
        }, 0L, shrinkEvery * 20L);

        // 존 밖 피해(10틱당 반칸)
        damageTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.get();
            double safeSq = safe * safe;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(world)) continue;
                double dx = p.getLocation().getX() - cx;
                double dz = p.getLocation().getZ() - cz;
                if ((dx*dx + dz*dz) > safeSq) {
                    p.damage(1.0); // 반칸
                    p.spawnParticle(Particle.CLOUD, p.getLocation().add(0,1,0), 3, 0.2,0.2,0.2, 0);
                }
            }
            // 현재 테두리 파티클 얇게 계속 표시
            drawRing(world, cx, cy, cz, safe + 0.5, 10);
        }, 0L, damageCheckTicks);
    }

    private void drawRing(World w, double x, double y, double z, double r, int ticks) {
        final int points = 120;
        for (int i=0;i<points;i++) {
            double t = (2*Math.PI) * i / points;
            double px = x + r * Math.cos(t);
            double pz = z + r * Math.sin(t);
            Location loc = new Location(w, px, y, pz);
            w.spawnParticle(Particle.CLOUD, loc, 2, 0,0,0, 0);
        }
    }

    public void shutdown() { stopTasks(); }

    private void stopTasks() {
        Tasker.cancel(shrinkTaskId); shrinkTaskId = -1;
        Tasker.cancel(damageTaskId); damageTaskId = -1;
    }
}