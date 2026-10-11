package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ustawienia naszej autokonfiguracji (properties = właściwości), prefiks {@code boot02.greeting}.
 * Rekord jest niezmienny; {@code @DefaultValue} (wartość domyślna) podaje nazwę sklepu, gdy właściwości brak.
 * Więcej o {@code @ConfigurationProperties} — lekcja s02_configuration/Cfg02ConfigurationProperties.
 *
 * @param enabled  przełącznik {@code boot02.greeting.enabled}; tu tylko dla porządku (i podpowiedzi w IDE) —
 *                 naprawdę czyta go warunek {@code @ConditionalOnProperty} na klasie autokonfiguracji
 * @param shopName {@code boot02.greeting.shop-name} — nazwa sklepu w powitaniu
 */
@ConfigurationProperties(prefix = "boot02.greeting")
public record Boot02GreetingProperties(boolean enabled, @DefaultValue("Sklep Kursanta") String shopName) {
}
