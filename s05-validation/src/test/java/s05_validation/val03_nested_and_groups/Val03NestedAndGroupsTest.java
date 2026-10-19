package s05_validation.val03_nested_and_groups;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testy lekcji Val03NestedAndGroups. {@code @WebMvcTest} — warstwa webowa z MockMvc i bean Validator z Boota.
 * Ścieżki pól sortujemy (zbiór naruszeń nie ma kolejności), a komunikaty — tylko własne, więc niezależne od języka.
 */
@WebMvcTest
class Val03NestedAndGroupsTest {

    @Autowired
    Validator validator;

    @Autowired
    MockMvc mockMvc;

    private static Set<String> paths(Set<? extends ConstraintViolation<?>> violations) {
        Set<String> result = new TreeSet<>();
        violations.forEach(v -> result.add(v.getPropertyPath().toString()));
        return result;
    }

    private static final ShippingAddress GOOD_ADDRESS = new ShippingAddress("Długa 5", "Gdańsk", "80-001");
    private static final OrderLine GOOD_LINE = new OrderLine("ELE-001", 1);

    @Test
    @DisplayName("1. Kaskada @Valid: błędy w adresie i w pozycjach mają ścieżki address.street, lines[0].quantity...")
    void section1_cascade() {
        OrderRequest order = new OrderRequest("zle",
                new ShippingAddress("", "Kraków", "123"),
                List.of(new OrderLine("ELE-001", 0), new OrderLine("x", 5), GOOD_LINE),
                null);

        assertThat(paths(validator.validate(order))).containsExactly(
                "address.street", "address.zip", "customerEmail", "lines[0].quantity", "lines[1].sku");

        assertThat(validator.validate(new OrderRequest("jan@example.com", GOOD_ADDRESS, List.of(GOOD_LINE), null)))
                .isEmpty();
    }

    @Test
    @DisplayName("2. Ograniczenia elementów kontenera: List<@NotBlank String> — ścieżka notes[1].<list element>")
    void section2_containerElements() {
        OrderRequest order = new OrderRequest("jan@example.com", GOOD_ADDRESS, List.of(GOOD_LINE),
                Arrays.asList("dzwonić przed dostawą", " ", null));   // Arrays.asList dopuszcza null (List.of — nie)

        assertThat(paths(validator.validate(order))).containsExactly("notes[1].<list element>", "notes[2].<list element>");

        // @NotNull + @Valid na polu: brak adresu to błąd @NotNull; samo @Valid dla null NIC nie sprawdza
        assertThat(paths(validator.validate(new OrderRequest("jan@example.com", null, List.of(GOOD_LINE), null))))
                .containsExactly("address");
        record OptionalAddress(@Valid ShippingAddress address) {
        }
        assertThat(validator.validate(new OptionalAddress(null))).as("null przechodzi kaskadę bez uwag").isEmpty();

        // pusta lista pozycji łamie @NotEmpty (to ograniczenie samej listy, nie jej elementów)
        assertThat(paths(validator.validate(new OrderRequest("jan@example.com", GOOD_ADDRESS, List.of(), null))))
                .containsExactly("lines");

        // @Valid na polu typu List działa tak samo jak List<@Valid ...>
        record ValidOnList(@Valid List<OrderLine> lines) {
        }
        assertThat(paths(validator.validate(new ValidOnList(List.of(new OrderLine("x", 1))))))
                .containsExactly("lines[0].sku");
    }

    @Test
    @DisplayName("3. PUŁAPKA: bez @Valid na polach zagnieżdżonych walidator nie zagląda do adresu ani pozycji")
    void section3_noCascade() {
        OrderRequestNoCascade order = new OrderRequestNoCascade("zle",
                new ShippingAddress("", "", "123"),   // adres cały zły
                List.of(new OrderLine("x", 0)),        // pozycja zła
                List.of(" "));                         // uwaga pusta

        assertThat(paths(validator.validate(order))).containsExactly("customerEmail");   // tylko własne pole rekordu

        // pytanie kontrolne 1 dosłownie: poprawny e-mail, cały adres zły, jedna dobra pozycja → zero naruszeń
        assertThat(validator.validate(new OrderRequestNoCascade("jan@example.com",
                new ShippingAddress("", "", "x"), List.of(new OrderLine("ELE-001", 1)), null))).isEmpty();

        // pytanie kontrolne 2: trzecia pozycja ze złym SKU
        assertThat(paths(validator.validate(new OrderRequest("jan@example.com", GOOD_ADDRESS,
                List.of(GOOD_LINE, GOOD_LINE, new OrderLine("x", 1)), null)))).containsExactly("lines[2].sku");
    }

    @Test
    @DisplayName("4. Grupy: te same dane, różne reguły; Default i grupa bez Default")
    void section4_groups() {
        ProductDto withIdBadRest = new ProductDto(5L, " ", null, null);

        assertThat(paths(validator.validate(withIdBadRest))).containsExactly("name", "price");                // Default
        assertThat(paths(validator.validate(withIdBadRest, OnCreate.class))).containsExactly("id", "name", "price");
        assertThat(paths(validator.validate(withIdBadRest, OnUpdate.class))).containsExactly("name", "price");

        // PUŁAPKA: Strict nie rozszerza Default — reguły bez grupy (name, price) są pominięte
        assertThat(paths(validator.validate(withIdBadRest, Strict.class))).containsExactly("category");
        // można poprosić o obie grupy naraz
        assertThat(paths(validator.validate(withIdBadRest, Default.class, Strict.class)))
                .containsExactly("category", "name", "price");

        ProductDto noId = new ProductDto(null, "Kawa", BigDecimal.ONE, null);
        // pytanie kontrolne 4 dosłownie: dane poprawne w grupie Default, ale grupa Strict wymaga kategorii
        assertThat(validator.validate(noId)).isEmpty();
        assertThat(paths(validator.validate(noId, Strict.class))).containsExactly("category");
        assertThat(validator.validate(noId, OnCreate.class)).isEmpty();
        assertThat(paths(validator.validate(noId, OnUpdate.class))).containsExactly("id");
    }

    @Test
    @DisplayName("5. @Validated(Grupa.class) przy parametrze kontrolera wybiera grupę; @Valid sprawdza tylko Default")
    void section5_groupsInController() throws Exception {
        String withId = "{\"id\":5,\"name\":\"Kawa\",\"price\":1}";
        String withoutId = "{\"name\":\"Kawa\",\"price\":1}";

        // tworzenie: id podane → 400
        MvcResult create = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(withId))
                .andReturn();
        assertThat(create.getResponse().getStatus()).isEqualTo(400);
        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) create.getResolvedException();
        assertThat(ex.getBindingResult().getFieldErrors()).extracting(FieldError::getField).containsExactly("id");
        assertThat(ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage())
                .isEqualTo("Id nadaje serwer — nie podawaj go przy tworzeniu");
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(withoutId))
                .andExpect(status().isCreated());

        // aktualizacja: brak id w ciele → 400
        MvcResult update = mockMvc.perform(put("/api/products/5").contentType(MediaType.APPLICATION_JSON)
                .content(withoutId)).andReturn();
        assertThat(update.getResponse().getStatus()).isEqualTo(400);
        mockMvc.perform(put("/api/products/5").contentType(MediaType.APPLICATION_JSON).content(withId))
                .andExpect(status().isOk());

        // samo @Valid = tylko Default: id i tak nikogo nie obchodzi
        mockMvc.perform(post("/api/products/default").contentType(MediaType.APPLICATION_JSON).content(withId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("6. Kaskada w kontrolerze: błędy pól zagnieżdżonych niosą pełną ścieżkę (address.city, lines[0].quantity)")
    void section6_cascadeInController() throws Exception {
        String body = "{\"customerEmail\":\"jan@example.com\","
                + "\"address\":{\"street\":\"Długa 5\",\"city\":\" \",\"zip\":\"80-001\"},"
                + "\"lines\":[{\"sku\":\"ELE-001\",\"quantity\":0}]}";

        MvcResult result = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        MethodArgumentNotValidException ex = (MethodArgumentNotValidException) result.getResolvedException();
        assertThat(ex.getBindingResult().getFieldErrors().stream().map(FieldError::getField).sorted().toList())
                .containsExactly("address.city", "lines[0].quantity");

        // ten sam błędny adres w rekordzie BEZ kaskady → zamówienie przechodzi (201)
        mockMvc.perform(post("/api/orders/no-cascade").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}
