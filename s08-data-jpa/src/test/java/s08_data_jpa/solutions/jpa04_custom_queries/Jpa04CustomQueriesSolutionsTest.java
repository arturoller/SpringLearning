package s08_data_jpa.solutions.jpa04_custom_queries;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import s08_data_jpa.jpa04_custom_queries.Category;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.SampleProducts;
import s08_data_jpa.jpa04_custom_queries.StockValue;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jpa04CustomQueriesExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * Kontekst: Jpa04SolutionsApplication z tego pakietu.
 */
@DataJpaTest
@ActiveProfiles("jpa04")
@Tag("wzorzec")
class Jpa04CustomQueriesSolutionsTest {

    @Autowired
    ProductSolutionRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    @Test
    @DisplayName("Wzorzec 1: JPQL — produkty o stanie mniejszym niż próg, po stanie i sku")
    void solution1() {
        assertThat(Jpa04CustomQueriesSolutions.solution1(repository, 5)).extracting(Product::getSku)
                .containsExactly("ELE-002", "SPO-003", "DOM-001", "KSI-003", "ELE-004");
    }

    @Test
    @DisplayName("Wzorzec 2: rekord StockValue z wartością cena × stan liczoną w zapytaniu")
    void solution2() {
        List<StockValue> values = Jpa04CustomQueriesSolutions.solution2(repository, Category.KSIAZKI);
        assertThat(values).extracting(StockValue::sku).containsExactly("KSI-001", "KSI-002", "KSI-003");
        assertThat(values).extracting(StockValue::value).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1185.00"), new BigDecimal("1161.00"), new BigDecimal("297.00"));
    }

    @Test
    @DisplayName("Wzorzec 3: natywne SQL z LIMIT — najtańsze produkty kategorii")
    void solution3() {
        assertThat(Jpa04CustomQueriesSolutions.solution3(repository, "SPOZYWCZE", 2)).extracting(Product::getSku)
                .containsExactly("SPO-002", "SPO-003");
    }

    @Test
    @DisplayName("Wzorzec 4: zbiorcza obniżka cen; findById po niej widzi NOWĄ cenę")
    void solution4() {
        Product lamp = repository.findAll().stream()
                .filter(product -> product.getSku().equals("DOM-002")).findFirst().orElseThrow();

        assertThat(Jpa04CustomQueriesSolutions.solution4(repository, Category.DOM, 10)).isEqualTo(2);

        Product reloaded = repository.findById(lamp.getId()).orElseThrow();
        assertThat(reloaded.getPrice()).isEqualByComparingTo("116.10");
    }
}
