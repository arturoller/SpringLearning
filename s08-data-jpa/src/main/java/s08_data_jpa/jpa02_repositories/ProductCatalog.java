package s08_data_jpa.jpa02_repositories;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repozytorium TYLKO DO ODCZYTU (catalog = katalog). Rozszerza goły {@code Repository} (interfejs-znacznik bez metod)
 * i deklaruje wyłącznie potrzebne metody — z tymi samymi sygnaturami co w CrudRepository, więc Spring Data
 * podłącza do nich gotową implementację. Kto dostanie ProductCatalog, nie wywoła save ani delete — kompilator
 * na to nie pozwoli (sekcja 7 lekcji).
 */
public interface ProductCatalog extends Repository<Product, Long> {

    Optional<Product> findById(Long id);

    List<Product> findAll();

    long count();
}
