package s09_jpa_relations.rel03_many_to_many;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * ĆWICZENIA lekcji Rel03ManyToMany (testy: Rel03ManyToManyExercisesTest — uruchamiasz sam ▶).
 * Zastąp każde {@code throw} własnym kodem; zapytania o produkty dopisz w {@link ExerciseProductRepository}.
 * Dane: migracja db/migration/rel03/V2 (tabelka produktów i tagów w komentarzu na jej początku).
 */
@Service // service = usługa; bean (ziarno — obiekt zarządzany przez Springa) warstwy logiki
public class ProductTagExercises {

    private final ExerciseProductRepository products;
    private final TagRepository tags;
    private final ProductSupplierRepository productSuppliers;

    public ProductTagExercises(ExerciseProductRepository products, TagRepository tags,
                               ProductSupplierRepository productSuppliers) {
        this.products = products;
        this.tags = tags;
        this.productSuppliers = productSuppliers;
    }

    /**
     * ĆWICZENIE 1 (łatwe): dodaj produktowi tag o podanej nazwie. Gdy takiego tagu nie ma — utwórz go i zapisz
     * (bez kaskady w Product!). Dodanie tagu, który produkt już ma, nic nie zmienia.
     * Podpowiedź: sekcja 1 i 3 — {@code tags.findByName(name).orElseGet(() -> tags.save(new Tag(name)))},
     * potem {@code product.addTag(tag)}.
     */
    public void tagProduct(Long productId, String tagName) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zdejmij z produktu tag o podanej nazwie (brak tagu → nic nie rób).
     * Warunek testu: przy flush idzie DOKŁADNIE jedno polecenie {@code delete from product_tag where product_id=? and tag_id=?}.
     * Podpowiedź: sekcja 2 — {@code product.removeTag(tag)}; dzięki Set Hibernate usuwa jeden wiersz.
     */
    public void untagProduct(Long productId, String tagName) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): NAZWY produktów, które mają KTÓRYKOLWIEK z podanych tagów — bez powtórzeń, alfabetycznie
     * (biuro, promocja → Klawiatura, Kubek, Lampka, Mysz).
     * Podpowiedź: sekcja 5 — {@code @Query("select distinct p.name from Product p join p.tags t where t.name in :tagNames
     * order by p.name")} z wynikiem {@code List<String>}. Bez distinct Mysz wróci dwa razy!
     */
    public List<String> productNamesWithAnyTag(Collection<String> tagNames) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): nazwy produktów, które mają WSZYSTKIE podane tagi, alfabetycznie
     * (biuro, gaming → Klawiatura, Mysz; biuro, gaming, promocja → Mysz).
     * Podpowiedź: {@code join p.tags t where t.name in :tagNames group by p.name having count(t) = :tagCount} —
     * produkt, który ma wszystkie tagi, pasuje tyle razy, ile tagów podano. Drugi parametr metody:
     * {@code long tagCount} (count zwraca Long — s08_data_jpa/Jpa04CustomQueries, sekcja 5).
     */
    public List<String> productNamesWithAllTags(Collection<String> tagNames) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 5 (trudniejsze): nazwa dostawcy z najniższą ceną zakupu produktu albo pusty Optional, gdy produktu nikt
     * nie dostarcza (Klawiatura → Hurtownia Beta, Monitor → pusto).
     * Podpowiedź: sekcja 4 — metoda z nazwy w ProductSupplierRepository (możesz ją tam dopisać):
     * {@code Optional<ProductSupplier> findFirstByProductIdOrderByPurchasePriceAsc(Long productId)} (first = pierwszy).
     */
    public Optional<String> cheapestSupplierName(Long productId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }
}
