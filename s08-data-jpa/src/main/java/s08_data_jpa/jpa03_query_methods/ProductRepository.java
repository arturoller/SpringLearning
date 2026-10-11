package s08_data_jpa.jpa03_query_methods;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ProductRepository — repozytorium z metodami zapytań wyprowadzanymi z NAZW (derived query methods = metody
 * zapytań wyprowadzanych). Spring Data czyta nazwę metody, dzieli ją na części i buduje z nich zapytanie JPQL.
 * Obok każdej metody: zapytanie, które Hibernate wysłał do bazy (z logu testów, skrócone).
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /** ... where p.name = ? — Optional, bo wynik ma być co najwyżej jeden (name jest unikalne). */
    Optional<Product> findByName(String name);

    /** ... where p.price {@code <} ? */
    List<Product> findByPriceLessThan(BigDecimal maxPrice);

    /** ... where upper(p.name) like upper(?) escape '\' — Containing dokleja % z obu stron. */
    List<Product> findByNameContainingIgnoreCase(String fragment);

    /** ... where p.category = ? and p.active = true — „True” w nazwie = warunek bez parametru. */
    List<Product> findByCategoryAndActiveTrue(ProductCategory category);

    /** select count(p.id) ... where p.category = ? */
    long countByCategory(ProductCategory category);

    /** select p.id ... where p.name = ? fetch first ? rows only — wystarczy jeden wiersz, żeby wiedzieć „jest”. */
    boolean existsByName(String name);

    /** ... order by p.price desc fetch first ? rows only — Top3 = co najwyżej 3 wyniki. */
    List<Product> findTop3ByOrderByPriceDesc();

    /**
     * Usuwa nieaktywne produkty i zwraca ich liczbę. Spring Data najpierw je WCZYTUJE (select), potem usuwa po jednym
     * (em.remove). Potrzebuje transakcji DO ZAPISU — stąd {@code @Transactional} (sekcja 5 lekcji).
     */
    @Transactional
    long deleteByActiveFalse();

    /** To samo, ale BEZ {@code @Transactional} — do pokazania pułapki w sekcji 5 lekcji. */
    long deleteByCategory(ProductCategory category);
}
