package s02_configuration.cfg01_properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Bean z ustawieniami sklepu wczytanymi przez {@code @Value} — DOBRY wzorzec: wartości wchodzą przez
 * PARAMETRY konstruktora, więc pola mogą być {@code final}, a obiekt nigdy nie istnieje „w połowie”.
 * (Celowo zwykła klasa, nie rekord: rekordy przy {@code @Value} dostają adnotację także na akcesorach i polach,
 * co niepotrzebnie komplikuje sprawę. Rekordy świetnie pasują do {@code @ConfigurationProperties} — lekcja Cfg02.)
 */
@Component
public class ShopInfo {

    private final String name;
    private final String slogan;
    private final int maxItems;
    private final int vatPercent;
    private final Duration reservationTime;
    private final boolean newsletter;

    public ShopInfo(
            @Value("${cfg01.shop.name}") String name,                                    // klucz obowiązkowy
            @Value("${cfg01.shop.slogan:Wszystko dla programisty}") String slogan,       // po dwukropku: wartość domyślna
            @Value("${cfg01.shop.max-items}") int maxItems,                              // tekst "10" → int
            @Value("${cfg01.shop.vat-percent}") int vatPercent,
            @Value("${cfg01.shop.reservation-time}") Duration reservationTime,           // tekst "30s" → Duration
            @Value("${cfg01.shop.newsletter:false}") boolean newsletter) {               // tekst "true" → boolean
        this.name = name;
        this.slogan = slogan;
        this.maxItems = maxItems;
        this.vatPercent = vatPercent;
        this.reservationTime = reservationTime;
        this.newsletter = newsletter;
    }

    public String name() {
        return name;
    }

    public String slogan() {
        return slogan;
    }

    public int maxItems() {
        return maxItems;
    }

    public int vatPercent() {
        return vatPercent;
    }

    public Duration reservationTime() {
        return reservationTime;
    }

    public boolean newsletter() {
        return newsletter;
    }
}
