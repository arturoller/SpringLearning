package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test sekcji 5 lekcji Rel05NPlusOne z globalnym ustawieniem
 * {@code spring.jpa.properties.hibernate.default_batch_fetch_size=10} — osobna klasa, bo to inne ustawienie
 * Hibernate (inny kontekst). Kod pętli jest IDENTYCZNY jak w testach N+1 z Rel05NPlusOneTest — zmienia się tylko
 * liczba zapytań.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.default_batch_fetch_size=10")
@ActiveProfiles("rel05")
class Rel05NPlusOneBatchFetchTest {

    @Autowired
    ShopOrderRepository orders;

    @Autowired
    EntityManagerFactory emf;

    Statistics stats;

    @BeforeEach
    void clearStatistics() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
    }

    @Test
    @DisplayName("5. default_batch_fetch_size=10: zamówienia + klienci — 2 zapytania zamiast 6")
    void batchFetchManyToOne() {
        orders.findAllByOrderByNumber().forEach(order -> order.getCustomer().getName());

        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("5. default_batch_fetch_size=10: zamówienia + pozycje — 2 zapytania zamiast 7")
    void batchFetchCollection() {
        int totalLines = orders.findAllByOrderByNumber().stream().mapToInt(order -> order.getLines().size()).sum();

        assertThat(totalLines).isEqualTo(10);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }
}
