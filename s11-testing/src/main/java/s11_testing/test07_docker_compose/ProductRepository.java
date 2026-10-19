package s11_testing.test07_docker_compose;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/** Repozytorium produktów na JdbcClient + odczyt nazwy bazy, z którą aplikacja naprawdę rozmawia. */
@Repository
public class ProductRepository {

    private final JdbcClient jdbc;
    private final DataSource dataSource;

    public ProductRepository(JdbcClient jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public List<Product> findAll() {
        return jdbc.sql("SELECT id, sku, name, category, price FROM product ORDER BY sku")
                .query(Product.class)
                .list();
    }

    /** Nazwa i wersja bazy z metadanych połączenia, np. „PostgreSQL 18.6” albo „H2 2.4.240 (…)”. */
    public String databaseVersion() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new IllegalStateException("Nie udało się odczytać wersji bazy", e);
        }
    }
}
