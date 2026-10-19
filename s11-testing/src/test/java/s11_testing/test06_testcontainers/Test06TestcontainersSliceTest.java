package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 5 lekcji Test06Testcontainers: kontener jako bean ({@code @Import(Test06Containers.class)}) w teście
 * wycinkowym {@code @JdbcTest}. Wycinek domyślnie podmienia bazę ({@code replace = NON_TEST}) — ale NIE bazę
 * z {@code @ServiceConnection}: test naprawdę działa na PostgreSQL. {@code @Testcontainers(disabledWithoutDocker = true)}
 * służy tu tylko jako warunek „pomiń bez Dockera” (pól {@code @Container} nie ma — kontenerem zarządza Spring).
 */
@Testcontainers(disabledWithoutDocker = true)
@JdbcTest
@ActiveProfiles("test06")
@Import({Test06Containers.class, CustomerRepository.class})   // @JdbcTest nie skanuje @Repository — dołączamy sami
class Test06TestcontainersSliceTest {

    @Autowired
    CustomerRepository repository;

    @Test
    @DisplayName("5. @JdbcTest + kontener jako bean: baza z @ServiceConnection NIE jest podmieniana — to PostgreSQL 18")
    void sliceKeepsServiceConnectionDatabase() {
        assertThat(repository.databaseVersion()).startsWith("PostgreSQL 18");
        assertThat(repository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("5. Na PostgreSQL też działa wycofanie transakcji testu (rollback)")
    void rollbackAlsoOnPostgres() {
        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(repository.findAll()).hasSize(7);
    }
}
