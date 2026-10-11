package s02_configuration.cfg04_yaml;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Ustawienia sklepu z pliku cfg04.yml (prefiks {@code cfg04.shop}). Rekord nie wie, czy wartości przyszły z YAML-a,
 * czy z .properties — Boot spłaszcza oba formaty do tych samych kluczy.
 *
 * @param name        nazwa sklepu
 * @param city        miasto (polskie litery z YAML-a)
 * @param country     kraj — tu ukryta pułapka z wartością NO
 * @param openingDays dni otwarcia (lista YAML)
 * @param address     adres (obiekt zagnieżdżony)
 * @param conflict    klucz zdefiniowany jednocześnie w cfg04.yml i cfg04.properties
 * @param banner      baner promocyjny z drugiego dokumentu YAML (tylko w profilu cfg04-promo), inaczej "brak"
 */
@ConfigurationProperties(prefix = "cfg04.shop")
public record YamlShopProperties(
        String name,
        String city,
        String country,
        @DefaultValue List<String> openingDays,
        @DefaultValue Address address,
        String conflict,
        @DefaultValue("brak") String banner) {

    /** Adres sklepu. */
    public record Address(String street, String zip) {
    }
}
