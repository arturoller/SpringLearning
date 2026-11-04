package s11_testing.test05_test_configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfiguracja WARUNKOWA: {@code test05.newsletter.enabled=true} → e-mail, w każdym innym przypadku (także gdy
 * właściwości nie ma) → wersja „nic nie rób”. Takie warunki testujemy lekko przez {@code ApplicationContextRunner}
 * (sekcja 5 lekcji, s03_boot_basics/Boot01AutoConfiguration) — bez uruchamiania całej aplikacji.
 */
@Configuration
public class NewsletterConfig {

    @Bean
    @ConditionalOnProperty(name = "test05.newsletter.enabled", havingValue = "true") // having value = o wartości
    NewsletterSender emailNewsletterSender() {
        return new NewsletterSender.EmailNewsletterSender();
    }

    @Bean
    @ConditionalOnProperty(name = "test05.newsletter.enabled", havingValue = "false", matchIfMissing = true) // brak = też pasuje
    NewsletterSender noopNewsletterSender() {
        return new NewsletterSender.NoopNewsletterSender();
    }
}
