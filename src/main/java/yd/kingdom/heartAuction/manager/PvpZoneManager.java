package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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
    private int wallTaskId = -1;

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
            if (plugin.admins().isAdmin(p.getUniqueId())) continue; // 운영자 제외
            Location loc = pickRandomInside();
            p.teleport(loc);
            p.sendTitle("§cPVP 시작!", "§f경기장이 점점 축소됩니다!", 10, 60, 10);
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


        // 설정에서 최종 반지름과 축소 시간을 읽어옴
        int finalRadius = plugin.getConfig().getInt("pvp-zone.final-radius", 5);
        int totalShrinkTime = plugin.getConfig().getInt("pvp-zone.shrink-duration", 600); // 10분
        int totalShrinkBlocks = radius - finalRadius; // 초기 반지름 - 최종 반지름
        int shrinkInterval = totalShrinkTime / totalShrinkBlocks; // 블럭당 축소 간격

        shrinkTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.updateAndGet(v -> Math.max(finalRadius, v - 1));
            
            // 현재 테두리에 흰색 콘크리트 원기둥 생성
            createConcreteWall(safe);
            
            // 안전 반경 안내 메시지
            Bukkit.broadcastMessage("§7[존] 안전 반경: §e" + safe + "§7블럭");
            
            if (safe <= finalRadius) {
                stopTasks();
                Bukkit.broadcastMessage("§c[존] 최종 경기장이 완성되었습니다!");
            }
        }, 0L, shrinkInterval * 20L);


        // 벽 생성 작업을 별도로 실행 (블럭 변경이 많으므로)
        wallTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.get();
            if (safe > finalRadius) {
                createConcreteWall(safe);
            }
        }, 0L, 20L); // 1초마다 벽 생성
    }

    private void createConcreteWall(int radius) {
        // 반지름에 따라 점의 개수를 더 조밀하게 조정
        // 목표: 점 사이 거리가 최대 0.5블럭을 넘지 않도록
        int points = Math.max(240, (int)(4 * Math.PI * radius));
        final int height = 40; // 원기둥 높이
        
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI) * i / points;
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);
            
            // 높이 40칸의 원기둥 생성
            for (int y = 0; y < height; y++) {
                int blockY = (int) cy + y;
                Location loc = new Location(world, x, blockY, z);
                
                // 기존 블럭이 공기인 경우에만 콘크리트로 변경
                Block block = loc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.WHITE_CONCRETE);
                }
            }
        }
        
        // 점 사이 공간을 메우는 보간 블럭 추가
        fillGapsBetweenPoints(radius, points);
        
        // 내부 공간을 정리 (이미 존재하는 원기둥 내부는 제거)
        if (radius > 5) { // 최소 반지름보다 클 때만
            clearInnerSpace(radius - 1);
        }
    }
    
    private void fillGapsBetweenPoints(int radius, int points) {
        // 점 사이 공간을 메우는 보간 블럭 배치
        final int height = 40;
        
        for (int i = 0; i < points; i++) {
            double angle1 = (2 * Math.PI) * i / points;
            double angle2 = (2 * Math.PI) * ((i + 1) % points) / points;
            
            // 두 점 사이의 중간점들 계산
            int interpolations = Math.max(1, (int)(radius * 0.1)); // 반지름의 10%만큼 보간
            
            for (int j = 1; j < interpolations; j++) {
                double t = (double) j / interpolations;
                double angle = angle1 + (angle2 - angle1) * t;
                
                double x = cx + radius * Math.cos(angle);
                double z = cz + radius * Math.sin(angle);
                
                // 높이 40칸의 보간 블럭 생성
                for (int y = 0; y < height; y++) {
                    int blockY = (int) cy + y;
                    Location loc = new Location(world, x, blockY, z);
                    
                    Block block = loc.getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.WHITE_CONCRETE);
                    }
                }
            }
        }
    }
    
    private void clearInnerSpace(int innerRadius) {
        // 내부 공간을 정리하여 플레이어가 이동할 수 있도록 함
        // 반지름에 따라 점의 개수를 동적으로 조정
        int points = Math.max(120, (int)(2 * Math.PI * innerRadius));
        final int height = 40;
        
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI) * i / points;
            double x = cx + innerRadius * Math.cos(angle);
            double z = cz + innerRadius * Math.sin(angle);
            
            for (int y = 0; y < height; y++) {
                int blockY = (int) cy + y;
                Location loc = new Location(world, x, blockY, z);
                
                Block block = loc.getBlock();
                if (block.getType() == Material.WHITE_CONCRETE) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private void stopTasks() {
        if (shrinkTaskId != -1) {
            Tasker.cancel(shrinkTaskId);
            shrinkTaskId = -1;
        }
        if (wallTaskId != -1) {
            Tasker.cancel(wallTaskId);
            wallTaskId = -1;
        }
    }

    public void shutdown() {
        stopTasks();
        clearAllWalls();
    }

    public void clearAllWalls() {
        // 모든 흰색 콘크리트 벽 제거 (반지름 5부터 50까지)
        for (int r = 5; r <= radius; r++) {
            clearWallAtRadius(r);
        }
        Bukkit.broadcastMessage("§7[존] 모든 PVP 존 벽이 제거되었습니다.");
    }

    private void clearWallAtRadius(int radius) {
        // 특정 반지름의 벽을 모두 제거
        int points = Math.max(60, (int)(2 * Math.PI * radius));
        final int height = 40;
        
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI) * i / points;
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);
            
            for (int y = 0; y < height; y++) {
                int blockY = (int) cy + y;
                Location loc = new Location(world, x, blockY, z);
                
                Block block = loc.getBlock();
                if (block.getType() == Material.WHITE_CONCRETE) {
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public boolean isInsideSafeZone(Location loc) {
        if (!loc.getWorld().equals(world)) return false;
        double dx = loc.getX() - cx;
        double dz = loc.getZ() - cz;
        return (dx*dx + dz*dz) <= currentSafe.get() * currentSafe.get();
    }

    public int getCurrentRadius() {
        return currentSafe.get();
    }
}