package s14_openapi.api02_documenting_api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dane nowego produktu. Ograniczenia Bean Validation (@NotBlank, @Size, @Pattern, @DecimalMin, @Min, @Max) działają
 * przy walidacji ({@code @Valid}) I zostają odczytane przez springdoc do schematu. {@code @Schema} dodaje opis i przykład.
 */
@Schema(description = "Dane nowego produktu sklepu")
public record NewProduct(

        @Schema(description = "Kod produktu: trzy wielkie litery, myślnik, trzy cyfry", example = "ELE-001")
        @NotBlank
        @Pattern(regexp = "[A-Z]{3}-\\d{3}")
        String sku,

        @Schema(description = "Nazwa wyświetlana w sklepie", example = "Laptop Pro 14")
        @NotBlank
        @Size(min = 3, max = 60)
        String name,

        @Schema(description = "Kategoria", example = "ELEKTRONIKA",
                allowableValues = {"ELEKTRONIKA", "SPOZYWCZE", "KSIAZKI", "ODZIEZ", "DOM"})
        @NotNull
        String category,

        @Schema(description = "Cena w PLN", example = "5499.99")
        @NotNull
        @DecimalMin("0.01")
        BigDecimal price,

        @Schema(description = "Stan magazynu w sztukach", example = "7")
        @Min(0)
        @Max(10000)
        int stock) {
}
