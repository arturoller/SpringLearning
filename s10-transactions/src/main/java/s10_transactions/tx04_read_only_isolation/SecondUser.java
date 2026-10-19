package s10_transactions.tx04_read_only_isolation;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * SecondUser = „drugi użytkownik” bazy: WŁASNE połączenie JDBC wzięte prosto z puli, z wyłączonym autoCommit.
 * Nie bierze udziału w transakcjach Springa (nie używa DataSourceUtils), więc to naprawdę OSOBNA transakcja.
 *
 * <p>Po co? Zjawiska izolacji wymagają DWÓCH transakcji naraz. Zamiast dwóch wątków i {@code sleep} (wynik zależny od
 * szczęścia) sterujemy nimi krok po kroku w jednym wątku: „drugi użytkownik zmienia wiersz”, „pierwsza transakcja
 * czyta”, „drugi zatwierdza”… Kolejność kroków jest zawsze ta sama, więc testy są deterministyczne.</p>
 *
 * <p>Uwaga: drugi użytkownik nie może czekać na blokadę trzymaną przez pierwszą transakcję w TYM SAMYM wątku — wątek
 * stałby, aż baza zgłosi przekroczenie czasu oczekiwania na blokadę (lock timeout; w H2 domyślnie 2 s). Dlatego
 * w lekcji drugi użytkownik pisze zwykle wtedy, gdy pierwsza transakcja jeszcze niczego nie zablokowała (sama czytała).
 * Wyjątki są celowe: testy blokady pesymistycznej i limitu czasu (sekcje 5 i 6 lekcji) pokazują właśnie to
 * czekanie.</p>
 */
public final class SecondUser implements AutoCloseable {

    private final Connection connection;

    private SecondUser(Connection connection) {
        this.connection = connection;
    }

    /** Weź własne połączenie z puli i rozpocznij transakcję (autoCommit = false). */
    public static SecondUser connect(DataSource dataSource) {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            return new SecondUser(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Nie udało się otworzyć połączenia drugiego użytkownika", e);
        }
    }

    /** Wykonaj INSERT/UPDATE/DELETE w transakcji drugiego użytkownika (bez zatwierdzania). Zwraca liczbę wierszy. */
    public int update(String sql, Object... params) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Drugi użytkownik: błąd SQL", e);
        }
    }

    /** Wykonaj zmianę i od razu ją zatwierdź — „drugi użytkownik zrobił swoje i skończył”. */
    public int updateAndCommit(String sql, Object... params) {
        int rows = update(sql, params);
        commit();
        return rows;
    }

    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Drugi użytkownik: błąd commit", e);
        }
    }

    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new IllegalStateException("Drugi użytkownik: błąd rollback", e);
        }
    }

    /** Wycofaj to, czego nie zatwierdzono, i oddaj połączenie do puli. */
    @Override
    public void close() {
        try (connection) {
            connection.rollback();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new IllegalStateException("Drugi użytkownik: błąd zamykania", e);
        }
    }
}
