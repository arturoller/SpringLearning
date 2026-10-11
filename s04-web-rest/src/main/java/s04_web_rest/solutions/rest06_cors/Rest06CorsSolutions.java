package s04_web_rest.solutions.rest06_cors;

import java.net.URI;
import java.util.Locale;

/** Rozwiązania wzorcowe ćwiczeń 1–2 lekcji Rest06Cors — zajrzyj dopiero po własnej próbie! */
public final class Rest06CorsSolutions {

    private Rest06CorsSolutions() {
    }

    static String solution1(String url) {
        URI uri = URI.create(url);
        String origin = uri.getScheme() + "://" + uri.getHost();
        return uri.getPort() == -1 ? origin : origin + ":" + uri.getPort();
    }

    static boolean solution2(String url1, String url2) {
        URI a = URI.create(url1);
        URI b = URI.create(url2);
        String schemeA = a.getScheme().toLowerCase(Locale.ROOT);
        String schemeB = b.getScheme().toLowerCase(Locale.ROOT);
        return schemeA.equals(schemeB)
                && a.getHost().equalsIgnoreCase(b.getHost())
                && effectivePort(schemeA, a.getPort()) == effectivePort(schemeB, b.getPort());
    }

    /** Port jawny albo domyślny dla schematu: http → 80, https → 443. */
    private static int effectivePort(String scheme, int port) {
        if (port != -1) {
            return port;
        }
        return scheme.equals("https") ? 443 : 80;
    }
}
