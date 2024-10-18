package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class AspHarmonyPlugin extends JavaPlugin {
    private BookHttpServer httpServer;

    @Override
    public void onEnable() {
        System.out.println("Starting Plugin");

        BookService bookService = new BookService(this);
        this.httpServer = new BookHttpServer(bookService);
        httpServer.initiate();
    }

    @Override
    public void onDisable() {
        System.out.println("Shutting down Plugin and HTTP Server");
        if (httpServer != null) {
            httpServer.stop();
        }
    }
}