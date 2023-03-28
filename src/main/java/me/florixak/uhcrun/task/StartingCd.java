package me.florixak.uhcrun.task;

import me.florixak.uhcrun.UHCRun;
import me.florixak.uhcrun.config.Messages;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.player.PlayerManager;
import me.florixak.uhcrun.manager.SoundManager;
import me.florixak.uhcrun.manager.gameManager.GameState;
import me.florixak.uhcrun.player.UHCPlayer;
import me.florixak.uhcrun.utils.TimeUtils;
import me.florixak.uhcrun.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class StartingCd extends BukkitRunnable {

    private UHCRun plugin;
    private FileConfiguration config;
    public static int count;
    private int startWarning;

    public StartingCd(UHCRun plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
        this.count = config.getInt("starting-countdown");
        this.startWarning = config.getInt("starting-warning-time");
    }

    @Override
    public void run() {

        if (count <= 0) {
            cancel();
            plugin.getGame().setGameState(GameState.MINING);
            return;
        }
        if (count <= startWarning) {
            Utils.broadcast(Messages.GAME_STARTING.toString()
                    .replace("%countdown%", "" + TimeUtils.getFormattedTime(count)));
            for (UHCPlayer player : plugin.getPlayerManager().getPlayers()) {
                SoundManager.playStartingSound(player.getPlayer());
            }
        }
        plugin.getGame().checkGame();
        count--;
    }

}