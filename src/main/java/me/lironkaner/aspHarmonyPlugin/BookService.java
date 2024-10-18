package me.lironkaner.aspHarmonyPlugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
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

    // Get the spawn chunk of the default world
    private Chunk getSpawnChunk() {
        World world = Bukkit.getServer().getWorlds().get(0);  // Get the default world
        Location spawnLocation = world.getSpawnLocation();     // Get the world spawn location
        return world.getChunkAt(spawnLocation);                // Get the chunk at the spawn location
    }

    // Get all books stored in containers in the spawn chunk
    public CompletableFuture<List<Book>> getAllBooks() {
        return CompletableFuture.supplyAsync(() -> {
            List<Book> books = new ArrayList<>();
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    Chunk storageChunk = getSpawnChunk();  // Get the spawn chunk

                    // Search in the spawn chunk's container blocks (chests, shulker boxes, etc.)
                    for (Block block : getAllBlocksInChunk(storageChunk)) {
                        if (block.getState() instanceof Chest chest) {
                            searchInventory(chest.getInventory(), books);
                        } else if (block.getState() instanceof ShulkerBox shulkerBox) {
                            searchInventory(shulkerBox.getInventory(), books);
                        }
                    }

                    return books;
                }).get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Failed to fetch books", e);
            }
        });
    }

    // Get a specific book by ID from the spawn chunk
    public CompletableFuture<Book> getBook(int id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    Chunk storageChunk = getSpawnChunk();  // Get the spawn chunk

                    // Search in the spawn chunk's container blocks
                    for (Block block : getAllBlocksInChunk(storageChunk)) {
                        if (block.getState() instanceof Chest chest) {
                            ItemStack bookItem = findBookInInventory(chest.getInventory(), id);
                            if (bookItem != null) {
                                BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
                                return parseItemToBook(bookMeta);
                            }
                        } else if (block.getState() instanceof ShulkerBox shulkerBox) {
                            ItemStack bookItem = findBookInInventory(shulkerBox.getInventory(), id);
                            if (bookItem != null) {
                                BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
                                return parseItemToBook(bookMeta);
                            }
                        }
                    }

                    return null;
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Failed to get book", e);
            }
        });
    }

    // Create a new book and drop it in the world at the spawn location
    public void createBook(Book book) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack bookItem = new ItemStack(Material.WRITABLE_BOOK);
            BookMeta bookMeta = (BookMeta) bookItem.getItemMeta();
            if (bookMeta == null) return;

            // Set the book's details
            bookMeta.setTitle(book.getTitle());
            bookMeta.setAuthor("Minecraft Library");
            bookMeta.getPersistentDataContainer().set(bookIdKey, PersistentDataType.INTEGER, book.getId());

            // Add description and image URL as the first page
            bookMeta.addPage("Description: " + book.getDescription() + "\n\n---\n\n" + "Image URL: " + book.getImageUrl());

            // Add chapters as pages
            for (Chapter chapter : book.getChapters()) {
                bookMeta.addPage(chapter.getContent());
            }

            bookItem.setItemMeta(bookMeta);

            // Drop the book at the spawn location
            World world = Bukkit.getServer().getWorlds().get(0);  // Get the default world
            Location spawnLocation = world.getSpawnLocation();
            world.dropItemNaturally(spawnLocation, bookItem);
        });
    }

    // Update book by ID, modifies the book's content
    public void updateBook(Book book) {
        CompletableFuture.runAsync(() -> {
            try {
                Book existingBook = getBook(book.getId()).get();  // Get the book by ID
                if (existingBook == null) throw new RuntimeException("Book not found");

                deleteBook(book.getId());
                createBook(book);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Error updating book", e);
            }
        });
    }

    // Delete a book by ID from the spawn chunk
    public void deleteBook(int id) {
        CompletableFuture.runAsync(() -> {
            try {
                Book book = getBook(id).get();
                if (book == null) throw new RuntimeException("Book not found");

                Chunk storageChunk = getSpawnChunk();  // Get the spawn chunk

                // Search in the spawn chunk's container blocks
                for (Block block : getAllBlocksInChunk(storageChunk)) {
                    if (block.getState() instanceof Chest chest) {
                        ItemStack bookItem = findBookInInventory(chest.getInventory(), id);
                        if (bookItem != null) {
                            chest.getInventory().remove(bookItem);  // Remove the book from chest
                            return;
                        }
                    } else if (block.getState() instanceof ShulkerBox shulkerBox) {
                        ItemStack bookItem = findBookInInventory(shulkerBox.getInventory(), id);
                        if (bookItem != null) {
                            shulkerBox.getInventory().remove(bookItem);  // Remove the book from shulker box
                            return;
                        }
                    }
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Error deleting book", e);
            }
        });
    }

    // Helper method to search through an inventory and collect books
    private void searchInventory(Inventory inventory, List<Book> books) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (isBookItem(itemStack)) {
                BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
                if (bookMeta != null) {
                    books.add(parseItemToBook(bookMeta));
                }
            }
        }
    }

    // Helper method to find a specific book by ID in an inventory
    private ItemStack findBookInInventory(Inventory inventory, int bookId) {
        for (ItemStack itemStack : inventory.getContents()) {
            if (isBookItem(itemStack)) {
                BookMeta bookMeta = (BookMeta) itemStack.getItemMeta();
                if (bookMeta != null && bookMeta.getPersistentDataContainer().has(bookIdKey, PersistentDataType.INTEGER)) {
                    int storedId = bookMeta.getPersistentDataContainer().get(bookIdKey, PersistentDataType.INTEGER);
                    if (storedId == bookId) {
                        return itemStack;
                    }
                }
            }
        }
        return null;
    }

    // Helper method to get all blocks in a given chunk
    private List<Block> getAllBlocksInChunk(Chunk chunk) {
        List<Block> blocks = new ArrayList<>();
        for (int x = 0; x < 16; x++) {
            for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    // Helper method to check if an item is a valid book
    private boolean isBookItem(ItemStack itemStack) {
        return itemStack != null && (itemStack.getType() == Material.WRITTEN_BOOK || itemStack.getType() == Material.WRITABLE_BOOK);
    }

    // Helper method to convert a BookMeta to a Book object
    private Book parseItemToBook(BookMeta bookMeta) {
        Book book = new Book();
        book.setId(bookMeta.getPersistentDataContainer().get(bookIdKey, PersistentDataType.INTEGER));
        book.setTitle(bookMeta.getTitle());

        // Extract the description and image URL from the first page
        if (bookMeta.hasPages()) {
            String firstPage = bookMeta.getPage(1);
            String[] parts = firstPage.split("\n\n---\n\n");
            book.setDescription(parts.length > 0 ? parts[0].replace("Description: ", "") : "");
            book.setImageUrl(parts.length > 1 ? parts[1].replace("Image URL: ", "") : "");
        }

        // Extract chapters from the remaining pages
        List<Chapter> chapters = new ArrayList<>();
        for (int i = 1; i < bookMeta.getPages().size(); i++) {
            Chapter chapter = new Chapter();
            chapter.setContent(bookMeta.getPage(i + 1));
            chapters.add(chapter);
        }
        book.setChapters(chapters);
        return book;
    }
}
