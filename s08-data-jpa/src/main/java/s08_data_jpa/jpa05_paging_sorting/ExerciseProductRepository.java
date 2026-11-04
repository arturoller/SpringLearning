package s08_data_jpa.jpa05_paging_sorting;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repozytorium do ĆWICZEŃ lekcji Jpa05PagingSorting (testy: Jpa05PagingSortingExercisesTest).
 *
 * <p>Każda metoda {@code default} (metoda domyślna interfejsu — ma ciało) to „zamówienie” opisane po polsku.
 * Twoje zadanie: dopisz w tym interfejsie metodę zapytania (z nazwy, z parametrem {@code Pageable}, {@code Sort},
 * {@code Limit} albo {@code ScrollPosition}) i wywołaj ją w ciele metody default zamiast {@code throw}.
 * Numer strony w parametrach liczymy od 0 — jak w {@code PageRequest}.</p>
 */
public interface ExerciseProductRepository extends JpaRepository<Product, Long> {

    /**
     * ĆWICZENIE 1 (łatwe): strona produktów kategorii posortowanych po nazwie rosnąco, z liczbą wszystkich wyników.
     * Podpowiedź: sekcje 1–2 — {@code Page<Product> findByCategory(Category, Pageable)} i
     * {@code PageRequest.of(page, size, Sort.by("name"))}. Nazwy są unikalne, więc sortowanie jest jednoznaczne.
     */
    default Page<Product> categoryPageByName(Category category, int page, int size) {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): „przewijana” lista DOSTĘPNYCH produktów (stan większy od 0) od najtańszego — bez
     * kosztownego count, ale z informacją, czy jest następna porcja. Przy równej cenie wcześniej mniejsze id.
     * Podpowiedź: sekcje 2–3 — typ {@code Slice}, warunek StockGreaterThan, {@code Sort.by("price").and(Sort.by("id"))}.
     */
    default Slice<Product> availableCheapestFirst(int page, int size) {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): {@code howMany} najdroższych produktów kategorii (przy równej cenie mniejsze id pierwsze).
     * Liczba wyników ma być ARGUMENTEM, nie częścią nazwy metody.
     * Podpowiedź: sekcja 4 — parametr {@code Limit} i {@code Limit.of(howMany)}; OrderBy w nazwie.
     */
    default List<Product> mostExpensive(Category category, int howMany) {
        // TODO: dopisz metodę zapytania i wywołaj ją tutaj
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): nazwy WSZYSTKICH produktów alfabetycznie, ale pobierane z bazy oknami po 5
     * (paginacja klucza — keyset), a nie jednym zapytaniem ani przez OFFSET.
     * Podpowiedź: sekcja 5 — {@code Window<Product> findFirst5ByOrderByNameAscIdAsc(ScrollPosition position)};
     * zacznij od {@code ScrollPosition.keyset()}, w pętli bierz {@code window.positionAt(window.size() - 1)}, dopóki
     * {@code window.hasNext()}. Pomyśl, co zrobić, gdy okno jest puste (nie ma ostatniego elementu).
     */
    default List<String> allNamesInWindowsOfFive() {
        // TODO: dopisz metodę zapytania i pętlę po oknach tutaj
        throw new UnsupportedOperationException("TODO");
    }
}
