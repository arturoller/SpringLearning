package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import s04_web_rest.rest04_json.JsonModels.Customer;
import s04_web_rest.rest04_json.Rest04Exercises.Delivery;
import s04_web_rest.rest04_json.Rest04Exercises.OrderLine;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Rest04Json. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Ćwiczenia 1–2 padają na porównaniu JSON-a (brak adnotacji), 3–4 — z UnsupportedOperationException: TODO.
 */
@Tag("cwiczenie")
@JsonTest
class Rest04JsonExercisesTest {

    @Autowired
    JsonMapper jsonMapper;

    @Test
    @DisplayName("Ćwiczenie 1: OrderLine → {\"sku\":…,\"qty\":…,\"unitPrice\":…} bez ceny zakupu")
    void exercise1() {
        OrderLine line = new OrderLine("KSI-001", 2, new BigDecimal("79.00"), new BigDecimal("52.00"));

        assertThat(jsonMapper.writeValueAsString(line)).isEqualTo("{\"sku\":\"KSI-001\",\"qty\":2,\"unitPrice\":79.00}");
    }

    @Test
    @DisplayName("Ćwiczenie 2: Delivery — data dd.MM.yyyy, courierNote znika, gdy null")
    void exercise2() {
        assertThat(jsonMapper.writeValueAsString(new Delivery("ZAM-001", LocalDate.of(2026, 10, 21), null)))
                .isEqualTo("{\"orderId\":\"ZAM-001\",\"deliveryDate\":\"21.10.2026\"}");
        assertThat(jsonMapper.writeValueAsString(new Delivery("ZAM-002", LocalDate.of(2026, 10, 22), "Domofon 12")))
                .isEqualTo("{\"orderId\":\"ZAM-002\",\"deliveryDate\":\"22.10.2026\",\"courierNote\":\"Domofon 12\"}");
    }

    @Test
    @DisplayName("Ćwiczenie 3: tablica JSON → List<PriceTag> → wartość całości")
    void exercise3() {
        String json = """
                [{"sku":"KSI-001","price":79.00,"quantity":2},{"sku":"SPO-002","price":7.49,"quantity":4}]
                """;

        assertThat(Rest04Exercises.totalValue(jsonMapper, json)).isEqualByComparingTo("187.96");
        assertThat(Rest04Exercises.totalValue(jsonMapper, "[]")).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Ćwiczenie 4: customizer — null pomijane, nieznane pole → UnrecognizedPropertyException")
    void exercise4() {
        JsonMapper.Builder builder = JsonMapper.builder();
        Rest04Exercises.strictShopCustomizer().customize(builder);
        JsonMapper strict = builder.build();

        assertThat(strict.writeValueAsString(new Customer(2, "Maria Nowak", null)))
                .isEqualTo("{\"id\":2,\"name\":\"Maria Nowak\"}");
        assertThatThrownBy(() -> strict.readValue("{\"id\":1,\"name\":\"Jan\",\"vip\":true}", Customer.class))
                .isInstanceOf(UnrecognizedPropertyException.class);
    }
}
