package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * ProductExerciseRepository — repozytorium do ĆWICZEŃ lekcji Jpa04CustomQueries. Dopisz metody z {@code @Query}
 * i wywołaj je z metod domyślnych exercise1..3.
 *
 * <p>{@code @NoRepositoryBean} — Spring nie tworzy go przy starcie Jpa04Application (błąd w Twoim JPQL zatrzymałby
 * start całej lekcji — sekcja 1). Test ćwiczeń tworzy je ręcznie fabryką JpaRepositoryFactory.</p>
 */
@NoRepositoryBean
public interface ProductExerciseRepository extends JpaRepository<Product, Long> {

    /**
     * ĆWICZENIE 1 (łatwe): produkty, których nazwa zawiera podany fragment BEZ względu na wielkość liter,
     * posortowane po nazwie. Napisz własne {@code @Query} w JPQL z parametrem nazwanym :fragment.
     * Podpowiedź: lower(p.name) like lower(concat('%', :fragment, '%')).
     */
    default List<Product> exercise1(String fragment) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): rekordy ProductSummary (nazwa, cena) dla produktów kategorii, od NAJDROŻSZEGO.
     * Podpowiedź: {@code @Query} z „select new s08_data_jpa.jpa04_custom_queries.ProductSummary(p.name, p.price) ...”
     * albo z aliasami „p.name as name, p.price as price”.
     */
    default List<ProductSummary> exercise2(String category) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): dodaj {@code amount} sztuk do stanu magazynowego (stock) WSZYSTKICH produktów podanej
     * kategorii jednym zapytaniem UPDATE i zwróć liczbę zmienionych wierszy. Test wczytuje produkt PRZED Twoim
     * zapytaniem, a po nim sprawdza findById — bez wyczyszczenia kontekstu zobaczy starą wartość!
     * Podpowiedź: {@code @Modifying(clearAutomatically = true)} + {@code @Query("update Product p set ...")}, typ int.
     */
    default int exercise3(String category, int amount) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
