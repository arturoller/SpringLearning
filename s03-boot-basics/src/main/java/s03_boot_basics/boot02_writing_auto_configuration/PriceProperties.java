package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ustawienia mini-biblioteki cen — klucze {@code boot02.price.*} (properties = ustawienia, właściwości).
 * <p>
 * Rekord jest niezmienny (immutable): Boot wiąże ustawienia przez konstruktor. {@code @DefaultValue} (wartość domyślna)
 * działa, gdy klucza brak — biblioteka ma rozsądne ustawienia „z pudełka”.
 *
 * @param enabled  przełącznik całej biblioteki ({@code boot02.price.enabled}); domyślnie wyłączona — patrz sekcja 2 lekcji
 * @param currency oznaczenie waluty dopisywane po kwocie ({@code boot02.price.currency}), domyślnie „zł”
 * @param decimals liczba miejsc po przecinku ({@code boot02.price.decimals}), domyślnie 2
 */
@ConfigurationProperties("boot02.price") // configuration properties = ustawienia konfiguracyjne z prefiksem
public record PriceProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("zł") String currency,
        @DefaultValue("2") int decimals) {
}
