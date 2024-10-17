package me.lironkaner.aspHarmonyPlugin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.bukkit.Bukkit;

import static spark.Spark.*;

public class BookHttpServer {
    private final BookService bookService;
    private final Gson gson;

    public BookHttpServer(BookService bookService) {
        this.bookService = bookService;
        this.gson = new Gson();
    }

    public void initiate() {
        port(8000);

        get("/api/books", (req, res) -> {
            res.type("application/json");

            try {
                return gson.toJson(bookService.getAllBooks().get());
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error in GET /api/books endpoint: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Internal server error"));
            }
        });

        get("/api/books/:id", (req, res) -> {
            res.type("application/json");

            try {
                int id = Integer.parseInt(req.params(":id"));
                Book book = bookService.getBook(id).get();

                if (book != null)
                    return gson.toJson(book);

                res.status(404);
                return gson.toJson(new ErrorResponse("Book not found"));
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid book id"));
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error in GET /api/books/:id endpoint: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Internal server error"));
            }
        });

        post("/api/books", (req, res) -> {
            res.type("application/json");

            try {
                Book book = gson.fromJson(req.body(), Book.class);
                bookService.createBook(book);

                res.status(201);
                return gson.toJson(new SuccessResponse("Book created successfully"));
            } catch (JsonSyntaxException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Could not parse book"));
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error in POST /api/books endpoint: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Internal server error"));
            }
        });

        put("/api/books", (req, res) -> {
            res.type("application/json");

            try {
                Book book = gson.fromJson(req.body(), Book.class);
                bookService.updateBook(book).get();
                return gson.toJson(new SuccessResponse("Book updated successfully"));
            } catch (JsonSyntaxException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Could not parse book"));
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error in PUT /api/books endpoint: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Internal server error"));
            }
        });

        delete("/api/books/:id", (req, res) -> {
            res.type("application/json");

            try {
                int id = Integer.parseInt(req.params(":id"));

                bookService.deleteBook(id).get();
                return gson.toJson(new SuccessResponse("Book deleted successfully"));
            } catch (NumberFormatException e) {
                res.status(400);
                return gson.toJson(new ErrorResponse("Invalid book id"));
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error in DELETE /api/books/:id endpoint: " + e.getMessage());
                e.printStackTrace();
                res.status(500);
                return gson.toJson(new ErrorResponse("Internal server error"));
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