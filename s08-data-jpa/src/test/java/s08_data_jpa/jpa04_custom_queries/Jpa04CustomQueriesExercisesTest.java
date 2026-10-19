package s08_data_jpa.jpa04_custom_queries;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jpa04CustomQueries. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy albo
 * {@code mvnw -pl s08-data-jpa test -Dgroups=cwiczenie -DexcludedGroups=none}). Przed każdym testem w bazie
 * jest 14 produktów sklepu.
 */
@DataJpaTest
@ActiveProfiles("jpa04")
@Tag("cwiczenie")
class Jpa04CustomQueriesExercisesTest {

    @Autowired
    ProductExerciseRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    @Test
    @DisplayName("Ćwiczenie 1: JPQL — produkty o stanie mniejszym niż próg, po stanie i sku")
    void exercise1() {
        assertThat(Jpa04CustomQueries.exercise1(repository, 5)).extracting(Product::getSku)
                .containsExactly("ELE-002", "SPO-003", "DOM-001", "KSI-003", "ELE-004");
    }

    @Test
    @DisplayName("Ćwiczenie 2: rekord StockValue z wartością cena × stan liczoną w zapytaniu")
    void exercise2() {
        List<StockValue> values = Jpa04CustomQueries.exercise2(repository, Category.KSIAZKI);
        assertThat(values).extracting(StockValue::sku).containsExactly("KSI-001", "KSI-002", "KSI-003");
        assertThat(values).extracting(StockValue::value).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("1185.00"), new BigDecimal("1161.00"), new BigDecimal("297.00"));
    }

    @Test
    @DisplayName("Ćwiczenie 3: natywne SQL z LIMIT — najtańsze produkty kategorii")
    void exercise3() {
        assertThat(Jpa04CustomQueries.exercise3(repository, "SPOZYWCZE", 2)).extracting(Product::getSku)
                .containsExactly("SPO-002", "SPO-003");
    }

    @Test
    @DisplayName("Ćwiczenie 4: zbiorcza obniżka cen; findById po niej widzi NOWĄ cenę")
    void exercise4() {
        Product lamp = repository.findAll().stream()
                .filter(product -> product.getSku().equals("DOM-002")).findFirst().orElseThrow();   // w kontekście: 129.00

        assertThat(Jpa04CustomQueries.exercise4(repository, Category.DOM, 10)).isEqualTo(2);

        Product reloaded = repository.findById(lamp.getId()).orElseThrow();
        assertThat(reloaded.getPrice()).as("cena po obniżce o 10%%").isEqualByComparingTo("116.10");
    }
}
