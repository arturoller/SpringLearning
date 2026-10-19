package s04_web_rest.rest05_crud_api;

import s04_web_rest.rest05_crud_api.Product.Category;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object = obiekt do przenoszenia danych) — rekordy, które opisują KONTRAKT API: co klient wysyła
 * i co dostaje. Są oddzielone od modelu Product, więc model może się zmieniać bez psucia klientów (i odwrotnie).
 * Jeden rekord na każdy kierunek i każdą operację — każdy ma dokładnie te pola, których ta operacja potrzebuje.
 */
public final class ProductDtos {

    private ProductDtos() {
    }

    /** POST /api/products — tworzenie: sku nadaje klient (to kod produktu z katalogu), wszystkie pola wymagane. */
    public record CreateProductRequest(String sku, String name, Category category, BigDecimal price,
                                       BigDecimal purchasePrice, int stock) {

        Product toProduct() {
            return new Product(sku, name, category, price, purchasePrice, stock);
        }
    }

    /**
     * PUT /api/products/{sku} — ZASTĄPIENIE całego produktu. Nie ma tu sku: identyfikator jest w adresie, więc klient
     * nie może przez pomyłkę „przenieść” produktu pod inny kod. Brak pola = błąd (stock to int — Jackson 3 → 400).
     */
    public record ReplaceProductRequest(String name, Category category, BigDecimal price, BigDecimal purchasePrice,
                                        int stock) {

        Product toProduct(String sku) {
            return new Product(sku, name, category, price, purchasePrice, stock);
        }
    }

    /**
     * PATCH /api/products/{sku} — CZĘŚCIOWA zmiana. Wszystkie pola opcjonalne (typy obiektowe): null = „nie zmieniaj”.
     * Ograniczenie tego podejścia: przez PATCH nie da się ustawić pola NA null (null znaczy „bez zmian”).
     */
    public record PatchProductRequest(String name, BigDecimal price, Integer stock) {
    }

    /** Odpowiedź: bez purchasePrice, za to z polem wyliczonym available (czy jest na stanie). */
    public record ProductResponse(String sku, String name, Category category, BigDecimal price, int stock,
                                  boolean available) {

        static ProductResponse from(Product product) {
            return new ProductResponse(product.sku(), product.name(), product.category(), product.price(),
                    product.stock(), product.stock() > 0);
        }
    }
}
