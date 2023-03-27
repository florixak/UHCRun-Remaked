package me.florixak.uhcrun;

import me.florixak.uhcrun.action.ActionManager;
import me.florixak.uhcrun.commands.AnvilCommand;
import me.florixak.uhcrun.commands.CreatorModeCommand;
import me.florixak.uhcrun.commands.WorkbenchCommand;
import me.florixak.uhcrun.config.ConfigManager;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.kits.KitsManager;
import me.florixak.uhcrun.listener.ChatListener;
import me.florixak.uhcrun.manager.*;
import me.florixak.uhcrun.inventory.InventoryManager;
import me.florixak.uhcrun.listener.InteractListener;
import me.florixak.uhcrun.listener.GameListener;
import me.florixak.uhcrun.listener.PlayerListener;
import me.florixak.uhcrun.manager.gameManager.GameManager;
import me.florixak.uhcrun.player.PlayerManager;
import me.florixak.uhcrun.scoreboard.ScoreboardManager;
import me.florixak.uhcrun.sql.MySQL;
import me.florixak.uhcrun.sql.SQLGetter;
import me.florixak.uhcrun.utils.TeleportUtils;
import me.florixak.uhcrun.utils.TextUtils;
import me.florixak.uhcrun.utils.Utils;
import me.florixak.uhcrun.utils.VanishUtils;
import me.florixak.uhcrun.utils.placeholderapi.PlaceholderExp;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class UHCRun extends JavaPlugin {

    /*
    TODO:
        - spectator teleport
        - custom craft menu
        - celkově doladit
    */

    private static UHCRun plugin;
    private static Economy econ;
    private static LuckPerms luckPerms;

    public MySQL mysql;
    public SQLGetter data;

    private GameManager gameManager;
    private ConfigManager configManager;
    private PlayerManager playerManager;
    private ScoreboardManager scoreboardManager;
    private LocationManager lobbyManager;
    private BorderManager borderManager;
    private ActionManager actionManager;
    private InventoryManager inventoryManager;
    private StatisticsManager statisticManager;
    private LevelManager levelManager;
    private KitsManager kitsManager;
    private TaskManager taskManager;
    private TeamManager teamManager;

    private Utils utilities;
    private TeleportUtils teleportUtil;

    public static UHCRun getInstance() {
        return plugin;
    }

    @Override
    public void onEnable() {

        getLogger().info("Author: FloriXak");
        getLogger().info("Web: www.florixak.tk");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("Created 4fun");

        plugin = this;
        this.configManager = new ConfigManager();
        this.configManager.loadFiles(this);
        this.gameManager = new GameManager(this);
        this.playerManager = new PlayerManager();
        this.scoreboardManager = new ScoreboardManager(this);
        this.lobbyManager = new LocationManager(this);
        this.borderManager = new BorderManager(this);
        this.actionManager = new ActionManager(this);
        this.inventoryManager = new InventoryManager();
        this.inventoryManager.onEnable(this);
        this.statisticManager = new StatisticsManager(this);
        this.levelManager = new LevelManager(this);
        this.kitsManager = new KitsManager(this);
        this.teamManager = new TeamManager(this);
        this.taskManager = new TaskManager(this);

        this.utilities = new Utils(this);
        this.teleportUtil = new TeleportUtils(this);

        registerAddons();
        connectToDatabase();
        registerRecipes();

        try {
            new CreatorModeCommand(this);
            new WorkbenchCommand(this);
            new AnvilCommand(this);
        } catch (Exception e) {
            getLogger().info(TextUtils.color("&cThere is error in commands!"));
            e.printStackTrace();
        }

        try {
            getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
            getServer().getPluginManager().registerEvents(new GameListener(this), this);
            getServer().getPluginManager().registerEvents(new InteractListener(this), this);
            getServer().getPluginManager().registerEvents(new ChatListener(this), this);
            getServer().getPluginManager().registerEvents(new VanishUtils(), this);
        } catch (Exception e) {
            getLogger().info(TextUtils.color("&cThere is error in listeners!"));
            e.printStackTrace();
        }

        // getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getGame().runActivityRewards();
        getGame().runAutoBroadcast();
        getGame().updateScoreboard();
        getGame().checkBorder();

        try {
            gameManager.setOreSpawn();
            getLogger().info(TextUtils.color("&aAll ores are set!"));
        } catch (Exception e) {
            getLogger().info(TextUtils.color("&cThere is error in ore spawn!"));
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        inventoryManager.onDisable();
        teamManager.onDisable();

        disconnectDatabase();

    }

    private void connectToDatabase() {
        if (configManager.getFile(ConfigType.SETTINGS).getConfig().getBoolean("MySQL.enabled", true)) {
            mysql = new MySQL("localhost", 3306, null, null, null);
            data = new SQLGetter(plugin);
            data.createTable();
        }
    }
    private void disconnectDatabase() {
        if (configManager.getFile(ConfigType.SETTINGS).getConfig().getBoolean("MySQL.enabled", true)) {
            mysql.disconnect();
        }
    }

    private void registerRecipes() {
        try {
            new RecipeManager();
            getLogger().info(TextUtils.color("&aAll recipes are loaded! (" + RecipeManager.recipes + ")"));
        } catch (Exception e) {
            getLogger().info(TextUtils.color("&cThere is error in recipes!"));
            e.printStackTrace();
        }
    }

    private void registerAddons() {
        if (configManager.getFile(ConfigType.SETTINGS).getConfig().getBoolean("use-Vault", true)) {
            if (!setupVault()) {
                getLogger().info(TextUtils.color("&cNo economy plugin found. Disabling UHCRun."));
            }
            else {
                getLogger().info(TextUtils.color("&aVault plugin found."));
            }
        }

        if (configManager.getFile(ConfigType.SETTINGS).getConfig().getBoolean("use-LuckPerms", true)) {
            if (!setupLuckPerms()) {
                getLogger().info(TextUtils.color("&cLuckPerms plugin not found."));
            }
            else {
                getLogger().info(TextUtils.color("&aLuckPerms plugin found."));
            }
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderExp(this).register();
        }
    }
    private boolean setupVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
    private boolean setupLuckPerms() {
        for (Plugin plugin : getServer().getPluginManager().getPlugins()) {
            if (plugin.getName().contains("LuckPerms")) {
                return true;
            }
        }

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) {
            return false;
        }
        luckPerms = provider.getProvider();
        return luckPerms != null;
    }

    public static Economy getVault() {
        return econ;
    }
    public static LuckPerms getLuckPerms() {
        return luckPerms;
    }
    public GameManager getGame() {
        return gameManager;
    }
    public ConfigManager getConfigManager() {
        return configManager;
    }
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    public Utils getUtilities() { return utilities; }
    public LocationManager getLobbyManager() { return lobbyManager; }
    public BorderManager getBorderManager() { return borderManager; }
    public ActionManager getActionManager() {
        return actionManager;
    }
    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }
    public StatisticsManager getStatistics() {
        return statisticManager;
    }
    public LevelManager getLevelManager() {
        return levelManager;
    }
    public TeamManager getTeams() {
        return teamManager;
    }
    public KitsManager getKitsManager() {
        return kitsManager;
    }
    public TaskManager getTasks() {
        return taskManager;
    }
}