package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 3 lekcji Test04DataJpaTest: transakcja testu. {@code @DataJpaTest} owija KAŻDY test w transakcję i po teście
 * ją wycofuje (rollback) — dane nie przeciekają do następnych testów. To wygodne, ale ukrywa dwa rodzaje błędów:
 * {@code LazyInitializationException} (cała logika testu działa "w środku" jednej transakcji) oraz błędy, które
 * baza zgłasza dopiero przy zapisie (flush/commit). Odesłania: s09_jpa_relations/Rel04LazyLoading,
 * s10_transactions/Tx01Transactional.
 */
@DataJpaTest(showSql = false)
@ActiveProfiles("test04")
@Import(CategoryService.class)   // serwis nie należy do wycinka — dokładamy go jawnie
class Test04TransactionTest {

    @Autowired
    CategoryService categoryService;

    @Autowired
    ProductRepository repository;

    @Test
    @DisplayName("Sekcja 3: test działa w transakcji, która po teście zostanie WYCOFANA")
    void testRunsInTransaction() {
        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
    }

    @Test
    @DisplayName("Sekcja 3: PUŁAPKA — serwis czytający leniwą kolekcję BEZ @Transactional działa w teście, bo test sam trzyma transakcję")
    void lazyInitializationHiddenByTestTransaction() {
        List<String> names = categoryService.productNamesOf(1L);   // w aplikacji: LazyInitializationException

        assertThat(names).containsExactly("Klawiatura", "Monitor", "Mysz");
    }

    @Test
    @DisplayName("Sekcja 3: PUŁAPKA — naruszenie ograniczenia UNIQUE przy zmianie nazwy wychodzi dopiero przy flush, a bez flush test jest zielony")
    void constraintViolationHiddenUntilFlush() {
        Product keyboard = repository.findById(1L).orElseThrow();
        keyboard.setName("Mysz");   // w bazie jest już produkt o tej nazwie (kolumna ma UNIQUE)

        assertThatCode(() -> repository.findById(1L)).as("bez flush baza jeszcze nic nie wie o zmianie").doesNotThrowAnyException();
        assertThatThrownBy(() -> repository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);   // dopiero teraz: UPDATE → błąd bazy (tak samo przy commit)
    }
}
