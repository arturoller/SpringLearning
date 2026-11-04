package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import s09_jpa_relations.SqlCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel04LazyLoading na PEŁNEJ aplikacji (Rel04Application) — bez transakcji testu. Każde wywołanie
 * repozytorium ma tu własną, krótką transakcję: po powrocie z metody sesja Hibernate jest ZAMKNIĘTA, dokładnie
 * jak w prawdziwej aplikacji (kontroler → serwis → repozytorium). Każdy test ma własną bazę H2 (UUID w nazwie).
 */
class Rel04OutsideTransactionTest {

    private static ConfigurableApplicationContext start(String... extraArgs) {
        List<String> args = new ArrayList<>(List.of("--spring.profiles.active=rel04",
                "--spring.datasource.url=jdbc:h2:mem:rel04-" + UUID.randomUUID(), "--" + SqlCapture.PROPERTY));
        args.addAll(List.of(extraArgs));
        return new SpringApplicationBuilder(Rel04Application.class).web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new));
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: leniwa lista i leniwy klient poza transakcją → LazyInitializationException; getId() działa")
    void section2LazyInitializationExceptionOutsideTransaction() {
        try (ConfigurableApplicationContext app = start()) {
            ShopOrderRepository orders = app.getBean(ShopOrderRepository.class);
            ShopOrder order = orders.findById(1L).orElseThrow(); // transakcja repozytorium już się skończyła

            assertThatThrownBy(() -> order.getLines().size())
                    .isInstanceOf(LazyInitializationException.class)
                    .hasMessage("Cannot lazily initialize collection of role "
                            + "'s09_jpa_relations.rel04_lazy_loading.ShopOrder.lines' with key '1' (no session)");
            assertThat(order.getCustomer().getId()).as("id proxy zna bez bazy").isEqualTo(1L);
            assertThatThrownBy(() -> order.getCustomer().getName())
                    .isInstanceOf(LazyInitializationException.class)
                    .hasMessage("Could not initialize proxy [s09_jpa_relations.rel04_lazy_loading.Customer#1] - no session");
        }
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: wyniki serwisu w transakcji, join fetch, @EntityGraph i projekcji działają poza transakcją")
    void section3SolutionsWorkOutsideTransaction() {
        try (ConfigurableApplicationContext app = start()) {
            ShopOrderRepository orders = app.getBean(ShopOrderRepository.class);
            OrderReadService readService = app.getBean(OrderReadService.class);

            assertThat(readService.describe(3L)).isEqualTo("ZAM-003 | Jan Kowalski | Kubek ×4, Mysz ×1");

            ShopOrder withDetails = orders.findWithDetailsById(1L).orElseThrow();
            assertThat(withDetails.getCustomer().getName()).isEqualTo("Anna Nowak");
            assertThat(withDetails.getLines()).extracting(line -> line.getProduct().getName())
                    .containsExactly("Klawiatura", "Mysz");

            ShopOrder withCustomer = orders.findWithCustomerByOrderNumber("ZAM-002").orElseThrow();
            assertThat(withCustomer.getCustomer().getName()).isEqualTo("Anna Nowak");
            assertThatThrownBy(() -> withCustomer.getLines().size()).as("pozycji graf nie obejmował")
                    .isInstanceOf(LazyInitializationException.class);

            assertThat(orders.findViewByOrderNumber("ZAM-002")).contains(new OrderView("ZAM-002", "Anna Nowak"));
        }
    }

    @Test
    @DisplayName("Sekcja 3: ANTYWZORZEC enable_lazy_load_no_trans — „działa”, ale każde proxy otwiera własną sesję i połączenie")
    void section3EnableLazyLoadNoTransAntiPattern() {
        try (ConfigurableApplicationContext app = start("--spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true")) {
            ShopOrderRepository orders = app.getBean(ShopOrderRepository.class);
            Statistics stats = app.getBean(EntityManagerFactory.class).unwrap(SessionFactory.class).getStatistics();
            ShopOrder order = orders.findById(3L).orElseThrow();
            stats.clear();
            SqlCapture.clear();

            List<String> names = order.getLines().stream().map(line -> line.getProduct().getName()).toList();

            assertThat(names).containsExactly("Kubek", "Mysz");
            assertThat(SqlCapture.all()).as("pozycje + każdy produkt osobno").hasSize(3);
            assertThat(stats.getSessionOpenCount()).as("osobna sesja na KAŻDE leniwe dociągnięcie").isEqualTo(3);
        }
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: spring.jpa.open-in-view=true w aplikacji BEZ weba nic nie zmienia — brak interceptora, wyjątek zostaje")
    void section4OpenInViewHasNoEffectWithoutWeb() {
        try (ConfigurableApplicationContext app = start("--spring.jpa.open-in-view=true")) {
            // Interceptor OSIV rejestruje JpaWebConfiguration tylko w aplikacji webowej (servlet + Spring MVC).
            // W tym module nie ma nawet klas Spring MVC na ścieżce klas (classpath):
            assertThat(ClassUtils.isPresent("org.springframework.web.servlet.config.annotation.WebMvcConfigurer", null))
                    .isFalse();
            assertThat(app.containsBean("openEntityManagerInViewInterceptor")).isFalse();

            ShopOrder order = app.getBean(ShopOrderRepository.class).findById(1L).orElseThrow();
            assertThatThrownBy(() -> order.getLines().size()).isInstanceOf(LazyInitializationException.class);
        }
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: Hibernate.initialize w serwisie — lista działa poza transakcją, ale produkty pozycji nadal są proxy")
    void section5InitializeIsOneLevelOnly() {
        try (ConfigurableApplicationContext app = start()) {
            ShopOrder order = app.getBean(OrderReadService.class).findWithLinesInitialized(1L);

            assertThat(Hibernate.isInitialized(order.getLines())).isTrue();
            assertThat(order.getLines()).hasSize(2);
            assertThat(order.getLines().getFirst().getProduct().getId()).isEqualTo(1L);
            assertThatThrownBy(() -> order.getLines().getFirst().getProduct().getName())
                    .isInstanceOf(LazyInitializationException.class);
        }
    }

    @Test
    @DisplayName("Sekcja 5: getReferenceById poza transakcją — proxy z id, ale getter rzuca LazyInitializationException")
    void section5GetReferenceByIdOutsideTransaction() {
        try (ConfigurableApplicationContext app = start()) {
            CustomerRepository customers = app.getBean(CustomerRepository.class);
            Customer reference = customers.getReferenceById(2L);
            assertThat(reference.getId()).isEqualTo(2L);
            assertThatThrownBy(reference::getName).isInstanceOf(LazyInitializationException.class);

            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            String name = tx.execute(status -> customers.getReferenceById(2L).getName());
            assertThat(name).as("w transakcji proxy się wczytuje").isEqualTo("Jan Kowalski");
        }
    }
}
