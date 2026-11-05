package s08_data_jpa.jpa06_flyway_with_jpa;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Sekcje 1 i 4 lekcji Jpa06FlywayWithJpa na wycinku {@code @DataJpaTest}: {@code @DataJpaTest} podmienia bazę na
 * wbudowaną H2 o losowej nazwie, ale Flyway z profilu jpa06 i tak uruchamia na niej migracje, a Hibernate (validate)
 * sprawdza schemat. Kontekst wystartował = encje pasują do schematu po V3.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa06")
class Jpa06FlywayWithJpaTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Test
    @DisplayName("Sekcja 1: Flyway wykonał V1–V3, potem Hibernate sprawdził schemat (validate); EMF zależy od flywayInitializer")
    void section1FlywayBeforeHibernate() {
        List<String> versions = JdbcClient.create(dataSource)
                .sql("SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"success\" AND \"version\" IS NOT NULL "
                        + "ORDER BY \"installed_rank\"")
                .query(String.class).list();
        assertThat(versions).containsExactly("1", "2", "3");

        assertThat(emf.getProperties().get("hibernate.hbm2ddl.auto")).isEqualTo("validate");
        assertThat(beanFactory.getBeanDefinition("entityManagerFactory").getDependsOn())
                .as("Boot dopisuje zależność: najpierw migracje, potem EntityManagerFactory")
                .contains("flywayInitializer");
    }

    @Test
    @DisplayName("Sekcja 4: po V3 istniejące wiersze mają wagę z migracji albo wartość domyślną 0")
    void section4DataAfterV3() {
        assertThat(repository.findAll()).extracting(Product::getName, Product::getWeightGrams)
                .containsExactlyInAnyOrder(
                        tuple("Klawiatura", 450),
                        tuple("Mysz", 90),
                        tuple("Kubek", 320),
                        tuple("Lampka", 0),
                        tuple("Zeszyt", 0));
        assertThat(repository.findByWeightGramsGreaterThanOrderByIdAsc(100)).extracting(Product::getName)
                .containsExactly("Klawiatura", "Kubek");
    }
}
