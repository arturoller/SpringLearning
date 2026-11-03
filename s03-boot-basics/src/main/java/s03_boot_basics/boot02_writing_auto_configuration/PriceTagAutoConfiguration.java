package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Druga autokonfiguracja „biblioteki” — pokazuje KOLEJNOŚĆ (sekcja 4 lekcji).
 * <p>
 * {@code @ConditionalOnBean(PriceFormatter.class)} sprawdza beany, które JUŻ są zarejestrowane. Formater tworzy
 * {@link PriceAutoConfiguration}, więc ta klasa MUSI być oceniana po niej: {@code after = PriceAutoConfiguration.class}.
 * Bez {@code after} wynik zależałby od przypadkowej kolejności — raz etykiety by były, raz nie.
 * <p>
 * Izolacja: w innych lekcjach modułu nie ma beana PriceFormatter, więc ta klasa też się nie włącza.
 */
@AutoConfiguration(after = PriceAutoConfiguration.class) // after = po; jest też before = przed
@ConditionalOnBean(PriceFormatter.class)
public class PriceTagAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    PriceTagPrinter priceTagPrinter(PriceFormatter formatter) {
        return new PriceTagPrinter(formatter);
    }
}
