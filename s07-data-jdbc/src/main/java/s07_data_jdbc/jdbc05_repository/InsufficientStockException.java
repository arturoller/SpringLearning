package s07_data_jdbc.jdbc05_repository;

/** Błąd biznesowy: w magazynie jest mniej sztuk, niż chcemy przenieść. */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int inStock, int requested) {
        super("Produkt " + productName + ": w magazynie " + inStock + " szt., żądano " + requested);
    }
}
