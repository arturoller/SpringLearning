package s08_data_jpa.solutions.jpa03_query_methods;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import s08_data_jpa.jpa03_query_methods.Category;
import s08_data_jpa.jpa03_query_methods.Product;
import s08_data_jpa.jpa03_query_methods.SampleProducts;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Jpa03QueryMethodsExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze.
 * Kontekst: Jpa03SolutionsApplication z tego pakietu.
 */
@DataJpaTest
@ActiveProfiles("jpa03")
@Tag("wzorzec")
class Jpa03QueryMethodsSolutionsTest {

    @Autowired
    ProductSolutionRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    @Test
    @DisplayName("Wzorzec 1: książki tańsze niż 100 zł, od najtańszej")
    void solution1() {
        assertThat(Jpa03QueryMethodsSolutions.solution1(repository)).extracting(Product::getSku)
                .containsExactly("KSI-001", "KSI-003");
    }

    @Test
    @DisplayName("Wzorzec 2: liczba produktów z „kaw” w nazwie (bez względu na wielkość liter)")
    void solution2() {
        assertThat(Jpa03QueryMethodsSolutions.solution2(repository)).isEqualTo(2);
    }

    @Test
    @DisplayName("Wzorzec 3: dwa najtańsze produkty elektroniczne dostępne na stanie")
    void solution3() {
        assertThat(Jpa03QueryMethodsSolutions.solution3(repository)).extracting(Product::getSku)
                .containsExactly("ELE-003", "ELE-004");
    }

    @Test
    @DisplayName("Wzorzec 4: wybrane kategorie, cena 40–500 zł włącznie, posortowane po nazwie")
    void solution4() {
        assertThat(Jpa03QueryMethodsSolutions.solution4(repository, EnumSet.of(Category.SPOZYWCZE, Category.ODZIEZ)))
                .extracting(Product::getName)
                .containsExactly("Kawa ziarnista 1kg", "Kurtka zimowa", "Oliwa z oliwek", "T-shirt bawełniany");
    }
}
