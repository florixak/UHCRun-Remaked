package me.florixak.uhcrun.utility;

import me.florixak.uhcrun.UHCRun;
import me.florixak.uhcrun.action.actions.BroadcastMessageAction;
import me.florixak.uhcrun.action.actions.TitleAction;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.config.Messages;
import me.florixak.uhcrun.manager.KitsManager;
import me.florixak.uhcrun.manager.PlayerManager;
import me.florixak.uhcrun.task.AutoBroadcastMessages;
import me.florixak.uhcrun.task.ActivityRewards;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Utilities {

    private UHCRun plugin;
    private FileConfiguration config;

    public Utilities(UHCRun plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
    }

    @SuppressWarnings("deprecation")
    public ItemStack getPlayerHead(String player, String name) {
        boolean isNewVersion = Arrays.stream(Material.values())
                .map(Material::name).collect(Collectors.toList()).contains("PLAYER_HEAD");

        Material type = Material.matchMaterial(isNewVersion ? "PLAYER_HEAD" : "SKULL_ITEM");
        ItemStack item = new ItemStack(type, 1);

        if (!isNewVersion) item.setDurability((short) 3);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (name != null) meta.setDisplayName(TextUtil.color(name));
        meta.setOwner(player);

        item.setItemMeta(meta);
        return item;
    }
    public void skullTeleport(Player p, ItemStack item) {
        if (item.getType() != Material.AIR && item.getType() != null) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta.getDisplayName() != null) {
                if (Bukkit.getPlayer(meta.getDisplayName()) != null) {
                    Player player = Bukkit.getPlayer(meta.getDisplayName());
                    if (player == null) {
                        p.closeInventory();
                        p.sendMessage(Messages.OFFLINE_PLAYER.toString());
                        return;
                    }
                    p.teleport(player);
                }
            }
        }
    }

    public void addPotion(Player p, PotionEffectType effect, int duration, int power) {
        p.addPotionEffect(new PotionEffect(effect, duration, power));
    }

    public void timber(Block block) {

        if (!(block.getType() == XMaterial.OAK_LOG.parseMaterial()
                || block.getType() == XMaterial.BIRCH_LOG.parseMaterial()
                || block.getType() == XMaterial.ACACIA_LOG.parseMaterial()
                || block.getType() == XMaterial.JUNGLE_LOG.parseMaterial()
                || block.getType() == XMaterial.SPRUCE_LOG.parseMaterial()
                || block.getType() == XMaterial.DARK_OAK_LOG.parseMaterial())) return;

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_WOOD_BREAK, 2f, 1f);
        block.breakNaturally();

        timber(block.getLocation().add(0,1,0).getBlock());
        timber(block.getLocation().add(1,0,0).getBlock());
        timber(block.getLocation().add(0,1,1).getBlock());

        timber(block.getLocation().subtract(0,1,0).getBlock());
        timber(block.getLocation().subtract(1,0,0).getBlock());
        timber(block.getLocation().subtract(0,0,1).getBlock());
    }
    public void runActivityRewards() {
        if (config.getBoolean("rewards-per-time.enabled", true)) {
            int interval = plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getInt("rewards-per-time.interval")*20;
            new ActivityRewards(plugin).runTaskTimer(plugin, 0, interval);
        }
    }
    public void runAutoBroadcast() {
        if (config.getBoolean("auto-broadcast.enabled", true)) {
            int interval = plugin.getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getInt("auto-broadcast.interval")*20;
            new AutoBroadcastMessages(plugin).runTaskTimer(plugin, 0, interval);
        }
    }
    public void checkBorder() {
        if (!plugin.getBorderManager().exist()) {
            plugin.getLogger().info(TextUtil.color("&aBorder was succesfully created!"));
            plugin.getBorderManager().removeBorder();
            plugin.getBorderManager().createBorder();
        }
    }
    public void updateScoreboard(){
        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getOnlinePlayers().stream().filter(player -> PlayerManager.isOnline(player)).forEach(plugin.getScoreboardManager()::updateScoreboard);
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    public void removeScoreboard(){
        Bukkit.getOnlinePlayers().stream().filter(player -> PlayerManager.isOnline(player)).forEach(plugin.getScoreboardManager()::removeScoreboard);
    }

}