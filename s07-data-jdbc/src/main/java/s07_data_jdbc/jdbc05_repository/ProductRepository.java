package s07_data_jdbc.jdbc05_repository;

import java.util.List;
import java.util.Optional;

/**
 * Wzorzec repozytorium (repository pattern): interfejs mówi, CO aplikacja robi z produktami, ale nie mówi JAK
 * (JDBC, JPA, pamięć). Serwisy zależą od interfejsu, więc implementację można podmienić — także na atrapę w teście.
 */
public interface ProductRepository {

    /** Produkt o podanym numerze albo {@code Optional.empty()}, gdy go nie ma (brak to nie błąd). */
    Optional<Product> findById(long id);

    /** Wszystkie produkty, posortowane po numerze. */
    List<Product> findAll();

    /** Produkty z kategorii, posortowane po numerze. */
    List<Product> findByCategory(String category);

    /**
     * Zapis: {@code id == null} → INSERT (baza nadaje numer), inaczej UPDATE. Zwraca zapisany produkt
     * (z numerem i nową wersją). UPDATE starej wersji → {@code OptimisticLockingFailureException}.
     */
    Product save(Product product);

    /** Usuwa produkt; zwraca {@code true}, gdy coś usunięto, {@code false}, gdy takiego numeru nie było. */
    boolean deleteById(long id);
}
