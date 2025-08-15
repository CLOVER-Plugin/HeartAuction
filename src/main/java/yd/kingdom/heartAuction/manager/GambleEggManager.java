package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import yd.kingdom.heartAuction.HeartAuction;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GambleEggManager implements Listener {
    private final HeartAuction plugin;
    private final YamlConfiguration bet;

    public GambleEggManager(HeartAuction plugin) {
        this.plugin = plugin;
        this.bet = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "bet.yml"));
        // 이벤트 등록
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ===== 세션 관리 ===== */
    private static class Session {
        final UUID playerId;
        final BoundingBox box;  // 구덩이 영역
        final Chest chest;      // 배당 상자
        final int stake;        // 상자에서 꺼낸 다이아 합
        final int guess;        // 플레이어 입력 정답
        int eggsLeft = 10;      // 남은 달걀
        boolean finished = false;

        Session(UUID playerId, BoundingBox box, Chest chest, int stake, int guess) {
            this.playerId = playerId; this.box = box; this.chest = chest; this.stake = stake; this.guess = guess;
        }
    }
    private final Map<UUID, Session> sessions = new HashMap<>();

    public void run(Player p, int guess) {
        var c = plugin.getConfig();
        World w = Bukkit.getWorld(c.getString("egg-gamble.pit-origin.world"));
        int ox = c.getInt("egg-gamble.pit-origin.x");
        int oy = c.getInt("egg-gamble.pit-origin.y");
        int oz = c.getInt("egg-gamble.pit-origin.z");
        int sx = c.getInt("egg-gamble.size-x", 3);
        int sy = c.getInt("egg-gamble.size-y", 5);
        int sz = c.getInt("egg-gamble.size-z", 3);

        Location chestLoc = new Location(
                Bukkit.getWorld(c.getString("egg-gamble.chest.world")),
                c.getInt("egg-gamble.chest.x"),
                c.getInt("egg-gamble.chest.y"),
                c.getInt("egg-gamble.chest.z")
        );
        Chest chest = (Chest) chestLoc.getBlock().getState();

        // 상자 다이아 총합 회수
        int diamonds = chest.getInventory().all(Material.DIAMOND).values().stream().mapToInt(i -> i.getAmount()).sum();
        chest.getInventory().remove(Material.DIAMOND);
        if (diamonds <= 0) { p.sendMessage("§c상자에 다이아가 없습니다."); return; }

        // 기존 세션이 있으면 덮어쓰기(정리)
        Session old = sessions.remove(p.getUniqueId());
        if (old != null) old.finished = true;

        // 경계 박스(모서리 기준)
        BoundingBox box = BoundingBox.of(
                new Location(w, ox, oy, oz),
                new Location(w, ox + sx, oy + sy, oz + sz)
        );

        // 세션 생성
        Session s = new Session(p.getUniqueId(), box, chest, diamonds, guess);
        sessions.put(p.getUniqueId(), s);

        // 달걀 10개 지급
        p.getInventory().addItem(new ItemStack(Material.EGG, 10));
        p.sendMessage("§a달걀 10개를 모두 던지세요! (구덩이 영역 밖 투척은 취소됩니다)");
    }

    @EventHandler
    public void onEggHit(PlayerEggThrowEvent e) {
        Player p = e.getPlayer();
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.finished) return;

        // 충돌(깨진) 위치 기준으로 영역 체크
        if (!s.box.contains(e.getEgg().getLocation().toVector())) {
            // 구덩이 밖에서 깨졌다면: 시도 불인정 + 병아리 스폰 금지 + 달걀 환급
            e.setHatching(false);
            ItemStack refund = new ItemStack(Material.EGG, 1);
            Map<Integer, ItemStack> notFit = p.getInventory().addItem(refund);
            if (!notFit.isEmpty()) {
                // 인벤이 가득하면 바닥에 드랍
                p.getWorld().dropItemNaturally(p.getLocation(), refund);
            }
            p.sendMessage("§c구덩이 영역 밖에서 깨졌습니다! 다시 던지세요!.");
            return;
        }

        // 구덩이 안에서 정상적으로 깨짐 → 유효 시도 1회 차감
        if (s.eggsLeft > 0) s.eggsLeft--;

        // 10회 유효 시도 모두 끝나면 1초 후 집계 (병아리 스폰 반영 대기)
        if (s.eggsLeft <= 0 && !s.finished) {
            s.finished = true;
            Bukkit.getScheduler().runTaskLater(plugin, () -> finishSession(p, s), 20L);
        }
    }

    /* ===== 집계/정산 ===== */
    private void finishSession(Player p, Session s) {
        // 집계
        int babies = countBabies(s);
        if (babies != s.guess) {
            p.sendMessage("§c실패! 입력: " + s.guess + " / 실제: " + babies);
            clearEntities(s);
            sessions.remove(s.playerId);
            return;
        }

        // 성공: 배율 계산 후 상자에 0번 슬롯부터 지급
        double mult = bet.getDouble("chicken." + babies, bet.getDouble("chicken.default", 0.0));
        int payout = (int) Math.floor(s.stake * mult);
        depositDiamonds(s.chest, payout);
        p.sendMessage("§a정답! 새끼 닭: " + babies + "마리 §7/ 배율 " + mult + " / 지급 " + payout);

        // 보상 수령 완료(상자 내 다이아 소진) 시 엔티티 정리, 최대 60초 대기
        waitAndClearAfterLoot(s);
        sessions.remove(s.playerId);
    }

    private int countBabies(Session s) {
        World w = s.chest.getWorld();
        int c = 0;
        for (Entity e : w.getNearbyEntities(s.box)) {
            if (e.getType() == EntityType.CHICKEN) {
                Chicken ch = (Chicken) e;
                if (!ch.isAdult()) c++;
            }
        }
        return c;
    }

    private void clearEntities(Session s) {
        World w = s.chest.getWorld();
        for (Entity e : w.getNearbyEntities(s.box)) {
            if (e.getType() == EntityType.CHICKEN) e.remove();
        }
    }

    /* ===== 공통 헬퍼 ===== */

    private void depositDiamonds(Chest chest, int amount) {
        var inv = chest.getBlockInventory();

        // 1) 기존 다이아 스택 보충
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != Material.DIAMOND) continue;
            int space = it.getMaxStackSize() - it.getAmount();
            if (space <= 0) continue;
            int add = Math.min(space, amount);
            it.setAmount(it.getAmount() + add);
            amount -= add;
        }

        // 2) 빈 슬롯부터 새 스택 채우기
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) continue;
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            inv.setItem(i, new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }

        // 3) 남으면 바닥 드랍(안전장치)
        while (amount > 0) {
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            chest.getWorld().dropItem(chest.getLocation().add(0.5, 1.0, 0.5), new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }
    }

    private void waitAndClearAfterLoot(Session s) {
        new BukkitRunnable() {
            int seconds = 0;
            @Override public void run() {
                boolean hasDiamonds = false;
                for (ItemStack it : s.chest.getBlockInventory().getContents()) {
                    if (it != null && it.getType() == Material.DIAMOND && it.getAmount() > 0) {
                        hasDiamonds = true; break;
                    }
                }
                if (!hasDiamonds || seconds >= 60) { // 수령 완료 or 타임아웃
                    clearEntities(s);
                    cancel();
                }
                seconds++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}