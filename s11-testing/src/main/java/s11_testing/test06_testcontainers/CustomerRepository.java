package s11_testing.test06_testcontainers;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * Repozytorium klientów na JdbcClient (zwykłe SQL — szczegóły: dział s07_data_jdbc). To samo SQL działa na H2
 * i na PostgreSQL... prawie zawsze — lekcja Test06Testcontainers pokazuje, kiedy nie.
 */
@Repository
public class CustomerRepository {

    private final JdbcClient jdbc;
    private final DataSource dataSource;

    public CustomerRepository(JdbcClient jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    public List<Customer> findAll() {
        return jdbc.sql("SELECT id, name, city, email, vip FROM customer ORDER BY id")
                .query(Customer.class)
                .list();
    }

    public long count() {
        return jdbc.sql("SELECT COUNT(*) FROM customer").query(Long.class).single();
    }

    /** Nazwa i wersja bazy, z którą naprawdę rozmawia aplikacja (metadane połączenia JDBC). */
    public String databaseVersion() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            return meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new IllegalStateException("Nie udało się odczytać wersji bazy", e);
        }
    }
}
