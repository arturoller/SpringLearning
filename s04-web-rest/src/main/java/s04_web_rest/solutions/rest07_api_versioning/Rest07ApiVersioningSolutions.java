package s04_web_rest.solutions.rest07_api_versioning;

import java.util.Arrays;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń 1 i 3 lekcji Rest07ApiVersioning — zajrzyj dopiero po własnej próbie! */
public final class Rest07ApiVersioningSolutions {

    private Rest07ApiVersioningSolutions() {
    }

    static String solution1(String version) {
        int[] parts = parse(version);
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    static String solution3(List<String> declared, String requested) {
        int[] wanted = parse(requested);
        String best = null;
        int[] bestVersion = null;
        for (String declaration : declared) {
            int[] version = parse(declaration.endsWith("+") ? declaration.substring(0, declaration.length() - 1) : declaration);
            boolean notNewerThanWanted = Arrays.compare(version, wanted) <= 0;   // porównanie liczbowe po częściach
            if (notNewerThanWanted && (bestVersion == null || Arrays.compare(version, bestVersion) > 0)) {
                best = declaration;
                bestVersion = version;
            }
        }
        if (best == null) {
            return null;                                     // wszystkie zadeklarowane wersje są nowsze niż żądana
        }
        boolean baseline = best.endsWith("+");
        return baseline || Arrays.equals(bestVersion, wanted) ? best : null;
    }

    /** "1.2" → {1, 2, 0}; błędny tekst → IllegalArgumentException. */
    private static int[] parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Pusta wersja");
        }
        String[] texts = version.split("\\.");
        if (texts.length > 3) {
            throw new IllegalArgumentException("Za dużo części wersji: " + version);
        }
        int[] parts = new int[3];
        try {
            for (int i = 0; i < texts.length; i++) {
                parts[i] = Integer.parseInt(texts[i]);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nieczytelna wersja: " + version, e);
        }
        return parts;
    }
}
