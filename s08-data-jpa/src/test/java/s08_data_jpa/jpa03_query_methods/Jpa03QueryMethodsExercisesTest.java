package s08_data_jpa.jpa03_query_methods;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Jpa03QueryMethods. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy albo
 * {@code mvnw -pl s08-data-jpa test -Dgroups=cwiczenie -DexcludedGroups=none}). Przed każdym testem w bazie
 * jest 14 produktów sklepu.
 */
@DataJpaTest
@ActiveProfiles("jpa03")
@Tag("cwiczenie")
class Jpa03QueryMethodsExercisesTest {

    @Autowired
    ProductExerciseRepository repository;

    @BeforeEach
    void insertShopProducts() {
        repository.saveAllAndFlush(SampleProducts.all());
    }

    @Test
    @DisplayName("Ćwiczenie 1: książki tańsze niż 100 zł, od najtańszej")
    void exercise1() {
        assertThat(Jpa03QueryMethods.exercise1(repository)).extracting(Product::getSku)
                .containsExactly("KSI-001", "KSI-003");
    }

    @Test
    @DisplayName("Ćwiczenie 2: liczba produktów z „kaw” w nazwie (bez względu na wielkość liter)")
    void exercise2() {
        assertThat(Jpa03QueryMethods.exercise2(repository)).isEqualTo(2);
    }

    @Test
    @DisplayName("Ćwiczenie 3: dwa najtańsze produkty elektroniczne dostępne na stanie")
    void exercise3() {
        assertThat(Jpa03QueryMethods.exercise3(repository)).extracting(Product::getSku)
                .containsExactly("ELE-003", "ELE-004");
    }

    @Test
    @DisplayName("Ćwiczenie 4: wybrane kategorie, cena 40–500 zł włącznie, posortowane po nazwie")
    void exercise4() {
        assertThat(Jpa03QueryMethods.exercise4(repository, EnumSet.of(Category.SPOZYWCZE, Category.ODZIEZ)))
                .extracting(Product::getName)
                .containsExactly("Kawa ziarnista 1kg", "Kurtka zimowa", "Oliwa z oliwek", "T-shirt bawełniany");
    }
}
