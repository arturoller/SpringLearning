package s21_capstone.cap01_shop_api.customer;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import s21_capstone.cap01_shop_api.customer.CustomerDtos.CreateCustomerRequest;
import s21_capstone.cap01_shop_api.customer.CustomerDtos.CustomerResponse;

import java.net.URI;
import java.util.List;

/**
 * Klienci.
 * <pre>
 *   GET  /api/customers       200 lista (po id)
 *   GET  /api/customers/{id}  200 / 404
 *   POST /api/customers       201 + Location / 400 walidacja / 409 email zajęty
 * </pre>
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerResponse> all() {
        return customerService.all();
    }

    @GetMapping("/{id}")
    public CustomerResponse one(@PathVariable long id) {
        return customerService.get(id);
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }
}
