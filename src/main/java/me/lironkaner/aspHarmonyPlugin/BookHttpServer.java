package me.lironkaner.aspHarmonyPlugin;

import com.google.gson.Gson;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.concurrent.CompletionException;

import static spark.Spark.*;

public class BookHttpServer {
    private final Gson gson = new Gson();
    private final BookService bookService;

    public BookHttpServer(BookService bookService) {
        this.bookService = bookService;
    }

    public void start() {
        port(8000);

        get("/", (req, res) -> "AspHarmony spark service.");

        get("/api/books", (req, res) -> {
            res.type("application/json");

            List<Book> books = bookService.getAllBooks().get();
            return gson.toJson(books);
        });

        get("/api/books/:id", (req, res) -> {
            res.type("application/json");

            int id = Integer.parseInt(req.params(":id"));
            Book book = bookService.getBook(id).get();

            if (book != null) {
                return gson.toJson(book);
            }

            res.status(404);
            return gson.toJson(new ErrorResponse("Book not found"));
        });

        post("/api/books", (req, res) -> {
            res.type("application/json");

            Book book = gson.fromJson(req.body(), Book.class);
            bookService.createBook(book);

            res.status(201);
            return gson.toJson(new SuccessResponse("Book created successfully"));
        });

        put("/api/books", (req, res) -> {
            res.type("application/json");

            Book book = gson.fromJson(req.body(), Book.class);
            bookService.updateBook(book);

            res.status(200);
            return gson.toJson(new SuccessResponse("Book updated successfully"));
        });

        delete("/api/books/:id", (req, res) -> {
            res.type("application/json");

            int id = Integer.parseInt(req.params(":id"));

            try {
                bookService.deleteBook(id);
                res.status(200);
                return gson.toJson(new SuccessResponse("Book deleted successfully"));
            } catch (CompletionException e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Unexpected error: " + e.getCause().getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Unexpected error: " + e.getMessage()));
            }
        });

        exception(Exception.class, (e, req, res) -> {
            Bukkit.getLogger().severe("Unhandled exception: " + e.getMessage());
            e.printStackTrace();
            res.status(500);
            res.body(gson.toJson(new ErrorResponse("Internal server error")));
        });

        System.out.println("HTTP Server started on port 8000");
    }

    public void stop() {
        spark.Spark.stop();
        System.out.println("HTTP Server stopped");
    }

    private static class ErrorResponse {
        String error;
        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    private static class SuccessResponse {
        String message;
        public SuccessResponse(String message) {
            this.message = message;
        }
    }
}
