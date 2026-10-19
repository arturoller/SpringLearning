package s11_testing.test05_test_configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Lekcja Test05TestConfiguration, sekcja 1 — PUŁAPKA na żywo: zwykła {@code @Configuration} (NIE
 * {@code @TestConfiguration}) położona w {@code src/test/java} w pakiecie lekcji. Skanowanie komponentów
 * {@code Test05Application} JĄ ZNAJDUJE, więc jej bean trafia do kontekstu KAŻDEGO testu tej lekcji — także tych,
 * które o niej nic nie wiedzą. Tu bean jest nieszkodliwy (tekst), ale wyobraź sobie podmieniony DataSource albo zegar.
 */
@Configuration
class AccidentallyScannedConfiguration {

    @Bean
    String accidentallyScanned() {
        return "jestem w każdym kontekście tej lekcji";
    }
}
