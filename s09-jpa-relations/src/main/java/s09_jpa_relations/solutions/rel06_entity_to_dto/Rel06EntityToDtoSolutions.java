package s09_jpa_relations.solutions.rel06_entity_to_dto;

import org.springframework.transaction.support.TransactionTemplate;
import s09_jpa_relations.rel06_entity_to_dto.Customer;
import s09_jpa_relations.rel06_entity_to_dto.CustomerContactDto;
import s09_jpa_relations.rel06_entity_to_dto.CustomerDto;
import s09_jpa_relations.rel06_entity_to_dto.CustomerMapper;
import s09_jpa_relations.rel06_entity_to_dto.CustomerRepository;
import s09_jpa_relations.rel06_entity_to_dto.ShopOrder;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel06EntityToDto — zajrzyj dopiero po własnej próbie! */
public final class Rel06EntityToDtoSolutions {

    private Rel06EntityToDtoSolutions() {
    }

    static CustomerDto.OrderDto solution1(ShopOrder order) {
        return new CustomerDto.OrderDto(order.getNumber(), order.getStatus());
    }

    static CustomerContactDto solution2(Customer customer) {
        String contact = customer.getEmail() != null
                ? customer.getEmail()
                : "brak e-maila, kontakt przez miasto: " + customer.getCity();
        return new CustomerContactDto(customer.getName(), contact);   // internalNote celowo pomijamy
    }

    static CustomerDto solution3(TransactionTemplate tx, CustomerRepository customers, CustomerMapper mapper,
                                 String name) {
        return tx.execute(status -> {
            Long id = customers.findByName(name).orElseThrow().getId();          // zapytanie 1
            // Zapytanie 2: left join fetch zamówień. Klient jest już w kontekście utrwalania, ale jego kolekcja
            // orders jest leniwa — join fetch ją wypełnia, więc mapper nie wyśle trzeciego zapytania.
            Customer customer = customers.findWithOrdersById(id).orElseThrow();
            return mapper.toDto(customer);                                        // w transakcji — bezpiecznie
        });
    }
}
