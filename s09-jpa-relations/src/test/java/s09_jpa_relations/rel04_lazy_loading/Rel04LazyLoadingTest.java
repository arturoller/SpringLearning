package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.collection.spi.PersistentBag;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Rel04LazyLoading — „WYNIK” każdej sekcji.
 *
 * <p>{@code @SpringBootTest} (cały kontekst: serwis, kontroler, autokonfiguracja open-in-view), a NIE
 * {@code @DataJpaTest}: ten drugi otacza każdy test transakcją, więc sesja byłaby otwarta i
 * LazyInitializationException nigdy by się nie pojawił. Tu testy NIE są transakcyjne — transakcję otwieramy
 * jawnie przez TransactionTemplate (szablon transakcji) albo wywołując metody serwisu {@code @Transactional}.
 * {@code @AutoConfigureMockMvc} — MockMvc do zapytań HTTP bez prawdziwego serwera (sekcja 5).
 * Dane startowe (ZAM-001 Jana Kowalskiego z 2 pozycjami...) wstawia migracja V2__sample_data.sql.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("rel04")
class Rel04LazyLoadingTest {

    @Autowired
    OrderService orderService;

    @Autowired
    ShopOrderRepository orders;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    EntityManagerFactory emf;

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Domyślne fetch w adnotacjach JPA: @ManyToOne/@OneToOne EAGER, @OneToMany/@ManyToMany LAZY")
    void defaultFetchTypes() throws NoSuchMethodException {
        assertThat(ManyToOne.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.EAGER);
        assertThat(OneToOne.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.EAGER);
        assertThat(OneToMany.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.LAZY);
        assertThat(ManyToMany.class.getMethod("fetch").getDefaultValue()).isEqualTo(FetchType.LAZY);
    }

    @Test
    @DisplayName("2. Proxy: Customer$HibernateProxy, instanceof Customer; getId bez SELECT, getName — 1 SELECT")
    void proxyFromInside() {
        tx.executeWithoutResult(status -> {
            Customer customer = orders.findByNumber("ZAM-001").orElseThrow().getCustomer();
            Statistics stats = statistics();

            assertThat(customer.getClass().getSimpleName()).isEqualTo("Customer$HibernateProxy");
            assertThat(customer).isInstanceOf(HibernateProxy.class).isInstanceOf(Customer.class);
            assertThat(customer.getClass()).as("klasa proxy to NIE Customer.class").isNotEqualTo(Customer.class);
            assertThat(Hibernate.isInitialized(customer)).isFalse();

            assertThat(customer.getId()).isNotNull();
            assertThat(stats.getPrepareStatementCount()).as("getId — bez zapytania").isZero();

            assertThat(customer.getName()).isEqualTo("Jan Kowalski");
            assertThat(stats.getPrepareStatementCount()).as("getName — SELECT z customers").isEqualTo(1);
            assertThat(Hibernate.unproxy(customer)).isExactlyInstanceOf(Customer.class).isNotSameAs(customer);
        });
    }

    @Test
    @DisplayName("2. Leniwa kolekcja: PersistentBag; pierwsze size() wczytuje całą kolekcję jednym SELECT")
    void lazyCollectionFromInside() {
        tx.executeWithoutResult(status -> {
            List<OrderLine> lines = orders.findByNumber("ZAM-001").orElseThrow().getLines();
            Statistics stats = statistics();

            assertThat(lines).isInstanceOf(PersistentBag.class);
            assertThat(Hibernate.isInitialized(lines)).isFalse();

            assertThat(lines.size()).isEqualTo(2);
            assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
            assertThat(Hibernate.isInitialized(lines)).isTrue();
        });
    }

    @Test
    @DisplayName("3. Poza sesją getId() na proxy działa — id jest zapisane w proxy")
    void proxyIdWorksOutsideSession() {
        ShopOrder order = orderService.findOrder("ZAM-001");   // transakcja serwisu już zamknięta

        assertThat(order.getCustomer().getId()).isNotNull();
        assertThat(Hibernate.isInitialized(order.getCustomer())).isFalse();
    }

    @Test
    @DisplayName("3. Poza sesją getName() na proxy → LazyInitializationException „Could not initialize proxy ... - no session”")
    void proxyOutsideSessionThrows() {
        ShopOrder order = orderService.findOrder("ZAM-001");
        Customer customer = order.getCustomer();

        assertThatThrownBy(customer::getName)
                .isInstanceOf(LazyInitializationException.class)
                .hasMessage("Could not initialize proxy [s09_jpa_relations.rel04_lazy_loading.Customer#"
                        + customer.getId() + "] - no session");
    }

    @Test
    @DisplayName("3. Poza sesją leniwa kolekcja → „Cannot lazily initialize collection of role ... (no session)”")
    void collectionOutsideSessionThrows() {
        ShopOrder order = orderService.findOrder("ZAM-001");

        assertThatThrownBy(() -> order.getLines().size())
                .isInstanceOf(LazyInitializationException.class)
                .hasMessage("Cannot lazily initialize collection of role "
                        + "'s09_jpa_relations.rel04_lazy_loading.ShopOrder.lines' with key '" + order.getId()
                        + "' (no session)");
    }

    @Test
    @DisplayName("3. PUŁAPKA: Hibernate.getClass(proxy) inicjalizuje proxy — poza sesją też rzuca wyjątek")
    void hibernateGetClassInitializesProxy() {
        Customer customer = orderService.findOrder("ZAM-001").getCustomer();

        assertThatThrownBy(() -> Hibernate.getClass(customer)).isInstanceOf(LazyInitializationException.class);
    }

    @Test
    @DisplayName("4. join fetch: klient wczytany w tym samym SELECT (1 zapytanie) — działa także po transakcji")
    void joinFetchLoadsCustomer() {
        Statistics stats = statistics();

        ShopOrder order = orderService.findOrderWithCustomer("ZAM-001");

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(order.getCustomer()).isExactlyInstanceOf(Customer.class);   // prawdziwy obiekt, nie proxy
        assertThat(order.getCustomer().getName()).isEqualTo("Jan Kowalski");
    }

    @Test
    @DisplayName("4. @EntityGraph: pozycje wczytane w tym samym SELECT (1 zapytanie) — działa także po transakcji")
    void entityGraphLoadsLines() {
        Statistics stats = statistics();

        ShopOrder order = orderService.findOrderWithLines("ZAM-001");

        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        assertThat(order.getLines()).extracting(OrderLine::getProductName)
                .containsExactly("Laptop Pro 14", "Słuchawki BT");
    }

    @Test
    @DisplayName("4. Leniwe pole użyte WEWNĄTRZ metody @Transactional — działa")
    void lazyFieldInsideTransactionWorks() {
        assertThat(orderService.customerNameOf("ZAM-002")).isEqualTo("Maria Nowak");
    }

    @Test
    @DisplayName("4. PUŁAPKA: opakowanie unmodifiableList oszukuje Hibernate.isInitialized (zawsze true) i initialize")
    void unmodifiableWrapperFoolsIsInitialized() {
        tx.executeWithoutResult(status -> {
            ShopOrder order = orders.findByNumber("ZAM-001").orElseThrow();
            List<OrderLine> wrapper = Collections.unmodifiableList(order.getLines());

            assertThat(Hibernate.isInitialized(wrapper)).as("opakowanie — „zwykły obiekt”").isTrue();
            Hibernate.initialize(wrapper);                                      // nic nie robi
            assertThat(Hibernate.isInitialized(order.getLines())).as("prawdziwa kolekcja").isFalse();
        });
    }

    @Test
    @DisplayName("5. open-in-view (domyślnie włączone): Boot rejestruje OpenEntityManagerInViewInterceptor")
    void openInViewInterceptorRegistered() {
        assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).hasSize(1);
    }

    @Test
    @DisplayName("5. open-in-view: kontroler czyta leniwe pole PO transakcji — działa, połączenie zajęte do końca żądania")
    void openInViewLetsControllerReadLazyField() throws Exception {
        mvc.perform(get("/api/rel04/orders/ZAM-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Jan Kowalski"))
                .andExpect(jsonPath("$.busyConnections").value(1));
    }

    @Test
    @DisplayName("5. open-in-view: nawet bez leniwych pól połączenie jest zajęte do końca żądania (busyConnections = 1)")
    void openInViewHoldsConnection() throws Exception {
        mvc.perform(get("/api/rel04/orders/ZAM-001/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOWE"))
                .andExpect(jsonPath("$.busyConnections").value(1));
    }

    private Statistics statistics() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }
}
