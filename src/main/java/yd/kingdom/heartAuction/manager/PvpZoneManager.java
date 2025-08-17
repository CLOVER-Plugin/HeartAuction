package yd.kingdom.heartAuction.manager;

import org.bukkit.*;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Tasker;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class PvpZoneManager {
    private final HeartAuction plugin;
    private final Random rnd = new Random();

    private World world; private double cx, cz; private double ringY;
    private int radius;
    private int shrinkEvery;       // seconds
    private int damageCheckTicks;  // ticks

    private final AtomicInteger currentSafe = new AtomicInteger();
    private int shrinkTaskId = -1;
    private int damageTaskId = -1;

    public PvpZoneManager(HeartAuction plugin) { this.plugin = plugin; reload(); }

    public void reload() {
        var c = plugin.getConfig();
        world = Bukkit.getWorld(c.getString("pvp-zone.world", "world"));
        double cy = c.getDouble("pvp-zone.center-y", 0);
        cx = c.getDouble("pvp-zone.center-x");
        cz = c.getDouble("pvp-zone.center-z");
        radius = c.getInt("pvp-zone.radius", 50);
        shrinkEvery = c.getInt("pvp-zone.shrink-every-seconds", 60);
        damageCheckTicks = c.getInt("pvp-zone.damage-check-ticks", 10);
        // 파티클 Y: 지정값이 있으면 사용, 없으면 중심 최고지대 + 2
        ringY = cy > 0 ? cy : world.getHighestBlockYAt((int) Math.floor(cx), (int) Math.floor(cz)) + 2;
        currentSafe.set(radius);
    }

    public void teleportAllIntoZone() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.admins().isAdmin(p.getUniqueId())) continue;
            p.teleport(randomInside());
            p.sendTitle("§cPVP 시작!", "§f존 밖은 지속 피해", 10, 60, 10);
        }
    }

    private Location randomInside() {
        double r = radius * Math.sqrt(rnd.nextDouble());
        double t = rnd.nextDouble() * Math.PI * 2;
        double x = cx + r * Math.cos(t);
        double z = cz + r * Math.sin(t);
        int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        return new Location(world, x + 0.5, y, z + 0.5);
    }

    public boolean isOutside(Location loc) {
        if (loc.getWorld() == null || !loc.getWorld().equals(world)) return true;
        double dx = loc.getX() - cx, dz = loc.getZ() - cz;
        return (dx * dx + dz * dz) > (currentSafe.get() * currentSafe.get());
    }

    public void startShrinking() {
        stopTasks();
        currentSafe.set(radius);

        // 반경 수축(1분마다 1블록) + 굵은 링 표시
        shrinkTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.updateAndGet(v -> Math.max(0, v - 1));
            drawRing(safe, true);
            Bukkit.broadcastMessage("§7[존] 안전 반경: §e" + safe + "§7블럭");
            if (safe <= 0) stopTasks();
        }, 0L, shrinkEvery * 20L);

        // 존 밖 피해 & 얇은 링 상시 표시 (10틱당 1.0데미지 = 반칸)
        damageTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.get();
            double safeSq = safe * safe;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(world)) continue;
                double dx = p.getLocation().getX() - cx;
                double dz = p.getLocation().getZ() - cz;
                if ((dx * dx + dz * dz) > safeSq) {
                    p.damage(1.0);
                    p.spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 3, .2, .2, .2, 0);
                }
            }
            drawRing(safe, false);
        }, 0L, damageCheckTicks);
    }

    /** WHITE REDSTONE(=dust)로 선명한 링 표시 */
    private void drawRing(double radius, boolean thickBurst) {
        if (radius <= 0) return;
        int points = (int) Math.max(60, radius * 8);
        Particle.DustOptions white = new Particle.DustOptions(Color.fromRGB(255,255,255), thickBurst ? 1.5f : 1.0f);
        for (int i = 0; i < points; i++) {
            double t = 2 * Math.PI * i / points;
            double x = cx + radius * Math.cos(t);
            double z = cz + radius * Math.sin(t);
            world.spawnParticle(Particle.DUST, x, ringY, z, thickBurst ? 2 : 1, 0, 0, 0, 0, white);
        }
    }

    public void shutdown() { stopTasks(); }

    private void stopTasks() {
        Tasker.cancel(shrinkTaskId); shrinkTaskId = -1;
        Tasker.cancel(damageTaskId); damageTaskId = -1;
    }
}