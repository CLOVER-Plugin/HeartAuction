package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Items;
import yd.kingdom.heartAuction.util.Tasker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionManager {
    private final HeartAuction plugin;
    private final PvpZoneManager pvp;
    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private int repeatTask = -1;

    private volatile AuctionRound current;

    public AuctionManager(HeartAuction plugin, PvpZoneManager pvp) {
        this.plugin = plugin; this.pvp = pvp;
    }

    public void beginPeaceAuctions(int peaceSeconds) {
        if (running.get()) return;
        running.set(true);
        int gap = plugin.getConfig().getInt("auction-interval-seconds", 180);
        int pre = plugin.getConfig().getInt("auction-pre-title-seconds", 30);

        long endAt = System.currentTimeMillis() + peaceSeconds * 1000L;
        repeatTask = Tasker.runTimer(() -> {
            if (System.currentTimeMillis() >= endAt) { stopRepeat(); return; }
            scheduleOneAuction(pre);
        }, 0L, gap * 20L);
    }

    public void stopRepeat() { Tasker.cancel(repeatTask); repeatTask = -1; running.set(false); }

    public void shutdown() { stopRepeat(); }

    public void join(Player p) {
        participants.add(p.getUniqueId());
        p.sendMessage("§a[경매] 참여 대기 등록 완료! 시작 알림이 오면 채팅으로 한 번만 숫자를 입력하세요.");
    }

    public boolean isExpectingBid(UUID id) {
        AuctionRound r = current; return r != null && r.awaiting.contains(id);
    }

    public boolean submitBid(Player p, String raw) {
        AuctionRound r = current; if (r == null) return false;
        if (!r.awaiting.contains(p.getUniqueId())) return false;

        int n;
        try { n = Integer.parseInt(raw.trim()); } catch (Exception e) { p.sendMessage("§c숫자만 입력하세요!"); return true; }
        if (r.bids.containsKey(p.getUniqueId())) { p.sendMessage("§c이미 입찰했습니다!"); return true; }
        if (!Items.hasItem(p, Material.DIAMOND, n)) { p.sendMessage("§c인벤토리에 다이아가 부족합니다!"); return true; }

        // 즉시 차감
        Items.takeItem(p, Material.DIAMOND, n);
        r.bids.put(p.getUniqueId(), n);
        Bukkit.broadcastMessage("§e[경매] §f"+p.getName()+"§7 님이 입찰을 완료했습니다.");

        // 모두 입찰 완료 시 조기 종료
        if (r.bids.size() >= r.awaiting.size()) r.endNow();
        return true;
    }

    private void scheduleOneAuction(int preSeconds) {
        if (current != null) return;
        int amount = 1 + new Random().nextInt(5);
        current = new AuctionRound(amount);
        var r = current;

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!participants.contains(p.getUniqueId())) return;
            p.sendTitle("§e경매 예고", "§f"+preSeconds+"초 후 경매가 시작됩니다.", 10, 40, 10);
        });

        Tasker.runLater(r::start, preSeconds * 20L);
    }

    private class AuctionRound {
        final int amount;
        final Set<UUID> awaiting = new HashSet<>();
        final Map<UUID, Integer> bids = new HashMap<>();
        boolean tieReauction = false;
        int taskId = -1;
        List<UUID> prevTops = Collections.emptyList();

        AuctionRound(int amount) { this.amount = amount; }

        void start() {
            for (UUID id : participants) awaiting.add(id);
            if (awaiting.isEmpty()) { current = null; return; }

            Bukkit.broadcastMessage("§6[경매] 경매가 시작되었습니다! 이번 물품: §c체력증가 §ex"+amount);
            Bukkit.broadcastMessage("§6[경매] §7채팅에 입찰 다이아 수를 적으세요. (1회만)");

            // 3초 카운트다운 (자동 취소)
            new org.bukkit.scheduler.BukkitRunnable(){
                int left = 3;
                @Override public void run(){
                    if (left <= 0) { cancel(); return; }
                    Bukkit.broadcastMessage("§7[경매] "+left+"초 후 시작... /경매로 참여");
                    left--;
                }
            }.runTaskTimer(plugin, 0L, 20L);

            taskId = Tasker.runLater(this::finishPhase, 30 * 20L);
        }

        void endNow() { Tasker.cancel(taskId); finishPhase(); }

        void finishPhase() {
            List<Map.Entry<UUID,Integer>> list = new ArrayList<>(bids.entrySet());
            if (list.isEmpty()) {
                if (tieReauction && !prevTops.isEmpty()) {
                    UUID winPrev = prevTops.get(0);
                    Player pp = Bukkit.getPlayer(winPrev);
                    if (pp != null) {
                        pp.getInventory().addItem(Items.healthBoostItem(amount));
                        pp.sendMessage("§a[경매] 재경매 무응답으로 이전 최고 입찰자에게 지급되었습니다.");
                    }
                    Bukkit.broadcastMessage("§6[경매] 재경매 무응답 → 이전 최고가 낙찰 처리.");
                    current = null;
                    return;
                }
                current = null;
                return;
            }
            list.sort((a,b)-> Integer.compare(b.getValue(), a.getValue()));

            int top = list.get(0).getValue();
            List<UUID> tops = new ArrayList<>();
            for (var e : list) if (e.getValue() == top) tops.add(e.getKey());

            if (tops.size() >= 2) { // 재경매(동점자만)
                tieReauction = true; awaiting.clear(); awaiting.addAll(tops);
                bids.clear();
                prevTops = new ArrayList<>(tops);
                Bukkit.broadcastMessage("§6[경매] 재경매! 10초 내에 이전 가격 이상으로 입찰하세요.");
                taskId = Tasker.runLater(this::finishPhase, 10 * 20L);
                return;
            }

            // 단일 낙찰자
            UUID win = list.get(0).getKey();
            Player p = Bukkit.getPlayer(win);
            if (p != null) {
                p.getInventory().addItem(Items.healthBoostItem(amount));
                p.sendMessage("§a[경매] 낙찰! 체력증가 아이템 x"+amount+" 지급.");
            }
            Bukkit.broadcastMessage("§6[경매] 낙찰자: §e"+(p!=null?p.getName():win)+" §7(입찰가 비공개)");
            current = null;
        }
    }
}