package s11_testing.test03_mock_mvc_tester;

/** Wyjątek domenowy: produkt o tym SKU już istnieje. Kontroler zamienia go na 409 Conflict (konflikt). */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("Produkt o SKU " + sku + " już istnieje");
    }
}
