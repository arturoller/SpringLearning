package s08_data_jpa.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ProductExerciseRepository — repozytorium do ĆWICZEŃ lekcji Jpa03QueryMethods. Dopisujesz tu metody zapytań,
 * a metody domyślne (default) exercise1..3 mają je wywołać.
 *
 * <p>{@code @NoRepositoryBean} (no repository bean = „nie twórz z tego beana”) — Spring NIE tworzy tego repozytorium
 * przy starcie Jpa03Application. Dlaczego? Literówka w Twojej metodzie zatrzymałaby start CAŁEJ lekcji (sekcja 7)
 * i wszystkie jej testy. Test ćwiczeń tworzy repozytorium ręcznie fabryką JpaRepositoryFactory — błąd w nazwie
 * metody zobaczysz tylko w teście ćwiczeń.</p>
 */
@NoRepositoryBean
public interface ProductExerciseRepository extends JpaRepository<Product, Long> {
    List<Product> findByTitle(String t);

    /**
     * ĆWICZENIE 1 (łatwe): produkty podanej kategorii posortowane po nazwie rosnąco.
     * Dopisz metodę zapytania (np. findByCategoryOrderByNameAsc) i wywołaj ją tutaj zamiast rzucać wyjątek.
     */
    default List<Product> exercise1(ProductCategory category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): AKTYWNE produkty z ceną w przedziale [min, max] (obie granice włącznie), posortowane
     * po cenie rosnąco. Podpowiedź: słowo Between w nazwie (włącza obie granice), ActiveTrue, OrderByPriceAsc.
     */
    default List<Product> exercise2(BigDecimal min, BigDecimal max) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): NAJTAŃSZY aktywny produkt danej kategorii (Optional — może go nie być).
     * Podpowiedź: findFirstBy... + OrderByPriceAsc; typ zwracany Optional{@code <Product>}.
     */
    default Optional<Product> exercise3(ProductCategory category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
