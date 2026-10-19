package s21_capstone.cap01_shop_api.customer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s21_capstone.cap01_shop_api.common.ConflictException;
import s21_capstone.cap01_shop_api.common.NotFoundException;
import s21_capstone.cap01_shop_api.customer.CustomerDtos.CreateCustomerRequest;
import s21_capstone.cap01_shop_api.customer.CustomerDtos.CustomerResponse;

import java.util.List;
import java.util.Map;

/** Logika klientów (te same zasady co w ProductService: readOnly na klasie, DTO na zewnątrz, wyjątki domeny). */
@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;

    public CustomerService(CustomerRepository customers) {
        this.customers = customers;
    }

    public List<CustomerResponse> all() {
        return customers.findAll(Sort.by("id")).stream().map(CustomerResponse::from).toList();
    }

    public CustomerResponse get(long id) {
        return customers.findById(id).map(CustomerResponse::from)
                .orElseThrow(() -> new NotFoundException("klient", id));
    }

    /** Email unikalny: sprawdzenie w serwisie (czytelny błąd) + {@code uk_customers_email} w bazie (wyścig). */
    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        String email = (request.email() == null || request.email().isBlank()) ? null : request.email().strip();
        if (email != null && customers.existsByEmail(email)) {
            throw emailTaken(email);
        }
        try {
            Customer saved = customers.saveAndFlush(
                    new Customer(request.name().strip(), request.city().strip(), email,
                            Boolean.TRUE.equals(request.vip())));
            return CustomerResponse.from(saved);
        } catch (DataIntegrityViolationException raceLost) {
            throw emailTaken(email);
        }
    }

    private static ConflictException emailTaken(String email) {
        return new ConflictException("EMAIL_TAKEN", "Klient z adresem " + email + " już istnieje", Map.of("email", email));
    }
}
