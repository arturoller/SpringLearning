package s02_configuration.cfg02_configuration_properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Ustawienia dostawy sklepu, związane z kluczami zaczynającymi się od {@code cfg02.shop.delivery}.
 * To REKORD (niezmienny) — Boot wiąże wartości przez konstruktor (constructor binding = wiązanie przez konstruktor).
 * Rekord ma jeden konstruktor, więc NIE trzeba żadnej dodatkowej adnotacji (od Boota 3 tak jest zawsze).
 *
 * @param carrier      nazwa firmy kurierskiej (brak klucza → null)
 * @param maxParcels   maksymalna liczba paczek (brak klucza → 5)
 * @param timeout      czas oczekiwania na kuriera (tekst "45s" → Duration; brak klucza → 30 sekund)
 * @param maxLabelSize największy dozwolony rozmiar etykiety (tekst "512KB" → DataSize; brak → 512 kilobajtów)
 * @param warehouse    obiekt zagnieżdżony (klucze cfg02.shop.delivery.warehouse.city/street); nigdy null
 * @param cities       lista miast (brak klucza → pusta lista dzięki {@code @DefaultValue})
 * @param prices       mapa miasto → cena dostawy (brak kluczy → pusta mapa)
 */
@ConfigurationProperties(prefix = "cfg02.shop.delivery")
public record DeliveryProperties(
        String carrier,
        @DefaultValue("5") int maxParcels,
        @DefaultValue("30s") Duration timeout,
        @DefaultValue("512KB") DataSize maxLabelSize,
        @DefaultValue Warehouse warehouse,
        @DefaultValue List<String> cities,
        @DefaultValue Map<String, BigDecimal> prices) {

    /** Obiekt zagnieżdżony — też rekord. {@code @DefaultValue} przy polu {@code warehouse} tworzy go zawsze. */
    public record Warehouse(@DefaultValue("Warszawa") String city, String street) {
    }
}
