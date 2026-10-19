package s11_testing.solutions.test06_testcontainers;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s11_testing.test06_testcontainers.CustomerImporter;
import s11_testing.test06_testcontainers.NewCustomer;

import java.util.List;

/**
 * Rozwiązanie wzorcowe ćwiczenia 2 lekcji Test06Testcontainers — zajrzyj dopiero po własnej próbie!
 * Zamiast prowokować błąd UNIQUE (który na PostgreSQL przerywa całą transakcję), najpierw sprawdzamy, czy e-mail
 * już jest. Ten sam kod działa na H2 i na PostgreSQL. Uwaga: przy imporcie równoległym dwa wątki mogłyby jednocześnie
 * „nie znaleźć” tego samego e-maila — wtedy ostatnią linią obrony nadal jest ograniczenie UNIQUE w bazie.
 */
@Service
public class SafeCustomerImporterSolution {

    private final JdbcClient jdbc;

    public SafeCustomerImporterSolution(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public CustomerImporter.ImportResult importAll(List<NewCustomer> customers) {
        int imported = 0;
        int skipped = 0;
        for (NewCustomer customer : customers) {
            if (customer.email() != null && emailExists(customer.email())) {
                skipped++;
                continue;
            }
            jdbc.sql("INSERT INTO customer (name, city, email, vip) VALUES (:name, :city, :email, :vip)")
                    .paramSource(customer)
                    .update();
            imported++;
        }
        return new CustomerImporter.ImportResult(imported, skipped);
    }

    private boolean emailExists(String email) {
        return jdbc.sql("SELECT COUNT(*) FROM customer WHERE email = :email")
                .param("email", email)
                .query(Long.class)
                .single() > 0;
    }
}
