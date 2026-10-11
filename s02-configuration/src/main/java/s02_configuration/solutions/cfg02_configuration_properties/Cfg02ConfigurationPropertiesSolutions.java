package s02_configuration.solutions.cfg02_configuration_properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import s02_configuration.cfg02_configuration_properties.DeliveryProperties;

import java.math.BigDecimal;

/** Rozwiązania wzorcowe ćwiczeń lekcji Cfg02ConfigurationProperties — zajrzyj dopiero po własnej próbie! */
public final class Cfg02ConfigurationPropertiesSolutions {

    private Cfg02ConfigurationPropertiesSolutions() {
    }

    /** Rozwiązanie 1: prefiks w adnotacji nad rekordem, wartości domyślne przy polach. */
    @ConfigurationProperties(prefix = "cfg02.exercise.discount")
    public record Discount(@DefaultValue("10") int percent, @DefaultValue("true") boolean enabled) {
    }

    /** Rozwiązanie 2: lista po przecinku, klucz mapy to ostatni człon nazwy (ASCII). */
    public static String[] solution2() {
        return new String[]{
                "cfg02.shop.delivery.carrier=DPD",
                "cfg02.shop.delivery.max-parcels=2",
                "cfg02.shop.delivery.cities=Poznań,Wrocław",
                "cfg02.shop.delivery.prices.poznan=11.00",
                "cfg02.shop.delivery.prices.wroclaw=13.50"
        };
    }

    /** Rozwiązanie 3: walidacja ręczna, potem mnożenie ceny przez liczbę paczek. */
    public static BigDecimal solution3(DeliveryProperties properties, String cityKey, int parcels) {
        if (parcels > properties.maxParcels()) {
            throw new IllegalArgumentException("Za dużo paczek: " + parcels + " (limit " + properties.maxParcels() + ")");
        }
        BigDecimal price = properties.prices().get(cityKey);
        if (price == null) {
            throw new IllegalArgumentException("Brak ceny dla: " + cityKey);
        }
        return price.multiply(BigDecimal.valueOf(parcels));
    }
}
