package s20_modulith.mod01_modules.customers;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * API modułu {@code customers}: spis klientów (directory = spis, katalog). Moduł jest mały i nie ma części
 * wewnętrznej — to też jest w porządku: wtedy cały moduł jest jego API.
 */
@Service
public class CustomerDirectory {

    private final Map<Long, CustomerView> customers = Map.of(
            1L, new CustomerView(1, "Jan Kowalski", "Warszawa", true),
            2L, new CustomerView(2, "Maria Nowak", "Kraków", false),
            4L, new CustomerView(4, "Zofia Krawczyk", "Warszawa", true));

    public Optional<CustomerView> findById(long id) {
        return Optional.ofNullable(customers.get(id));
    }
}
