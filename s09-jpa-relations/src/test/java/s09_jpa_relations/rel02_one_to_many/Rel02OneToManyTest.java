package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.HibernateException;
import org.hibernate.PropertyValueException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel02OneToMany na wycinku {@code @DataJpaTest} (encje, repozytoria, SQL — więcej nie trzeba).
 * Dane: 4 produkty, 3 zamówienia, 5 pozycji z migracji db/migration/rel02/V2; każdy test jest wycofywany.
 * Złe encje (toString w pętli, REMOVE na @ManyToOne, tabela łącząca) — Rel02BrokenMappingsTest w rel02_pitfalls.
 */
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel02")
class Rel02OneToManyTest {

    private static final String SELECT_LINES = "select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,"
            + "l1_0.unit_price from order_line l1_0 where l1_0.order_id=? order by l1_0.id";
    private static final String INSERT_LINE =
            "insert into order_line (order_id,product_id,quantity,unit_price,id) values (?,?,?,?,default)";

    @Autowired
    private ShopOrderRepository orders;

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderLineRepository lines;

    @Autowired
    private InvoiceRepository invoices;

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
    @DisplayName("Sekcja 1: lista z mappedBy jest leniwa — wczytuje się przy pierwszym użyciu, w kolejności @OrderBy")
    void section1LazyMappedByCollection() {
        ShopOrder order = orders.findById(1L).orElseThrow();
        assertThat(SqlCapture.all()).as("samo zamówienie").hasSize(1);

        assertThat(order.getLines()).extracting(line -> line.getProduct().getName())
                .containsExactly("Klawiatura", "Mysz");
        assertThat(SqlCapture.all().get(1)).isEqualTo(SELECT_LINES);
        assertThat(order.getLines()).allMatch(line -> line.getOrder() == order);
    }

    @Test
    @DisplayName("Sekcja 1: addLine ustawia obie strony — kaskada zapisuje pozycję z order_id")
    void section1HelperMethodSetsBothSides() {
        ShopOrder empty = orders.findById(3L).orElseThrow();
        OrderLine line = empty.addLine(products.findById(4L).orElseThrow(), 2);
        SqlCapture.clear();

        orders.flush();

        assertThat(SqlCapture.all()).containsExactly(INSERT_LINE);
        assertThat(line.getOrder()).isSameAs(empty);
        tem.clear();
        assertThat(orders.findById(3L).orElseThrow().getLines()).hasSize(1)
                .first().satisfies(l -> assertThat(l.getUnitPrice()).isEqualByComparingTo("24.50"));
    }

    @Test
    @DisplayName("Sekcja 1: PUŁAPKA — tylko getLines().add(line) bez setOrder → PropertyValueException przy flush")
    void section1OnlyInverseSideFails() {
        ShopOrder empty = orders.findById(3L).orElseThrow();
        Product mug = products.findById(4L).orElseThrow();
        empty.getLines().add(new OrderLine(mug, 1, mug.getPrice())); // brak line.setOrder(empty)!
        SqlCapture.clear();

        assertThatThrownBy(() -> orders.flush())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasCauseInstanceOf(PropertyValueException.class)
                .hasMessageContaining("not-null property references a null or transient value")
                .hasMessageContaining("OrderLine.order");
        assertThat(SqlCapture.startingWith("insert")).as("do bazy nie poszło nic").isEmpty();
    }

    @Test
    @DisplayName("Sekcja 1: PUŁAPKA odwrotna — tylko line.setOrder: baza OK, ale wczytana lista w pamięci nieaktualna")
    void section1OnlyOwningSideLeavesStaleCollection() {
        ShopOrder empty = orders.findById(3L).orElseThrow();
        assertThat(empty.getLines()).isEmpty(); // lista wczytana (pusta)

        OrderLine line = new OrderLine(products.getReferenceById(1L), 1, new BigDecimal("149.90"));
        line.setOrder(empty); // tylko strona właściciela
        SqlCapture.clear();
        lines.save(line);
        lines.flush();

        assertThat(SqlCapture.all()).as("strona właściciela wystarczy bazie").containsExactly(INSERT_LINE);
        assertThat(empty.getLines()).as("lista w pamięci o niczym nie wie").isEmpty();
        assertThat(orders.findById(3L).orElseThrow().getLines()).as("ten sam obiekt z kontekstu — nadal pusto").isEmpty();
        tem.clear();
        assertThat(orders.findById(3L).orElseThrow().getLines()).as("po clear — świeży odczyt z bazy").hasSize(1);
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: jednokierunkowe @OneToMany + @JoinColumn — INSERT bez klucza obcego, potem UPDATE na każdą pozycję")
    void section2UnidirectionalWithJoinColumnUpdates() {
        Invoice invoice = new Invoice("FV-2026/11/1");
        invoice.getLines().add(new InvoiceLine("Klawiatura", new BigDecimal("149.90")));
        invoice.getLines().add(new InvoiceLine("Dostawa", new BigDecimal("15.00")));

        invoices.save(invoice);
        invoices.flush();

        assertThat(SqlCapture.all()).containsExactly(
                "insert into invoice (invoice_number,id) values (?,default)",
                "insert into invoice_line (amount,description,id) values (?,?,default)",
                "insert into invoice_line (amount,description,id) values (?,?,default)",
                "update invoice_line set invoice_id=? where id=?",
                "update invoice_line set invoice_id=? where id=?");

        SqlCapture.clear();
        invoice.getLines().removeFirst();
        invoices.flush();

        assertThat(SqlCapture.all()).as("usunięcie z listy: najpierw zerowanie klucza, potem DELETE (orphanRemoval)")
                .containsExactly("update invoice_line set invoice_id=null where invoice_id=? and id=?",
                        "delete from invoice_line where id=?");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: cascade = ALL — jeden save zamówienia zapisuje zamówienie i wszystkie pozycje (3 INSERT-y)")
    void section3CascadePersist() {
        Product keyboard = products.findById(1L).orElseThrow();
        Product mouse = products.findById(2L).orElseThrow();
        ShopOrder order = new ShopOrder("ZAM-100");
        order.addLine(keyboard, 1);
        order.addLine(mouse, 3);
        SqlCapture.clear();

        orders.save(order);
        orders.flush();

        assertThat(SqlCapture.all()).containsExactly(
                "insert into orders (order_number,id) values (?,default)", INSERT_LINE, INSERT_LINE);
        assertThat(order.getLines()).allMatch(line -> line.getId() != null);
    }

    @Test
    @DisplayName("Sekcja 3: orphanRemoval — removeLine kończy się DELETE pozycji; clear() usuwa wszystkie")
    void section3OrphanRemoval() {
        ShopOrder order = orders.findById(2L).orElseThrow();
        order.removeLine(order.getLines().getFirst()); // Monitor
        SqlCapture.clear();
        orders.flush();

        assertThat(SqlCapture.all()).containsExactly("delete from order_line where id=?");
        assertThat(lines.countByOrderId(2L)).isEqualTo(2);

        order.getLines().forEach(line -> line.setOrder(null));
        order.getLines().clear();
        SqlCapture.clear();
        orders.flush();
        assertThat(SqlCapture.all()).containsExactly("delete from order_line where id=?", "delete from order_line where id=?");
    }

    @Test
    @DisplayName("Sekcja 3: usunięcie zamówienia — SELECT pozycji, DELETE każdej pozycji, na końcu DELETE zamówienia")
    void section3CascadeRemove() {
        orders.deleteById(2L);
        orders.flush();

        assertThat(SqlCapture.all()).containsExactly(
                "select so1_0.id,so1_0.order_number from orders so1_0 where so1_0.id=?",
                SELECT_LINES,
                "delete from order_line where id=?",
                "delete from order_line where id=?",
                "delete from order_line where id=?",
                "delete from orders where id=?");
        assertThat(products.count()).as("produkty zostały — kaskada idzie tylko w dół").isEqualTo(4);
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — podmiana kolekcji z orphanRemoval → HibernateException „collection with orphan deletion…”")
    void section4ReplacingOrphanRemovalCollectionFails() {
        ShopOrder order = orders.findById(1L).orElseThrow();
        order.setLines(new ArrayList<>());

        assertThatThrownBy(() -> orders.flush())
                .isInstanceOf(JpaSystemException.class)
                .hasCauseExactlyInstanceOf(HibernateException.class)
                .hasMessageContaining("A collection with orphan deletion was no longer referenced by the owning entity "
                        + "instance: s09_jpa_relations.rel02_one_to_many.ShopOrder.lines");
    }

    @Test
    @DisplayName("Sekcja 4: kolekcja Hibernate (PersistentBag) zamiast ArrayList; @OrderBy dokleja ORDER BY")
    void section4CollectionTypeAndOrder() {
        ShopOrder order = orders.findById(2L).orElseThrow();

        assertThat(order.getLines().getClass().getSimpleName()).isEqualTo("PersistentBag");
        assertThat(order.getLines()).extracting(OrderLine::getId).containsExactly(3L, 4L, 5L);
        assertThat(SqlCapture.last()).endsWith("order by l1_0.id");
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: total() w encji wczytuje pozycje; zapytanie sum — 1 zapytanie, 0 encji, 0 dla pustego")
    void section5TotalInEntityVsAggregateQuery() {
        ShopOrder order = orders.findById(1L).orElseThrow();
        assertThat(order.total()).isEqualByComparingTo("267.90");
        assertThat(SqlCapture.all()).as("zamówienie + jego pozycje").hasSize(2);
        assertThat(stats.getEntityLoadCount()).as("1 zamówienie + 2 pozycje").isEqualTo(3);

        tem.clear();
        stats.clear();
        SqlCapture.clear();
        assertThat(lines.sumTotalOfOrder(2L)).isEqualByComparingTo("1056.99");
        assertThat(SqlCapture.all()).containsExactly("select coalesce(sum((ol1_0.quantity*ol1_0.unit_price)),0) "
                + "from order_line ol1_0 where ol1_0.order_id=?");
        assertThat(stats.getEntityLoadCount()).isZero();

        assertThat(lines.sumTotalOfOrder(3L)).as("puste zamówienie — coalesce daje 0").isEqualByComparingTo("0");
        assertThat(orders.findById(3L).orElseThrow().total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Sekcja 5: total() widzi też NIEZAPISANE zmiany pozycji, zapytanie do bazy — dopiero po flush")
    void section5TotalSeesUnsavedChanges() {
        ShopOrder order = orders.findById(1L).orElseThrow();
        order.getLines().getLast().setQuantity(10); // Mysz ×10 zamiast ×2

        assertThat(order.total()).isEqualByComparingTo("739.90"); // 149.90 + 10 × 59.00
        // FlushMode AUTO: przed zapytaniem JPQL o order_line Hibernate sam robi flush brudnej pozycji
        assertThat(lines.sumTotalOfOrder(1L)).isEqualByComparingTo("739.90");
        assertThat(SqlCapture.all()).anyMatch(sql -> sql.startsWith("update order_line set"));
    }
}
