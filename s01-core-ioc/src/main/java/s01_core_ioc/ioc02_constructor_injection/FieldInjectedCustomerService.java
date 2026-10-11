package s01_core_ioc.ioc02_constructor_injection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * FieldInjectedCustomerService = ta sama logika, ale wersja ANTYWZORZEC: zależność wstrzyknięta do pola.
 * Pole nie może być {@code final}, a konstruktor jest pusty — z zewnątrz nie widać, że klasa czegokolwiek potrzebuje.
 */
@Service
public class FieldInjectedCustomerService {

    @Autowired // wstrzyknięcie do pola przez refleksję (reflection) — już po utworzeniu obiektu
    private CustomerRepository repository;

    public String displayName(long id) {
        Customer customer = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Nie ma klienta o id " + id));
        return customer.firstName() + " " + customer.lastName();
    }
}
