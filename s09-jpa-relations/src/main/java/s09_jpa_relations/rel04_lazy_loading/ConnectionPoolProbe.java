package s09_jpa_relations.rel04_lazy_loading;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Sonda puli połączeń — TYLKO do nauki. Mówi, ile połączeń z puli HikariCP (pula połączeń z bazą, domyślna
 * w Spring Boot) jest w tej chwili zajętych. Dzięki niej widać, że przy open-in-view połączenie jest trzymane
 * do końca żądania HTTP, a nie tylko do końca transakcji.
 */
@Component
public class ConnectionPoolProbe {

    private final DataSource dataSource;

    public ConnectionPoolProbe(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int busyConnections() {
        try {
            return dataSource.unwrap(HikariDataSource.class).getHikariPoolMXBean().getActiveConnections();
        } catch (SQLException e) {
            throw new IllegalStateException("To nie jest pula HikariCP", e);
        }
    }
}
