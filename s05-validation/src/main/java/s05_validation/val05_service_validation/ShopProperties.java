package s05_validation.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * ShopProperties = ustawienia sklepu z application.properties (klucze z prefiksem {@code val05.shop}).
 * {@code @Validated} powoduje sprawdzenie reguł PRZY STARCIE aplikacji — zła konfiguracja = aplikacja się nie uruchamia
 * (fail fast = szybka porażka). Klasa NIE jest tu zarejestrowana jako bean; robią to testy (sekcja 4).
 */
@ConfigurationProperties("val05.shop")
@Validated
public record ShopProperties(
        @NotBlank String name,                    // nazwa sklepu — obowiązkowa
        @Min(1) @Max(1000) int maxItems,          // maksymalna liczba pozycji w koszyku
        @Valid Support support) {                 // zagnieżdżone ustawienia — @Valid, żeby reguły w środku działały

    /** Support = dane kontaktowe działu obsługi (zagnieżdżone ustawienia). */
    public record Support(@NotBlank String email) {
    }
}
