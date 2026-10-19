package s00_start.start08_debugging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import s00_start.start08_debugging.DebugShop.EmailNotifier;
import s00_start.start08_debugging.DebugShop.OrderService;
import s00_start.start08_debugging.DebugShop.PriceReport;
import s00_start.start08_debugging.DebugShop.PriceSource;
import s00_start.start08_debugging.DebugShop.ShopInfo;
import s00_start.start08_debugging.DebugShop.SmsNotifier;
import s00_start.start08_debugging.DebugShop.StackSnapshot;
import s00_start.start08_debugging.DebugShop.StockSnapshot;

import java.util.Arrays;
import java.util.List;

/**
 * Celowo zepsute (i kilka poprawnych) konfiguracje do lekcji Start08Debugging. Każda jest zwykłą klasą
 * {@code @Configuration} BEZ skanowania — uruchamia ją wyłącznie test, jawnie ("ta klasa i nic więcej"),
 * więc nie psuje żadnej innej lekcji modułu.
 */
public final class BrokenConfigs {

    private BrokenConfigs() {
    }

    /** Brakuje beana: PriceReport potrzebuje PriceSource, a nikt go nie dostarcza. */
    @Configuration
    public static class MissingBean {
        @Bean
        PriceReport priceReport(PriceSource prices) {
            return new PriceReport(prices);
        }
    }

    /** Dwóch kandydatów: OrderService chce jednego Notifier, a są dwa (emailNotifier i smsNotifier). */
    @Configuration
    public static class TwoCandidates {
        @Bean
        EmailNotifier emailNotifier() {
            return new EmailNotifier();
        }

        @Bean
        SmsNotifier smsNotifier() {
            return new SmsNotifier();
        }

        @Bean
        OrderService orderService(DebugShop.Notifier notifier) {
            return new OrderService(notifier);
        }
    }

    /**
     * Brak właściwości: shop.currency nie jest nigdzie ustawiona. Statyczny PropertySourcesPlaceholderConfigurer
     * to dokładnie to, co w aplikacji Boota dokłada autokonfiguracja (PropertyPlaceholderAutoConfiguration) —
     * bez niego zwykły kontekst Springa wstrzyknąłby do pola dosłowny tekst "${shop.currency}" zamiast zgłosić błąd.
     */
    @Configuration
    public static class MissingProperty {
        @Bean
        static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }

        @Bean
        ShopInfo shopInfo(@Value("${shop.currency}") String currency) {
            return new ShopInfo(currency);
        }
    }

    /** Wyjątek rzucony w środku tworzenia beana — Spring opakuje go w kilka warstw. */
    @Configuration
    public static class ThrowingBean {
        @Bean
        StockSnapshot stockSnapshot() {
            throw new IllegalStateException("Magazyn niedostępny");
        }
    }

    /** Poprawna konfiguracja, która zapisuje ramki stosu widoczne w momencie wywołania metody @Bean. */
    @Configuration
    public static class StackProbe {
        @Bean
        StackSnapshot stackSnapshot() {
            List<String> frames = Arrays.stream(Thread.currentThread().getStackTrace())
                    .map(StackTraceElement::toString)
                    .toList();
            return new StackSnapshot(frames);
        }
    }

    /** Poprawna konfiguracja, która loguje wpisy na poziomach DEBUG i INFO (do sekcji o logach). */
    @Configuration
    public static class LoggingConfig {
        private static final Logger LOG = LoggerFactory.getLogger(LoggingConfig.class);

        @Bean
        String diagnosticMarker() {
            LOG.debug("[Start08] wpis DEBUG: tworzę bean diagnosticMarker");
            LOG.info("[Start08] wpis INFO: tworzę bean diagnosticMarker");
            return "marker";
        }
    }

    /** Poprawna konfiguracja z włączoną autokonfiguracją — do raportu warunków (debug) i do testu zajętego portu. */
    @Configuration
    @EnableAutoConfiguration
    public static class WithAutoConfiguration {
    }
}
