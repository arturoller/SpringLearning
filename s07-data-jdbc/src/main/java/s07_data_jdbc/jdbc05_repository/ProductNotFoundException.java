package s07_data_jdbc.jdbc05_repository;

/**
 * Wyjątek domenowy (biznesowy): produktu o podanym id nie ma. Niesprawdzany (RuntimeException), bo brak produktu
 * nie jest sytuacją, z której wołający musi się wycofywać w każdej linijce kodu.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(long id) {
        super("Nie ma produktu o id " + id);
    }
}
