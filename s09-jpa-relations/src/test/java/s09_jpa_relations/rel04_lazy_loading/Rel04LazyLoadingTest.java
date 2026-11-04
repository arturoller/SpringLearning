package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import s09_jpa_relations.SqlCapture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel04LazyLoading na wycinku {@code @DataJpaTest}. UWAGA — to jest sedno sekcji 2: każdy test
 * wycinka działa w JEDNEJ transakcji (wycofywanej na końcu), więc sesja Hibernate jest otwarta przez cały test
 * i leniwe relacje „po prostu działają”. Prawdziwe granice transakcji: Rel04OutsideTransactionTest (pełna aplikacja).
 */
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel04")
@Import(OrderReadService.class) // wycinek JPA nie skanuje @Service — dołączamy serwis ręcznie
class Rel04LazyLoadingTest {

    @Autowired
    private ShopOrderRepository orders;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private OrderReadService readService;

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
    @DisplayName("Sekcja 1: domyślne typy pobierania w JPA — „do jednego” EAGER, „do wielu” LAZY")
    void section1DefaultFetchTypes() throws Exception {
        assertThat(ManyToOne.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.EAGER);
        assertThat(OneToOne.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.EAGER);
        assertThat(OneToMany.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.LAZY);
        assertThat(ManyToMany.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.LAZY);
    }

    @Test
    @DisplayName("Sekcja 1: po find — klient to proxy $HibernateProxy, lista to PersistentBag, oba niezainicjalizowane")
    void section1ProxiesAndPersistentCollections() {
        ShopOrder order = orders.findById(1L).orElseThrow();
        assertThat(SqlCapture.all()).containsExactly(
                "select so1_0.id,so1_0.customer_id,so1_0.order_number from orders so1_0 where so1_0.id=?");

        Customer customer = order.getCustomer();
        assertThat(customer.getClass().getName())
                .startsWith("s09_jpa_relations.rel04_lazy_loading.Customer$HibernateProxy");
        assertThat(customer).isInstanceOf(Customer.class); // proxy to PODKLASA encji
        assertThat(Hibernate.isInitialized(customer)).isFalse();
        assertThat(order.getLines().getClass().getSimpleName()).isEqualTo("PersistentBag");
        assertThat(Hibernate.isInitialized(order.getLines())).isFalse();

        assertThat(customer.getId()).isEqualTo(1L);
        assertThat(SqlCapture.all()).as("getId() na proxy — bez SQL").hasSize(1);

        assertThat(customer.getName()).isEqualTo("Anna Nowak");
        assertThat(SqlCapture.last())
                .isEqualTo("select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?");
        assertThat(Hibernate.isInitialized(customer)).isTrue();
        assertThat(Hibernate.getClass(customer)).as("prawdziwa klasa encji za proxy").isEqualTo(Customer.class);

        assertThat(customer.getOrders().getClass().getSimpleName()).isEqualTo("PersistentSet");
        assertThat(order.getLines()).hasSize(2); // size() inicjalizuje listę
        assertThat(SqlCapture.last()).isEqualTo("select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,"
                + "l1_0.unit_price from order_line l1_0 where l1_0.order_id=? order by l1_0.id");
        assertThat(Hibernate.isInitialized(order.getLines())).isTrue();
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: PUŁAPKA testów — w @DataJpaTest leniwa lista działa „po wyjściu z repozytorium”, bo test ma transakcję")
    void section2DataJpaTestHidesLazyInitializationException() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).as("transakcja testu").isTrue();

        ShopOrder order = orders.findById(1L).orElseThrow();
        assertThat(order.getLines()).extracting(line -> line.getProduct().getName())
                .as("w produkcji tu byłby LazyInitializationException").containsExactly("Klawiatura", "Mysz");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // not supported = bez transakcji testu
    @DisplayName("Sekcja 2: bez transakcji testu (NOT_SUPPORTED) wycinek pokazuje prawdę — LazyInitializationException")
    void section2WithoutTestTransactionLazyInitializationExceptionIsVisible() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();

        ShopOrder order = orders.findById(1L).orElseThrow();
        assertThatThrownBy(() -> order.getLines().size())
                .isInstanceOf(LazyInitializationException.class)
                .hasMessage("Cannot lazily initialize collection of role "
                        + "'s09_jpa_relations.rel04_lazy_loading.ShopOrder.lines' with key '1' (no session)");
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: serwis @Transactional(readOnly = true) czyta leniwe relacje w transakcji — 4 zapytania")
    void section3ServiceTransaction() {
        assertThat(readService.describe(1L)).isEqualTo("ZAM-001 | Anna Nowak | Klawiatura ×1, Mysz ×2");
        assertThat(SqlCapture.all()).as("zamówienie, pozycje, 2 produkty, klient").hasSize(5);
    }

    @Test
    @DisplayName("Sekcja 3: join fetch — zamówienie, klient, pozycje i produkty jednym zapytaniem")
    void section3JoinFetch() {
        ShopOrder order = orders.findWithDetailsById(1L).orElseThrow();

        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "l1_0.order_id,l1_0.id,l1_0.product_id,p1_0.id,p1_0.name,p1_0.price,l1_0.quantity,l1_0.unit_price,so1_0.order_number "
                + "from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id "
                + "join order_line l1_0 on so1_0.id=l1_0.order_id join product p1_0 on p1_0.id=l1_0.product_id "
                + "where so1_0.id=? order by l1_0.id");
        assertThat(Hibernate.isInitialized(order.getCustomer())).isTrue();
        assertThat(order.getLines()).extracting(line -> line.getProduct().getName()).containsExactly("Klawiatura", "Mysz");
        assertThat(SqlCapture.all()).as("nic się już nie dociąga").hasSize(1);
    }

    @Test
    @DisplayName("Sekcja 3: @EntityGraph — klient wczytany JOIN-em bez pisania JPQL; projekcja — bez encji")
    void section3EntityGraphAndProjection() {
        ShopOrder order = orders.findWithCustomerByOrderNumber("ZAM-003").orElseThrow();
        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "so1_0.order_number from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id "
                + "where so1_0.order_number=?");
        assertThat(Hibernate.isInitialized(order.getCustomer())).isTrue();
        assertThat(Hibernate.isInitialized(order.getLines())).as("graf mówił tylko o kliencie").isFalse();

        tem.clear();
        stats.clear();
        SqlCapture.clear();
        assertThat(orders.findViewByOrderNumber("ZAM-003")).contains(new OrderView("ZAM-003", "Jan Kowalski"));
        assertThat(SqlCapture.all()).containsExactly("select so1_0.order_number,c1_0.name from orders so1_0 "
                + "join customer c1_0 on c1_0.id=so1_0.customer_id where so1_0.order_number=?");
        assertThat(stats.getEntityLoadCount()).as("projekcja nie wczytuje encji").isZero();
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: getReferenceById — proxy bez SQL; SELECT przy pierwszym getterze innym niż getId")
    void section5GetReferenceById() {
        Customer reference = customers.getReferenceById(2L);
        assertThat(SqlCapture.all()).isEmpty();
        assertThat(Hibernate.isInitialized(reference)).isFalse();
        assertThat(reference.getId()).isEqualTo(2L);
        assertThat(SqlCapture.all()).isEmpty();

        assertThat(reference.getName()).isEqualTo("Jan Kowalski");
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Sekcja 5: getReferenceById na nieistniejącym id — wyjątek dopiero przy użyciu proxy")
    void section5GetReferenceByIdOfMissingEntity() {
        Customer ghost = customers.getReferenceById(99L); // jeszcze bez błędu i bez SQL
        assertThat(SqlCapture.all()).isEmpty();

        assertThatThrownBy(ghost::getName)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("s09_jpa_relations.rel04_lazy_loading.Customer")
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Sekcja 5: Hibernate.initialize — wczytuje wskazane proxy/kolekcję od razu")
    void section5HibernateInitialize() {
        Customer customer = customers.findById(1L).orElseThrow();
        assertThat(Hibernate.isInitialized(customer.getOrders())).isFalse();

        Hibernate.initialize(customer.getOrders());

        assertThat(Hibernate.isInitialized(customer.getOrders())).isTrue();
        assertThat(SqlCapture.all()).hasSize(2);
        assertThat(customer.getOrders()).extracting(ShopOrder::getOrderNumber)
                .containsExactlyInAnyOrder("ZAM-001", "ZAM-002");
    }
}
