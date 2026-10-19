package s11_testing.test06_testcontainers;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ĆWICZENIE 2 lekcji Test06Testcontainers (szkielet): import klientów, który pomija duplikaty e-maili i działa
 * TAKŻE na PostgreSQL. Testy: Test06TestcontainersExercisesTest (H2 i PostgreSQL w kontenerze).
 */
@Service
public class SafeCustomerImporter {

    private final JdbcClient jdbc;

    public SafeCustomerImporter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Zapisz klientów w jednej transakcji; klienta z e-mailem, który już jest w tabeli, pomiń (policz jako skipped).
     * Klient bez e-maila (null) zawsze jest zapisywany. Podpowiedź: NIE prowokuj błędu UNIQUE — zanim wstawisz,
     * sprawdź zapytaniem SELECT COUNT(*), czy taki e-mail już jest.
     */
    @Transactional
    public CustomerImporter.ImportResult importAll(List<NewCustomer> customers) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
