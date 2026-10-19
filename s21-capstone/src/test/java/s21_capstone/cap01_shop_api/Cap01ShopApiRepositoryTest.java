package s21_capstone.cap01_shop_api;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
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
import s21_capstone.cap01_shop_api.order.OrderRepository;
import s21_capstone.cap01_shop_api.order.OrderStatus;
import s21_capstone.cap01_shop_api.order.OrderSummary;
import s21_capstone.cap01_shop_api.order.ShopOrder;
import s21_capstone.cap01_shop_api.product.Category;
import s21_capstone.cap01_shop_api.product.Product;
import s21_capstone.cap01_shop_api.product.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy WYCINKA bazy danych projektu Cap01ShopApi: {@code @DataJpaTest} ładuje tylko JPA (encje, repozytoria, Flyway),
 * bez kontrolerów i serwisów. Boot 4 podmienia bazę na wbudowaną H2 o losowej nazwie (replace = NON_TEST), a Flyway
 * z profilu cap01 tworzy w niej schemat i dane startowe. Każdy test działa w transakcji wycofywanej na końcu.
 *
 * <p>Statystyki Hibernate (generate_statistics) włączamy tylko tutaj: {@code getPrepareStatementCount()} = ile
 * instrukcji SQL poszło do bazy (s09_jpa_relations/Rel05NPlusOne).</p>
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("cap01")
class Cap01ShopApiRepositoryTest {

    @Autowired
    OrderRepository orders;

    @Autowired
    ProductRepository products;

    @Autowired
    EntityManager entityManager;

    Statistics statistics;

    @BeforeEach
    void resetStatistics() {
        statistics = entityManager.unwrap(Session.class).getSessionFactory().getStatistics();
        statistics.clear();
    }

    @Test
    @DisplayName("6. PRZED: lista zamówień z encji + leniwe relacje = N+1 (dla 5 zamówień: 1 + 4 klientów + 5 list pozycji = 10 zapytań)")
    void entityListCausesNPlusOne() {
        Page<ShopOrder> page = orders.findAll(PageRequest.of(0, 10, Sort.by("id")));
        List<String> rows = page.getContent().stream()
                .map(order -> order.number() + " " + order.getCustomer().getName() + " " + order.getLines().size())
                .toList();

        assertThat(rows).containsExactly("ZAM-001 Jan Kowalski 2", "ZAM-002 Maria Nowak 2", "ZAM-003 Jan Kowalski 1",
                "ZAM-004 Zofia Krawczyk 2", "ZAM-005 Adam Mazur 1");
        // 1 zapytanie o stronę (count pominięty: pierwsza strona nie jest pełna) + 4 RÓŻNYCH klientów (Jan drugi raz
        // jest już w kontekście utrwalania) + 5 kolekcji pozycji = 10. Przy 50 zamówieniach byłoby ich ponad 50.
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("6. PO: projekcja DTO z join i count — jedno zapytanie na stronę, niezależnie od liczby zamówień")
    void summaryProjectionIsOneQuery() {
        Page<OrderSummary> page = orders.findSummaries(null, PageRequest.of(0, 10, Sort.by("id")));

        assertThat(page.getContent()).extracting(OrderSummary::number, OrderSummary::customerName,
                        OrderSummary::lineCount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("ZAM-001", "Jan Kowalski", 2L),
                        org.assertj.core.groups.Tuple.tuple("ZAM-002", "Maria Nowak", 2L),
                        org.assertj.core.groups.Tuple.tuple("ZAM-003", "Jan Kowalski", 1L),
                        org.assertj.core.groups.Tuple.tuple("ZAM-004", "Zofia Krawczyk", 2L),
                        org.assertj.core.groups.Tuple.tuple("ZAM-005", "Adam Mazur", 1L));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("6. Pełna strona: zapytanie o dane + osobne count = 2 zapytania; filtr statusu działa")
    void fullPageAddsCountQuery() {
        Page<OrderSummary> page = orders.findSummaries(null, PageRequest.of(0, 2, Sort.by(Sort.Order.desc("id"))));

        assertThat(page.getContent()).extracting(OrderSummary::number).containsExactly("ZAM-005", "ZAM-004");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);

        Page<OrderSummary> fresh = orders.findSummaries(OrderStatus.NOWE, PageRequest.of(0, 10));
        assertThat(fresh.getContent()).extracting(OrderSummary::number).containsExactly("ZAM-001");
    }

    @Test
    @DisplayName("6. Szczegóły zamówienia: join fetch klienta, pozycji i produktów — 1 zapytanie, potem zero dociągania")
    void detailsInOneQuery() {
        ShopOrder order = orders.findDetailedById(4L).orElseThrow();

        assertThat(order.getCustomer().getName()).isEqualTo("Zofia Krawczyk");
        assertThat(order.getLines()).extracting(line -> line.getProduct().getSku()).containsExactly("DOM-001", "SPO-001");
        assertThat(order.getTotal()).isEqualByComparingTo("2028.98");
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. Wyszukiwanie produktów: opcjonalne filtry (null = bez warunku), wielkość liter bez znaczenia, strony")
    void productSearch() {
        Page<Product> coffee = products.search(null, "KAW", PageRequest.of(0, 10, Sort.by("sku")));
        assertThat(coffee.getContent()).extracting(Product::getSku).containsExactly("DOM-001", "SPO-001");

        Page<Product> books = products.search(Category.KSIAZKI, null, PageRequest.of(0, 2, Sort.by("price", "sku")));
        assertThat(books.getContent()).extracting(Product::getSku).containsExactly("KSI-001", "KSI-003");
        assertThat(books.getTotalElements()).isEqualTo(3);
        assertThat(books.getTotalPages()).isEqualTo(2);

        Page<Product> all = products.search(null, null, PageRequest.of(0, 5));
        assertThat(all.getTotalElements()).isEqualTo(14);
    }

    @Test
    @DisplayName("3. Cena z chwili zamówienia: zmiana ceny produktu NIE zmienia pozycji ani sumy starego zamówienia")
    void priceSnapshot() {
        Product headphones = products.findBySku("ELE-003").orElseThrow();
        headphones.update(headphones.getName(), headphones.getCategory(), new BigDecimal("399.90"), headphones.getStock());
        entityManager.flush();
        entityManager.clear();

        ShopOrder first = orders.findDetailedById(1L).orElseThrow();
        assertThat(first.getLines().getFirst().getUnitPrice()).isEqualByComparingTo("349.90");
        assertThat(first.getTotal()).isEqualByComparingTo("764.79");
    }
}
