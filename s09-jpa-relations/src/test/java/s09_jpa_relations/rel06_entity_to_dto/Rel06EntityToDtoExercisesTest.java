package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy ĆWICZEŃ lekcji Rel06EntityToDto. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Encje do ćwiczeń 1–2 czytamy w transakcji (TransactionTemplate), a metody ćwiczeń wołamy też w niej.
 */
@Tag("cwiczenie")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("rel06")
class Rel06EntityToDtoExercisesTest {

    @Autowired
    TransactionTemplate tx;

    @Autowired
    CustomerRepository customers;

    @Autowired
    CustomerMapper mapper;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("Ćwiczenie 1: zamówienie → OrderDto (numer i status)")
    void exercise1() {
        CustomerDto.OrderDto dto = tx.execute(status -> {
            ShopOrder order = customers.findByName("Maria Nowak").orElseThrow().getOrders().getFirst();
            return Rel06EntityToDto.exercise1(order);
        });

        assertThat(dto).isEqualTo(new CustomerDto.OrderDto("ZAM-002", "OPLACONE"));
    }

    @Test
    @DisplayName("Ćwiczenie 2: kontakt klienta — e-mail albo informacja o jego braku; bez notatki wewnętrznej")
    void exercise2() {
        List<CustomerContactDto> contacts = tx.execute(status -> List.of(
                Rel06EntityToDto.exercise2(customers.findByName("Jan Kowalski").orElseThrow()),
                Rel06EntityToDto.exercise2(customers.findByName("Maria Nowak").orElseThrow())));

        assertThat(contacts).containsExactly(
                new CustomerContactDto("Jan Kowalski", "jan@example.com"),
                new CustomerContactDto("Maria Nowak", "brak e-maila, kontakt przez miasto: Kraków"));
    }

    @Test
    @DisplayName("Ćwiczenie 3: CustomerDto po nazwie — w transakcji, 2 zapytania, DTO kompletne po transakcji")
    void exercise3() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        CustomerDto dto = Rel06EntityToDto.exercise3(tx, customers, mapper, "Jan Kowalski");

        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(dto.orders()).extracting(CustomerDto.OrderDto::number).containsExactly("ZAM-001", "ZAM-003");
        assertThat(dto.city()).isEqualTo("Warszawa");
    }
}
