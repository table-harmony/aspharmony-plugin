package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StopCommand implements CommandExecutor {
    private final AspHarmonyPlugin plugin;

    public StopCommand(AspHarmonyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getHttpServer() != null) {
            plugin.stopHttpServer();
            sender.sendMessage(ChatColor.GREEN + "Http Server has stopped.");
        } else {
            sender.sendMessage(ChatColor.RED + "No Http Server is running.");
        }

        return true;
    }
}
