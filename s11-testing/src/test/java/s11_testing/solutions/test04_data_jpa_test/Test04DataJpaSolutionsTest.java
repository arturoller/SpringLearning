package s11_testing.solutions.test04_data_jpa_test;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import s11_testing.test04_data_jpa_test.CategoryService;
import s11_testing.test04_data_jpa_test.Category;
import s11_testing.test04_data_jpa_test.Product;
import s11_testing.test04_data_jpa_test.ProductRepository;
import s11_testing.test04_data_jpa_test.ProductSummary;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rozwiązania wzorcowe ćwiczeń Test04DataJpaTest. @Tag("wzorzec") — biegną w domyślnym ./mvnw test. */
@Tag("wzorzec")
class Test04DataJpaSolutionsTest {

    @Nested
    @DataJpaTest(showSql = false)
    @ActiveProfiles("test04")
    class Exercises1To4 {

        @Autowired
        ProductRepository repository;

        @Autowired
        TestEntityManager tem;

        @Autowired
        JdbcClient jdbc;

        @Test
        @DisplayName("Wzorzec 1: projekcja dla kategorii Dom → Kubek, Lampka")
        void projection() {
            assertThat(repository.findByCategoryNameOrderByName("Dom")).extracting(ProductSummary::getName)
                    .containsExactly("Kubek", "Lampka");
        }

        @Test
        @DisplayName("Wzorzec 2: persistAndFlush → wiersz jest w bazie")
        void persistAndFlush() {
            Category home = tem.find(Category.class, 2L);

            tem.persistAndFlush(new Product("Zeszyt", home, new BigDecimal("4.50"), 40));

            assertThat(jdbc.sql("SELECT COUNT(*) FROM product WHERE name = 'Zeszyt'").query(Long.class).single()).isEqualTo(1);
        }

        @Test
        @DisplayName("Wzorzec 3: stara cena z kontekstu, po clear() nowa z bazy")
        void staleThenFresh() {
            repository.findById(2L).orElseThrow();
            jdbc.sql("UPDATE product SET price = 1.00 WHERE id = 2").update();

            assertThat(repository.findById(2L).orElseThrow().getPrice()).isEqualByComparingTo("59.00");
            tem.clear();
            assertThat(repository.findById(2L).orElseThrow().getPrice()).isEqualByComparingTo("1.00");
        }

        @Test
        @Sql("/test04/extra-products.sql")
        @DisplayName("Wzorzec 4: @Sql dodaje Zeszyt — 6 produktów i Zeszyt w findLowStock(40)")
        void sql() {
            assertThat(repository.count()).isEqualTo(6);
            assertThat(repository.findLowStock(40)).extracting(Product::getName).contains("Zeszyt");
        }
    }

    @Nested
    @DataJpaTest(showSql = false)
    @ActiveProfiles("test04")
    @Import(CategoryService.class)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class Exercise5 {

        @Autowired
        CategoryService service;

        @Test
        @DisplayName("Wzorzec 5: bez transakcji testu — LazyInitializationException; wersja z @Transactional działa")
        void lazy() {
            assertThatThrownBy(() -> service.productNamesOf(1L)).isInstanceOf(LazyInitializationException.class);
            assertThat(service.productNamesOfInTransaction(1L)).containsExactly("Klawiatura", "Monitor", "Mysz");
        }
    }
}
