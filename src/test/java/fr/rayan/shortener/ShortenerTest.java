package fr.rayan.shortener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShortenerTest {
    private ShortenerServer server;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @BeforeEach
    void start() throws Exception {
        server = new ShortenerServer(0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    private String base() {
        return "http://localhost:" + server.port();
    }

    private HttpResponse<String> post(String body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(base() + "/api/shorten"))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base() + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void shortenThenRedirectAndCountClicks() throws Exception {
        var created = post("https://fr.wikipedia.org/wiki/Langage_C");
        assertEquals(201, created.statusCode());
        String shortUrl = created.body();
        String code = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);

        var redirect = get("/" + code);
        assertEquals(302, redirect.statusCode());
        assertEquals("https://fr.wikipedia.org/wiki/Langage_C", redirect.headers().firstValue("Location").orElseThrow());
        get("/" + code);

        var stats = get("/api/stats/" + code);
        assertEquals(200, stats.statusCode());
        assertTrue(stats.body().contains("\"clics\":2"), stats.body());
    }

    @Test
    void sameUrlGivesSameCode() throws Exception {
        assertEquals(post("https://example.com").body(), post("https://example.com").body());
        assertNotEquals(post("https://example.com/a").body(), post("https://example.com/b").body());
    }

    @Test
    void rejectsInvalidUrls() throws Exception {
        assertEquals(400, post("javascript:alert(1)").statusCode());
        assertEquals(400, post("ftp://example.com").statusCode());
        assertEquals(400, post("pas une url").statusCode());
        assertEquals(400, post("").statusCode());
        assertEquals(400, post("https://example.com/" + "a".repeat(3000)).statusCode());
    }

    @Test
    void unknownCodesReturn404() throws Exception {
        assertEquals(404, get("/zzzzzz").statusCode());
        assertEquals(404, get("/api/stats/zzzzzz").statusCode());
        assertEquals(404, get("/api/stats/%22x").statusCode());
        assertEquals(405, get("/api/shorten").statusCode());
        assertEquals(200, get("/").statusCode());
    }

    @Test
    void base62RoundTrip() {
        for (long v : new long[] {0, 1, 61, 62, 238_328, Long.MAX_VALUE}) assertEquals(v, Base62.decode(Base62.encode(v)));
        assertEquals("10", Base62.encode(62));
        assertThrows(IllegalArgumentException.class, () -> Base62.decode("a-b"));
    }
}
