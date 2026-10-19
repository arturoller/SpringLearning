package s17_events.evt02_transactional_events;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Dwóch słuchaczy zapisujących punkty do bazy PO zatwierdzeniu transakcji zamówienia: naiwny i poprawny. */
@Component
public class PointsListeners {

    private final OrderRepository repository;
    private final DataSource dataSource;
    private final EventLog log;

    public PointsListeners(OrderRepository repository, DataSource dataSource, EventLog log) {
        this.repository = repository;
        this.dataSource = dataSource;
        this.log = log;
    }

    /** Naiwny: zwykły zapis w fazie AFTER_COMMIT, bez nowej transakcji. Dziennik pokazuje, w jakim stanie jest wtedy połączenie. */
    @TransactionalEventListener
    public void naive(NaivePointsRequested event) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            log.add("naiwny: transakcja aktywna=" + TransactionSynchronizationManager.isActualTransactionActive()
                    + ", autoCommit=" + connection.getAutoCommit());
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
        repository.insertPoints(event.orderId(), event.customer(), event.points());
        try (Connection other = dataSource.getConnection();
             PreparedStatement statement = other.prepareStatement("SELECT COUNT(*) FROM loyalty_points WHERE order_id = ?")) {
            statement.setString(1, event.orderId());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                log.add("naiwny: inna sesja widzi zapis=" + (resultSet.getInt(1) > 0));
            }
        }
        if (event.failAfterWrite()) {
            throw new IllegalStateException("Awaria po zapisie punktów (naiwny)");
        }
    }

    /** Poprawny: zapis w NOWEJ transakcji (REQUIRES_NEW), niezależnej od już zakończonej transakcji zamówienia. */
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void correct(CorrectPointsRequested event) {
        repository.insertPoints(event.orderId(), event.customer(), event.points());
        if (event.failAfterWrite()) {
            throw new IllegalStateException("Awaria po zapisie punktów (REQUIRES_NEW)");
        }
    }
}
