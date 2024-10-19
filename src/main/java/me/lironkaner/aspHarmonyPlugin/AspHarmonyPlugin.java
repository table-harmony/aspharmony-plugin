package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class AspHarmonyPlugin extends JavaPlugin {
    private BookHttpServer httpServer;

    @Override
    public void onEnable() {
        System.out.println("Starting Plugin");

        startHttpServer();

        Objects.requireNonNull(this.getCommand("stopHttpServer")).setExecutor(new StopCommand(this));
        Objects.requireNonNull(this.getCommand("startHttpServer")).setExecutor(new StartCommand(this));
    }

    @Override
    public void onDisable() {
        System.out.println("Shutting down Plugin and HTTP Server");
        stopHttpServer();
    }

    public BookHttpServer getHttpServer() {
        return httpServer;
    }

    public void stopHttpServer() {
        if (httpServer == null)
            return;

        httpServer.stop();
        httpServer = null;
    }

    public void startHttpServer() {
        if (httpServer != null)
            return;

        BookService bookService = new BookService(this);
        httpServer = new BookHttpServer(bookService);

        httpServer.start();
    }
}