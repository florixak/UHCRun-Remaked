package me.florixak.uhcrun.game.kits;

import me.florixak.uhcrun.player.UHCPlayer;
import me.florixak.uhcrun.utils.text.TextUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class Kit {

    private final String name;
    private final String displayName;
    private final List<ItemStack> items;
    private final Material displayItem;
    private final double cost;

    public Kit(String name, String displayName, Material displayItem, double cost, List<ItemStack> items) {
        this.name = name;
        this.displayName = displayName;
        this.displayItem = displayItem;
        this.cost = cost;
        this.items = items;
    }

    public String getName() {
        return this.name;
    }

    public String getDisplayName() {
        return TextUtils.color(this.displayName);
    }

    public Material getDisplayItem() {
        return this.displayItem;
    }

    public double getCost() {
        return this.cost;
    }

    public boolean isFree() {
        return getCost() == 0;
    }

    public List<ItemStack> getItems() {
        return this.items;
    }

    public void giveKit(UHCPlayer uhcPlayer) {
        Player p = uhcPlayer.getPlayer();

        for (ItemStack item : getItems()) {
            p.getInventory().addItem(item);
        }
    }
}
