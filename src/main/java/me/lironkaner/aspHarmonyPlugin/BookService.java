package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class BookService {
    private final AspHarmonyPlugin plugin;
    private final NamespacedKey bookIdKey;

    public BookService(AspHarmonyPlugin plugin) {
        this.plugin = plugin;
        this.bookIdKey = new NamespacedKey(plugin, "book_id");
    }

    public CompletableFuture<List<Book>> getAllBooks() {
        CompletableFuture<List<Book>> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                List<Book> books = new ArrayList<>();
                try {
                    Bukkit.getServer().getWorlds().forEach(world -> {
                        world.getEntitiesByClass(org.bukkit.entity.Item.class).forEach(item -> {
                            ItemStack itemStack = item.getItemStack();
                            if (itemStack.getType() == Material.WRITTEN_BOOK) {
                                Book book = parseBookFromItemStack(itemStack);
                                if (book != null) {
                                    books.add(book);
                                }
                            }
                        });
                    });
                    future.complete(books);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Error in getAllBooks: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);

        return future;
    }

    public CompletableFuture<Book> getBook(int id) {
        CompletableFuture<Book> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (org.bukkit.World world : Bukkit.getServer().getWorlds()) {
                        for (org.bukkit.entity.Item item : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
                            ItemStack itemStack = item.getItemStack();
                            if (itemStack.getType() == Material.WRITTEN_BOOK) {
                                Book book = parseBookFromItemStack(itemStack);
                                if (book != null && book.getId() == id) {
                                    future.complete(book);
                                    return;
                                }
                            }
                        }
                    }
                    future.complete(null); // Book not found
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Error in getBook: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);

        return future;
    }

    public CompletableFuture<Void> deleteBook(int id) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getServer().getWorlds().forEach(world -> {
                        world.getEntitiesByClass(org.bukkit.entity.Item.class).forEach(item -> {
                            ItemStack itemStack = item.getItemStack();
                            if (itemStack.getType() == Material.WRITTEN_BOOK) {
                                BookMeta meta = (BookMeta) itemStack.getItemMeta();
                                if (meta != null) {
                                    PersistentDataContainer container = meta.getPersistentDataContainer();
                                    if (container.has(bookIdKey, PersistentDataType.INTEGER) &&
                                            container.get(bookIdKey, PersistentDataType.INTEGER) == id) {
                                        item.remove();
                                    }
                                }
                            }
                        });
                    });
                    future.complete(null);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Error in deleteBook: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);

        return future;
    }

    public CompletableFuture<Void> updateBook(Book book) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Bukkit.getServer().getWorlds().forEach(world -> {
                        world.getEntitiesByClass(org.bukkit.entity.Item.class).forEach(item -> {
                            ItemStack itemStack = item.getItemStack();
                            if (itemStack.getType() == Material.WRITTEN_BOOK) {
                                BookMeta meta = (BookMeta) itemStack.getItemMeta();
                                if (meta != null) {
                                    PersistentDataContainer container = meta.getPersistentDataContainer();
                                    if (container.has(bookIdKey, PersistentDataType.INTEGER) &&
                                            container.get(bookIdKey, PersistentDataType.INTEGER) == book.getId()) {
                                        updateBookItemStack(itemStack, book);
                                        item.setItemStack(itemStack);
                                    }
                                }
                            }
                        });
                    });
                    future.complete(null);
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Error in updateBook: " + e.getMessage());
                    e.printStackTrace();
                    future.completeExceptionally(e);
                }
            }
        }.runTask(plugin);

        return future;
    }

    public void createBook(Book book) {
        Bukkit.getLogger().info("Creating book '" + book.getTitle() + "Id: " + book.getId());

        ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
        updateBookItemStack(bookItem, book);

        // Get the main world
        World world = Bukkit.getServer().getWorlds().getFirst();

        // Create a specific location for book creation
        Location bookLocation = new Location(world, 0, 100, 0);

        // Ensure the chunk is loaded
        world.loadChunk(bookLocation.getChunk());

        // Create the book at the specified location
        world.dropItem(bookLocation, bookItem);

        Bukkit.getLogger().info("Created book '" + book.getTitle() + "' at location: " + bookLocation);
    }

    private Book parseBookFromItemStack(ItemStack itemStack) {
        if (itemStack.getType() != Material.WRITTEN_BOOK) return null;

        BookMeta meta = (BookMeta) itemStack.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(bookIdKey, PersistentDataType.INTEGER)) return null;

        int id = container.get(bookIdKey, PersistentDataType.INTEGER);
        String title = meta.getTitle();
        String author = meta.getAuthor();
        List<String> pages = meta.getPages();

        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setDescription(author); // Using author field for description
        book.setImageUrl(""); // No image URL in Minecraft books

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            Chapter chapter = new Chapter();
            chapter.setIndex(i + 1);
            chapter.setTitle("Page " + (i + 1));
            chapter.setContent(pages.get(i));
            chapters.add(chapter);
        }
        book.setChapters(chapters);

        return book;
    }

    private void updateBookItemStack(ItemStack itemStack, Book book) {
        BookMeta meta = (BookMeta) itemStack.getItemMeta();
        if (meta == null) return;

        meta.setTitle(book.getTitle());
        meta.setAuthor(book.getDescription()); // Using description as author

        List<String> pages = new ArrayList<>();
        for (Chapter chapter : book.getChapters()) {
            pages.add(chapter.getContent());
        }
        meta.setPages(pages);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(bookIdKey, PersistentDataType.INTEGER, book.getId());

        itemStack.setItemMeta(meta);
    }
}