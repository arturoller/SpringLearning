package s11_testing.test06_testcontainers;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repozytorium z trzema zapytaniami NATYWNYMI (native = w SQL konkretnej bazy, nie w JPQL). Hibernate nie tłumaczy
 * ich na dialekt bazy — idą do bazy dokładnie tak, jak je napisano. Dlatego to, czy działają, zależy od SILNIKA:
 * <ul>
 *   <li>{@link #insertIfAbsent} — {@code ON CONFLICT DO NOTHING}: tylko PostgreSQL (H2 2.4: błąd składni),</li>
 *   <li>{@link #findTop3MostExpensiveH2Style} — {@code SELECT TOP 3}: tylko H2 (PostgreSQL: błąd składni),</li>
 *   <li>{@link #findMostExpensive} — {@code LIMIT}: działa w obu.</li>
 * </ul>
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Wstawia produkt, jeśli nie ma jeszcze produktu o tej nazwie („upsert” bez aktualizacji). Zwraca liczbę wstawionych
     * wierszy: 1 albo 0. Składnia PostgreSQL — w H2 to błąd składni (nawet w trybie MODE=PostgreSQL).
     */
    @Modifying // modifying = zmieniające dane (INSERT/UPDATE/DELETE), a nie SELECT
    @Query(value = "INSERT INTO product (name, price) VALUES (:name, :price) ON CONFLICT (name) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("name") String name, @Param("price") BigDecimal price);

    /**
     * BŁĄD CELOWY (lekcja, sekcja 1): składnia H2/SQL Server. Test na H2 przechodzi, a na PostgreSQL — i na
     * produkcji — zapytanie się nie wykona. Poprawna, przenośna wersja: {@link #findMostExpensive(int)}.
     */
    @Query(value = "SELECT TOP 3 * FROM product ORDER BY price DESC, name", nativeQuery = true)
    List<Product> findTop3MostExpensiveH2Style();

    /** {@code n} najdroższych produktów (przy równej cenie — po nazwie). {@code LIMIT} rozumieją i H2, i PostgreSQL. */
    @Query(value = "SELECT * FROM product ORDER BY price DESC, name LIMIT :n", nativeQuery = true)
    List<Product> findMostExpensive(@Param("n") int n);
}
