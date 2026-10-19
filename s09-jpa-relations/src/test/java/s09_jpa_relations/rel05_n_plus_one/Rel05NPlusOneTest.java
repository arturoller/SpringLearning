package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.loader.MultipleBagFetchException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel05NPlusOne — „WYNIK” każdej sekcji to LICZBA zapytań SQL.
 *
 * <p>{@code @DataJpaTest} — każdy test w transakcji (sesja otwarta, leniwe pola można czytać), dane startowe
 * z migracji V2__sample_data.sql (7 klientów, 6 zamówień, 10 pozycji). Przed każdym testem czyścimy statystyki
 * Hibernate; {@code getPrepareStatementCount()} = liczba instrukcji SQL wysłanych od tej chwili.</p>
 */
@DataJpaTest
@ActiveProfiles("rel05")
class Rel05NPlusOneTest {

    @Autowired
    ShopOrderRepository orders;

    @Autowired
    CustomerRepository customers;

    @Autowired
    EntityManager em;

    @Autowired
    EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void clearStatistics() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    @Test
    @DisplayName("1. N+1 na @ManyToOne: 6 zamówień od 5 klientów → 1 + 5 = 6 zapytań")
    void nPlusOneOnManyToOne() {
        List<String> names = orders.findAllByOrderByNumber().stream()
                .map(order -> order.getCustomer().getName())
                .toList();

        assertThat(names).hasSize(6).containsOnlyOnce("Maria Nowak");
        assertThat(stats.getPrepareStatementCount()).as("1 po zamówienia + 5 po RÓŻNYCH klientów").isEqualTo(6);
    }

    @Test
    @DisplayName("2. N+1 na kolekcji: 6 zamówień → 1 + 6 = 7 zapytań")
    void nPlusOneOnCollection() {
        int totalLines = orders.findAllByOrderByNumber().stream()
                .mapToInt(order -> order.getLines().size())
                .sum();

        assertThat(totalLines).isEqualTo(10);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("3. join fetch klienta: wszystko w 1 zapytaniu")
    void joinFetchManyToOne() {
        orders.findAllWithCustomer().forEach(order -> order.getCustomer().getName());

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. join fetch kolekcji: 1 zapytanie, 6 zamówień na liście (bez duplikatów, choć wierszy SQL jest 10)")
    void joinFetchCollection() {
        List<ShopOrder> loaded = orders.findAllWithLines();
        int totalLines = loaded.stream().mapToInt(order -> order.getLines().size()).sum();

        assertThat(loaded).hasSize(6).doesNotHaveDuplicates();
        assertThat(totalLines).isEqualTo(10);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. Zapytanie o kolumny przez złączenie: powtórzenia SĄ w wyniku (10 numerów dla 6 zamówień)")
    void duplicatesVisibleInColumnQuery() {
        List<String> numbers = em.createQuery(
                        "select o.number from ShopOrder o join o.lines l order by o.number", String.class)
                .getResultList();

        assertThat(numbers).hasSize(10).filteredOn("ZAM-003"::equals).hasSize(3);
    }

    @Test
    @DisplayName("4. @EntityGraph(attributePaths = \"customer\"): 1 zapytanie, bez pisania JPQL")
    void entityGraph() {
        List<ShopOrder> loaded = orders.findAllWithCustomerGraphByOrderByNumber();
        loaded.forEach(order -> order.getCustomer().getName());

        assertThat(loaded).hasSize(6);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("5. @BatchSize(size = 10) na Customer.orders: 7 klientów i ich zamówienia w 2 zapytaniach")
    void batchSizeOnCollection() {
        int totalOrders = customers.findAllByOrderByName().stream()
                .mapToInt(customer -> customer.getOrders().size())
                .sum();

        assertThat(totalOrders).isEqualTo(6);
        assertThat(stats.getPrepareStatementCount()).as("1 po klientów + 1 paczka zamówień").isEqualTo(2);
    }

    @Test
    @DisplayName("6. Projekcja DTO: 1 zapytanie, 3 kolumny, ani jednej encji w kontekście utrwalania")
    void dtoProjection() {
        List<OrderSummary> summaries = orders.findSummaries();

        assertThat(summaries).hasSize(6)
                .first().isEqualTo(new OrderSummary("ZAM-001", "Jan Kowalski", "Warszawa"));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(stats.getEntityLoadCount()).as("żadnych encji").isZero();
    }

    @Test
    @DisplayName("7a. join fetch kolekcji + stronicowanie: Hibernate 7.4 stronicuje w podzapytaniu — 1 zapytanie, 6 encji")
    void paginationWithCollectionFetchInSubquery() {
        List<ShopOrder> page = em.createQuery(
                        "select o from ShopOrder o join fetch o.lines order by o.number", ShopOrder.class)
                .setFirstResult(2)
                .setMaxResults(2)
                .getResultList();

        assertThat(page).extracting(ShopOrder::getNumber).containsExactly("ZAM-003", "ZAM-004");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(stats.getEntityLoadCount()).as("2 zamówienia + 4 ich pozycje").isEqualTo(6);
    }

    @Test
    @DisplayName("7a. PUŁAPKA: stronicowanie W PAMIĘCI (stary tryb) — ten sam wynik, ale wczytane wszystkie 16 encji")
    void paginationInMemoryLoadsEverything() {
        List<ShopOrder> page = em.createQuery(
                        "select o from ShopOrder o join fetch o.lines order by o.number", ShopOrder.class)
                .setHint("org.hibernate.limitInMemory", true)   // wymuszenie trybu „w pamięci” (wskazówka Hibernate)
                .setFirstResult(2)
                .setMaxResults(2)
                .getResultList();

        assertThat(page).extracting(ShopOrder::getNumber).containsExactly("ZAM-003", "ZAM-004");
        assertThat(stats.getEntityLoadCount()).as("6 zamówień + 10 pozycji").isEqualTo(16);
    }

    @Test
    @DisplayName("7b. PUŁAPKA: Page + join fetch kolekcji — wyliczone zapytanie liczące liczy wiersze złączenia (10, nie 6)")
    void pageWithJoinFetchCountsJoinedRows() {
        Page<ShopOrder> page = orders.findPageWithLinesWrongCount(PageRequest.of(0, 2, Sort.by("number")));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).as("ZŁE: liczba pozycji zamiast zamówień").isEqualTo(10);
        assertThat(page.getTotalPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("7b. Poprawnie: countQuery = \"select count(o) from ShopOrder o\" → 6 zamówień, 3 strony")
    void pageWithExplicitCountQuery() {
        Page<ShopOrder> page = orders.findPageWithLines(PageRequest.of(0, 2, Sort.by("number")));

        assertThat(page.getContent()).extracting(ShopOrder::getNumber).containsExactly("ZAM-001", "ZAM-002");
        assertThat(Hibernate.isInitialized(page.getContent().getFirst().getLines())).isTrue();
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("7c. Dwie kolekcje List w jednym join fetch → MultipleBagFetchException")
    void multipleBagFetch() {
        assertThatThrownBy(() -> em.createQuery(
                        "select c from Customer c join fetch c.orders o join fetch o.lines", Customer.class)
                .getResultList())
                .isInstanceOf(IllegalArgumentException.class)
                .hasCauseInstanceOf(MultipleBagFetchException.class)
                .hasMessageContaining("cannot simultaneously fetch multiple bags");
    }
}
