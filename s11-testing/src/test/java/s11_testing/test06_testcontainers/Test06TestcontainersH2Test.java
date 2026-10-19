package s11_testing.test06_testcontainers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 3 lekcji Test06Testcontainers: TEN SAM scenariusz importu co w Test06TestcontainersPostgresTest, ale na H2.
 * Tu wszystko „działa” — i właśnie dlatego test tylko na H2 przepuściłby błąd, który na produkcyjnym PostgreSQL
 * wywraca import. Docker niepotrzebny.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test06")
class Test06TestcontainersH2Test {

    @Autowired
    CustomerRepository repository;

    @Autowired
    CustomerImporter importer;

    @Autowired
    JdbcClient jdbc;

    @Test
    @DisplayName("3. H2: import pomija duplikat i zapisuje resztę — test zielony, choć na PostgreSQL ten sam kod pada")
    void importSkipsDuplicateOnH2() {
        var customers = List.of(
                new NewCustomer("Jan Kowalski (duplikat)", "Warszawa", "jan@example.com", false),
                new NewCustomer("Krzysztof Wrona", "Lublin", "krzysztof@example.com", false));

        CustomerImporter.ImportResult result = importer.importAll(customers);

        assertThat(result).isEqualTo(new CustomerImporter.ImportResult(1, 1));
        assertThat(repository.databaseVersion()).startsWith("H2");

        // sprzątanie: import zatwierdził transakcję, a kontekst (i baza H2) jest współdzielony z innymi testami
        jdbc.sql("DELETE FROM customer WHERE email = 'krzysztof@example.com'").update();
        assertThat(repository.count()).isEqualTo(7);
    }
}
