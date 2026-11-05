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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel05NPlusOne na wycinku {@code @DataJpaTest} — tu wystarczy, bo liczymy zapytania W transakcji
 * (problem N+1 istnieje właśnie wtedy, gdy leniwe relacje DZIAŁAJĄ). Dane: 10 zamówień, 4 klientów, 19 pozycji,
 * 11 płatności (db/migration/rel05/V2). Ustawienia Hibernate dla całej aplikacji (default_batch_fetch_size,
 * fail_on_pagination_over_collection_fetch) — Rel05HibernateSettingsTest.
 */
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel05")
@ExtendWith(OutputCaptureExtension.class) // przechwytywanie logu — do ostrzeżenia HHH90003004
class Rel05NPlusOneTest {

    private static final String SELECT_ORDERS =
            "select so1_0.id,so1_0.customer_id,so1_0.order_number from orders so1_0 order by so1_0.order_number";
    private static final String SELECT_CUSTOMER = "select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?";
    private static final String SELECT_LINES = "select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,"
            + "l1_0.unit_price from order_line l1_0 where l1_0.order_id=? order by l1_0.id";

    @Autowired
    private ShopOrderRepository orders;

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        SqlCapture.clear();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: 10 zamówień + klient każdego (4 różnych) + pozycje każdego = 1 + 4 + 10 zapytań")
    void section1NPlusOne() {
        List<ShopOrder> all = orders.findAllOrdered();
        assertThat(SqlCapture.all()).containsExactly(SELECT_ORDERS);

        all.forEach(order -> order.getCustomer().getName());
        assertThat(SqlCapture.all()).as("+1 na KAŻDEGO RÓŻNEGO klienta (reszta jest już w kontekście)").hasSize(1 + 4)
                .endsWith(SELECT_CUSTOMER);

        all.forEach(order -> order.getLines().size());
        assertThat(SqlCapture.all()).as("+1 na KAŻDE zamówienie").hasSize(1 + 4 + 10);
        assertThat(SqlCapture.startingWith("select l1_0")).hasSize(10).containsOnly(SELECT_LINES);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: join fetch klienta — 1 zapytanie z JOIN zamiast 1 + 4")
    void section2JoinFetchToOne() {
        List<ShopOrder> all = orders.findAllWithCustomer();
        all.forEach(order -> order.getCustomer().getName());

        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "so1_0.order_number from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id "
                + "order by so1_0.order_number");
    }

    @Test
    @DisplayName("Sekcja 2: join fetch klienta, pozycji i produktów — 1 zapytanie, 10 zamówień bez duplikatów (20 wierszy SQL)")
    void section2JoinFetchCollection() {
        List<ShopOrder> all = orders.findAllWithCustomerAndLines();
        all.forEach(order -> order.getLines().forEach(line -> line.getProduct().getName()));

        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "l1_0.order_id,l1_0.id,l1_0.product_id,p1_0.id,p1_0.name,p1_0.price,l1_0.quantity,l1_0.unit_price,"
                + "so1_0.order_number from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id "
                + "left join order_line l1_0 on so1_0.id=l1_0.order_id left join product p1_0 on p1_0.id=l1_0.product_id "
                + "order by so1_0.order_number,l1_0.id");
        assertThat(all).as("Hibernate 6+ sam usuwa powtórzenia zamówień — bez distinct").hasSize(10);
        assertThat(all).extracting(order -> order.getLines().size()).containsExactly(2, 2, 3, 2, 2, 2, 2, 2, 2, 0);

        Object rows = tem.getEntityManager().createNativeQuery(
                "select count(*) from orders o left join order_line l on o.id = l.order_id").getSingleResult();
        assertThat(((Number) rows).longValue()).as("tyle wierszy zwraca baza: 19 pozycji + 1 puste zamówienie").isEqualTo(20);
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — zwykły (inner) join fetch kolekcji gubi zamówienie bez pozycji")
    void section2InnerJoinFetchLosesEmptyParents() {
        List<ShopOrder> inner = tem.getEntityManager()
                .createQuery("select o from ShopOrder o join fetch o.lines order by o.orderNumber", ShopOrder.class)
                .getResultList();

        assertThat(inner).hasSize(9).extracting(ShopOrder::getOrderNumber).doesNotContain("ZAM-010");
    }

    @Test
    @DisplayName("Sekcja 2: join fetch kolekcji + stronicowanie po polach rodzica — Hibernate 7 stronicuje w podzapytaniu SQL")
    void section2PaginationWithCollectionFetchUsesSubquery(CapturedOutput output) {
        List<ShopOrder> page = tem.getEntityManager()
                .createQuery("select o from ShopOrder o left join fetch o.lines order by o.orderNumber", ShopOrder.class)
                .setFirstResult(3).setMaxResults(3)
                .getResultList();

        assertThat(page).extracting(ShopOrder::getOrderNumber).containsExactly("ZAM-004", "ZAM-005", "ZAM-006");
        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,l1_0.order_id,l1_0.id,"
                + "l1_0.product_id,l1_0.quantity,l1_0.unit_price,so1_0.order_number from (select so1_0.id,"
                + "so1_0.customer_id,so1_0.order_number from orders so1_0 order by so1_0.order_number "
                + "offset ? rows fetch first ? rows only) so1_0(id,customer_id,order_number) "
                + "left join order_line l1_0 on so1_0.id=l1_0.order_id order by so1_0.order_number,l1_0.id");
        assertThat(output).doesNotContain("HHH90003004");
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — sortowanie po polu kolekcji: stronicowanie w PAMIĘCI i ostrzeżenie HHH90003004")
    void section2PaginationInMemory(CapturedOutput output) {
        List<ShopOrder> page = tem.getEntityManager()
                .createQuery("select o from ShopOrder o left join fetch o.lines l order by l.id", ShopOrder.class)
                .setMaxResults(3)
                .getResultList();

        assertThat(page).hasSize(3);
        assertThat(SqlCapture.last()).as("bez offset/fetch first — baza oddaje WSZYSTKIE wiersze")
                .doesNotContain("fetch first").doesNotContain("offset");
        assertThat(output).contains(
                "HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory");
    }

    @Test
    @DisplayName("Sekcja 2: DOBRA PRAKTYKA — strona samych id (stronicuje baza), potem join fetch dla tych id")
    void section2TwoStepPagination() {
        List<Long> ids = orders.findIdsPage(PageRequest.of(1, 3));
        List<ShopOrder> page = orders.findWithLinesByIdIn(ids);

        assertThat(ids).containsExactly(4L, 5L, 6L);
        assertThat(page).extracting(ShopOrder::getOrderNumber).containsExactly("ZAM-004", "ZAM-005", "ZAM-006");
        assertThat(SqlCapture.all()).containsExactly(
                "select so1_0.id from orders so1_0 order by so1_0.order_number offset ? rows fetch first ? rows only",
                "select so1_0.id,so1_0.customer_id,l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.unit_price,"
                        + "so1_0.order_number from orders so1_0 left join order_line l1_0 on so1_0.id=l1_0.order_id "
                        + "where so1_0.id in (?,?,?) order by so1_0.order_number,l1_0.id");
        assertThat(page).allMatch(order -> Hibernate.isInitialized(order.getLines()));
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: @EntityGraph(attributePaths) — zapytanie z nazwy metody + JOIN klienta i LEFT JOIN pozycji")
    void section3EntityGraph() {
        List<ShopOrder> ofAnna = orders.findByCustomerIdOrderByOrderNumber(1L);
        ofAnna.forEach(order -> order.getLines().size());

        assertThat(ofAnna).extracting(ShopOrder::getOrderNumber).containsExactly("ZAM-001", "ZAM-002", "ZAM-006", "ZAM-010");
        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.unit_price,so1_0.order_number "
                + "from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id "
                + "left join order_line l1_0 on so1_0.id=l1_0.order_id where so1_0.customer_id=? "
                + "order by so1_0.order_number,l1_0.id");
    }

    @Test
    @DisplayName("Sekcja 3: @NamedEntityGraph z encji użyty przez nazwę — ten sam SQL bez warunku, 10 zamówień")
    void section3NamedEntityGraph() {
        List<ShopOrder> all = orders.findAllByOrderByOrderNumber();
        all.forEach(order -> order.getCustomer().getName());
        all.forEach(order -> order.getLines().size());

        assertThat(all).hasSize(10);
        assertThat(SqlCapture.all()).hasSize(1);
        assertThat(SqlCapture.last()).contains("join customer c1_0").contains("left join order_line l1_0");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: @BatchSize(size = 5) na płatnościach — 10 zamówień = 1 + 2 zapytania z IN (5 id)")
    void section4BatchSize() {
        List<ShopOrder> all = orders.findAllOrdered();
        all.forEach(order -> order.getPayments().size());

        assertThat(SqlCapture.all()).containsExactly(SELECT_ORDERS,
                "select p1_0.order_id,p1_0.id,p1_0.amount from payment p1_0 where p1_0.order_id in (?,?,?,?,?) order by p1_0.id",
                "select p1_0.order_id,p1_0.id,p1_0.amount from payment p1_0 where p1_0.order_id in (?,?,?,?,?) order by p1_0.id");
        assertThat(all).extracting(order -> order.getPayments().size()).containsExactly(1, 2, 1, 1, 2, 1, 1, 1, 1, 0);
    }

    @Test
    @DisplayName("Sekcja 4: join fetch DWÓCH list naraz → MultipleBagFetchException (opakowany w IllegalArgumentException)")
    void section4MultipleBagFetchException() {
        EntityManager em = tem.getEntityManager();

        assertThatThrownBy(() -> em.createQuery(
                "select o from ShopOrder o left join fetch o.lines left join fetch o.payments", ShopOrder.class)
                .getResultList())
                .isInstanceOf(IllegalArgumentException.class)
                .hasCauseInstanceOf(MultipleBagFetchException.class)
                .hasMessageContaining("cannot simultaneously fetch multiple bags: "
                        + "[s09_jpa_relations.rel05_n_plus_one.ShopOrder.lines, s09_jpa_relations.rel05_n_plus_one.ShopOrder.payments]");
        assertThat(SqlCapture.all()).as("Hibernate odrzuca zapytanie, zanim cokolwiek wyśle do bazy").isEmpty();
    }

    @Test
    @DisplayName("Sekcja 4: dwie listy bez wyjątku — dwa zapytania; drugie „dokleja” płatności do zamówień z kontekstu")
    void section4TwoQueriesForTwoBags() {
        List<ShopOrder> all = orders.findAllWithCustomerAndLines();
        orders.fetchPayments(all);

        assertThat(SqlCapture.all()).hasSize(2);
        assertThat(SqlCapture.last()).isEqualTo("select so1_0.id,so1_0.customer_id,so1_0.order_number,p1_0.order_id,"
                + "p1_0.id,p1_0.amount from orders so1_0 left join payment p1_0 on so1_0.id=p1_0.order_id "
                + "where so1_0.id in (?,?,?,?,?,?,?,?,?,?) order by p1_0.id");
        assertThat(all).allMatch(order -> Hibernate.isInitialized(order.getLines())
                && Hibernate.isInitialized(order.getPayments()));
        assertThat(all).extracting(order -> order.getPayments().size()).containsExactly(1, 2, 1, 1, 2, 1, 1, 1, 1, 0);
        assertThat(SqlCapture.all()).as("nic więcej się nie dociąga").hasSize(2);

        Object rows = tem.getEntityManager().createNativeQuery(
                "select count(*) from orders o left join payment p on o.id = p.order_id").getSingleResult();
        assertThat(((Number) rows).longValue()).as("wiersze drugiego zapytania: 11 płatności + puste ZAM-010").isEqualTo(12);
    }

    @Test
    @DisplayName("Sekcja 4: dlaczego jedna lista jako Set nie jest darmowa — JOIN dwóch kolekcji = iloczyn wierszy (24 zamiast 20)")
    void section4CartesianProductOfTwoCollections() {
        Object rows = tem.getEntityManager().createNativeQuery("select count(*) from orders o "
                + "left join order_line l on o.id = l.order_id left join payment p on o.id = p.order_id").getSingleResult();

        assertThat(((Number) rows).longValue()).isEqualTo(24);
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: projekcja DTO (select new + group by) — 1 zapytanie, 0 encji, sumy policzone w bazie")
    void section5DtoProjection() {
        List<OrderRow> rows = orders.findOrderRows();

        assertThat(SqlCapture.all()).containsExactly("select so1_0.order_number,c1_0.name,count(l1_0.id),"
                + "coalesce(sum((l1_0.quantity*l1_0.unit_price)),0) from orders so1_0 "
                + "join customer c1_0 on c1_0.id=so1_0.customer_id left join order_line l1_0 on so1_0.id=l1_0.order_id "
                + "group by so1_0.order_number,c1_0.name order by so1_0.order_number");
        assertThat(stats.getEntityLoadCount()).isZero();
        assertThat(rows).hasSize(10);
        assertThat(rows.get(2)).satisfies(row -> {
            assertThat(row.orderNumber()).isEqualTo("ZAM-003");
            assertThat(row.customerName()).isEqualTo("Jan Kowalski");
            assertThat(row.lineCount()).isEqualTo(3);
            assertThat(row.total()).isEqualByComparingTo("356.00");
        });
        assertThat(rows.getLast()).satisfies(row -> {
            assertThat(row.lineCount()).isZero();
            assertThat(row.total()).isEqualByComparingTo("0");
        });
    }
}
