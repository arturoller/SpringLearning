package s11_testing.test04_data_jpa_test;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 3 lekcji Test04DataJpaTest: {@code @Transactional(propagation = Propagation.NOT_SUPPORTED)} na klasie WYŁĄCZA
 * transakcję testu — test działa tak, jak kod aplikacji bez własnej transakcji. Teraz ten sam serwis, który w
 * {@code Test04TransactionTest} "działał", rzuca {@code LazyInitializationException}. (Tu nic nie zapisujemy,
 * więc dane z migracji zostają nietknięte.)
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test04")
@Import(CategoryService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED) // not supported = bez transakcji: test jej nie dostaje
class Test04NoTransactionTest {

    @Autowired
    CategoryService categoryService;

    @Test
    @DisplayName("Sekcja 3: z NOT_SUPPORTED test nie ma transakcji")
    void noTransactionInTest() {
        assertThat(TestTransaction.isActive()).isFalse();
    }

    @Test
    @DisplayName("Sekcja 3: bez transakcji testu serwis czytający leniwą kolekcję rzuca LazyInitializationException — jak w aplikacji")
    void lazyInitializationAppearsWithoutTestTransaction() {
        assertThatThrownBy(() -> categoryService.productNamesOf(1L))
                .isInstanceOf(LazyInitializationException.class)
                .hasMessageContaining("Category.products");
    }

    @Test
    @DisplayName("Sekcja 3: poprawiona metoda z @Transactional działa także bez transakcji testu")
    void fixedServiceWorks() {
        assertThat(categoryService.productNamesOfInTransaction(1L)).containsExactly("Klawiatura", "Monitor", "Mysz");
    }
}
