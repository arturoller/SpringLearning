package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy sekcji 2 i 3 na PEŁNEJ aplikacji lekcji (zwykły JUnit, każdy test własna baza). Test transakcji nie może
 * być zwykłym @JdbcTest: tam sam test jest już jedną dużą transakcją (wycofywaną na końcu), więc nie widać,
 * co zatwierdziła transakcja serwisu. Tu nikt nie otwiera transakcji "z zewnątrz" — widać prawdziwy commit i rollback.
 */
class Jdbc05ServiceTransactionTest {

    private static ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(Jdbc05Application.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=jdbc05",
                        "--spring.datasource.url=jdbc:h2:mem:jdbc05-" + UUID.randomUUID());
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: w pełnej aplikacji bean @Repository jest opakowany w proxy tłumaczące wyjątki")
    void section2RepositoryIsProxied() {
        try (ConfigurableApplicationContext app = start()) {
            ProductRepository repository = app.getBean(ProductRepository.class);
            assertThat(AopUtils.isAopProxy(repository)).isTrue();
            assertThatThrownBy(() -> repository.save(Product.create("Mysz", "dom", new BigDecimal("1"), 1)))
                    .as("JdbcClient już tłumaczył wyjątek; proxy nie zmienia go ani nie opakowuje drugi raz")
                    .isExactlyInstanceOf(DuplicateKeyException.class);
        }
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: udane przeniesienie zmienia oba stany (commit)")
    void section3Commit() {
        try (ConfigurableApplicationContext app = start()) {
            ProductService service = app.getBean(ProductService.class);
            ProductRepository repository = app.getBean(ProductRepository.class);

            service.transferStock(4, 5, 30); // Kubek (100 szt.) → Lampka (0 szt.)

            assertThat(repository.findById(4).orElseThrow().stock()).isEqualTo(70);
            assertThat(repository.findById(5).orElseThrow().stock()).isEqualTo(30);
        }
    }

    @Test
    @DisplayName("Sekcja 3: z @Transactional wyjątek po pierwszym zapisie wycofuje OBA zapisy — stan bez zmian")
    void section3RollbackWithTransaction() {
        try (ConfigurableApplicationContext app = start()) {
            ProductService service = app.getBean(ProductService.class);
            ProductRepository repository = app.getBean(ProductRepository.class);

            assertThatThrownBy(() -> service.transferStock(4, 999, 30))
                    .isInstanceOf(ProductNotFoundException.class);

            assertThat(repository.findById(4).orElseThrow().stock())
                    .as("pierwszy UPDATE został wycofany").isEqualTo(100);
        }
    }

    @Test
    @DisplayName("Sekcja 3: bez transakcji ten sam błąd zostawia towar „zgubiony” — pierwszy zapis już zatwierdzony")
    void section3NoRollbackWithoutTransaction() {
        try (ConfigurableApplicationContext app = start()) {
            ProductService service = app.getBean(ProductService.class);
            ProductRepository repository = app.getBean(ProductRepository.class);

            assertThatThrownBy(() -> service.transferStockWithoutTransaction(4, 999, 30))
                    .isInstanceOf(ProductNotFoundException.class);

            assertThat(repository.findById(4).orElseThrow().stock())
                    .as("30 sztuk zniknęło z magazynu i nigdzie nie trafiło").isEqualTo(70);
        }
    }

    @Test
    @DisplayName("Sekcja 3: serwis ma proxy transakcyjne tylko dzięki @Transactional (bean ProductService jest proxy)")
    void section3ServiceIsTransactionalProxy() {
        try (ConfigurableApplicationContext app = start()) {
            assertThat(AopUtils.isAopProxy(app.getBean(ProductService.class))).isTrue();
        }
    }
}
