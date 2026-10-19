package s06_error_handling.err03_controller_advice;

/** Brak produktu o podanym SKU — czysty wyjątek domeny, bez żadnej wiedzy o HTTP (status ustala porada). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("Nie ma produktu o SKU " + sku);
    }
}
