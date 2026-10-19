package s09_jpa_relations.rel01_many_to_one;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test sekcji 3 lekcji Rel01ManyToOne — osobna klasa, bo potrzebuje INNEGO ustawienia Hibernate:
 * {@code hibernate.check_nullability=false} (sprawdzanie nulli wyłączone). Tak właśnie zachowuje się Hibernate 7, gdy na
 * ścieżce klas jest Bean Validation — wtedy pilnowanie NOT NULL zostaje bazie danych.
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.check_nullability=false")
@ActiveProfiles("rel01")
class Rel01ManyToOneNullCheckTest {

    @Autowired
    TestEntityManager em;

    @Test
    @DisplayName("3. Bez check_nullability Hibernate wysyła INSERT z NULL — odrzuca go dopiero baza (NOT NULL z migracji)")
    void nullCheckDisabledDatabaseRejects() {
        // Product ma id z IDENTITY, więc INSERT idzie od razu przy persist — i to on łamie NOT NULL.
        assertThatThrownBy(() -> em.persist(new Product("Bez kategorii", BigDecimal.ONE, null)))
                .isInstanceOf(ConstraintViolationException.class);
    }
}
