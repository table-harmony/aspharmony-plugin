package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class AspHarmonyPlugin extends JavaPlugin {
    private BookService bookService;
    private BookHttpServer httpServer;

    @Override
    public void onEnable() {
        System.out.println("Starting Plugin");

        this.bookService = new BookService(this);
        this.httpServer = new BookHttpServer(this.bookService);
        httpServer.initiate();
    }

    @Override
    public void onDisable() {
        System.out.println("Shutting down Plugin and HTTP Server");
        if (httpServer != null) {
            httpServer.stop();
        }
    }

    public BookService getBookService() {
        return bookService;
    }
}