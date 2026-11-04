package s08_data_jpa.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repozytorium do ĆWICZEŃ lekcji Jpa03QueryMethods (testy: Jpa03QueryMethodsExercisesTest).
 *
 * <p>Każda metoda {@code default} (metoda domyślna interfejsu — ma ciało) to „zamówienie” opisane po polsku.
 * Spring Data NIE buduje zapytań z metod default — wywołuje je jak zwykłą metodę. Twoje zadanie: dopisz w tym
 * interfejsie metodę zapytania z nazwy (bez ciała, np. {@code List<Product> findBy...(...)}) i wywołaj ją w ciele
 * metody default zamiast {@code throw}. Nazwę wymyślasz sam — sprawdzi ją start testu (literówka = błąd startu,
 * sekcja 3 lekcji).</p>
 */
public interface ExerciseProductRepository extends JpaRepository<Product, Long> {

    /**
     * ĆWICZENIE 1 (łatwe): produkty danego dostawcy posortowane po nazwie rosnąco.
     * Podpowiedź: sekcja 1 — {@code findBy<Pole>OrderBy<Pole>Asc}.
     */
    default List<Product> bySupplierSortedByName(String supplier) {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): produkty z danej kategorii, które są DOSTĘPNE (stan magazynu większy od zera),
     * od najtańszego.
     * Podpowiedź: sekcja 1 — dwa warunki połączone And, GreaterThan, OrderBy…Asc; zero przekaż jako argument.
     */
    default List<Product> availableInCategoryCheapestFirst(Category category) {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): policz produkty, których nazwa ZACZYNA SIĘ od podanego tekstu, bez względu na wielkość
     * liter ({@code "kubek"} → 2).
     * Podpowiedź: sekcja 1 i 2 — {@code countBy…}, słowa kluczowe StartingWith i IgnoreCase; wynik {@code long}.
     */
    default long countNameStartingWithIgnoreCase(String prefix) {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): DWA najtańsze produkty, które NIE MAJĄ dostawcy (supplier = NULL).
     * Podpowiedź: sekcje 1 i 4 — Top2, jawne słowo IsNull (metoda bez parametru!), OrderBy…Asc. Dlaczego nie
     * {@code findTop2BySupplierOrderByPriceAsc(null)}? Działa tak samo, ale nazwa z IsNull mówi wprost, o co pytasz.
     */
    default List<Product> twoCheapestWithoutSupplier() {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }
}
