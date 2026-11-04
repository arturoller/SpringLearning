package s07_data_jdbc.jdbc05_repository;

/** Błąd biznesowy: nie ma produktu o podanym numerze (niesprawdzany — RuntimeException, więc wycofuje transakcję). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o numerze " + id);
    }
}
