package me.florixak.uhcrun.task;

import me.florixak.uhcrun.UHCRun;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.config.Messages;
import me.florixak.uhcrun.manager.PlayerManager;
import me.florixak.uhcrun.manager.gameManager.GameManager;
import me.florixak.uhcrun.manager.gameManager.GameState;
import me.florixak.uhcrun.utility.TimeConvertor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class MiningCountdown extends BukkitRunnable {

    private GameManager gameManager;
    private FileConfiguration config;
    public static int count;

    public MiningCountdown(GameManager gameManager) {
        this.gameManager = gameManager;
        this.config = UHCRun.plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
        this.count = config.getInt("mining-countdown");
    }

    @Override
    public void run() {
        if (count <= 0) {
            cancel();
            gameManager.setGameState(GameState.FIGHTING);
            return;
        }

        if (count == count/2) {
            Bukkit.broadcastMessage(Messages.GAME_STARTING.toString().replace("%count%", "" + TimeConvertor.convertCountdown(count)));
        }

        if (PlayerManager.online.size() < config.getInt("min-players-to-start")) {
            cancel();
            gameManager.setGameState(GameState.ENDING);
            return;
        }
        UHCRun.plugin.getUtilities().checkGame();

        count--;
    }
}
