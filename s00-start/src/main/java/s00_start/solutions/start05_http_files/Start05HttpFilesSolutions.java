package s00_start.solutions.start05_http_files;

import s00_start.start05_http_files.Start05HttpFiles.HttpRequestLine;

import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Start05HttpFiles — zajrzyj dopiero po własnej próbie! */
public final class Start05HttpFilesSolutions {

    private static final Set<String> METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private Start05HttpFilesSolutions() {
    }

    static String solution1(String method, String path) {
        return method + " http://localhost:8080" + path;
    }

    static String solution2(int statusCode) {
        return switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 415 -> "Unsupported Media Type";
            case 500 -> "Internal Server Error";
            default -> "Inny kod: " + statusCode;
        };
    }

    static HttpRequestLine solution3(String line) {
        String[] parts = line.strip().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Linia zapytania to: METODA adres — a jest: " + line);
        }
        String method = parts[0];
        String url = parts[1];
        if (!METHODS.contains(method)) {
            throw new IllegalArgumentException("Nieznana metoda HTTP: " + method);
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("Adres musi zaczynać się od http:// albo https://: " + url);
        }
        return new HttpRequestLine(method, url);
    }
}
