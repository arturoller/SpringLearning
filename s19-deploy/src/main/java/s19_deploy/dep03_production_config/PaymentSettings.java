package s19_deploy.dep03_production_config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

/**
 * Ustawienia bramki płatności (payment gateway) sklepu — wiązane z kluczy {@code shop.payment.*}.
 * Klucz API to SEKRET: nie ma go w żadnym pliku w repozytorium, przychodzi z zewnątrz (zmienna środowiska, plik
 * spoza JAR-a, katalog z sekretami). Aplikacja bez niego NIE startuje (fail fast — „szybka porażka”).
 */
@ConfigurationProperties("shop.payment")
public record PaymentSettings(URI url, String apiKey, @DefaultValue("5s") Duration timeout) {

    public PaymentSettings {
        if (url == null) {
            throw new IllegalArgumentException("Brak ustawienia shop.payment.url (adres bramki płatności)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Brak ustawienia shop.payment.api-key (klucz API bramki płatności) "
                    + "— podaj je zmienną środowiska SHOP_PAYMENT_APIKEY albo plikiem spoza JAR-a");
        }
    }

    /** Klucz do pokazania w logu lub odpowiedzi: reszta gwiazdki, a ostatnie 2 znaki widać tylko przy kluczu od 8 znaków. */
    public String maskedApiKey() {
        int visible = apiKey.length() >= 8 ? 2 : 0;   // krótki klucz zamaskuj w całości
        return "*".repeat(apiKey.length() - visible) + apiKey.substring(apiKey.length() - visible);
    }

    /** Domyślny toString rekordu wypisałby też klucz (do logów!) — nadpisujemy go, żeby sekret nie wyciekał. */
    @Override
    public String toString() {
        return "PaymentSettings[url=" + url + ", apiKey=" + maskedApiKey() + ", timeout=" + timeout + "]";
    }
}
