package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Bean;
import s03_boot_basics.boot02_writing_auto_configuration.ShopFarewell;
import s03_boot_basics.boot02_writing_auto_configuration.ShopGreeter;
import s03_boot_basics.boot02_writing_auto_configuration.ShopGreeterAutoConfiguration;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Boot02WritingAutoConfiguration — zajrzyj dopiero po własnej próbie!
 * Rozwiązanie ćwiczenia 2 (test) jest w Boot02WritingAutoConfigurationSolutionsTest (src/test, ten sam pakiet).
 */
public final class Boot02WritingAutoConfigurationSolutions {

    private Boot02WritingAutoConfigurationSolutions() {
    }

    /**
     * Rozwiązanie 1: {@code @DefaultValue} przy polach rekordu. Tekst "100" Boot konwertuje na {@code int}.
     *
     * @param shopName           nazwa sklepu
     * @param maxGreetingsPerDay ile powitań dziennie wysyła sklep
     */
    @ConfigurationProperties(prefix = "s03.exercise.greeter")
    public record ExerciseGreeterProperties(
            @DefaultValue("SpringLearning") String shopName,
            @DefaultValue("100") int maxGreetingsPerDay) {
    }

    /**
     * Rozwiązanie 3: kolejność {@code after} + dwa warunki na metodzie {@code @Bean}.
     * (Uwaga: ta klasa jest w pakiecie {@code s03_boot_basics.solutions…}, alfabetycznie PO
     * {@code s03_boot_basics.boot02…ShopGreeterAutoConfiguration}, więc zadziałałaby nawet bez {@code after} —
     * „przypadkiem”. Klasa ze szkieletu ćwiczenia leży przed nią i bez {@code after} nie działa. Dlatego zależność
     * od innej autokonfiguracji zawsze zapisuj jawnie — lekcja, sekcja 4.)
     */
    @AutoConfiguration(after = ShopGreeterAutoConfiguration.class)
    public static class FarewellAutoConfiguration {

        @Bean
        @ConditionalOnBean(ShopGreeter.class)
        @ConditionalOnMissingBean
        ShopFarewell shopFarewell() {
            return new ShopFarewell();
        }
    }
}
