package s04_web_rest.rest04_json;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s04_web_rest.rest04_json.JsonModels.Category;
import s04_web_rest.rest04_json.JsonModels.Customer;
import s04_web_rest.rest04_json.JsonModels.CustomerWithoutNulls;
import s04_web_rest.rest04_json.JsonModels.LabeledProduct;
import s04_web_rest.rest04_json.JsonModels.NewProduct;
import s04_web_rest.rest04_json.JsonModels.PrettyCategory;
import s04_web_rest.rest04_json.JsonModels.ProductCard;
import s04_web_rest.rest04_json.JsonModels.PublicProduct;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * JsonDemoController = kontroler, który zwraca i przyjmuje rekordy z JsonModels. Dane STAŁE (żadnego
 * LocalDate.now()) — dzięki temu JSON jest zawsze taki sam i testy mogą porównać go co do znaku.
 */
@RestController
@RequestMapping("/api/json")
public class JsonDemoController {

    private static final LocalDate ADDED_ON = LocalDate.of(2026, 10, 19);
    private static final Instant UPDATED_AT = Instant.parse("2026-10-19T08:30:00Z");

    /** GET /api/json/card — domyślny JSON rekordu: daty ISO, BigDecimal jako liczba ze skalą (5499.99). */
    @GetMapping("/card")
    public ProductCard card() {
        return new ProductCard("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7,
                ADDED_ON, UPDATED_AT);
    }

    /** GET /api/json/public — @JsonProperty, @JsonIgnore, @JsonFormat w akcji. */
    @GetMapping("/public")
    public PublicProduct publicProduct() {
        return new PublicProduct("ELE-001", "Laptop Pro 14", new BigDecimal("4100.00"), ADDED_ON,
                new BigDecimal("5499.99"));
    }

    /** GET /api/json/customer — klient bez emaila: "email":null w JSON-ie. */
    @GetMapping("/customer")
    public Customer customer() {
        return new Customer(2, "Maria Nowak", null);
    }

    /** GET /api/json/customer-without-nulls — to samo z @JsonInclude(NON_NULL): pola email w ogóle nie ma. */
    @GetMapping("/customer-without-nulls")
    public CustomerWithoutNulls customerWithoutNulls() {
        return new CustomerWithoutNulls(2, "Maria Nowak", null);
    }

    /** GET /api/json/labeled — enum z własnym toString(): Jackson 3 pisze "książki", a nie "KSIAZKI". */
    @GetMapping("/labeled")
    public LabeledProduct labeled() {
        return new LabeledProduct("KSI-001", PrettyCategory.KSIAZKI);
    }

    /** POST /api/json/products — odsyła to, co Jackson PRZECZYTAŁ z ciała (echo = echo, odbicie). */
    @PostMapping("/products")
    public NewProduct echo(@RequestBody NewProduct product) {
        return product;
    }
}
