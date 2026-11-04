package s04_web_rest.rest05_crud_api;

import java.math.BigDecimal;

/** DTO WYJŚCIOWE: dokładnie to, co widzi klient (bez kodu wewnętrznego). */
public record ProductResponse(long id, String name, BigDecimal price) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(product.id(), product.name(), product.price());
    }
}
