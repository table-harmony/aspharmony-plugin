package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class StartCommand implements CommandExecutor {
    private final AspHarmonyPlugin plugin;

    public StartCommand(AspHarmonyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.getHttpServer() == null) {
            plugin.startHttpServer();
            sender.sendMessage(ChatColor.GREEN + "Http Server has started.");
        } else {
            sender.sendMessage(ChatColor.RED + "Http Server is already running.");
        }

        return true;
    }
}
