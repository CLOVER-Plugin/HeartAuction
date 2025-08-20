package yd.kingdom.heartAuction.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Tasker {
    private static Plugin plugin;
    public static void init(Plugin p){ plugin = p; }
    public static int runTimer(Runnable r, long delay, long period){
        return Bukkit.getScheduler().runTaskTimer(plugin, r, delay, period).getTaskId();
    }
    public static int runLater(Runnable r, long delay){
        return Bukkit.getScheduler().runTaskLater(plugin, r, delay).getTaskId();
    }
    public static void cancel(int id){ if (id != -1) Bukkit.getScheduler().cancelTask(id); }
    public static void shutdown(){}
}