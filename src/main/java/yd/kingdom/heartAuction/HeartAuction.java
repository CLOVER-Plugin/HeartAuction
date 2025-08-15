package yd.kingdom.heartAuction;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import yd.kingdom.heartAuction.command.*;
import yd.kingdom.heartAuction.listener.ChatListener;
import yd.kingdom.heartAuction.listener.InteractListener;
import yd.kingdom.heartAuction.listener.InventoryListener;
import yd.kingdom.heartAuction.manager.*;
import yd.kingdom.heartAuction.util.Tasker;

public final class HeartAuction extends JavaPlugin {

    private static HeartAuction inst;
    private AdminManager adminManager;
    private GameManager gameManager;
    private PvpZoneManager pvpZoneManager;
    private AuctionManager auctionManager;
    private MissionManager missionManager;
    private ShopManager shopManager;
    private GambleFlowerManager flowerManager;
    private GambleEggManager eggManager;

    @Override
    public void onEnable() {
        inst = this;
        saveDefaultConfig();
        saveResource("shop.yml", false);
        saveResource("reward.yml", false);
        saveResource("bet.yml", false);

        // Managers
        adminManager = new AdminManager(this);
        pvpZoneManager = new PvpZoneManager(this);
        auctionManager = new AuctionManager(this, pvpZoneManager);
        missionManager = new MissionManager(this);
        shopManager = new ShopManager(this);
        flowerManager = new GambleFlowerManager(this);
        eggManager = new GambleEggManager(this);
        gameManager = new GameManager(this, pvpZoneManager, auctionManager);

        // Commands
        getCommand("운영자").setExecutor(new AdminCommand(adminManager));
        getCommand("게임시작").setExecutor(new GameStartCommand(gameManager));
        getCommand("미션지급").setExecutor(new MissionGiveCommand(missionManager));
        getCommand("미션포기").setExecutor(new MissionForfeitCommand(missionManager));
        getCommand("경매").setExecutor(new AuctionJoinCommand(auctionManager));
        getCommand("spawn").setExecutor(new SpawnCommand());
        getCommand("꽃도박").setExecutor(new FlowerBetCommand(flowerManager));
        getCommand("달걀도박").setExecutor(new EggBetCommand(eggManager));

        // Listeners
        Bukkit.getPluginManager().registerEvents(new ChatListener(auctionManager), this);
        Bukkit.getPluginManager().registerEvents(new InteractListener(this, missionManager), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(shopManager), this);

        Tasker.init(this);
        getLogger().info("HeartAuction enabled.");

    }

    @Override
    public void onDisable() {
        auctionManager.shutdown();
        pvpZoneManager.shutdown();
        Tasker.shutdown();
    }

    public static HeartAuction get() { return inst; }
    public AdminManager admins() { return adminManager; }
    public GameManager game() { return gameManager; }
    public PvpZoneManager pvp() { return pvpZoneManager; }
    public AuctionManager auction() { return auctionManager; }
    public MissionManager missions() { return missionManager; }
    public ShopManager shop() { return shopManager; }
}
