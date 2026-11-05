package s04_web_rest.rest04_json;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Kontroler lekcji Rest04Json: dane „przykładowe” do odczytu (GET) i „lustra” (POST odsyła to, co dostał) — dzięki
 * temu widać zarówno serializację (obiekt → JSON), jak i deserializację (JSON → obiekt).
 */
@RestController
@RequestMapping("/rest04")
public class JsonController {

    @GetMapping("/products/sample")
    public Product sampleProduct() {
        return new Product(1, "Kawa", new BigDecimal("24.90"), Category.DRINKS);
    }

    @PostMapping("/products/echo")
    public Product echoProduct(@RequestBody Product product) {
        return product;
    }

    @GetMapping("/customers/sample")
    public Customer sampleCustomer() {
        return new Customer(7, "Anna Nowak", "tajny-hash", null, LocalDate.of(1990, 5, 17));
    }

    @PostMapping("/customers/echo")
    public Customer echoCustomer(@RequestBody Customer customer) {
        return customer;
    }

    /** Pokazuje, co naprawdę dotarło do rekordu: czy {@code passwordHash} z żądania został przeczytany. */
    @PostMapping("/customers/inspect")
    public String inspectCustomer(@RequestBody Customer customer) {
        return "passwordHash=" + customer.passwordHash();
    }

    @GetMapping("/orders/sample")
    public Order sampleOrder() {
        return new Order(100, LocalDate.of(2026, 3, 15), Instant.parse("2026-03-15T10:15:30Z"),
                new BigDecimal("49.80"), OrderStatus.PAID);
    }

    @PostMapping("/orders/echo")
    public Order echoOrder(@RequestBody Order order) {
        return order;
    }
}
