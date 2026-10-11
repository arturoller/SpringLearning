package s05_validation.val03_nested_and_groups;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Sekcje 1 i 5 lekcji Val03NestedAndGroups w kontrolerze: {@code @WebMvcTest} z MockMvc (tylko warstwa web). */
@WebMvcTest(OrderController.class)
class Val03NestedAndGroupsWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    private List<String> fieldsOf(MvcResult result) {
        assertThat(result.getResolvedException()).isInstanceOf(MethodArgumentNotValidException.class);
        return ((MethodArgumentNotValidException) result.getResolvedException()).getBindingResult()
                .getFieldErrors().stream().map(f -> f.getField()).sorted().toList();
    }

    private MvcResult send(String url, String json, int expectedStatus) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus)).andReturn();
    }

    @Test
    @DisplayName("1. Zagnieżdżone błędy w JSON-ie mają w odpowiedzi pełne ścieżki pól (address.postalCode, items[1].sku...)")
    void nestedInController() throws Exception {
        String bad = "{\"customer\":\"\",\"address\":{\"street\":\"\",\"postalCode\":\"1\"},"
                + "\"items\":[{\"sku\":\"a\",\"quantity\":1},{\"sku\":\"\",\"quantity\":0}]}";
        assertThat(fieldsOf(send("/api/orders", bad, 400)))
                .containsExactly("address.postalCode", "address.street", "customer", "items[1].quantity", "items[1].sku");

        String good = "{\"customer\":\"Ala\",\"address\":{\"street\":\"Długa 1\",\"postalCode\":\"00-001\"},"
                + "\"items\":[{\"sku\":\"a\",\"quantity\":1}]}";
        send("/api/orders", good, 200);
    }

    @Test
    @DisplayName("5. @Validated(OnCreate/OnUpdate) wybiera grupę; zwykłe @Valid nie uruchamia reguł z grup")
    void groupsInController() throws Exception {
        // tworzenie: id ma być puste
        send("/api/products", "{\"name\":\"Mleko\"}", 200);
        assertThat(fieldsOf(send("/api/products", "{\"id\":3,\"name\":\"\"}", 400))).containsExactly("id", "name");

        // aktualizacja: id obowiązkowe
        assertThat(fieldsOf(mockMvc.perform(put("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Mleko\"}")).andExpect(status().isBadRequest()).andReturn())).containsExactly("id");
        mockMvc.perform(put("/api/products").contentType(MediaType.APPLICATION_JSON).content("{\"id\":3,\"name\":\"Mleko\"}"))
                .andExpect(status().isOk());

        // PUŁAPKA: @Valid = Default, a reguły ProductDto są w grupach — wszystko przechodzi
        send("/api/products-with-valid", "{\"id\":3,\"name\":\"\"}", 200);

        // Pułapka Default: sama grupa OnCreate pomija @NotBlank bez grupy; z Default.class i OnCreate.class — łapie oba
        assertThat(fieldsOf(send("/api/mixed-only-oncreate", "{\"id\":3,\"name\":\"\"}", 400))).containsExactly("id");
        assertThat(fieldsOf(send("/api/mixed-with-default", "{\"id\":3,\"name\":\"\"}", 400))).containsExactly("id", "name");
    }
}
