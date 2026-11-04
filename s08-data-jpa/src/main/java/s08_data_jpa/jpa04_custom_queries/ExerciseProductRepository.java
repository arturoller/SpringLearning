package s08_data_jpa.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repozytorium do ĆWICZEŃ lekcji Jpa04CustomQueries (testy: Jpa04CustomQueriesExercisesTest).
 *
 * <p>Każda metoda {@code default} (metoda domyślna interfejsu — ma ciało) to „zamówienie” opisane po polsku.
 * Twoje zadanie: dopisz w tym interfejsie metodę z adnotacją {@code @Query} (i, gdy trzeba, {@code @Modifying})
 * i wywołaj ją w ciele metody default zamiast {@code throw}. Błąd w JPQL zobaczysz już przy starcie testu
 * (Spring Data sprawdza zapytania JPQL przy tworzeniu repozytorium — sekcja 1 lekcji).</p>
 */
public interface ExerciseProductRepository extends JpaRepository<Product, Long> {

    /**
     * ĆWICZENIE 1 (łatwe): produkty, których nazwa ZAWIERA fragment bez względu na wielkość liter, posortowane po
     * nazwie rosnąco ({@code "kub"} → Kubek, Kubek termiczny).
     * Podpowiedź: sekcja 1 — {@code lower(p.name) like lower(concat('%', :fragment, '%'))}, {@code order by p.name}.
     */
    default List<Product> searchByNameFragment(String fragment) {
        // TODO: dopisz metodę z @Query i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): nazwy i ceny produktów kategorii jako rekordy {@link ProductPrice}, od najtańszego.
     * Podpowiedź: sekcja 3 — {@code select new s08_data_jpa.jpa04_custom_queries.ProductPrice(p.name, p.price) …}
     * albo bez {@code new}: {@code select p.name, p.price …} z typem wyniku {@code List<ProductPrice>}.
     */
    default List<ProductPrice> pricesInCategory(Category category) {
        // TODO: dopisz metodę z @Query i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): wartość magazynu kategorii = suma (cena × stan) wszystkich jej produktów.
     * Dla kategorii BEZ produktów zwróć 0 (nie null!). HOME → 3630.00.
     * Podpowiedź: sekcja 5 — {@code sum(p.price * p.stock)}; sum bez wierszy daje null: {@code coalesce(…, 0)}
     * w JPQL albo obsługa null w ciele metody default.
     */
    default BigDecimal stockValue(Category category) {
        // TODO: dopisz metodę z @Query i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): JEDNYM poleceniem UPDATE ustaw stan magazynu {@code amount} wszystkim produktom,
     * których stan wynosi 0, i zwróć liczbę zmienionych wierszy. Test najpierw wczytuje Lampkę (stan 0), a po Twojej
     * metodzie oczekuje, że {@code findById} zwróci ją już z NOWYM stanem.
     * Podpowiedź: sekcja 4 — {@code @Modifying} z odpowiednim atrybutem; bez niego findById zwróci starą encję z pamięci.
     */
    default int restockEmpty(int amount) {
        // TODO: dopisz metodę z @Modifying @Query i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }
}
