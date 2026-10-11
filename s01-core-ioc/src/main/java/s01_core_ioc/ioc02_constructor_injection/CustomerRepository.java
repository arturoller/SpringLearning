package s01_core_ioc.ioc02_constructor_injection;

import java.util.Optional;

/**
 * CustomerRepository = repozytorium klientów (miejsce, skąd serwis bierze dane). To interfejs z JEDNĄ metodą,
 * więc w teście można go zastąpić lambdą (test double = „dubler” bez Mockito).
 * {@code Optional} (opcjonalny) — wynik, który może być pusty, gdy klienta nie ma.
 */
@FunctionalInterface // functional = funkcyjny: dokładnie jedna metoda abstrakcyjna
public interface CustomerRepository {

    Optional<Customer> findById(long id);
}
