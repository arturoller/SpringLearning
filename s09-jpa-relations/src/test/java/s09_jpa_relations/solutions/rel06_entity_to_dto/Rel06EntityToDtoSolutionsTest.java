package s09_jpa_relations.solutions.rel06_entity_to_dto;

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
import s09_jpa_relations.rel06_entity_to_dto.CustomerContactDto;
import s09_jpa_relations.rel06_entity_to_dto.CustomerDto;
import s09_jpa_relations.rel06_entity_to_dto.CustomerMapper;
import s09_jpa_relations.rel06_entity_to_dto.CustomerRepository;
import s09_jpa_relations.rel06_entity_to_dto.Rel06Application;
import s09_jpa_relations.rel06_entity_to_dto.ShopOrder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprawdzenia z Rel06EntityToDtoExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze.
 * Pakiet solutions nie ma klasy startowej, więc wskazujemy ją wprost: {@code @SpringBootTest(classes = ...)}.
 * Encje do ćwiczeń 1–2 czytamy w transakcji (TransactionTemplate), a metody ćwiczeń wołamy też w niej.
 */
@Tag("wzorzec")
@SpringBootTest(classes = Rel06Application.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("rel06")
class Rel06EntityToDtoSolutionsTest {

    @Autowired
    TransactionTemplate tx;

    @Autowired
    CustomerRepository customers;

    @Autowired
    CustomerMapper mapper;

    @Autowired
    EntityManagerFactory emf;

    @Test
    @DisplayName("Wzorzec 1: zamówienie → OrderDto (numer i status)")
    void solution1() {
        CustomerDto.OrderDto dto = tx.execute(status -> {
            ShopOrder order = customers.findByName("Maria Nowak").orElseThrow().getOrders().getFirst();
            return Rel06EntityToDtoSolutions.solution1(order);
        });

        assertThat(dto).isEqualTo(new CustomerDto.OrderDto("ZAM-002", "OPLACONE"));
    }

    @Test
    @DisplayName("Wzorzec 2: kontakt klienta — e-mail albo informacja o jego braku; bez notatki wewnętrznej")
    void solution2() {
        List<CustomerContactDto> contacts = tx.execute(status -> List.of(
                Rel06EntityToDtoSolutions.solution2(customers.findByName("Jan Kowalski").orElseThrow()),
                Rel06EntityToDtoSolutions.solution2(customers.findByName("Maria Nowak").orElseThrow())));

        assertThat(contacts).containsExactly(
                new CustomerContactDto("Jan Kowalski", "jan@example.com"),
                new CustomerContactDto("Maria Nowak", "brak e-maila, kontakt przez miasto: Kraków"));
    }

    @Test
    @DisplayName("Wzorzec 3: CustomerDto po nazwie — w transakcji, 2 zapytania, DTO kompletne po transakcji")
    void solution3() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();

        CustomerDto dto = Rel06EntityToDtoSolutions.solution3(tx, customers, mapper, "Jan Kowalski");

        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
        assertThat(dto.orders()).extracting(CustomerDto.OrderDto::number).containsExactly("ZAM-001", "ZAM-003");
        assertThat(dto.city()).isEqualTo("Warszawa");
    }
}
