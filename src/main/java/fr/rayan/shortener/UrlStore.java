package fr.rayan.shortener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Stockage en mémoire, sûr en accès concurrent. */
public class UrlStore {
    static final int MAX_URL_LENGTH = 2048;
    // On démarre à 62^3 pour que les codes fassent au moins 4 caractères.
    private static final long FIRST_ID = 62L * 62 * 62;

    private final AtomicLong nextId = new AtomicLong(FIRST_ID);
    private final Map<String, String> urlsByCode = new ConcurrentHashMap<>();
    private final Map<String, String> codesByUrl = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> clicks = new ConcurrentHashMap<>();

    /** Retourne le code court de l'URL (le même si elle a déjà été raccourcie). */
    public String shorten(String rawUrl) {
        String url = validate(rawUrl);
        return codesByUrl.computeIfAbsent(url, u -> {
            String code = Base62.encode(nextId.getAndIncrement());
            urlsByCode.put(code, u);
            clicks.put(code, new AtomicLong());
            return code;
        });
    }

    /** Résout un code et compte la visite. */
    public Optional<String> resolve(String code) {
        String url = urlsByCode.get(code);
        if (url != null) clicks.get(code).incrementAndGet();
        return Optional.ofNullable(url);
    }

    public Optional<Long> clickCount(String code) {
        AtomicLong n = clicks.get(code);
        return n == null ? Optional.empty() : Optional.of(n.get());
    }

    static String validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) throw new IllegalArgumentException("URL vide");
        String url = rawUrl.trim();
        if (url.length() > MAX_URL_LENGTH) throw new IllegalArgumentException("URL trop longue");
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("seules les URL http et https sont acceptées");
            }
            if (uri.getHost() == null) throw new IllegalArgumentException("URL sans nom de domaine");
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL mal formée", e);
        }
        return url;
    }
}
