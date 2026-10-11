package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jdbc05Repository.
 *
 * <ul>
 *   <li>{@code @JdbcTest} — wycinek aplikacji tylko z warstwą JDBC (DataSource, JdbcClient, transakcje); szybszy niż
 *       {@code @SpringBootTest}. Każdy test działa w transakcji cofanej na końcu (rollback).</li>
 *   <li>{@code @ActiveProfiles("jdbc05")} — profil lekcji: skrypt schematu tworzy tabelę products.</li>
 *   <li>{@code @Import(ProductRepository.class)} — @JdbcTest nie skanuje @Repository, więc dołączamy bean ręcznie.</li>
 *   <li>{@code @Sql} — dane startowe tylko na ten test (wykonywane w transakcji testu, więc też są cofane).</li>
 * </ul>
 */
@JdbcTest
@ActiveProfiles("jdbc05")
@Import(ProductRepository.class)
class Jdbc05RepositoryTest {

    @Autowired
    ProductRepository repository;

    @Autowired
    JdbcClient jdbcClient;

    @Test
    @DisplayName("1. Repozytorium chowa SQL: insert, findAll (z ORDER BY), updatePrice, deleteById")
    @Sql("/db/jdbc05/two-products.sql")
    void repositoryHidesSql() {
        assertThat(repository.findAll()).extracting(Product::name).containsExactly("Herbata", "Kawa");

        Product milk = repository.insert("Mleko", 349);
        assertThat(repository.findAll()).extracting(Product::name).containsExactly("Herbata", "Kawa", "Mleko");

        assertThat(repository.updatePrice(milk.id(), 399)).isTrue();
        assertThat(repository.getById(milk.id()).priceCents()).isEqualTo(399);

        assertThat(repository.deleteById(milk.id())).isTrue();
        assertThat(repository.findAll()).hasSize(2);
    }

    @Test
    @DisplayName("2. Klucz generowany: update(keyHolder, \"id\") zwraca id nadane przez bazę, różne dla kolejnych wstawień")
    void generatedKeys() {
        Product first = repository.insert("Cukier", 450);
        Product second = repository.insert("Sol", 200);

        assertThat(first.id()).isPositive();
        assertThat(second.id()).isGreaterThan(first.id());
        // id z KeyHolder to naprawdę id wiersza w bazie
        assertThat(jdbcClient.sql("SELECT name FROM products WHERE id = :id").param("id", second.id())
                .query(String.class).single()).isEqualTo("Sol");
    }

    @Test
    @DisplayName("3. Brak wyniku: findById daje Optional.empty, getById - wyjątek domenowy, updatePrice - false")
    void notFound() {
        assertThat(repository.findById(999)).isEmpty();
        assertThatThrownBy(() -> repository.getById(999))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Nie ma produktu o id 999");
        assertThat(repository.updatePrice(999, 100)).isFalse();     // 0 zmienionych wierszy, bez wyjątku
        assertThat(repository.deleteById(999)).isFalse();
    }

    @Test
    @DisplayName("4. Błędy bazy to niesprawdzane DataAccessException: DuplicateKey, DataIntegrityViolation, BadSqlGrammar")
    void dataAccessExceptions() {
        repository.insert("Kawa", 2999);

        assertThatThrownBy(() -> repository.insert("Kawa", 1))             // druga "Kawa" łamie UNIQUE
                .isInstanceOf(DuplicateKeyException.class)
                .isInstanceOf(DataIntegrityViolationException.class)       // DuplicateKey jest węższy
                .isInstanceOf(NonTransientDataAccessException.class)
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> repository.insert(null, 1))               // NULL w kolumnie NOT NULL
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateKeyException.class);

        assertThatThrownBy(() -> jdbcClient.sql("SELEKT * FROM products").query().listOfRows())   // literówka w SQL
                .isInstanceOf(BadSqlGrammarException.class)
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("5a. Test zostawia w bazie produkt, ale rollback cofa go po teście")
    @Sql("/db/jdbc05/two-products.sql")
    void rollbackA() {
        repository.insert("Dodatkowy A", 1);
        assertThat(repository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("5b. Drugi test widzi dokładnie swoje dwa produkty - niezależnie od kolejności testów")
    @Sql("/db/jdbc05/two-products.sql")
    void rollbackB() {
        repository.insert("Dodatkowy B", 1);
        assertThat(repository.findAll()).extracting(Product::name).containsExactly("Dodatkowy B", "Herbata", "Kawa");
    }
}
