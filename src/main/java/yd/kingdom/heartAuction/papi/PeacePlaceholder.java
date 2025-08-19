package yd.kingdom.heartAuction.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import yd.kingdom.heartAuction.HeartAuction;

public class PeacePlaceholder extends PlaceholderExpansion {

    private final HeartAuction plugin;

    public PeacePlaceholder(HeartAuction plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "peace"; } // → %peace_time%
    @Override public String getAuthor()     { return String.join(", ", plugin.getDescription().getAuthors()); }
    @Override public String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()      { return true; } // 리로드 후에도 유지
    @Override public boolean canRegister()  { return true; }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if ("time".equalsIgnoreCase(params)) {
            long sec = plugin.game().getPeaceRemainingSeconds();
            long mm = sec / 60;
            long ss = sec % 60;
            return String.format("%02d:%02d", mm, ss);
        }
        return null;
    }
}