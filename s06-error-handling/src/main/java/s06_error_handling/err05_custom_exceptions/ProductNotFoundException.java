package s06_error_handling.err05_custom_exceptions;

import java.util.Map;

/** Nie ma produktu o podanym SKU. Odpowiada „zasobowi, którego nie ma” → 404. */
public final class ProductNotFoundException extends ShopException {

    private final String sku;

    public ProductNotFoundException(String sku) {
        super("PRODUKT_NIE_ISTNIEJE", "Nie ma produktu o SKU " + sku);
        this.sku = sku;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of("sku", sku);
    }
}
