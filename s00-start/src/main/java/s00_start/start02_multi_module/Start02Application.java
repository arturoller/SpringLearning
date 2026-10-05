package s00_start.start02_multi_module;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Start02MultiModule. Pusta aplikacja — nie ma żadnego własnego beana. Wszystko, co pojawi się
 * w jej kontekście, dodała AUTOKONFIGURACJA Spring Boota na podstawie bibliotek z classpath modułu s00-start.
 * Właśnie to sprawdza test Start02MultiModuleTest.
 */
@SpringBootApplication
public class Start02Application {

    public static void main(String[] args) {
        SpringApplication.run(Start02Application.class, args);
    }
}
