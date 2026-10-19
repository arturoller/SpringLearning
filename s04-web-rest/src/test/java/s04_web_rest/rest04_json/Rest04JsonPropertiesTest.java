package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sekcje 4 i 5 lekcji Rest04Json: właściwości spring.jackson.* zmieniają JsonMapper CAŁEJ aplikacji.
 * Ustawiamy je tylko w tym teście ({@code properties = ...}), żeby nie zmieniać zachowania lekcji.
 */
@WebMvcTest(controllers = JsonDemoController.class, properties = {
        "spring.jackson.default-property-inclusion=non_null",
        "spring.jackson.deserialization.fail-on-unknown-properties=true"})
class Rest04JsonPropertiesTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("4. spring.jackson.default-property-inclusion=non_null — pola null znikają w KAŻDEJ klasie")
    void nonNullGlobally() throws Exception {
        mockMvc.perform(get("/api/json/customer"))   // Customer NIE ma @JsonInclude
                .andExpect(content().json("""
                        {"id":2,"name":"Maria Nowak"}
                        """, JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("5. spring.jackson.deserialization.fail-on-unknown-properties=true — nieznane pole → 400")
    void failOnUnknownProperties() throws Exception {
        mockMvc.perform(post("/api/json/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"sku":"SPO-001","name":"Kawa ziarnista 1kg","category":"SPOZYWCZE","price":64.99,"stock":120,
                         "color":"brązowy"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResolvedException())
                        .isInstanceOf(HttpMessageNotReadableException.class)
                        .hasMessageContaining("Unrecognized property \"color\""));
    }
}
