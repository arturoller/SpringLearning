package s08_data_jpa.jpa02_repositories;

/** Wyjątek biznesowy: nie ma produktu o podanym numerze (jak w s07_data_jdbc/Jdbc05Repository). */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o numerze " + id);
    }
}
