package s11_testing.test06_testcontainers;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CustomerImporter = import listy klientów (np. z pliku od działu marketingu) w JEDNEJ transakcji. Klient z e-mailem,
 * który już jest w bazie, ma zostać POMINIĘTY, a reszta — zapisana.
 *
 * <p>UWAGA — to jest kod z błędem, którego test na H2 NIE wykryje (lekcja Test06Testcontainers, sekcja 3): po błędzie
 * UNIQUE w środku transakcji PostgreSQL odrzuca każde kolejne polecenie tej transakcji, więc „pomiń i jedź dalej”
 * działa tylko na H2.</p>
 */
@Service
public class CustomerImporter {

    private final JdbcClient jdbc;

    public CustomerImporter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public ImportResult importAll(List<NewCustomer> customers) {
        int imported = 0;
        int skipped = 0;
        for (NewCustomer customer : customers) {
            try {
                jdbc.sql("INSERT INTO customer (name, city, email, vip) VALUES (:name, :city, :email, :vip)")
                        .paramSource(customer)   // parametry z pól rekordu: :name, :city, :email, :vip
                        .update();
                imported++;
            } catch (DuplicateKeyException e) {
                skipped++;   // duplikat e-maila: pomijamy i jedziemy dalej (na PostgreSQL — nie da się!)
            }
        }
        return new ImportResult(imported, skipped);
    }

    /** Wynik importu: ilu klientów zapisano, ilu pominięto jako duplikaty. */
    public record ImportResult(int imported, int skipped) {
    }
}
