package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.context.annotation.Bean;

/**
 * PRZYKŁAD BŁĘDU (pytanie kontrolne 5): „autokonfiguracja” BEZ {@code @ConditionalOnMissingBean}. Gdy użytkownik ma
 * własny GreetingService, w kontekście są DWA beany tego typu, a każde wstrzyknięcie GreetingService przez konstruktor
 * kończy się błędem NoUniqueBeanDefinitionException (brak jednoznacznego beana).
 * Bez adnotacji i bez wpisu w pliku .imports — używa jej tylko test (Boot02WritingAutoConfigurationTest).
 */
public class Boot02NoBackOffAutoConfiguration {

    @Bean
    GreetingService greetingService() {   // ← brakuje @ConditionalOnMissingBean
        return new DefaultGreetingService("Sklep bez wycofania");
    }
}
