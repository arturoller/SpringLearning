package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import s03_boot_basics.boot02_writing_auto_configuration.PriceAutoConfiguration;
import s03_boot_basics.boot02_writing_auto_configuration.PriceFormatter;
import s03_boot_basics.boot02_writing_auto_configuration.PriceTagPrinter;
import s03_boot_basics.boot02_writing_auto_configuration.ShippingCostCalculator;

import java.math.BigDecimal;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Boot02WritingAutoConfiguration — zajrzyj dopiero po własnej próbie!
 * <p>
 * Uwaga: tych klas NIE dopisujemy do pliku AutoConfiguration.imports — testujemy je przez ApplicationContextRunner.
 * Gdyby trafiły do pliku, ładowałaby je każda aplikacja modułu (utrata izolacji lekcji).
 */
public final class Boot02WritingAutoConfigurationSolutions {

    private Boot02WritingAutoConfigurationSolutions() {
    }

    /** Rozwiązanie 1: stała ścieżka zasobu — taka sama w każdym jarze z autokonfiguracjami (Boot 3 i 4). */
    public static String solution1ImportsFileLocation() {
        return "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";
    }

    /** Rozwiązanie 2: przełącznik bez matchIfMissing + ustępowanie przed beanem użytkownika. */
    @AutoConfiguration
    @ConditionalOnProperty(name = "boot02.exercise.shipping.enabled", havingValue = "true")
    public static class ShippingAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean
        ShippingCostCalculator shippingCostCalculator() {
            return new ShippingCostCalculator(new BigDecimal("200"));
        }
    }

    /** Rozwiązanie 3 (ustawienia): rekord z prefiksem i wartością domyślną. */
    @ConfigurationProperties("boot02.exercise.delivery")
    public record DeliveryProperties(@DefaultValue("150") BigDecimal freeFrom) {
    }

    /** Rozwiązanie 3 (autokonfiguracja): rejestruje rekord ustawień i z niego buduje kalkulator. */
    @AutoConfiguration
    @ConditionalOnProperty(name = "boot02.exercise.delivery.enabled", havingValue = "true")
    @EnableConfigurationProperties(DeliveryProperties.class)
    public static class DeliveryAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean
        ShippingCostCalculator shippingCostCalculator(DeliveryProperties properties) {
            return new ShippingCostCalculator(properties.freeFrom());
        }
    }

    /** Rozwiązanie 4: after = PriceAutoConfiguration, więc @ConditionalOnBean widzi już formater. */
    @AutoConfiguration(after = PriceAutoConfiguration.class)
    @ConditionalOnBean(PriceFormatter.class)
    public static class PriceTagAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean
        PriceTagPrinter priceTagPrinter(PriceFormatter formatter) {
            return new PriceTagPrinter(formatter);
        }
    }
}
