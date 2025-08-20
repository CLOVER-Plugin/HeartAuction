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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
    private int clearTaskId = -1;

    // 배치 제거/관리용: 생성한 벽 블록 좌표 추적 (x,y,z 키)
    private final Set<String> wallBlocks = ConcurrentHashMap.newKeySet();

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
            Location loc = randomInside();
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

        // 시작 시 반경이 50이면 즉시 1회 안내
        if (radius == 50) {
            Bukkit.broadcastMessage("§7[존] 안전 반경: §e50§7블럭");
        }

        shrinkTaskId = Tasker.runTimer(() -> {
            int safe = currentSafe.updateAndGet(v -> Math.max(finalRadius, v - 1));
            
            // 현재 테두리에 흰색 콘크리트 원기둥 생성
            createConcreteWall(safe);
            
            // 안전 반경 안내 메시지: 25/5 블럭일 때만 출력
            if (safe == 25 || safe == 5) {
                Bukkit.broadcastMessage("§7[존] 안전 반경: §e" + safe + "§7블럭");
            }
            
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

    private static String keyOf(int x, int y, int z) { return x + "," + y + "," + z; }

    private void createConcreteWall(int radius) {
        // 반지름에 따라 점의 개수를 매우 조밀하게 조정 (최대 0.25블럭 간격)
        int points = Math.max(480, (int)(8 * Math.PI * radius));
        final int height = 40; // 원기둥 높이
        
        // 기본 원형 점들 생성
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI) * i / points;
            double x = cx + radius * Math.cos(angle);
            double z = cz + radius * Math.sin(angle);
            
            // 높이 40칸의 원기둥 생성
            for (int y = 0; y < height; y++) {
                int blockY = (int) ringY + y;
                int bx = (int) Math.floor(x);
                int bz = (int) Math.floor(z);
                Block block = world.getBlockAt(bx, blockY, bz);
                if (block.getType() == Material.AIR) {
                    block.setType(Material.WHITE_CONCRETE, false);
                    wallBlocks.add(keyOf(bx, blockY, bz));
                }
            }
        }
        
        // 점 사이 공간을 완벽하게 메우는 고급 보간 시스템
        fillGapsAdvanced(radius, points);
        
        // 테두리 영역 보강 (내부는 비우기)
        fillRadiusEdge(radius);
        
        // 내부 공간을 정리 (이미 존재하는 원기둥 내부는 제거)
        int finalRadius = plugin.getConfig().getInt("pvp-zone.final-radius", 5);
        if (radius > finalRadius) { // 최소 반지름보다 클 때만
            clearInnerSpace(radius - 1);
        }
    }
    
    private void fillGapsAdvanced(int radius, int points) {
        // 고급 보간 시스템: 선형 + 원형 보간
        final int height = 40;
        
        for (int i = 0; i < points; i++) {
            double angle1 = (2 * Math.PI) * i / points;
            double angle2 = (2 * Math.PI) * ((i + 1) % points) / points;
            
            // 두 점 사이의 중간점들 계산 (더 조밀하게)
            int interpolations = Math.max(3, (int)(radius * 0.2)); // 반지름의 20%만큼 보간
            
            for (int j = 1; j < interpolations; j++) {
                double t = (double) j / interpolations;
                double angle = angle1 + (angle2 - angle1) * t;
                
                double x = cx + radius * Math.cos(angle);
                double z = cz + radius * Math.sin(angle);
                
                // 높이 40칸의 보간 블럭 생성
                for (int y = 0; y < height; y++) {
                    int blockY = (int) ringY + y;
                    int bx = (int) Math.floor(x);
                    int bz = (int) Math.floor(z);
                    Block block = world.getBlockAt(bx, blockY, bz);
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.WHITE_CONCRETE, false);
                        wallBlocks.add(keyOf(bx, blockY, bz));
                    }
                }
            }
        }
    }
    
    private void fillRadiusEdge(int radius) {
        // 반지름 테두리만 콘크리트로 생성 (내부는 채우지 않음)
        final int height = 40;
        final double wallThickness = 1.0; // 벽 두께 1블럭
        
        int scanRange = (int) Math.ceil(radius + wallThickness) + 1;
        
        for (int dx = -scanRange; dx <= scanRange; dx++) {
            for (int dz = -scanRange; dz <= scanRange; dz++) {
                double x = cx + dx;
                double z = cz + dz;
                
                double distance = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                if (distance >= radius - 0.5 && distance <= radius + 0.5) {
                    for (int y = 0; y < height; y++) {
                        int blockY = (int) ringY + y;
                        int bx = (int) Math.floor(x);
                        int bz = (int) Math.floor(z);
                        Block block = world.getBlockAt(bx, blockY, bz);
                        if (block.getType() == Material.AIR) {
                            block.setType(Material.WHITE_CONCRETE, false);
                            wallBlocks.add(keyOf(bx, blockY, bz));
                        }
                    }
                }
            }
        }
    }
    
    private void clearInnerSpace(int innerRadius) {
        // 내부 공간을 정리하여 플레이어가 이동할 수 있도록 함 (콘크리트 제거)
        int points = Math.max(120, (int)(2 * Math.PI * innerRadius));
        final int height = 40;
        
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI) * i / points;
            double x = cx + innerRadius * Math.cos(angle);
            double z = cz + innerRadius * Math.sin(angle);
            
            for (int y = 0; y < height; y++) {
                int blockY = (int) ringY + y;
                int bx = (int) Math.floor(x);
                int bz = (int) Math.floor(z);
                Block block = world.getBlockAt(bx, blockY, bz);
                if (block.getType() == Material.WHITE_CONCRETE) {
                    block.setType(Material.AIR, false);
                    wallBlocks.remove(keyOf(bx, blockY, bz));
                }
            }
        }
    }

    private void stopTasks() {
        if (shrinkTaskId != -1) { Tasker.cancel(shrinkTaskId); shrinkTaskId = -1; }
        if (wallTaskId != -1) { Tasker.cancel(wallTaskId); wallTaskId = -1; }
        if (clearTaskId != -1) { Tasker.cancel(clearTaskId); clearTaskId = -1; }
    }

    public void shutdown() {
        stopTasks();
        clearAllWalls();
    }

    public void clearAllWalls() {
        // 배치 제거: 매 틱마다 일정량의 블록만 제거하여 서버 렉/타임아웃 방지
        if (clearTaskId != -1) return; // 이미 진행 중
        final int blocksPerTick = 2000; // 상황에 맞게 조정 가능

        clearTaskId = Tasker.runTimer(() -> {
            int removed = 0;
            var it = wallBlocks.iterator();
            while (it.hasNext() && removed < blocksPerTick) {
                String key = it.next();
                String[] s = key.split(",", 3);
                int bx = Integer.parseInt(s[0]);
                int by = Integer.parseInt(s[1]);
                int bz = Integer.parseInt(s[2]);
                Block block = world.getBlockAt(bx, by, bz);
                if (block.getType() == Material.WHITE_CONCRETE) {
                    block.setType(Material.AIR, false);
                }
                it.remove();
                removed++;
            }
            if (wallBlocks.isEmpty()) {
                Tasker.cancel(clearTaskId);
                clearTaskId = -1;
                Bukkit.broadcastMessage("§7[존] 모든 PVP 존 벽이 제거되었습니다.");
            }
        }, 0L, 1L);
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