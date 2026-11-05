package s11_testing.test04_data_jpa_test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Test04DataJpaTest: {@code @Commit} / {@code @Rollback(false)} (zatwierdź transakcję testu) i
 * zapis bez transakcji testu. UWAGA: kolejność testów jest tu USTALONA ({@code @Order}) celowo — pokazujemy, co
 * jeden test zostawia następnemu. Klasa ma własną konfigurację ({@code properties}), więc własny kontekst i własną
 * losową bazę: zatwierdzone wiersze NIE przeciekają do pozostałych klas lekcji.
 */
@DataJpaTest(showSql = false, properties = "demo.commit=true")
@ActiveProfiles("test04")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Test04CommitTest {

    @Autowired
    ProductRepository repository;

    private Product product(String name) {
        return new Product(name, repository.findById(1L).orElseThrow().getCategory(), new BigDecimal("4.50"), 1);
    }

    private boolean exists(String name) {
        return repository.findAll().stream().anyMatch(p -> p.getName().equals(name));
    }

    @Test
    @Order(1)
    @Commit   // commit = zatwierdź: po tym teście transakcja NIE zostanie wycofana
    @DisplayName("Sekcja 3 (1/5): @Commit — wstawiony produkt zostaje w bazie po teście")
    void committed() {
        repository.save(product("Zeszyt-C"));
    }

    @Test
    @Order(2)
    @DisplayName("Sekcja 3 (2/5): kolejny test widzi produkt zatwierdzony przez poprzedni; własny wpis tego testu zostanie wycofany")
    void seesCommittedAndRollsBackOwn() {
        assertThat(exists("Zeszyt-C")).isTrue();

        repository.save(product("Zeszyt-E"));   // domyślnie: rollback

        assertThat(exists("Zeszyt-E")).isTrue();
    }

    @Test
    @Order(3)
    @Rollback(false)   // to samo co @Commit; tu nic nie zmieniamy, sprawdzamy tylko stan
    @DisplayName("Sekcja 3 (3/5): wpis z wycofanego testu zniknął, a zatwierdzony został; @Rollback(false) = @Commit")
    void rolledBackIsGone() {
        assertThat(exists("Zeszyt-E")).isFalse();
        assertThat(exists("Zeszyt-C")).isTrue();
    }

    @Test
    @Order(4)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)   // bez transakcji testu: save zatwierdza się samo
    @DisplayName("Sekcja 3 (4/5): NOT_SUPPORTED — repozytorium zapisuje we własnej transakcji i od razu ją zatwierdza")
    void notSupportedCommitsImmediately() {
        assertThat(TestTransaction.isActive()).isFalse();

        repository.save(product("Zeszyt-D"));
    }

    @Test
    @Order(5)
    @DisplayName("Sekcja 3 (5/5): zapis z testu bez transakcji przetrwał — nikt go nie wycofał")
    void notSupportedSurvived() {
        assertThat(exists("Zeszyt-D")).isTrue();
    }
}
