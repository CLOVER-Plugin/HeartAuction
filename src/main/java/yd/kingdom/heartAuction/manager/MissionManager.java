package yd.kingdom.heartAuction.manager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import yd.kingdom.heartAuction.HeartAuction;
import yd.kingdom.heartAuction.util.Items;
import yd.kingdom.heartAuction.util.Texts;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class MissionManager {
    private final HeartAuction plugin;

    private final Map<UUID, String> currentKey = new ConcurrentHashMap<>();
    private final Map<String, MissionSpec> pool = new HashMap<>();
    private final Map<String, Integer> rewards = new HashMap<>();

    public MissionManager(HeartAuction plugin) {
        this.plugin = plugin;
        // 1) 양털 5개 가져오기 (아무 색) - 10개
        pool.put("HAS_WOOL_ANY_5", new SumOfMaterialsMission("양털 5개 가져오기", matsEndingWith("_WOOL"), 5));

        // 2) 건초더미 하나 만들어오기 - 20개
        pool.put("CRAFT_HAY_BLOCK_1", new SimpleItemMission("건초더미 1개 만들어오기", Material.HAY_BLOCK, 1));

        // 3) 썩은고기 5개 가져오기 - 15개
        pool.put("HAS_ROTTEN_FLESH_5", new SimpleItemMission("썩은고기 5개 가져오기", Material.ROTTEN_FLESH, 5));

        // 4) 침대 2개 만들기(색 무관) - 15개
        pool.put("CRAFT_BED_ANY_2", new SumOfMaterialsMission("침대 2개 만들기", matsEndingWith("_BED"), 2));

        // 5) 씨앗 15개 가져오기 - 10개
        pool.put("HAS_SEEDS_ANY_15", new SumOfMaterialsMission(
                "씨앗 15개 가져오기",
                new HashSet<>(Arrays.asList(
                        Material.WHEAT_SEEDS, Material.MELON_SEEDS, Material.PUMPKIN_SEEDS,
                        Material.BEETROOT_SEEDS, Material.TORCHFLOWER_SEEDS
                )),
                15
        ));

        // 6) 색깔 염료 5개(서로 다른 색) - 30개
        pool.put("HAS_5_DISTINCT_DYES", new UniquePredicateMission(
                "색깔 염료 5개(서로 다른 색)",
                m -> m.name().endsWith("_DYE"),
                5
        ));

        // 7) 그림 1개 만들어오기 - 10개
        pool.put("CRAFT_PAINTING_1", new SimpleItemMission("그림 1개 만들어오기", Material.PAINTING, 1));

        // 9) 먹을 것 5개(서로 다른 종류, 몬스터템 X) - 25개
        pool.put("HAS_5_DISTINCT_FOOD_NO_MONSTER", new UniquePredicateMission(
                "먹을 것 5개(서로 다른 종류, 몬스터템 X)",
                m -> m.isEdible() && m != Material.ROTTEN_FLESH && m != Material.SPIDER_EYE,
                5
        ));

        // 10) 몬스터 관련 아이템 3개(중복 X) - 20개
        pool.put("HAS_3_DISTINCT_MONSTER_DROPS", new UniqueInSetMission(
                "몬스터 관련 아이템 3개(중복 X)",
                new HashSet<>(Arrays.asList(
                        Material.ROTTEN_FLESH, Material.BONE, Material.STRING, Material.GUNPOWDER,
                        Material.SPIDER_EYE, Material.SLIME_BALL, Material.ENDER_PEARL,
                        Material.PHANTOM_MEMBRANE, Material.BLAZE_ROD, Material.GHAST_TEAR
                )),
                3
        ));

        // 11) 황금사과 1개 만들어오기 - 30개
        pool.put("CRAFT_GOLDEN_APPLE_1", new SimpleItemMission("황금사과 1개 만들어오기", Material.GOLDEN_APPLE, 1));

        // 12) 참나무 묘목 30개 가져오기 - 20개
        pool.put("HAS_OAK_SAPLING_30", new SimpleItemMission("참나무 묘목 30개 가져오기", Material.OAK_SAPLING, 30));

        // 13) 조약돌 한 세트(64) 캐오기(심층암 X 표기는 안내용) - 20개
        pool.put("HAS_COBBLESTONE_64", new SimpleItemMission("조약돌 한 세트(64) 가져오기", Material.COBBLESTONE, 64));

        // 15) 쿠키 2개 만들기 - 30개
        pool.put("CRAFT_COOKIE_2", new SimpleItemMission("쿠키 2개 만들기", Material.COOKIE, 2));

        // 16) 아이템 액자 5개 만들기 - 15개
        pool.put("CRAFT_ITEM_FRAME_5", new SimpleItemMission("아이템 액자 5개 만들기", Material.ITEM_FRAME, 5));

        // 17) 화로 10개 만들어오기 - 15개
        pool.put("CRAFT_FURNACE_10", new SimpleItemMission("화로 10개 만들어오기", Material.FURNACE, 10));

        // 18) 잭 오 랜턴 5개 만들어오기 - 30개
        pool.put("CRAFT_JACK_O_LANTERN_5", new SimpleItemMission("잭 오 랜턴 5개 만들어오기", Material.JACK_O_LANTERN, 5));

        // reward.yml 로드
        File f = new File(plugin.getDataFolder(), "reward.yml");
        YamlConfiguration yc = YamlConfiguration.loadConfiguration(f);
        if (yc.getConfigurationSection("rewards") != null) {
            for (String k : yc.getConfigurationSection("rewards").getKeys(false)) {
                rewards.put(k, yc.getInt("rewards." + k, 1));
            }
        }

        // reward.yml에 없을 때 기본 보상(요청 수치) 채워주기
        putDefaultReward("HAS_WOOL_ANY_5", 10);
        putDefaultReward("CRAFT_HAY_BLOCK_1", 20);
        putDefaultReward("HAS_ROTTEN_FLESH_5", 15);
        putDefaultReward("CRAFT_BED_ANY_2", 15);
        putDefaultReward("HAS_SEEDS_ANY_15", 10);
        putDefaultReward("HAS_5_DISTINCT_DYES", 30);
        putDefaultReward("CRAFT_PAINTING_1", 10);
        putDefaultReward("DO_AEGYO", 10);      // 최소 10
        putDefaultReward("HAS_5_DISTINCT_FOOD_NO_MONSTER", 25);
        putDefaultReward("HAS_3_DISTINCT_MONSTER_DROPS", 20);
        putDefaultReward("CRAFT_GOLDEN_APPLE_1", 30);
        putDefaultReward("HAS_OAK_SAPLING_30", 20);
        putDefaultReward("HAS_COBBLESTONE_64", 20);
        putDefaultReward("DO_AEGYO_2", 10);    // 최소 10
        putDefaultReward("CRAFT_COOKIE_2", 30);
        putDefaultReward("CRAFT_ITEM_FRAME_5", 15);
        putDefaultReward("CRAFT_FURNACE_10", 15);
        putDefaultReward("CRAFT_JACK_O_LANTERN_5", 30);

        // 액션바 갱신 + 자동 클리어
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                String key = currentKey.get(p.getUniqueId());
                String msg = (key == null ? "§7[미션] §f미션 없음" : "§e[미션] §f" + pool.get(key).title());
                Texts.actionBar(p, msg);
                if (key != null && pool.get(key).check(p)) complete(p, key);
            }
        }, 20L, 20L);
    }

    public void giveRandom(Player p) {
        List<String> keys = new ArrayList<>(pool.keySet());
        Collections.shuffle(keys);
        String pick = keys.get(0);
        currentKey.put(p.getUniqueId(), pick);
        p.sendMessage("§a[미션] 새 미션: §f" + pool.get(pick).title());
    }

    public void forfeitAndNew(Player p) {
        if (!Items.take(p, Material.DIAMOND, 1)) {
            p.sendMessage("§c다이아 1개가 필요합니다.");
            return;
        }
        giveRandom(p);
    }

    private void complete(Player p, String key) {
        Integer reward = rewards.getOrDefault(key, 1);
        p.sendMessage("§b[미션] 완료! 보상으로 다이아 §e"+reward+"§b개 지급");
        p.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DIAMOND, reward));
        currentKey.remove(p.getUniqueId());
        // 다음 미션 자동 지급
        giveRandom(p);
    }

    private void putDefaultReward(String key, int amount) {
        rewards.putIfAbsent(key, amount);
    }

    interface MissionSpec {
        String title();
        boolean check(Player p);
    }

    // 단일 아이템 수량
    static class SimpleItemMission implements MissionSpec {
        final String title; final Material mat; final int amount;
        SimpleItemMission(String title, Material mat, int amount) { this.title = title; this.mat = mat; this.amount = amount; }
        public String title() { return title; }
        public boolean check(Player p) { return Items.count(p, mat) >= amount; }
    }

    // 여러 재료(집합) 합계 수량
    static class SumOfMaterialsMission implements MissionSpec {
        final String title; final Set<Material> mats; final int amount;
        SumOfMaterialsMission(String title, Set<Material> mats, int amount) { this.title = title; this.mats = mats; this.amount = amount; }
        public String title() { return title; }
        public boolean check(Player p) {
            int total = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                if (mats.contains(it.getType())) total += it.getAmount();
                if (total >= amount) return true;
            }
            return total >= amount;
        }
    }

    // 조건을 만족하는 "서로 다른" 아이템 종류 개수
    static class UniquePredicateMission implements MissionSpec {
        final String title; final Predicate<Material> filter; final int distinctNeeded;
        UniquePredicateMission(String title, Predicate<Material> filter, int distinctNeeded) { this.title = title; this.filter = filter; this.distinctNeeded = distinctNeeded; }
        public String title() { return title; }
        public boolean check(Player p) {
            Set<Material> seen = new HashSet<>();
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                Material m = it.getType();
                if (it.getAmount() > 0 && filter.test(m)) seen.add(m);
                if (seen.size() >= distinctNeeded) return true;
            }
            return seen.size() >= distinctNeeded;
        }
    }

    // 지정된 집합에서 "서로 다른" 아이템 종류 개수
    static class UniqueInSetMission implements MissionSpec {
        final String title; final Set<Material> set; final int distinctNeeded;
        UniqueInSetMission(String title, Set<Material> set, int distinctNeeded) { this.title = title; this.set = set; this.distinctNeeded = distinctNeeded; }
        public String title() { return title; }
        public boolean check(Player p) {
            Set<Material> seen = new HashSet<>();
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null) continue;
                Material m = it.getType();
                if (it.getAmount() > 0 && set.contains(m)) seen.add(m);
                if (seen.size() >= distinctNeeded) return true;
            }
            return seen.size() >= distinctNeeded;
        }
    }

    // 운영자 수동 승인용: 플레이어 엔티티 태그로 판정
    static class ScoreboardTagMission implements MissionSpec {
        final String title; final String tag;
        ScoreboardTagMission(String title, String tag) { this.title = title; this.tag = tag; }
        public String title() { return title; }
        public boolean check(Player p) {
            if (p.getScoreboardTags().contains(tag)) {
                // 한 번 완료되면 태그 제거(재사용 방지)
                p.removeScoreboardTag(tag);
                return true;
            }
            return false;
        }
    }

    /* ===== 유틸 ===== */
    private static Set<Material> matsEndingWith(String suffix) {
        Set<Material> s = new HashSet<>();
        for (Material m : Material.values()) {
            if (m.name().endsWith(suffix)) s.add(m);
        }
        return s;
    }
}