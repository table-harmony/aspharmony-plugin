package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.NamespacedKey;

import java.util.List;

public class BookService {
    private final AspHarmonyPlugin plugin;
    private final NamespacedKey bookIdKey;

    public BookService(AspHarmonyPlugin plugin) {
        this.plugin = plugin;
        this.bookIdKey = new NamespacedKey(plugin, "book_id");
    }

    public List<Book> getAllBooks() {
        return null;
    }

    public Book getBook(int id) {
        return null;
    }

    public void deleteBook(int id) {
       return;
    }

    public void updateBook(Book book) {
        return;
    }

    public void createBook(Book book) {
        return;
    }
}