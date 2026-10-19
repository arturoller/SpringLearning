package s07_data_jdbc.jdbc05_repository;

/**
 * Wyjątek domenowy (biznesowy): produkt o takim SKU już istnieje. Repozytorium zamienia na niego techniczny
 * {@code DuplicateKeyException}, żeby warstwy wyżej (serwis, kontroler) nie musiały znać szczegółów JDBC.
 */
public class DuplicateSkuException extends RuntimeException {

    private final String sku;

    public DuplicateSkuException(String sku, Throwable cause) {
        super("Produkt o SKU " + sku + " już istnieje", cause);
        this.sku = sku;
    }

    public String sku() {
        return sku;
    }
}
