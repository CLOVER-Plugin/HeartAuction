package yd.kingdom.heartAuction.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Items {
    public static boolean hasItem(Player p, Material m, int amount) { return count(p, m) >= amount; }

    public static int count(Player p, Material m) {
        int c = 0; for (ItemStack it : p.getInventory().getContents()) if (it != null && it.getType() == m) c += it.getAmount();
        return c;
    }

    public static boolean take(Player p, Material m, int amount) { if (!hasItem(p, m, amount)) return false; takeItem(p, m, amount); return true; }

    public static void takeItem(Player p, Material m, int amount) {
        int left = amount;
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || it.getType() != m) continue;
            int t = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - t);
            left -= t;
            if (left <= 0) break;
        }
        p.updateInventory();
    }

    public static ItemStack withLore(ItemStack it, String lore) {
        ItemMeta meta = it.getItemMeta();
        java.util.List<String> l = meta.hasLore()? meta.getLore() : new java.util.ArrayList<>();
        l.add(lore); meta.setLore(l); it.setItemMeta(meta); return it;
    }

    // === 체력 증가 아이템 ===
    public static boolean isHealthItem(ItemStack it) {
        if (it.getType() != Material.RED_CONCRETE) return false;
        ItemMeta m = it.getItemMeta(); if (m == null || !m.hasLore()) return false;
        return m.getLore().stream().anyMatch(s -> s.contains("최대체력 1칸 증가"));
    }

    public static ItemStack healthBoostItem(int amount) {
        ItemStack it = new ItemStack(Material.RED_CONCRETE, amount);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName("§c체력 증가");
        meta.setLore(java.util.List.of("§7우클릭 시 최대체력 1칸 증가"));
        it.setItemMeta(meta); return it;
    }

    public static void consumeOne(ItemStack it) { it.setAmount(Math.max(0, it.getAmount() - 1)); }
}