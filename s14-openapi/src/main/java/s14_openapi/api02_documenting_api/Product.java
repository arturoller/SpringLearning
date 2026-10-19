package s14_openapi.api02_documenting_api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** Produkt w odpowiedzi API. */
@Schema(description = "Produkt sklepu")
public record Product(
        @Schema(description = "Kod produktu", example = "ELE-001") String sku,
        @Schema(description = "Nazwa produktu", example = "Laptop Pro 14") String name,
        @Schema(description = "Kategoria", example = "ELEKTRONIKA") String category,
        @Schema(description = "Cena w PLN", example = "5499.99") BigDecimal price,
        @Schema(description = "Stan magazynu w sztukach", example = "7") int stock) {
}
