package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Testy-ĆWICZENIA lekcji Test04DataJpaTest: TY piszesz test (usuń linię z UnsupportedOperationException; na klasie
 * dodaj {@code @DataJpaTest(showSql = false)} i {@code @ActiveProfiles("test04")} — najlepiej w nowej klasie).
 * @Tag("cwiczenie") — uruchamiasz sam. Rozwiązania wzorcowe: pakiet s11_testing.solutions.test04_data_jpa_test.
 */
@Tag("cwiczenie")
class Test04DataJpaExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: projekcja — findByCategoryNameOrderByName(\"Dom\") zwraca Kubek i Lampkę (w tej kolejności)")
    void exercise1() {
        // TODO: @Autowired ProductRepository; assertThat(...).extracting(ProductSummary::getName).containsExactly(...)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 2: TestEntityManager.persistAndFlush zapisuje nowy produkt — sprawdź wiersz w bazie przez JdbcClient")
    void exercise2() {
        // TODO: Category dom = tem.find(Category.class, 2L); tem.persistAndFlush(new Product(...)); jdbc.sql("SELECT ...")
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 3: PUŁAPKA kontekstu — po UPDATE przez JdbcClient findById zwraca starą cenę, a po tem.clear() nową")
    void exercise3() {
        // TODO: findById(2L) → jdbc UPDATE → findById (stara cena) → tem.clear() → findById (nowa cena)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 4: @Sql — po skrypcie /test04/extra-products.sql produktów jest 6, a findLowStock(40) zawiera Zeszyt")
    void exercise4() {
        // TODO: @Sql("/test04/extra-products.sql") na metodzie; repository.count() i repository.findLowStock(40)
        throw new UnsupportedOperationException("TODO");
    }

    @Test
    @DisplayName("Ćwiczenie 5: bez transakcji testu (NOT_SUPPORTED) CategoryService.productNamesOf rzuca LazyInitializationException, a wersja z @Transactional działa")
    void exercise5() {
        // TODO: @Import(CategoryService.class) + @Transactional(propagation = Propagation.NOT_SUPPORTED) na klasie
        throw new UnsupportedOperationException("TODO");
    }
}
