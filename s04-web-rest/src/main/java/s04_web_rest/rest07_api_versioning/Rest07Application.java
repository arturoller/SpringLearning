package s04_web_rest.rest07_api_versioning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Rest07ApiVersioning. Wersjonowanie API włącza ApiVersioningConfig z tego samego pakietu
 * (inne lekcje go nie widzą). Zapytania: s04-web-rest/http/rest07_api_versioning.http — każde z nagłówkiem API-Version.
 */
@SpringBootApplication
public class Rest07Application {

    public static void main(String[] args) {
        SpringApplication.run(Rest07Application.class, args);
    }
}
