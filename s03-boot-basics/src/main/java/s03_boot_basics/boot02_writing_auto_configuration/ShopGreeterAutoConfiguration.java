package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autokonfiguracja „biblioteki sklepu” — tworzy bean {@link ShopGreeter}, jeśli:
 * <ol>
 *   <li>klasa {@code ShopGreeter} jest na ścieżce klas ({@code @ConditionalOnClass}) — w prawdziwej bibliotece to
 *       sprawdzenie, czy użytkownik dołączył jar z kodem biblioteki;</li>
 *   <li>{@code s03.greeter.enabled=true} ({@code @ConditionalOnBooleanProperty}; brak klucza = wyłączona);</li>
 *   <li>użytkownik nie zdefiniował własnego beana {@code ShopGreeter} ({@code @ConditionalOnMissingBean}).</li>
 * </ol>
 * Zarejestrowana w pliku
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} (src/main/resources).
 * Leży w pakiecie lekcji, a mimo to {@code @SpringBootApplication} jej NIE skanuje — pilnuje tego
 * {@code AutoConfigurationExcludeFilter} (lekcja, sekcja 3).
 */
@AutoConfiguration
@ConditionalOnClass(ShopGreeter.class)
@ConditionalOnBooleanProperty("s03.greeter.enabled")
@EnableConfigurationProperties(ShopGreeterProperties.class)
public class ShopGreeterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ShopGreeter shopGreeter(ShopGreeterProperties properties) {
        return new ShopGreeter(properties.shopName(), properties.greeting());
    }
}
