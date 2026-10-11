package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Boot01AutoConfiguration. Celowo PUSTA — nie definiuje żadnego własnego beana.
 * Wszystko, co znajdziesz w jej kontekście poza nią samą, dołożyła autokonfiguracja (auto-configuration).
 *
 * <p>Uruchom ją z argumentem {@code --debug} (IntelliJ: Run → Edit Configurations → Program arguments), a w logu
 * zobaczysz raport warunków: „CONDITIONS EVALUATION REPORT” z listami „Positive matches” i „Negative matches”.</p>
 */
@SpringBootApplication
public class Boot01Application {

    public static void main(String[] args) {
        SpringApplication.run(Boot01Application.class, args);
    }
}
