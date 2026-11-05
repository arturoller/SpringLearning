package s12_security.sec01_pitfalls.defaults;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sekcja 1 lekcji Sec01Basics: aplikacja ze starterem security i BEZ żadnej konfiguracji bezpieczeństwa — tylko jeden
 * kontroler. Wszystko, co działa, pochodzi z autokonfiguracji Boota.
 */
@SpringBootApplication
@RestController
public class DefaultsApplication {

    @GetMapping("/sec01/defaults/hello")
    public String hello() {
        return "Witaj w sklepie";
    }
}
