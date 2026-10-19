package s14_openapi.api02_documenting_api;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Kontroler produktów Z adnotacjami dokumentującymi. Porównaj z kontrolerem z Api01Springdoc: ta sama logika, ale
 * dokument ma opisy, przykłady, prawdziwe statusy (201, 400, 404) i znacznik z polskim opisem.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Produkty", description = "Katalog produktów sklepu")
public class ProductController {

    private final List<Product> products = new ArrayList<>(List.of(
            new Product("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", new BigDecimal("5499.99"), 7),
            new Product("KSI-001", "Czysty kod", "KSIAZKI", new BigDecimal("79.00"), 15)));

    @Operation(summary = "Lista produktów", description = "Zwraca wszystkie produkty albo tylko z jednej kategorii.")
    @GetMapping
    public List<Product> all(
            @Parameter(description = "Nazwa kategorii do przefiltrowania", example = "KSIAZKI")
            @RequestParam(required = false) String category) {
        return products.stream().filter(p -> category == null || p.category().equals(category)).toList();
    }

    @Operation(summary = "Pobranie produktu po kodzie")
    @ApiResponse(responseCode = "200", description = "Znaleziono produkt")
    @ApiResponse(responseCode = "404", description = "Nie ma produktu o takim kodzie",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{sku}")
    public Product one(@Parameter(description = "Kod produktu", example = "ELE-001") @PathVariable String sku) {
        return products.stream().filter(p -> p.sku().equals(sku)).findFirst()
                .orElseThrow(() -> new ProductNotFoundException(sku));
    }

    /** Punkt końcowy techniczny — działa, ale {@code @Hidden} (ukryty) wyrzuca go z dokumentu. */
    @Hidden
    @GetMapping("/internal/count")
    public int count() {
        return products.size();
    }

    @Operation(summary = "Dodanie produktu", description = "Tworzy produkt; adres nowego zasobu jest w nagłówku Location.")
    @ApiResponse(responseCode = "201", description = "Produkt utworzony")
    @ApiResponse(responseCode = "400", description = "Dane nie przeszły walidacji",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody NewProduct in) {
        Product created = new Product(in.sku(), in.name(), in.category(), in.price(), in.stock());
        products.add(created);
        return ResponseEntity.created(URI.create("/api/products/" + created.sku())).body(created);
    }
}
