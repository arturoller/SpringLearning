package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * NASZA autokonfiguracja — zarejestrowana w pliku
 * {@code src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 *
 * <ul>
 *   <li>{@code @AutoConfiguration} — „to jest autokonfiguracja”: {@code @Configuration(proxyBeanMethods = false)}
 *       + możliwość ustawienia kolejności (before/after — przed/po innych autokonfiguracjach).</li>
 *   <li>{@code @ConditionalOnProperty(..., havingValue = "true")} — BEZPIECZNIK: bez {@code boot02.greeting.enabled=true}
 *       cała klasa jest pomijana. Plik .imports działa dla KAŻDEJ aplikacji Boota w tym module (wspólny classpath),
 *       więc bez bezpiecznika nasz bean pojawiłby się też w lekcjach Boot01, Boot03 i Boot04.</li>
 *   <li>{@code @EnableConfigurationProperties} — rejestruje Boot02GreetingProperties jako bean z ustawieniami.</li>
 *   <li>{@code @ConditionalOnMissingBean} na metodzie — „Twój bean wygrywa”: gdy użytkownik zdefiniuje własny
 *       GreetingService, nasz domyślny nie powstaje.</li>
 * </ul>
 *
 * <p>Leży w pakiecie skanowanym przez Boot02Application, a mimo to nie jest rejestrowana jako zwykła konfiguracja:
 * filtr AutoConfigurationExcludeFilter z {@code @SpringBootApplication} pomija przy skanowaniu klasy wpisane do
 * pliku .imports.</p>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "boot02.greeting.enabled", havingValue = "true")
@EnableConfigurationProperties(Boot02GreetingProperties.class)
public class Boot02GreetingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GreetingService greetingService(Boot02GreetingProperties properties) {
        return new DefaultGreetingService(properties.shopName());
    }
}
