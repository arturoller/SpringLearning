package s21_capstone.cap01_shop_api.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO produktów (Data Transfer Object — obiekt do przenoszenia danych przez API). Osobny rekord na każdą operację:
 * żądanie przyjmuje DOKŁADNIE to, co wolno ustawić, a odpowiedź pokazuje to, co klient ma widzieć
 * (s04_web_rest/Rest05CrudApi, s09_jpa_relations/Rel06EntityToDto).
 *
 * <p>Komunikaty walidacji są podane jawnie (po polsku). Domyślne komunikaty Hibernate Validatora zależą od języka
 * systemu — test sprawdzający „must not be blank” przeszedłby u jednej osoby, a u drugiej nie.</p>
 *
 * <p>Liczby w ŻĄDANIACH są typu obiektowego ({@code Integer}, a nie {@code int}) z {@code @NotNull}. Dlaczego?
 * Jackson 3 domyślnie odrzuca brak wartości dla typu prostego (FAIL_ON_NULL_FOR_PRIMITIVES): JSON bez pola
 * {@code stock} kończy się wtedy ogólnym 400 „Failed to read request”, ZANIM ruszy walidacja — klient nie wie, czego
 * brakuje. Z {@code Integer} brak pola to {@code null}, a {@code @NotNull} daje czytelny błąd {@code stock}.</p>
 */
public final class ProductDtos {

    private ProductDtos() {
    }

    /** POST /api/products — nowy produkt. */
    public record CreateProductRequest(
            @NotBlank(message = "sku jest wymagane")
            @Pattern(regexp = "[A-Z]{3}-\\d{3}", message = "sku ma format ABC-123")
            String sku,
            @NotBlank(message = "nazwa jest wymagana")
            @Size(max = 100, message = "nazwa może mieć najwyżej 100 znaków")
            String name,
            @NotNull(message = "kategoria jest wymagana")
            Category category,
            @NotNull(message = "cena jest wymagana")
            @DecimalMin(value = "0.01", message = "cena musi być dodatnia")
            @Digits(integer = 8, fraction = 2, message = "cena: najwyżej 8 cyfr przed i 2 po przecinku")
            BigDecimal price,
            @NotNull(message = "stan jest wymagany")
            @PositiveOrZero(message = "stan nie może być ujemny")
            Integer stock) {
    }

    /** PUT /api/products/{sku} — zastąpienie danych (sku jest w adresie, nie w ciele). */
    public record UpdateProductRequest(
            @NotBlank(message = "nazwa jest wymagana")
            @Size(max = 100, message = "nazwa może mieć najwyżej 100 znaków")
            String name,
            @NotNull(message = "kategoria jest wymagana")
            Category category,
            @NotNull(message = "cena jest wymagana")
            @DecimalMin(value = "0.01", message = "cena musi być dodatnia")
            @Digits(integer = 8, fraction = 2, message = "cena: najwyżej 8 cyfr przed i 2 po przecinku")
            BigDecimal price,
            @NotNull(message = "stan jest wymagany")
            @PositiveOrZero(message = "stan nie może być ujemny")
            Integer stock) {
    }

    /** Odpowiedź: bez technicznego id, z polem wyliczonym {@code available}. */
    public record ProductResponse(String sku, String name, Category category, BigDecimal price, int stock,
                                  boolean available) {

        public static ProductResponse from(Product product) {
            return new ProductResponse(product.getSku(), product.getName(), product.getCategory(), product.getPrice(),
                    product.getStock(), product.getStock() > 0);
        }
    }
}
