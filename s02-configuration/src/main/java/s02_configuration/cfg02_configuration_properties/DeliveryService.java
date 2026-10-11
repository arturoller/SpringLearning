package s02_configuration.cfg02_configuration_properties;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Usługa dostawy. Dostaje JEDEN obiekt z ustawieniami (a nie sześć osobnych {@code @Value}) — i da się ją przetestować
 * zwykłym {@code new DeliveryService(new DeliveryProperties(...))}, bez Springa.
 */
@Service
public class DeliveryService {

    private final DeliveryProperties properties;

    public DeliveryService(DeliveryProperties properties) {
        this.properties = properties;
    }

    public boolean delivers(String city) {
        return properties.cities().contains(city);
    }

    /** Cena dostawy do miasta o kluczu z mapy (np. "warszawa"); brak ceny to błąd. */
    public BigDecimal priceFor(String cityKey) {
        BigDecimal price = properties.prices().get(cityKey);
        if (price == null) {
            throw new IllegalArgumentException("Brak ceny dostawy dla: " + cityKey);
        }
        return price;
    }

    public String describe() {
        return properties.carrier() + " z magazynu w " + properties.warehouse().city();
    }
}
