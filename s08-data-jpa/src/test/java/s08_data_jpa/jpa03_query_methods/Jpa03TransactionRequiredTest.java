package s08_data_jpa.jpa03_query_methods;

import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 1 i 2 — metody, które potrzebują transakcji, wywołane na PEŁNEJ aplikacji BEZ transakcji z zewnątrz
 * (w {@code @DataJpaTest} cały test jest transakcją, więc tego błędu nie dałoby się zobaczyć). Każdy test ma własną bazę.
 */
class Jpa03TransactionRequiredTest {

    private ConfigurableApplicationContext app;
    private ProductRepository repository;

    @BeforeEach
    void start() {
        app = new SpringApplicationBuilder(Jpa03Application.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=jpa03",
                        "--spring.datasource.url=jdbc:h2:mem:jpa03-" + UUID.randomUUID());
        repository = app.getBean(ProductRepository.class);
    }

    @AfterEach
    void stop() {
        app.close();
    }

    @Test
    @DisplayName("Sekcja 1: deleteBy… z @Transactional działa poza transakcją, removeBy… bez niej — TransactionRequiredException")
    void section1DeleteNeedsTransaction() {
        assertThat(repository.deleteByStock(0)).isEqualTo(2);

        assertThatThrownBy(() -> repository.removeBySupplier("Nowak"))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasCauseInstanceOf(TransactionRequiredException.class)
                .hasMessageContaining("No EntityManager with actual transaction available");
        assertThat(repository.count()).as("8 = 10 − 2 usunięte przez deleteByStock; removeBy niczego nie usunęło").isEqualTo(8);
    }

    @Test
    @DisplayName("Sekcja 2: Stream poza transakcją — InvalidDataAccessApiUsageException; w transakcji działa")
    void section2StreamNeedsTransaction() {
        assertThatThrownBy(() -> {
            try (Stream<Product> products = repository.streamByCategory(Category.HOME)) {
                products.count();
            }
        }).isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("streaming query method without a surrounding transaction");

        TransactionTemplate tx = app.getBean(TransactionTemplate.class); // transakcja „ręcznie”, bez @Transactional
        Long count = tx.execute(status -> {
            try (Stream<Product> products = repository.streamByCategory(Category.HOME)) {
                return products.count();
            }
        });
        assertThat(count).isEqualTo(3);
    }
}
