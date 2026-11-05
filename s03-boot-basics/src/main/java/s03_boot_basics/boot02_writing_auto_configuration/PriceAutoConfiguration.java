package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autokonfiguracja mini-biblioteki cen. Zarejestrowana w pliku
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} (sekcja 2 lekcji).
 * <ul>
 *   <li>{@code @AutoConfiguration} — „to jest klasa autokonfiguracji” (odmiana {@code @Configuration}
 *       bez pośredników metod @Bean: proxyBeanMethods = false).</li>
 *   <li>{@code @ConditionalOnProperty(... havingValue = "true")} — przełącznik {@code boot02.price.enabled};
 *       bez niego klasa NIC nie robi (brak klucza = wyłączona).</li>
 *   <li>{@code @EnableConfigurationProperties} — rejestruje rekord {@link PriceProperties} jako bean z ustawieniami.</li>
 *   <li>{@code @ConditionalOnMissingBean} na metodzie — ustępowanie (back-off) przed beanem użytkownika.</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(name = "boot02.price.enabled", havingValue = "true")
@EnableConfigurationProperties(PriceProperties.class) // enable configuration properties = włącz wiązanie ustawień
public class PriceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean // brak PriceFormatter użytkownika → tworzymy nasz; jest → ustępujemy
    PriceFormatter priceFormatter(PriceProperties properties) {
        return new PriceFormatter(properties.currency(), properties.decimals());
    }
}
