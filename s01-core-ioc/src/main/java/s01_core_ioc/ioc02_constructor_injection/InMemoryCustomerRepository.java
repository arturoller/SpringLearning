package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * InMemoryCustomerRepository = repozytorium trzymane w pamięci (in memory), z kilkoma klientami na start.
 * {@code @Repository} (repozytorium) to odmiana {@code @Component} — Spring utworzy z tej klasy bean
 * (ziarno — obiekt zarządzany przez Springa). Konstruktor bez parametrów: ta klasa niczego nie potrzebuje.
 */
@Repository
public class InMemoryCustomerRepository implements CustomerRepository {

    private final Map<Long, Customer> customers = Map.of(
            1L, new Customer(1, "Anna", "Kowalska"),
            2L, new Customer(2, "Jan", "Nowak"),
            3L, new Customer(3, "Maria", "Wiśniewska"));

    @Override
    public Optional<Customer> findById(long id) {
        return Optional.ofNullable(customers.get(id));
    }
}
