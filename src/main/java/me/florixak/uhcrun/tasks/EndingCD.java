package me.florixak.uhcrun.tasks;

import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class EndingCD extends BukkitRunnable {

    public static int countdown;

    public EndingCD(GameManager gameManager) {
        FileConfiguration config = gameManager.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
        countdown = config.getInt("settings.game.countdowns.ending");
    }

    @Override
    public void run() {

        if (countdown <= 0) {
            cancel();
            Bukkit.shutdown();
            return;
        }
        countdown--;
    }
}