package me.florixak.uhcrun.hook;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import me.florixak.uhcrun.game.GameValues;
import org.bukkit.Bukkit;

public class ProtocolLibHook {

    public static void setupProtocolLib() {
        if (!hasProtocolLib())
            throw new UnsupportedOperationException("ProtocolLib plugin not found!");

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
    }

    public static boolean hasProtocolLib() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null && GameValues.CAN_USE_PROTOCOLLIB;
    }
}