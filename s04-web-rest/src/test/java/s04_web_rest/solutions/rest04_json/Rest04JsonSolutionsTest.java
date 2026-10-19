package s04_web_rest.solutions.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import s04_web_rest.rest04_json.JsonModels.Customer;
import s04_web_rest.solutions.rest04_json.Rest04Solutions.Delivery;
import s04_web_rest.solutions.rest04_json.Rest04Solutions.OrderLine;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Rest04JsonExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
@JsonTest
class Rest04JsonSolutionsTest {

    @Autowired
    JsonMapper jsonMapper;

    @Test
    @DisplayName("Wzorzec 1: OrderLine → {\"sku\":…,\"qty\":…,\"unitPrice\":…} bez ceny zakupu")
    void solution1() {
        OrderLine line = new OrderLine("KSI-001", 2, new BigDecimal("79.00"), new BigDecimal("52.00"));

        assertThat(jsonMapper.writeValueAsString(line)).isEqualTo("{\"sku\":\"KSI-001\",\"qty\":2,\"unitPrice\":79.00}");
    }

    @Test
    @DisplayName("Wzorzec 2: Delivery — data dd.MM.yyyy, courierNote znika, gdy null")
    void solution2() {
        assertThat(jsonMapper.writeValueAsString(new Delivery("ZAM-001", LocalDate.of(2026, 10, 21), null)))
                .isEqualTo("{\"orderId\":\"ZAM-001\",\"deliveryDate\":\"21.10.2026\"}");
        assertThat(jsonMapper.writeValueAsString(new Delivery("ZAM-002", LocalDate.of(2026, 10, 22), "Domofon 12")))
                .isEqualTo("{\"orderId\":\"ZAM-002\",\"deliveryDate\":\"22.10.2026\",\"courierNote\":\"Domofon 12\"}");
    }

    @Test
    @DisplayName("Wzorzec 3: tablica JSON → List<PriceTag> → wartość całości")
    void solution3() {
        String json = """
                [{"sku":"KSI-001","price":79.00,"quantity":2},{"sku":"SPO-002","price":7.49,"quantity":4}]
                """;

        assertThat(Rest04Solutions.totalValue(jsonMapper, json)).isEqualByComparingTo("187.96");
        assertThat(Rest04Solutions.totalValue(jsonMapper, "[]")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Wzorzec 4: customizer — null pomijane, nieznane pole → UnrecognizedPropertyException")
    void solution4() {
        JsonMapper.Builder builder = JsonMapper.builder();
        Rest04Solutions.strictShopCustomizer().customize(builder);
        JsonMapper strict = builder.build();

        assertThat(strict.writeValueAsString(new Customer(2, "Maria Nowak", null)))
                .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\"}");
        assertThatThrownBy(() -> strict.readValue("{\"id\":1,\"name\":\"Jan\",\"vip\":true}", Customer.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }
}
