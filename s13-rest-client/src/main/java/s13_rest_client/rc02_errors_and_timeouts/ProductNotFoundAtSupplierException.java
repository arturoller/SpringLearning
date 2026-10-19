package s13_rest_client.rc02_errors_and_timeouts;

/**
 * Wyjątek DOMENOWY (z języka naszego sklepu): „dostawca nie zna tego produktu”. Klient tłumaczy na niego odpowiedź
 * 404 dostawcy, żeby reszta aplikacji nie musiała znać klas HTTP (HttpClientErrorException.NotFound).
 * Własne wyjątki — t10_exceptions/Exceptions05CustomExceptions.
 */
public class ProductNotFoundAtSupplierException extends RuntimeException {

    private final String sku;

    public ProductNotFoundAtSupplierException(String sku) {
        super("Dostawca nie zna produktu " + sku);
        this.sku = sku;
    }

    public String getSku() {
        return sku;
    }
}
