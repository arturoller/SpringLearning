package s05_validation.val05_service_validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Ustawienia sklepu z application.properties (prefiks {@code val05.shop}) — walidowane przy starcie aplikacji. */
@ConfigurationProperties("val05.shop")
@Validated
public record ShopProperties(
        @NotBlank String name,
        @Min(1) @Max(1000) int maxItemsPerOrder,
        @Valid Delivery delivery) {

    /** Zagnieżdżone ustawienia — bez {@code @Valid} na polu {@code delivery} ich ograniczenia byłyby pominięte. */
    public record Delivery(@NotBlank String city, @Min(0) int freeFromAmount) {
    }
}
