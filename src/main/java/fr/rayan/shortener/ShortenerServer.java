package fr.rayan.shortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * API HTTP :
 *   POST /api/shorten        corps = URL en texte brut -> 201 + URL courte
 *   GET  /api/stats/{code}   -> nombre de clics
 *   GET  /{code}             -> redirection 302 vers l'URL d'origine
 *   GET  /                   -> petite page web
 */
public class ShortenerServer implements AutoCloseable {
    private static final int THREADS = 8;
    private static final String CODE_PATTERN = "[0-9a-zA-Z]{1,11}";

    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(THREADS);
    private final UrlStore store = new UrlStore();

    public ShortenerServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/shorten", this::handleShorten);
        server.createContext("/api/stats/", this::handleStats);
        server.createContext("/", this::handleRoot);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void handleShorten(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) {
            send(ex, 405, "text/plain", "Méthode non autorisée");
            return;
        }
        String body;
        try (InputStream in = ex.getRequestBody()) {
            byte[] bytes = in.readNBytes(UrlStore.MAX_URL_LENGTH + 1); // on ne lit jamais plus que nécessaire
            body = new String(bytes, StandardCharsets.UTF_8);
        }
        try {
            String code = store.shorten(body);
            String host = ex.getRequestHeaders().getFirst("Host");
            send(ex, 201, "text/plain", "http://" + (host != null ? host : "localhost:" + port()) + "/" + code);
        } catch (IllegalArgumentException e) {
            send(ex, 400, "text/plain", e.getMessage());
        }
    }

    private void handleStats(HttpExchange ex) throws IOException {
        String code = ex.getRequestURI().getPath().substring("/api/stats/".length());
        if (!code.matches(CODE_PATTERN)) {
            send(ex, 404, "text/plain", "Code inconnu");
            return;
        }
        store.clickCount(code).ifPresentOrElse(
                n -> sendQuietly(ex, 200, "application/json", "{\"code\":\"" + code + "\",\"clics\":" + n + "}"),
                () -> sendQuietly(ex, 404, "text/plain", "Code inconnu"));
    }

    private void handleRoot(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            send(ex, 200, "text/html; charset=utf-8", HomePage.HTML);
            return;
        }
        String code = path.substring(1);
        var url = code.matches(CODE_PATTERN) ? store.resolve(code) : java.util.Optional.<String>empty();
        if (url.isEmpty()) {
            send(ex, 404, "text/plain", "Lien introuvable");
            return;
        }
        ex.getResponseHeaders().set("Location", url.get());
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type.contains("charset") ? type : type + "; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void sendQuietly(HttpExchange ex, int status, String type, String body) {
        try {
            send(ex, status, type, body);
        } catch (IOException e) {
            System.err.println("Erreur d'envoi de la réponse : " + e.getMessage());
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
