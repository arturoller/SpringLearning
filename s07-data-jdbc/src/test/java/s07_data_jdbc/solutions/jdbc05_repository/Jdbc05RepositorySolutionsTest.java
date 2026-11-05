package s07_data_jdbc.solutions.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import s07_data_jdbc.jdbc05_repository.JdbcProductRepository;
import s07_data_jdbc.jdbc05_repository.Product;
import s07_data_jdbc.jdbc05_repository.ProductNotFoundException;
import s07_data_jdbc.jdbc05_repository.ProductRepository;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Jdbc05RepositoryExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@JdbcTest
@ActiveProfiles("jdbc05")
@Import(JdbcProductRepository.class)
class Jdbc05RepositorySolutionsTest {

    @Autowired
    private ProductRepository repository;

    @Test
    @DisplayName("Wzorzec 1: nazwy produktów od danej ceny, alfabetycznie")
    void solution1() {
        assertThat(Jdbc05RepositorySolutions.solution1NamesFromPrice(repository, new BigDecimal("79.00")))
                .containsExactly("Klawiatura", "Lampka", "Monitor");
        assertThat(Jdbc05RepositorySolutions.solution1NamesFromPrice(repository, new BigDecimal("1000"))).isEmpty();
    }

    @Test
    @DisplayName("Wzorzec 2: wynik próby zapisu — OK, DUPLICATE albo INTEGRITY")
    void solution2() {
        assertThat(Jdbc05RepositorySolutions.solution2TryInsert(repository,
                Product.create("Zeszyt", "dom", new BigDecimal("4.50"), 40))).isEqualTo("OK");
        assertThat(Jdbc05RepositorySolutions.solution2TryInsert(repository,
                Product.create("Mysz", "dom", new BigDecimal("4.50"), 40))).isEqualTo("DUPLICATE");
        assertThat(Jdbc05RepositorySolutions.solution2TryInsert(repository,
                Product.create("Grabie", "ogród", new BigDecimal("4.50"), 40))).isEqualTo("INTEGRITY");
    }

    @Test
    @DisplayName("Wzorzec 3: uzupełnienie magazynu i błąd dla nieistniejącego produktu")
    void solution3() {
        Product saved = Jdbc05RepositorySolutions.solution3Restock(repository, 5, 7);
        assertThat(saved.stock()).isEqualTo(7);
        assertThat(repository.findById(5).orElseThrow().stock()).isEqualTo(7);
        assertThatThrownBy(() -> Jdbc05RepositorySolutions.solution3Restock(repository, 999, 1))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Wzorzec 4: konflikt wersji jest ponawiany na świeżym odczycie, po trzech próbach wyjątek leci dalej")
    void solution4() {
        AtomicInteger calls = new AtomicInteger();
        Product result = Jdbc05RepositorySolutions.solution4UpdateWithRetry(repository, 1, product -> {
            if (calls.incrementAndGet() == 1) { // przy pierwszej próbie „ktoś inny” zdąży zapisać zmianę
                repository.save(repository.findById(1).orElseThrow().withStock(50));
            }
            return product.withStock(product.stock() + 1);
        });
        assertThat(calls).hasValue(2);
        assertThat(result.stock()).as("zmiana konkurenta (50) + nasza (+1)").isEqualTo(51);

        AtomicInteger hopeless = new AtomicInteger();
        assertThatThrownBy(() -> Jdbc05RepositorySolutions.solution4UpdateWithRetry(repository, 2, product -> {
            hopeless.incrementAndGet();
            repository.save(repository.findById(2).orElseThrow().withStock(hopeless.get())); // konflikt za każdym razem
            return product.withStock(0);
        })).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(hopeless).hasValue(3);
    }
}
