package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repozytorium zamówień — trzy sposoby wczytania TEGO SAMEGO zamówienia:
 * <ul>
 *   <li>{@code findByNumber} — samo zamówienie; customer to niezainicjowane proxy, lines — niezainicjowana kolekcja;</li>
 *   <li>{@code findWithCustomerByNumber} — {@code join fetch} (złącz i pobierz): klient w tym samym SELECT;</li>
 *   <li>{@code findWithLinesByNumber} — {@code @EntityGraph} (graf encji): pozycje w tym samym SELECT.</li>
 * </ul>
 * Słowo „With...” między find a By Spring Data pomija — służy tylko czytelności nazwy.
 */
public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    Optional<ShopOrder> findByNumber(String number);

    @Query("select o from ShopOrder o join fetch o.customer where o.number = :number")
    Optional<ShopOrder> findWithCustomerByNumber(String number);

    @EntityGraph(attributePaths = "lines")
    Optional<ShopOrder> findWithLinesByNumber(String number);
}
