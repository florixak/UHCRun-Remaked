package me.florixak.uhcrun.game;

import me.florixak.uhcrun.UHCRun;
import me.florixak.uhcrun.commands.*;
import me.florixak.uhcrun.config.ConfigManager;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.config.Messages;
import me.florixak.uhcrun.game.customDrop.CustomDropManager;
import me.florixak.uhcrun.game.deathchest.DeathChestManager;
import me.florixak.uhcrun.game.kits.KitsManager;
import me.florixak.uhcrun.game.perks.PerksManager;
import me.florixak.uhcrun.game.world.WorldManager;
import me.florixak.uhcrun.listener.ChatListener;
import me.florixak.uhcrun.listener.GameListener;
import me.florixak.uhcrun.listener.InteractListener;
import me.florixak.uhcrun.listener.InventoryClickListener;
import me.florixak.uhcrun.listener.events.GameEndEvent;
import me.florixak.uhcrun.manager.*;
import me.florixak.uhcrun.manager.lobby.LobbyManager;
import me.florixak.uhcrun.manager.lobby.LobbyType;
import me.florixak.uhcrun.manager.scoreboard.ScoreboardManager;
import me.florixak.uhcrun.player.PlayerListener;
import me.florixak.uhcrun.player.PlayerManager;
import me.florixak.uhcrun.player.UHCPlayer;
import me.florixak.uhcrun.sql.MySQL;
import me.florixak.uhcrun.sql.SQLGetter;
import me.florixak.uhcrun.tasks.*;
import me.florixak.uhcrun.teams.TeamManager;
import me.florixak.uhcrun.utils.TeleportUtils;
import me.florixak.uhcrun.utils.TimeUtils;
import me.florixak.uhcrun.utils.Utils;
import me.florixak.uhcrun.utils.XSeries.XMaterial;
import me.florixak.uhcrun.utils.XSeries.XSound;
import org.bukkit.Bukkit;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private final UHCRun plugin;
    private final FileConfiguration config;

    private GameState gameState;

    private MySQL mysql;
    private SQLGetter data;

    private static GameManager gameManager;
    private final ConfigManager configManager;
    private final PlayerManager playerManager;
    private final TeamManager teamManager;
    private final DeathmatchManager deathmatchManager;
    private final PerksManager perksManager;
    private final KitsManager kitsManager;
    private final TaskManager taskManager;
    private final BorderManager borderManager;
    private final ScoreboardManager scoreboardManager;
    private final TabManager tabManager;
    private final LobbyManager lobbyManager;
    private final CustomDropManager customDropManager;
    private final SoundManager soundManager;
    private final RecipeManager recipeManager;
    private final DeathChestManager deathChestManager;
    private final OreGenManager oreGenManager;
    private final WorldManager worldManager;

    private boolean forceStarted;
    private boolean pvp;

    public GameManager(UHCRun plugin) {
        this.plugin = plugin;
        gameManager = this;

        this.configManager = new ConfigManager();
        this.configManager.loadFiles(plugin);
        this.borderManager = new BorderManager();
        this.playerManager = new PlayerManager(this);
        this.scoreboardManager = new ScoreboardManager(this);
        this.tabManager = new TabManager(this);
        this.lobbyManager = new LobbyManager(this);
        this.kitsManager = new KitsManager(this);
        this.perksManager = new PerksManager(this);
        this.teamManager = new TeamManager(this);
        this.customDropManager = new CustomDropManager(this);
        this.taskManager = new TaskManager(this);
        this.deathChestManager = new DeathChestManager(this);
        this.deathmatchManager = new DeathmatchManager(this);
        this.oreGenManager = new OreGenManager(this);
        this.soundManager = new SoundManager();
        this.recipeManager = new RecipeManager(this);
        this.worldManager = new WorldManager();

        this.config = getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
    }

    public void loadNewGame() {
        setGameState(GameState.LOBBY);

        registerCommands();
        registerListeners();

        setForceStarted(false);
        setPvP(false);

        connectToDatabase();

        if (Bukkit.getWorld(getLobbyManager().getWorld(LobbyType.WAITING)) == null) {
            getWorldManager().createWorld("lobby", WorldType.FLAT, false);
        }
        if (Bukkit.getWorld(getLobbyManager().getWorld(LobbyType.ENDING)) == null) {
            getWorldManager().createWorld("lobby", WorldType.FLAT, false);
        }

        getBorderManager().setBorder();
        getOreGenManager().generateOres();

        getRecipeManager().registerRecipes();
        getCustomDropManager().loadDrops();
        getTeamManager().loadTeams();
        getKitsManager().loadKits();
        // TODO getPerksManager().loadPerks();

        getTaskManager().runGameChecking();
        getTaskManager().runScoreboardUpdate();
        getTaskManager().runActivityRewards();
        getTaskManager().runAutoBroadcast();
    }

    public GameState getGameState() {
        return this.gameState;
    }

    public void setGameState(GameState gameState) {
        if (this.gameState == gameState) return;
        this.gameState = gameState;

        switch (gameState) {

            case LOBBY:
                break;

            case STARTING:
                getTaskManager().startStartingCD();
                Utils.broadcast(Messages.GAME_STARTING.toString().replace("%countdown%", TimeUtils.getFormattedTime(getCurrentCountdown())));
                Bukkit.getOnlinePlayers().forEach(player -> getSoundManager().playStartingSound(player));
                break;

            case MINING:
                getPlayerManager().getOnlineList().forEach(UHCPlayer::ready);
                getTeamManager().getTeams().forEach(uhcTeam -> uhcTeam.teleport(TeleportUtils.getSafeLocation()));

                getTaskManager().startMiningCD();
                Utils.broadcast(Messages.MINING.toString().replace("%countdown%", TimeUtils.getFormattedTime(getCurrentCountdown())));
                Bukkit.getOnlinePlayers().forEach(player -> getSoundManager().playGameStarted(player));
                break;

            case PVP:
                if (GameValues.TELEPORT_AFTER_MINING) {
                    getTeamManager().teleportAfterMining();
                }
                setPvP(true);
                getTaskManager().startPvPCD();

                Utils.broadcast(Messages.PVP.toString());
                Utils.broadcast(Messages.BORDER_SHRINK.toString());
                break;

            case DEATHMATCH:
                getDeathmatchManager().prepareDeathmatch();
                getTaskManager().startDeathmatchCD();

                Utils.broadcast(Messages.DEATHMATCH.toString());
                Bukkit.getOnlinePlayers().forEach(player -> getSoundManager().playDMBegan(player));
                break;

            case ENDING:
                Utils.broadcast(Messages.GAME_ENDED.toString());
                Bukkit.getOnlinePlayers().forEach(player -> getSoundManager().playGameEnd(player));

                getPlayerManager().setUHCWinner();
                plugin.getServer().getPluginManager().callEvent(new GameEndEvent(getPlayerManager().getUHCWinner()));

                getTaskManager().startEndingCD();

                break;
        }
    }

    public int getCurrentCountdown() {
        switch (gameState) {
            case LOBBY:
                return -1;
            case STARTING:
                return StartingCD.getCountdown();
            case MINING:
                return MiningCD.getCountdown();
            case PVP:
                return PvPCD.getCountdown();
            case DEATHMATCH:
                return DeathmatchCD.getCountdown();
            case ENDING:
                return EndingCD.getCountdown();
        }
        return 0;
    }

    public void onDisable() {
        getPlayerManager().onDisable();
        getTeamManager().onDisable();
        getTaskManager().onDisable();
        getDeathChestManager().onDisable();
        getGameManager().clearDrops();
        disconnectDatabase();
    }

    public boolean isStarting() {
        return gameState.equals(GameState.STARTING);
    }
    public boolean isPlaying() {
        return !gameState.equals(GameState.LOBBY) && !gameState.equals(GameState.STARTING);
    }
    public boolean isEnding() {
        return gameState.equals(GameState.ENDING);
    }

    public boolean isForceStarted() {
        return this.forceStarted;
    }
    public void setForceStarted(boolean b) {
        this.forceStarted = b;
    }

    public boolean isPvP() {
        return this.pvp;
    }
    public void setPvP(boolean b) {
        this.pvp = b;
    }

    public void clearDrops() {
        List<Entity> entList = GameValues.GAME_WORLD.getEntities();

        for (Entity current : entList) {
            if (current instanceof Item) {
                current.remove();
            }
        }
    }

    public void timber(Block block) {

        if (!GameValues.WOOD_LOGS.contains(block.getType())) return;

        XSound.play(block.getLocation(), XSound.BLOCK_WOOD_BREAK.toString());
        block.breakNaturally(new ItemStack(XMaterial.OAK_PLANKS.parseMaterial(), 4));

        timber(block.getLocation().add(0, 1, 0).getBlock());
        timber(block.getLocation().add(1, 0, 0).getBlock());
        timber(block.getLocation().add(0, 1, 1).getBlock());

        timber(block.getLocation().subtract(0, 1, 0).getBlock());
        timber(block.getLocation().subtract(1, 0, 0).getBlock());
        timber(block.getLocation().subtract(0, 0, 1).getBlock());
    }

    public boolean isGameFull() {
        return playerManager.getOnlineList().size() >= playerManager.getMaxPlayers();
    }

    public MySQL getSQL() {
        return this.mysql;
    }
    public SQLGetter getData() {
        return this.data;
    }

    private void connectToDatabase() {
        String path = "settings.mysql";
        if (!config.getBoolean(path + ".enabled", false)) return;

        this.mysql = new MySQL(config.getString(path + ".host", "localhost"), config.getString(path + ".port", "3306"), config.getString(path + ".database", "uhcrun"), config.getString(path + ".username", "root"), config.getString(path + ".password", ""));
        this.data = new SQLGetter(this);
    }
    private void disconnectDatabase() {
        if (config.getBoolean("settings.MySQL.enabled", false)) {
            mysql.disconnect();
        }
    }
    public boolean isDatabaseConnected() {
        return this.mysql != null && this.mysql.hasConnection();
    }

    private void registerListeners() {
        List<Listener> listeners = new ArrayList<>();

        listeners.add(new GameListener(gameManager));
        listeners.add(new PlayerListener(gameManager));
        listeners.add(new ChatListener(gameManager));
        listeners.add(new InteractListener(gameManager));
        listeners.add(new InventoryClickListener(gameManager));

        for (Listener listener : listeners) {
            Bukkit.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    private void registerCommands() {
        registerCommand("uhcrun", new SetupCommand(gameManager));
        registerCommand("forcestart", new ForceStartCommand(gameManager));
        registerCommand("team", new TeamCommand(gameManager));
        registerCommand("workbench", new WorkbenchCommand(gameManager));
        registerCommand("anvil", new AnvilCommand(gameManager));
        registerCommand("kits", new KitsCommand(gameManager));
        registerCommand("statistics", new StatisticsCommand(gameManager));
        registerCommand("revive", new ReviveCommand(gameManager));
    }
    private void registerCommand(String commandN, CommandExecutor executor) {
        PluginCommand command = plugin.getCommand(commandN);

        if (command == null) {
            plugin.getLogger().info("Error in registering command! (" + command + ")");
            return;
        }
        command.setExecutor(executor);
    }

    public static GameManager getGameManager() {
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

    public TabManager getTabManager() {
        return tabManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public CustomDropManager getCustomDropManager() {
        return customDropManager;
    }

    public KitsManager getKitsManager() {
        return kitsManager;
    }

    public PerksManager getPerksManager() {
        return perksManager;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public DeathChestManager getDeathChestManager() {
        return deathChestManager;
    }

    public DeathmatchManager getDeathmatchManager() {
        return deathmatchManager;
    }

    public OreGenManager getOreGenManager() {
        return oreGenManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }
}