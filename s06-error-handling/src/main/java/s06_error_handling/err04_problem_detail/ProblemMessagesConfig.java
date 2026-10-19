package s06_error_handling.err04_problem_detail;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Źródło komunikatów (MessageSource) tej lekcji: pliki err04_messages*.properties z katalogu resources.
 * Bean o nazwie {@code messageSource} jest wykrywany przez kontekst i przekazywany do ResponseEntityExceptionHandler.
 * Własny bean zamiast właściwości {@code spring.messages.basename}, bo wspólny application.properties modułu jest pusty
 * (każda lekcja dzieli classpath z innymi).
 */
@Configuration(proxyBeanMethods = false)
public class ProblemMessagesConfig {

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("err04_messages");
        source.setDefaultEncoding("UTF-8");
        // Bez tego Spring dla nieznanego języka próbowałby języka SYSTEMU (u każdego innego) — wynik zależałby od komputera.
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
