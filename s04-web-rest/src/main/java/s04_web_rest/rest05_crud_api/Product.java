package s04_web_rest.rest05_crud_api;

import java.math.BigDecimal;

/**
 * Produkt w warstwie serwisu (model „wewnętrzny”). Ma pole {@code internalCode}, którego klient API NIE powinien widzieć —
 * dlatego kontroler nie zwraca tego rekordu, tylko {@link ProductResponse}.
 */
public record Product(long id, String name, BigDecimal price, String internalCode) {
}
