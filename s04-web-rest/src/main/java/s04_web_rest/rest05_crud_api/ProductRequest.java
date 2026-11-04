package s04_web_rest.rest05_crud_api;

import java.math.BigDecimal;

/** DTO WEJŚCIOWE dla POST i PUT: klient podaje tylko nazwę i cenę — id i kod wewnętrzny nadaje serwer. */
public record ProductRequest(String name, BigDecimal price) {
}
