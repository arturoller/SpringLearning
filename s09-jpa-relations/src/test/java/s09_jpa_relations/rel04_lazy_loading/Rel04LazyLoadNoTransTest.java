package s09_jpa_relations.rel04_lazy_loading;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test PUŁAPKI z sekcji 4 lekcji Rel04LazyLoading: {@code hibernate.enable_lazy_load_no_trans=true}. Osobna klasa,
 * bo to inne ustawienie Hibernate (inny kontekst). {@code webEnvironment = NONE} — serwer WWW niepotrzebny.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true")
@ActiveProfiles("rel04")
class Rel04LazyLoadNoTransTest {

    @Autowired
    OrderService orderService;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("4. enable_lazy_load_no_trans: wyjątku nie ma, ale każde leniwe pole poza transakcją = osobne zapytanie")
    void lazyLoadWithoutTransactionHidesTheProblem() {
        ShopOrder order = orderService.findOrder("ZAM-001");          // transakcja zamknięta
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        assertThat(order.getCustomer().getName()).isEqualTo("Jan Kowalski");
        assertThat(order.getLines()).hasSize(2);

        assertThat(stats.getPrepareStatementCount()).as("2 zapytania wysłane poza transakcją").isEqualTo(2);
    }
}
