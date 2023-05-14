package me.florixak.uhcrun.listener;

import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.config.Messages;
import me.florixak.uhcrun.customDrop.CustomDrop;
import me.florixak.uhcrun.events.GameEndEvent;
import me.florixak.uhcrun.events.GameKillEvent;
import me.florixak.uhcrun.game.GameManager;
import me.florixak.uhcrun.game.GameState;
import me.florixak.uhcrun.player.UHCPlayer;
import me.florixak.uhcrun.utils.TextUtils;
import me.florixak.uhcrun.utils.Utils;
import me.florixak.uhcrun.utils.XSeries.XMaterial;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class GameListener implements Listener {

    private GameManager gameManager;
    private FileConfiguration config, messages;
    private String prefix;

    public GameListener(GameManager gameManager) {
        this.gameManager = gameManager;
        this.config = gameManager.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
        this.messages = gameManager.getConfigManager().getFile(ConfigType.MESSAGES).getConfig();
        this.prefix = messages.getString("Messages.prefix");
    }

    @EventHandler
    public void handleGameEnd(GameEndEvent event) {

        String winner = event.getWinner();
        List<String> win_rewards_msg = messages.getStringList("Messages.rewards.win");
        List<String> lose_rewards_msg = messages.getStringList("Messages.rewards.lose");
        List<String> top_killers_msg = messages.getStringList("Messages.top_killers");
        List<UHCPlayer> top_killers = gameManager.getPlayerManager().getTopKillers();

        Utils.broadcast(Messages.WINNER.toString().replace("%winner%", winner));

        for (UHCPlayer uhcPlayer : gameManager.getPlayerManager().getPlayers()) {
            uhcPlayer.getData().addStatisticsForGame();

            if (!uhcPlayer.isOnline()) return;

            for (String message : top_killers_msg) {

                message.replace("%topkiller-1%", top_killers.get(0) != null ? top_killers.get(0).getName() : "None")
                        .replace("%topkiller-1-kills%", String.valueOf(top_killers.get(0).getKills()))
                        .replace("%topkiller-2%", top_killers.get(1) != null ? top_killers.get(1).getName() : "None")
                        .replace("%topkiller-2-kills%", String.valueOf(top_killers.get(1).getKills()))
                        .replace("%topkiller-3%", top_killers.get(2) != null ? top_killers.get(2).getName() : "None")
                        .replace("%topkiller-3-kills%", String.valueOf(top_killers.get(2).getKills())
                        );

                Utils.broadcast(message);
            }


            if (uhcPlayer.isWinner()) {
                for (String message : win_rewards_msg) {
                    uhcPlayer.sendMessage(TextUtils.color(message));
                }
            } else {
                for (String message : lose_rewards_msg) {
                    uhcPlayer.sendMessage(TextUtils.color(message));
                }
            }
        }
    }

    @EventHandler
    public void handleGameKill(GameKillEvent event) {
        UHCPlayer killer = event.getKiller();
        UHCPlayer victim = event.getVictim();

        if (killer != null) {
            killer.addKill();

            gameManager.getPerksManager().givePerk(killer);
            killer.getPlayer().giveExp(config.getInt("settings.rewards.kill.exp"));

            Utils.broadcast(Messages.KILL.toString().replace("%player%", victim.getName()).replace("%killer%", killer.getName()));
        } else {
            Utils.broadcast(Messages.DEATH.toString().replace("%player%", victim.getName()));
        }

        if (victim.getTeam().getLivingMembers().size() == 0 && gameManager.isTeamMode()) {
            Utils.broadcast("Team " + victim.getTeam().getName() + " was defeated!");
        }

        gameManager.getPlayerManager().clearPlayerInventory(victim.getPlayer());
        gameManager.getPlayerManager().setSpectator(victim);
    }

    @EventHandler
    public void handleBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        UHCPlayer uhcPlayer = gameManager.getPlayerManager().getUHCPlayer(p.getUniqueId());
        Block block = event.getBlock();

        if (!gameManager.isPlaying() || uhcPlayer.isDead()) {
            event.setCancelled(true);
            p.sendMessage(Messages.CANT_BREAK.toString());
            return;
        }

        if (gameManager.getCustomDropManager().hasCustomDrop(block.getType())) {
            CustomDrop customDrop = gameManager.getCustomDropManager().getCustomDrop(block.getType());
            customDrop.dropItem(event);
        }

    }

    @EventHandler
    public void handleBlockPlace(BlockPlaceEvent event) {
        UHCPlayer uhcPlayer = gameManager.getPlayerManager().getUHCPlayer(event.getPlayer().getUniqueId());
        if (!gameManager.isPlaying() || uhcPlayer.isDead()) {
            uhcPlayer.sendMessage(Messages.CANT_PLACE.toString());
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void handleEntityDrop(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals)) return;

        Random ran = new Random();
        int amount = 1;

        if (event.getEntity() instanceof Cow) {
            event.getDrops().clear();
            amount = ran.nextInt(3)+1;
            event.getDrops().add(new ItemStack(XMaterial.COOKED_BEEF.parseMaterial(), amount));
        }
        if (event.getEntity() instanceof Pig) {
            event.getDrops().clear();
            amount = ran.nextInt(3)+1;
            event.getDrops().add(new ItemStack(XMaterial.COOKED_BEEF.parseMaterial(), amount));
        }
        if (event.getEntity() instanceof Sheep) {
            event.getDrops().clear();
            amount = ran.nextInt(3)+1;
            event.getDrops().add(new ItemStack(XMaterial.COOKED_MUTTON.parseMaterial(), amount));
            event.getDrops().add(new ItemStack(XMaterial.STRING.parseMaterial(), amount));
        }
        if (event.getEntity() instanceof Chicken) {
            event.getDrops().clear();
            amount = ran.nextInt(3)+1;
            event.getDrops().add(new ItemStack(XMaterial.COOKED_CHICKEN.parseMaterial(), amount));
            event.getDrops().add(new ItemStack(XMaterial.STRING.parseMaterial(), amount));
        }
        if (event.getEntity() instanceof Rabbit) {
            event.getDrops().clear();
            amount = ran.nextInt(3)+1;
            event.getDrops().add(new ItemStack(XMaterial.COOKED_RABBIT.parseMaterial(), amount));
        }
    }

    @EventHandler
    public void handleDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player)) return;

        if (!gameManager.isPlaying()) {
            event.setCancelled(true);
            return;
        }

        DamageCause cause = event.getCause();
        if (gameManager.getGameState() == GameState.MINING) {
            if ((cause.equals(DamageCause.FIRE) && gameManager.isNoLavaBurn())
                    || (cause.equals(DamageCause.FIRE_TICK) && gameManager.isNoLavaBurn())
                    || (cause.equals(DamageCause.LAVA) && gameManager.isNoLavaBurn())
                    || (cause.equals(DamageCause.DROWNING) && gameManager.isNoDrowning())
                    || (cause.equals(DamageCause.FALL) && gameManager.isNoFallDamage())
                    || (cause.equals(DamageCause.FALLING_BLOCK) && gameManager.isNoFallBlockDamage())
                    || (cause.equals(DamageCause.ENTITY_EXPLOSION) && gameManager.isNoExplosionDamage())
                    || (cause.equals(DamageCause.BLOCK_EXPLOSION) && gameManager.isNoExplosionDamage())
            ) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void handleEntityHitEntity(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            if (!gameManager.isPlaying()) {
                event.setCancelled(true);
            }
            return;
        }

        Player damager = (Player) event.getDamager();
        UHCPlayer uhcPlayerD = gameManager.getPlayerManager().getUHCPlayer(damager.getUniqueId());

        if (!gameManager.isPlaying() || uhcPlayerD.isDead()) {
            event.setCancelled(true);
            return;
        }

        if (gameManager.getGameState().equals(GameState.MINING)
                || gameManager.getGameState().equals(GameState.ENDING)) {
            if (!(event.getEntity() instanceof Player)) return;
            event.setCancelled(true);
            return;
        }

        if (event.getEntity() instanceof Player) {
            Player entity = (Player) event.getEntity();
            UHCPlayer uhcPlayerE = gameManager.getPlayerManager().getUHCPlayer(entity.getUniqueId());

            if (uhcPlayerD.getTeam() == uhcPlayerE.getTeam() && config.getBoolean("settings.teams.friendly-fire")) {
                event.setCancelled(true);
                uhcPlayerD.sendMessage("Baka, this is your teammate...");
            }
        }
    }

    @EventHandler
    public void handleWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void handleTimber(BlockBreakEvent event) {
        gameManager.getUtils().timber(event.getBlock());
    }

    @EventHandler
    public void handleHunger(FoodLevelChangeEvent event) {
        Player p = (Player) event.getEntity();
        UHCPlayer player = gameManager.getPlayerManager().getUHCPlayer(p.getUniqueId());
        if (!gameManager.isPlaying() || player.isDead()) {
            p.setFoodLevel(20);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handleBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!gameManager.isPlaying()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handleBucketFill(PlayerBucketFillEvent event) {
        if (!gameManager.isPlaying()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handleHealthRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        RegainReason reason = event.getRegainReason();

        if (reason.equals(RegainReason.SATIATED)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void handleArrowHitHP(ProjectileHitEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        if (!(event.getEntity() instanceof Arrow) && !(event.getEntity() instanceof Snowball)) return;
        if (config.getBoolean("settings.game.projectile-hit-hp", false)) return;

        Player shooter = (Player) event.getEntity().getShooter();
        Player enemy = (Player) event.getHitEntity();

        shooter.sendMessage(Messages.SHOT_HP.toString()
                .replace("%player%", enemy.getDisplayName())
                .replace("%hp%", String.valueOf(enemy.getHealth())));
    }

    @EventHandler
    public void handleMonsterTargeting(EntityTargetEvent event) {
        if (event.getEntity() instanceof Monster) {
            if (event.getTarget() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void handleMonsterSpawning(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Monster) {
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.NATURAL) {
                event.setCancelled(true);
            }
        }
    }
}