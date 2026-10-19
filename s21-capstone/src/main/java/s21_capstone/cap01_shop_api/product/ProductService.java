package s21_capstone.cap01_shop_api.product;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.common.NotFoundException;
import s21_capstone.cap01_shop_api.common.PageResponse;
import s21_capstone.cap01_shop_api.product.ProductDtos.CreateProductRequest;
import s21_capstone.cap01_shop_api.product.ProductDtos.ProductResponse;
import s21_capstone.cap01_shop_api.product.ProductDtos.UpdateProductRequest;

import java.util.Map;

/**
 * Logika katalogu produktów. Zasady warstwy serwisu w całym projekcie:
 * <ul>
 *   <li>{@code @Transactional(readOnly = true)} na klasie — każda metoda czyta w transakcji (s10_transactions/Tx04ReadOnlyIsolation);
 *       metody zmieniające dane mają własne {@code @Transactional} (bez readOnly);</li>
 *   <li>na zewnątrz wychodzą tylko DTO, zbudowane WEWNĄTRZ transakcji — encja nie opuszcza serwisu;</li>
 *   <li>błędy to wyjątki domeny ({@link NotFoundException}, {@link ConflictException}) — serwis nie zna statusów HTTP.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    public PageResponse<ProductResponse> search(Category category, String text, Pageable pageable) {
        String normalized = (text == null || text.isBlank()) ? null : text.strip();
        return PageResponse.from(products.search(category, normalized, pageable).map(ProductResponse::from));
    }

    public ProductResponse get(String sku) {
        return ProductResponse.from(find(sku));
    }

    /**
     * Reguła w serwisie + ograniczenie w bazie. {@code existsBySku} daje czytelny błąd w typowym przypadku. Gdy dwa
     * żądania z tym samym sku przyjdą naraz, OBA mogą przejść sprawdzenie (check-then-act) — wtedy drugi INSERT odbije
     * się od {@code uk_products_sku}. {@code saveAndFlush} wysyła INSERT od razu, więc naruszenie wychodzi TUTAJ (jako
     * {@code DataIntegrityViolationException}) i zamieniamy je na ten sam 409, a nie przy commicie jako błąd 500.
     */
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (products.existsBySku(request.sku())) {
            throw skuTaken(request.sku());
        }
        Product product = new Product(request.sku(), request.name(), request.category(), request.price(),
                request.stock());
        try {
            products.saveAndFlush(product);
        } catch (DataIntegrityViolationException raceLost) {
            throw skuTaken(request.sku());
        }
        return ProductResponse.from(product);
    }

    /** Bez {@code save(...)}: encja z {@code find} jest zarządzana, Hibernate sam wyśle UPDATE przy commicie (dirty checking). */
    @Transactional
    public ProductResponse update(String sku, UpdateProductRequest request) {
        Product product = find(sku);
        product.update(request.name(), request.category(), request.price(), request.stock());
        return ProductResponse.from(product);
    }

    /**
     * Produktu z zamówień nie wolno usunąć — pilnuje tego klucz obcy {@code fk_order_lines_product}. Zamiast pytać
     * tabelę zamówień (pakiet product musiałby znać pakiet order), próbujemy usunąć i tłumaczymy naruszenie klucza obcego.
     */
    @Transactional
    public void delete(String sku) {
        Product product = find(sku);
        try {
            products.delete(product);
            products.flush();   // DELETE teraz — naruszenie klucza obcego wyjdzie w tej metodzie
        } catch (DataIntegrityViolationException usedInOrders) {
            throw new ConflictException("PRODUCT_IN_USE",
                    "Produkt " + sku + " występuje w zamówieniach — nie można go usunąć", Map.of("sku", sku));
        }
    }

    private Product find(String sku) {
        return products.findBySku(sku).orElseThrow(() -> new NotFoundException("produkt", sku));
    }

    private static ConflictException skuTaken(String sku) {
        return new ConflictException("SKU_TAKEN", "Produkt " + sku + " już istnieje", Map.of("sku", sku));
    }
}
