package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.stereotype.Service;

/**
 * CustomerService = serwis klientów — wersja ZALECANA: zależność przez konstruktor.
 * {@code @Service} (serwis) to odmiana {@code @Component}. Konstruktor jest JEDYNY, więc Spring wie, którego użyć —
 * adnotacja {@code @Autowired} (automatycznie podłącz) nie jest potrzebna.
 */
@Service
public class CustomerService {

    private final CustomerRepository repository; // final = pole ustawione raz, nigdy nie zmienione

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    /** Imię i nazwisko klienta, np. "Anna Kowalska"; nieznane id → IllegalArgumentException. */
    public String displayName(long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma klienta o id " + id));
        return customer.firstName() + " " + customer.lastName();
    }
}
