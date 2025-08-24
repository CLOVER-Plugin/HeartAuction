package yd.kingdom.heartAuction.listener;

import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import yd.kingdom.heartAuction.manager.ShopManager;

import java.util.EnumSet;
import java.util.Set;

public class CraftBlockListener implements Listener {
    private final ShopManager shop;
    private static final Set<Material> EXTRA_BANNED = EnumSet.of(
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS
    );

    public CraftBlockListener(ShopManager shop) {
        this.shop = shop;
    }

    private boolean isBanned(Material type) {
        if (type == null) return false;
        Set<Material> banned = shop.getRegisteredItems(); // 상점 등록 아이템
        return banned.contains(type) || EXTRA_BANNED.contains(type); // + 다이아 갑옷 세트
    }

    /* 1) 작업대/손제작 미리보기 단계에서 결과 제거 */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        ItemStack result = e.getInventory().getResult();
        if (result != null && isBanned(result.getType())) {
            e.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }

    /* 2) 실제 클릭 시도 시도 무효화(중복 방지용) */
    @EventHandler
    public void onCraft(CraftItemEvent e) {
        ItemStack result = e.getRecipe() != null ? e.getRecipe().getResult() : e.getCurrentItem();
        if (result != null && isBanned(result.getType())) {
            e.setCancelled(true);
            HumanEntity p = e.getWhoClicked();
            p.sendMessage("§c해당 아이템은 상점 전용이라 제작할 수 없습니다.");
        }
    }

    /* 3) 스미딩 테이블(네더라이트 업그레이드 등) 미리보기 차단 */
    @EventHandler
    public void onPrepareSmith(PrepareSmithingEvent e) {
        ItemStack result = e.getResult();
        if (result != null && isBanned(result.getType())) {
            e.setResult(new ItemStack(Material.AIR));
        }
    }

    /* 4) 스미딩 결과 클릭 차단(확정 시도) */
    @EventHandler
    public void onSmith(SmithItemEvent e) {
        ItemStack result = e.getInventory().getResult();
        if (result != null && isBanned(result.getType())) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage("§c해당 아이템은 상점 전용이라 업그레이드/제작할 수 없습니다.");
        }
    }

    /* 5) 양조대로 스플래시 포션 만드는 것도 금지(상점 등록된 경우) */
    @EventHandler
    public void onBrew(BrewEvent e) {
        // 상점에 SPLASH_POTION이 등록되어 있으면 생성 즉시 원복
        if (!isBanned(Material.SPLASH_POTION)) return;

        BrewerInventory inv = e.getContents();
        for (int i = 0; i < 3; i++) { // 결과 슬롯 3개
            ItemStack it = inv.getItem(i);
            if (it != null && it.getType() == Material.SPLASH_POTION) {
                // 스플래시 생성 시 바로 일반 포션으로 되돌림
                inv.setItem(i, new ItemStack(Material.POTION));
            }
        }
    }
}