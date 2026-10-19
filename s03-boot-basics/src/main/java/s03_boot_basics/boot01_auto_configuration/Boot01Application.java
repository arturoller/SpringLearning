package s03_boot_basics.boot01_auto_configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Boot01AutoConfiguration. Nie definiuje ŻADNEGO beana Jacksona — a mimo to
 * {@link ProductJsonService} dostaje gotowy {@code JsonMapper}. Skąd? Z autokonfiguracji (lekcja).
 *
 * <p>Uruchom ▶ z argumentem programu {@code --debug} (IntelliJ: Run → Edit Configurations → Program arguments),
 * a przed napisem „Started …” zobaczysz „CONDITIONS EVALUATION REPORT” — raport warunków autokonfiguracji.</p>
 *
 * <p>Na ścieżce klas (classpath) tego modułu nie ma serwera WWW, więc Boot sam wybiera typ aplikacji NONE:
 * kontekst powstaje, kod się wykonuje, aplikacja się kończy.</p>
 */
@SpringBootApplication
public class Boot01Application {

    public static void main(String[] args) {
        // try-with-resources: kontekst zamknie się sam po wypisaniu wyniku
        try (ConfigurableApplicationContext context = SpringApplication.run(Boot01Application.class, args)) {
            ProductJsonService json = context.getBean(ProductJsonService.class);
            System.out.println(json.toJson(new Product("ELE-001", "Laptop Pro 14", new BigDecimal("5499.99"))));
        }
    }
}
