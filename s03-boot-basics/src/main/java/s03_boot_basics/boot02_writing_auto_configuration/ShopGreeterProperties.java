package s03_boot_basics.boot02_writing_auto_configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Ustawienia „biblioteki sklepu” z prefiksem {@code s03.greeter} (rekord — niezmienny, wiązany przez konstruktor;
 * lekcja s02_configuration/Cfg02ConfigurationProperties).
 *
 * <pre>
 *   s03.greeter.enabled=true                 włącza autokonfigurację (domyślnie false — patrz lekcja)
 *   s03.greeter.shop-name=SpringLearning     nazwa sklepu
 *   s03.greeter.greeting=Dzień dobry         powitanie
 * </pre>
 *
 * {@code @DefaultValue} (wartość domyślna) działa przy wiązaniu przez konstruktor: gdy klucza brak, Boot podstawia
 * podany tekst (i konwertuje go na typ pola).
 *
 * @param enabled  czy autokonfiguracja jest włączona (warunek czyta ten klucz; pole jest tu dla porządku i opisu)
 * @param shopName nazwa sklepu
 * @param greeting powitanie
 */
@ConfigurationProperties(prefix = "s03.greeter")
public record ShopGreeterProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("SpringLearning") String shopName,
        @DefaultValue("Dzień dobry") String greeting) {
}
