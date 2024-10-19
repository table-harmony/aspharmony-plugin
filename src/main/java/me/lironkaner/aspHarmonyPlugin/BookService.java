package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BookService {
    private final AspHarmonyPlugin plugin;
    private final NamespacedKey bookIdKey;

    public BookService(AspHarmonyPlugin plugin) {
        this.plugin = plugin;
        this.bookIdKey = new NamespacedKey(plugin, "book_id");
    }

    public CompletableFuture<List<Book>> getAllBooks() {
        return CompletableFuture.supplyAsync(() -> {
            List<Book> books = new ArrayList<>();

            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    Chunk storageChunk = getSpawnChunk();

                    for (Container container: getChunkContainers(storageChunk)) {
                        List<Book> inventoryBooks = getInventoryBooks(container.getInventory());
                        books.addAll(inventoryBooks);
                    }

                    return books;
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to fetch books", e);
            }
        });
    }

    public CompletableFuture<Book> getBook(int id) {
        return CompletableFuture.supplyAsync(() -> {

            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    Chunk storageChunk = getSpawnChunk();

                    for (Container container : getChunkContainers(storageChunk)) {
                        ItemStack bookItem = searchInventory(container.getInventory(), id);
                        if (bookItem == null) continue;

                        BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
                        assert bookMeta != null;

                        return parseItemToBook(bookMeta);
                    }

                    return null;
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to get book", e);
            }
        });
    }

    public void createBook(Book book) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack bookItem = new ItemStack(Material.WRITABLE_BOOK);
            BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();

            if (bookMeta == null) return;

            bookMeta.getPersistentDataContainer().set(bookIdKey, PersistentDataType.INTEGER, book.getId());

            String page = "Title: " + book.getTitle() +
                    "\n\n---\n\n" +
                    "Description: " + book.getDescription() +
                    "\n\n---\n\n" +
                    "Image URL: " + book.getImageUrl();
            bookMeta.addPage(page);

            for (Chapter chapter : book.getChapters()) {
                String chapterPage = chapter.getTitle() +
                        "\n\n---\n\n" +
                        chapter.getContent();

                bookMeta.addPage(chapterPage);
            }

            bookItem.setItemMeta(bookMeta);

            World world = Bukkit.getServer().getWorlds().getFirst();
            Location spawnLocation = new Location(world, 16.5, 74, 16.5);
            world.dropItem(spawnLocation, bookItem);
        });
    }

    public void updateBook(Book book) {
        CompletableFuture.runAsync(() -> {
            try {
                Book existingBook = getBook(book.getId()).get();
                if (existingBook == null) throw new RuntimeException("Book not found");

                deleteBook(book.getId());
                createBook(book);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Error updating book", e);
            }
        });
    }

    public void deleteBook(int id) {
        CompletableFuture.runAsync(() -> {
            try {
                Book existingBook = getBook(id).get();  // Fetch the book based on ID
                if (existingBook == null) throw new RuntimeException("Book not found");

                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    Chunk storageChunk = getSpawnChunk();

                    for (Container container : getChunkContainers(storageChunk)) {
                        Inventory inventory = container.getInventory();

                        ItemStack bookItem = searchInventory(inventory, id);
                        if (bookItem == null) {
                            System.out.println("Book not found in this container.");
                            continue;
                        }

                        System.out.println("Book found and will be removed: " + bookItem);

                        inventory.remove(bookItem);

                        Chest chest = (Chest) container.getBlock().getState();
                        chest.update();
                        System.out.println("Chest updated, book removed.");

                        return null;
                    }
                    return null;
                }).get();

            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Error deleting book", e);
            }
        });
    }


    private List<Book> getInventoryBooks(Inventory inventory) {
        List<Book> books = new ArrayList<>();

        for (ItemStack itemStack : inventory.getContents()) {
            if (!isBookItem(itemStack))
                continue;

            BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();

            if (bookMeta != null) {
                books.add(parseItemToBook(bookMeta));
            }
        }

        return books;
    }

    private ItemStack searchInventory(Inventory inventory, int bookId) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (!isBookItem(itemStack))
                continue;

            BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();

            if (bookMeta == null || !bookMeta.getPersistentDataContainer().has(bookIdKey, PersistentDataType.INTEGER))
                continue;

            int storedId = bookMeta.getPersistentDataContainer().get(bookIdKey, PersistentDataType.INTEGER);

            if (storedId != bookId)
                continue;

            return itemStack;
        }
        return null;
    }

    private List<Container> getChunkContainers(Chunk chunk) {
        List<Container> blocks = new ArrayList<>();

        for (int x = 0; x < 16; x++) {
            for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);

                    if (block.getState() instanceof Container container)
                        blocks.add(container);
                }
            }
        }

        return blocks;
    }

    private Chunk getSpawnChunk() {
        World world = Bukkit.getServer().getWorlds().getFirst();
        Location spawnLocation = world.getSpawnLocation();
        return world.getChunkAt(spawnLocation);
    }

    private boolean isBookItem(ItemStack itemStack) {
        return itemStack != null && (itemStack.getType() == Material.WRITTEN_BOOK || itemStack.getType() == Material.WRITABLE_BOOK);
    }

    private Book parseItemToBook(BookMeta bookMeta) {
        Book book = new Book();
        book.setId(bookMeta.getPersistentDataContainer().get(bookIdKey, PersistentDataType.INTEGER));
        book.setTitle(bookMeta.getTitle());

        if (!bookMeta.hasPages())
            return null;

        String page = bookMeta.getPages().getFirst();
        String[] parts = page.split("\n\n---\n\n");
        book.setTitle(parts.length > 0 ? parts[0].replace("Title: ", "") : "");
        book.setDescription(parts.length > 1 ? parts[1].replace("Description: ", "") : "");
        book.setImageUrl(parts.length > 2 ? parts[2].replace("Image URL: ", "") : "");

        List<Chapter> chapters = new ArrayList<>();
        for (int i = 2; i <= bookMeta.getPages().size(); i++) {
            Chapter chapter = new Chapter();

            String chapterPage = bookMeta.getPage(i);
            String[] chapterParts = chapterPage.split("\n\n---\n\n");

            chapter.setTitle(chapterParts.length > 0 ? chapterParts[0] : "");
            chapter.setContent(chapterParts.length > 1 ? chapterParts[1] : "");

            chapters.add(chapter);
        }

        book.setChapters(chapters);
        return book;
    }
}