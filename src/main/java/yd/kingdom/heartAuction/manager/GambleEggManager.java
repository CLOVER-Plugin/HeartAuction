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
import java.util.*;

public class GambleEggManager implements Listener {
    private final HeartAuction plugin;
    private final YamlConfiguration bet;

    public GambleEggManager(HeartAuction plugin) {
        this.plugin = plugin;
        this.bet = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "bet.yml"));
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* ===== 세션 ===== */
    private static class Session {
        final UUID playerId;
        final BoundingBox box;
        final Chest chest;
        int eggsLeft = 10;
        boolean finished = false;
        Session(UUID playerId, BoundingBox box, Chest chest) {
            this.playerId = playerId; this.box = box; this.chest = chest;
        }
    }
    private final Map<UUID, Session> sessions = new HashMap<>();

    /** 인자 없이 실행: 상자 다이아(배팅액) × chicken.<병아리수> 배율 지급 */
    public void run(Player p) {
        var c = plugin.getConfig();

        // 구덩이 월드
        String wName = c.getString("egg-gamble.pit-origin.world");
        World w = Bukkit.getWorld(wName);
        if (w == null) { p.sendMessage("§cegg-gamble.pit-origin.world 월드를 찾을 수 없습니다: " + wName); return; }

        int ox = c.getInt("egg-gamble.pit-origin.x");
        int oy = c.getInt("egg-gamble.pit-origin.y");
        int oz = c.getInt("egg-gamble.pit-origin.z");
        int sx = c.getInt("egg-gamble.size-x", 3);
        int sy = c.getInt("egg-gamble.size-y", 5);
        int sz = c.getInt("egg-gamble.size-z", 3);

        // 상자 월드
        String cwName = c.getString("egg-gamble.chest.world");
        World cw = Bukkit.getWorld(cwName);
        if (cw == null) { p.sendMessage("§cegg-gamble.chest.world 월드를 찾을 수 없습니다: " + cwName); return; }

        Location chestLoc = new Location(cw,
                c.getInt("egg-gamble.chest.x"),
                c.getInt("egg-gamble.chest.y"),
                c.getInt("egg-gamble.chest.z"));

        var state = chestLoc.getBlock().getState();
        if (!(state instanceof Chest chest)) {
            p.sendMessage("§c설정된 위치에 §e상자§c가 없습니다. config.yml의 egg-gamble.chest를 확인하세요.");
            return;
        }

        // 배팅액 확인
        int stakeNow = chest.getBlockInventory().all(Material.DIAMOND).values()
                .stream().mapToInt(i -> i.getAmount()).sum();
        if (stakeNow <= 0) { p.sendMessage("§c상자에 배팅할 다이아를 먼저 넣어주세요."); return; }

        // 기존 세션 종료
        Session old = sessions.remove(p.getUniqueId());
        if (old != null) old.finished = true;

        BoundingBox box = BoundingBox.of(new Location(w, ox, oy, oz), new Location(w, ox + sx, oy + sy, oz + sz));
        Session s = new Session(p.getUniqueId(), box, chest);
        sessions.put(p.getUniqueId(), s);

        // 달걀 지급
        p.getInventory().addItem(new ItemStack(Material.EGG, 10));
        p.sendMessage("§a달걀 10개를 던지세요! (달걀이 §e구덩이 안에서 깨져야§f 유효 시도입니다)");
    }

    /** '깨지는 위치'로 유효 시도 판단 */
    @EventHandler
    public void onEggBreak(PlayerEggThrowEvent e) {
        Player p = e.getPlayer();
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.finished) return;

        if (!s.box.contains(e.getEgg().getLocation().toVector())) {
            e.setHatching(false); // 바깥에서 깨진 시도는 무효
            ItemStack refund = new ItemStack(Material.EGG, 1);
            Map<Integer, ItemStack> nofit = p.getInventory().addItem(refund);
            if (!nofit.isEmpty()) p.getWorld().dropItemNaturally(p.getLocation(), refund);
            p.sendMessage("§c구덩이 밖에서 깨졌습니다! 달걀 1개 환급, 다시 시도하세요.");
            return;
        }

        if (s.eggsLeft > 0) s.eggsLeft--;
        if (s.eggsLeft <= 0 && !s.finished) {
            s.finished = true;
            Bukkit.getScheduler().runTaskLater(plugin, () -> finishSession(p, s), 20L); // 1초 대기 후 집계
        }
    }

    /* ===== 집계/정산 ===== */
    private void finishSession(Player p, Session s) {
        int babies = countBabies(s.box, s.chest.getWorld());

        // 현재 상자 다이아 총합(배팅액) → 배율 계산 → 배팅액 제거 → 배당 지급
        int stake = s.chest.getBlockInventory().all(Material.DIAMOND).values()
                .stream().mapToInt(i -> i.getAmount()).sum();
        double mult = bet.getDouble("chicken." + babies, bet.getDouble("chicken.default", 0.0));
        int payout = (int) Math.floor(stake * mult);

        s.chest.getBlockInventory().remove(Material.DIAMOND);
        depositDiamonds(s.chest, payout);

        p.sendMessage("§a달걀도박 결과: 새끼 닭 §e" + babies + "마리§a / 배율 §e" + mult + "§a / 지급 §b" + payout + "개");

        // 결과 출력 직후, 구덩이 내부 엔티티 즉시 제거
        clearChickens(s.box, s.chest.getWorld());

        // 세션 종료
        sessions.remove(s.playerId);
    }

    private int countBabies(BoundingBox box, World w) {
        int c = 0;
        for (Entity e : w.getNearbyEntities(box))
            if (e.getType() == EntityType.CHICKEN && !((Chicken) e).isAdult()) c++;
        return c;
    }

    private void clearChickens(BoundingBox box, World w) {
        for (Entity e : w.getNearbyEntities(box))
            if (e.getType() == EntityType.CHICKEN) e.remove();
    }

    private void depositDiamonds(Chest chest, int amount) {
        var inv = chest.getBlockInventory();
        // 기존 스택 보충
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != Material.DIAMOND) continue;
            int space = it.getMaxStackSize() - it.getAmount();
            if (space <= 0) continue;
            int add = Math.min(space, amount);
            it.setAmount(it.getAmount() + add);
            amount -= add;
        }
        // 빈 슬롯부터 채우기
        for (int i = 0; i < inv.getSize() && amount > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() != Material.AIR) continue;
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            inv.setItem(i, new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }
        // 남으면 드랍
        while (amount > 0) {
            int add = Math.min(amount, Material.DIAMOND.getMaxStackSize());
            chest.getWorld().dropItem(chest.getLocation().add(0.5, 1.0, 0.5), new ItemStack(Material.DIAMOND, add));
            amount -= add;
        }
    }

    private void waitAndClearAfterLoot(Chest chest, BoundingBox box, World w) {
        new BukkitRunnable() {
            int seconds = 0;
            @Override public void run() {
                boolean hasDia = Arrays.stream(chest.getBlockInventory().getContents())
                        .anyMatch(it -> it != null && it.getType() == Material.DIAMOND && it.getAmount() > 0);
                if (!hasDia || seconds >= 60) { clearChickens(box, w); cancel(); }
                seconds++;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
}