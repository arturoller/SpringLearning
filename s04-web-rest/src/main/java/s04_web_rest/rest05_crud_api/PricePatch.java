package s04_web_rest.rest05_crud_api;

import java.math.BigDecimal;

/** DTO WEJŚCIOWE dla PATCH: częściowa zmiana — tylko cena. */
public record PricePatch(BigDecimal price) {
}
