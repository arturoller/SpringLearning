package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.hibernate.PropertyValueException;
import org.hibernate.SessionFactory;
import org.hibernate.TransientPropertyValueException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel01ManyToOne na wycinku {@code @DataJpaTest} (wystarczy: sprawdzamy encje, repozytoria i SQL).
 * Dane: 4 klientów i 5 zamówień z migracji db/migration/rel01/V2. Każdy test jest wycofywany (rollback).
 * SqlCapture (StatementInspector) zapisuje każde polecenie SQL; przed sprawdzaniem „co poszło do bazy” czyścimy
 * kontekst utrwalania ({@code tem.clear()}), żeby encje nie były brane z pamięci.
 */
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel01")
class Rel01ManyToOneTest {

    @Autowired
    private ShopOrderRepository orders;

    @Autowired
    private EagerShopOrderRepository eagerOrders;

    @Autowired
    private CustomerRepository customers;

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

    private static List<String> numbers(List<ShopOrder> list) {
        return list.stream().map(ShopOrder::getOrderNumber).toList();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 1: @ManyToOne + @JoinColumn — z kolumny customer_id Hibernate składa obiekt klienta")
    void section1MappingReadsCustomerThroughForeignKey() {
        ShopOrder order = orders.findById(3L).orElseThrow();

        assertThat(order.getOrderNumber()).isEqualTo("ZAM-003");
        assertThat(order.getCustomer().getId()).as("id z kolumny customer_id").isEqualTo(1L);
        assertThat(order.getCustomer().getName()).isEqualTo("Anna Nowak");
        assertThat(order.getCustomer()).as("ten sam klient co zamówienie 1 — jeden obiekt w kontekście")
                .isSameAs(orders.findById(1L).orElseThrow().getCustomer());
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: domyślne EAGER — find() robi JOIN, a JPQL o listę daje N+1 (1 + 3 zapytania)")
    void section2DefaultEagerJoinOnFindAndNPlusOneOnJpql() {
        EagerShopOrder one = tem.find(EagerShopOrder.class, 1L);

        assertThat(SqlCapture.all()).as("find z EAGER — jedno zapytanie z JOIN").containsExactly(
                "select eso1_0.id,eso1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,eso1_0.order_number "
                        + "from orders eso1_0 join customer c1_0 on c1_0.id=eso1_0.customer_id where eso1_0.id=?");
        assertThat(Hibernate.isInitialized(one.getCustomer())).as("klient wczytany od razu").isTrue();

        tem.clear();
        SqlCapture.clear();
        List<EagerShopOrder> all = eagerOrders.findAllOrdered();

        assertThat(all).hasSize(5);
        assertThat(SqlCapture.all()).as("1 zapytanie o zamówienia + osobny SELECT na każdego z 3 różnych klientów")
                .hasSize(4)
                .first().isEqualTo("select eso1_0.id,eso1_0.customer_id,eso1_0.order_number from orders eso1_0 "
                        + "order by eso1_0.order_number");
        assertThat(SqlCapture.all().subList(1, 4))
                .allMatch(sql -> sql.equals("select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?"));
    }

    @Test
    @DisplayName("Sekcja 2: LAZY — find() bez joina, w polu proxy; getId() bez zapytania, getName() → SELECT")
    void section2LazyProxy() {
        ShopOrder order = tem.find(ShopOrder.class, 1L);

        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,so1_0.order_number,"
                + "so1_0.ordered_on from orders so1_0 where so1_0.id=?");
        Customer customer = order.getCustomer();
        assertThat(customer.getClass().getSimpleName()).isEqualTo("Customer$HibernateProxy");
        assertThat(Hibernate.isInitialized(customer)).isFalse();

        assertThat(customer.getId()).isEqualTo(1L);
        assertThat(SqlCapture.all()).as("proxy zna id — bez zapytania").hasSize(1);

        assertThat(customer.getName()).isEqualTo("Anna Nowak");
        assertThat(SqlCapture.all()).as("pierwszy inny getter → SELECT klienta").hasSize(2)
                .last().isEqualTo("select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?");
    }

    @Test
    @DisplayName("Sekcja 2: LAZY — lista JPQL to 1 zapytanie; join fetch daje klientów w tym samym zapytaniu")
    void section2LazyListAndJoinFetch() {
        assertThat(numbers(orders.findAllOrdered())).hasSize(5);
        assertThat(stats.getPrepareStatementCount()).as("klientów nikt nie dotknął — nikt ich nie czyta").isEqualTo(1);

        tem.clear();
        SqlCapture.clear();
        List<ShopOrder> withCustomers = orders.findAllWithCustomer();

        assertThat(withCustomers).allMatch(o -> Hibernate.isInitialized(o.getCustomer()));
        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "so1_0.order_number,so1_0.ordered_on from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id "
                + "order by so1_0.order_number");
    }

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA — toString leniwego klienta (np. w logu) wysyła dodatkowy SELECT")
    void section2ToStringOfLazyRelationQueriesDatabase() {
        ShopOrder order = orders.findById(1L).orElseThrow();
        SqlCapture.clear();

        String log = "Zamówienie " + order + " klienta " + order.getCustomer();

        assertThat(log).contains("Customer[id=1, name=Anna Nowak]");
        assertThat(SqlCapture.all()).as("sam log zrobił zapytanie do bazy")
                .containsExactly("select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: getReferenceById — zapis zamówienia istniejącego klienta to sam INSERT, bez SELECT-a")
    void section3GetReferenceByIdNoSelect() {
        Customer jan = customers.getReferenceById(2L);
        assertThat(SqlCapture.all()).as("proxy bez zapytania").isEmpty();

        ShopOrder saved = orders.save(new ShopOrder("ZAM-100", LocalDate.of(2026, 11, 1), jan));
        orders.flush();

        assertThat(SqlCapture.all()).containsExactly(
                "insert into orders (customer_id,order_number,ordered_on,id) values (?,?,?,default)");
        assertThat(Hibernate.isInitialized(jan)).as("klient nadal niewczytany").isFalse();

        tem.clear();
        assertThat(orders.findById(saved.getId()).orElseThrow().getCustomer().getName()).isEqualTo("Jan Kowalski");
    }

    @Test
    @DisplayName("Sekcja 3: getReferenceById z nieistniejącym id — błąd FK przy INSERT albo EntityNotFoundException przy getterze")
    void section3ReferenceToMissingRow() {
        assertThatThrownBy(() -> orders.saveAndFlush(
                new ShopOrder("ZAM-101", LocalDate.of(2026, 11, 1), customers.getReferenceById(99L))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("FK_ORDERS_CUSTOMER");

        tem.clear();
        assertThatThrownBy(() -> customers.getReferenceById(99L).getName())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No row with the given identifier exists");
    }

    @Test
    @DisplayName("Sekcja 3: optional = false — zamówienie bez klienta odrzuca Hibernate PRZED wysłaniem INSERT-a")
    void section3NullCustomerRejectedBeforeInsert() {
        assertThatThrownBy(() -> orders.save(new ShopOrder("ZAM-102", LocalDate.of(2026, 11, 1), null)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasCauseInstanceOf(PropertyValueException.class)
                .hasMessageContaining("not-null property references a null or transient value");
        assertThat(SqlCapture.all()).as("do bazy nie poszło nic").isEmpty();
    }

    @Test
    @DisplayName("Sekcja 3: PUŁAPKA — nowy (transient) klient bez kaskady → TransientPropertyValueException już przy save")
    void section3TransientCustomerWithoutCascade() {
        Customer newCustomer = new Customer("Nowy Klient", "nowy@sklep.pl");

        assertThatThrownBy(() -> orders.save(new ShopOrder("ZAM-103", LocalDate.of(2026, 11, 1), newCustomer)))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseInstanceOf(TransientPropertyValueException.class)
                .hasMessageContaining("references an unsaved transient instance")
                .hasMessageContaining("(persist the transient instance)");
        assertThat(SqlCapture.all()).as("ani klient, ani zamówienie nie poszli do bazy").isEmpty();
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: o.customer.email → JOIN do customer; o.customer.id → sama kolumna customer_id (bez joina)")
    void section4QueriesThroughRelation() {
        assertThat(numbers(orders.findByCustomerEmailOrderByOrderNumber("anna@sklep.pl")))
                .containsExactly("ZAM-001", "ZAM-003");
        assertThat(SqlCapture.last()).isEqualTo("select so1_0.id,so1_0.customer_id,so1_0.order_number,so1_0.ordered_on "
                + "from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id where c1_0.email=? "
                + "order by so1_0.order_number");

        assertThat(numbers(orders.findOfCustomer(2L))).containsExactly("ZAM-002", "ZAM-005");
        String byIdJpql = SqlCapture.last();
        assertThat(byIdJpql).isEqualTo("select so1_0.id,so1_0.customer_id,so1_0.order_number,so1_0.ordered_on "
                + "from orders so1_0 where so1_0.customer_id=? order by so1_0.order_number");

        assertThat(numbers(orders.findByCustomerIdOrderByOrderNumber(2L))).containsExactly("ZAM-002", "ZAM-005");
        assertThat(SqlCapture.last()).as("metoda z nazwy — ten sam SQL bez joina").isEqualTo(byIdJpql);
        assertThat(stats.getEntityLoadCount()).as("klienci nie zostali wczytani (tylko 4 zamówienia)").isEqualTo(4);
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: usunięcie klienta z zamówieniami → DataIntegrityViolationException przy flush (klucz obcy)")
    void section5DeletingParentViolatesForeignKey() {
        customers.deleteById(1L);
        assertThat(SqlCapture.all()).as("deleteById najpierw wczytuje klienta; DELETE czeka na flush")
                .containsExactly("select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?");

        assertThatThrownBy(() -> customers.flush())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasCauseInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("Referential integrity constraint violation")
                .hasMessageContaining("FK_ORDERS_CUSTOMER");
        assertThat(SqlCapture.last()).isEqualTo("delete from customer where id=?");
    }

    @Test
    @DisplayName("Sekcja 5: PUŁAPKA — flush przez EntityManager daje surowy wyjątek Hibernate (bez tłumaczenia Springa)")
    void section5FlushThroughEntityManagerIsNotTranslated() {
        customers.deleteById(1L);

        assertThatThrownBy(() -> tem.flush())
                .isExactlyInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("FK_ORDERS_CUSTOMER");
    }

    @Test
    @DisplayName("Sekcja 5: klienta BEZ zamówień można usunąć")
    void section5DeletingCustomerWithoutOrders() {
        customers.deleteById(4L); // Piotr — bez zamówień
        customers.flush();
        tem.clear();

        assertThat(customers.findByEmail("piotr@sklep.pl")).isEmpty();
        assertThat(customers.count()).isEqualTo(3);
    }
}
