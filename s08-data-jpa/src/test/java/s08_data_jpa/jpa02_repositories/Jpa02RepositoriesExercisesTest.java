package s08_data_jpa.jpa02_repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy ĆWICZEŃ lekcji Jpa02Repositories. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa02")
class Jpa02RepositoriesExercisesTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private TestEntityManager tem;

    private static Product product(String name) {
        return new Product(name, Category.OFFICE, new BigDecimal("10.00"), 3, Instant.parse("2026-10-01T08:00:00Z"));
    }

    @Test
    @DisplayName("Ćwiczenie 1: cena istniejącego produktu albo pusty Optional")
    void exercise1() {
        assertThat(Jpa02Repositories.exercise1PriceOf(repository, 2L)).hasValueSatisfying(p -> assertThat(p).isEqualByComparingTo("59.00"));
        assertThat(Jpa02Repositories.exercise1PriceOf(repository, 999L)).isEmpty();
    }

    @Test
    @DisplayName("Ćwiczenie 2: usunięcie istniejącego produktu (true) i brak zmian dla nieistniejącego (false)")
    void exercise2() {
        assertThat(Jpa02Repositories.exercise2DeleteIfExists(repository, 4L)).isTrue();
        assertThat(Jpa02Repositories.exercise2DeleteIfExists(repository, 999L)).isFalse();
        tem.flush();
        assertThat(repository.count()).isEqualTo(4);
        assertThat(repository.existsById(4L)).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 3: zapis kilku produktów naraz i ich numery w tej samej kolejności")
    void exercise3() {
        List<Long> ids = Jpa02Repositories.exercise3AddAll(repository, List.of(product("Zeszyt"), product("Teczka")));

        assertThat(ids).hasSize(2).doesNotContainNull();
        tem.clear();
        assertThat(repository.findById(ids.get(0))).get().extracting(Product::getName).isEqualTo("Zeszyt");
        assertThat(repository.findById(ids.get(1))).get().extracting(Product::getName).isEqualTo("Teczka");
    }

    @Test
    @DisplayName("Ćwiczenie 4: kopia produktu pod nową nazwą, oryginał bez zmian, błąd dla nieistniejącego")
    void exercise4() {
        Product copy = Jpa02Repositories.exercise4Copy(repository, 2L, "Mysz bezprzewodowa");
        tem.flush();
        tem.clear();

        assertThat(copy.getId()).isNotEqualTo(2L);
        assertThat(repository.count()).isEqualTo(6);
        Product original = repository.findById(2L).orElseThrow();
        assertThat(original.getName()).isEqualTo("Mysz");
        Product reloaded = repository.findById(copy.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Mysz bezprzewodowa");
        assertThat(reloaded.getPrice()).isEqualByComparingTo(original.getPrice());
        assertThat(reloaded.getCategory()).isEqualTo(original.getCategory());
        assertThatThrownBy(() -> Jpa02Repositories.exercise4Copy(repository, 999L, "X")).isInstanceOf(ProductNotFoundException.class);
    }
}
