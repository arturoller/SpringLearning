package s04_web_rest.rest04_json;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rest04Json — część „bez HTTP”. Wycinek: {@code @JsonTest} — startuje tylko to, co potrzebne do JSON
 * (autokonfiguracja Jacksona: bean JsonMapper, moduły, ustawienia {@code spring.jackson.*} oraz testery JSON),
 * bez Tomcata i bez kontrolerów. Najwęższy możliwy wycinek dla pytania „jak ten obiekt wygląda w JSON?”.
 */
@JsonTest
class Rest04JsonMapperTest {

    @Autowired
    JsonMapper mapper;                 // bean zbudowany przez Boota — TEN SAM, którego używa Spring MVC

    @Autowired
    JacksonTester<Product> productTester;   // pomocnik testowy: zapis/odczyt + asercje na JSON

    @Test
    @DisplayName("1. Boot 4 udostępnia mapper jako bean tools.jackson.databind.json.JsonMapper (Jackson 3)")
    void mapperIsJackson3Bean() {
        assertThat(mapper).isInstanceOf(JsonMapper.class);
        assertThat(mapper.getClass().getPackageName()).isEqualTo("tools.jackson.databind.json");
    }

    @Test
    @DisplayName("1. Rekord → JSON → rekord: nazwy pól z komponentów rekordu, wynik równy oryginałowi")
    void recordRoundTrip() {
        Product kawa = new Product(1, "Kawa", new BigDecimal("24.90"), Category.DRINKS);

        String json = mapper.writeValueAsString(kawa);
        assertThat(json).isEqualTo("{\"id\":1,\"name\":\"Kawa\",\"price\":24.90,\"category\":\"DRINKS\"}");

        Product back = mapper.readValue(json, Product.class);
        assertThat(back).isEqualTo(kawa);   // rekord ma equals() po wszystkich polach
    }

    @Test
    @DisplayName("1. JacksonTester: asercje na JSON przez ścieżki JsonPath i porównanie z odczytanym obiektem")
    void jacksonTester() throws Exception {
        Product kawa = new Product(1, "Kawa", new BigDecimal("24.90"), Category.DRINKS);

        assertThat(productTester.write(kawa)).extractingJsonPathStringValue("$.name").isEqualTo("Kawa");
        assertThat(productTester.write(kawa)).extractingJsonPathStringValue("$.category").isEqualTo("DRINKS");
        assertThat(productTester.parseObject("{\"id\":2,\"name\":\"Herbata\",\"price\":15.99,\"category\":\"DRINKS\"}"))
                .isEqualTo(new Product(2, "Herbata", new BigDecimal("15.99"), Category.DRINKS));
    }

    @Test
    @DisplayName("1. Zły JSON i brakujące pole typu prostego to wyjątki Jacksona — niekontrolowane (RuntimeException)")
    void jacksonExceptionsAreUnchecked() {
        assertThatThrownBy(() -> mapper.readValue("{id:1", Product.class))
                .isInstanceOf(JacksonException.class)
                .isInstanceOf(RuntimeException.class);

        // brak „id” (typ long) → Jackson 3 odmawia wpisania null do typu prostego
        assertThatThrownBy(() -> mapper.readValue("{\"name\":\"x\"}", Product.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    @DisplayName("2. @JsonProperty zmienia nazwę, @JsonIgnore ukrywa pole, NON_NULL pomija null, @JsonFormat daje własny format daty")
    void annotations() {
        Customer anna = new Customer(7, "Anna Nowak", "tajny-hash", null, LocalDate.of(1990, 5, 17));
        assertThat(mapper.writeValueAsString(anna))
                .isEqualTo("{\"id\":7,\"full_name\":\"Anna Nowak\",\"birthDate\":\"17.05.1990\"}");

        Customer withPhone = new Customer(7, "Anna Nowak", "tajny-hash", "+48 600 100 200", LocalDate.of(1990, 5, 17));
        assertThat(mapper.writeValueAsString(withPhone)).contains("\"phone\":\"+48 600 100 200\"");
    }

    @Test
    @DisplayName("2. Przy odczycie: obowiązuje nowa nazwa (full_name), stara (fullName) jest nieznana i ignorowana")
    void annotationsOnInput() {
        Customer parsed = mapper.readValue(
                "{\"id\":1,\"full_name\":\"Jan\",\"passwordHash\":\"wstrzyknięty\",\"birthDate\":\"17.05.1990\"}", Customer.class);
        assertThat(parsed.fullName()).isEqualTo("Jan");
        assertThat(parsed.passwordHash()).isNull();                 // @JsonIgnore działa też w drugą stronę
        assertThat(parsed.birthDate()).isEqualTo(LocalDate.of(1990, 5, 17));

        Customer oldName = mapper.readValue("{\"id\":1,\"fullName\":\"Jan\"}", Customer.class);
        assertThat(oldName.fullName()).isNull();                    // „fullName” to teraz nieznane pole
    }

    /** Przykład „na sucho”: ten sam rekord bez adnotacji i z domyślnym ustawieniem — nazwa pola = nazwa komponentu. */
    record Plain(String fullName) {
    }

    record Renamed(@JsonProperty("full_name") String fullName) {
    }

    @Test
    @DisplayName("2. PRZED/PO: bez adnotacji JSON ma nazwę komponentu (fullName), z @JsonProperty — full_name")
    void renameBeforeAfter() {
        assertThat(mapper.writeValueAsString(new Plain("Jan"))).isEqualTo("{\"fullName\":\"Jan\"}");
        assertThat(mapper.writeValueAsString(new Renamed("Jan"))).isEqualTo("{\"full_name\":\"Jan\"}");
    }

    @Test
    @DisplayName("3. Daty domyślnie jako tekst ISO-8601: LocalDate → \"2026-03-15\", Instant → \"…Z\" (nie liczby)")
    void datesAsIsoText() {
        Order order = new Order(100, LocalDate.of(2026, 3, 15), Instant.parse("2026-03-15T10:15:30Z"),
                new BigDecimal("49.80"), OrderStatus.PAID);

        assertThat(mapper.writeValueAsString(order)).isEqualTo(
                "{\"id\":100,\"orderDate\":\"2026-03-15\",\"placedAt\":\"2026-03-15T10:15:30Z\","
                        + "\"total\":49.80,\"status\":\"PAID\"}");
    }

    @Test
    @DisplayName("3. Odczyt Instant z przesunięciem strefy (+01:00) daje ten sam moment przeliczony na UTC")
    void instantWithOffset() {
        Order order = mapper.readValue("{\"id\":1,\"placedAt\":\"2026-03-15T11:15:30+01:00\"}", Order.class);
        assertThat(order.placedAt()).isEqualTo(Instant.parse("2026-03-15T10:15:30Z"));
    }

    @Test
    @DisplayName("3. Zła data lub nieznana stała enuma → InvalidFormatException (w kontrolerze zamieni się w 400)")
    void invalidDateAndEnum() {
        assertThatThrownBy(() -> mapper.readValue("{\"id\":1,\"orderDate\":\"15-03-2026\"}", Order.class))
                .isInstanceOf(InvalidFormatException.class);
        assertThatThrownBy(() -> mapper.readValue("{\"id\":1,\"status\":\"LOST\"}", Order.class))
                .isInstanceOf(InvalidFormatException.class);
        // wielkość liter ma znaczenie: „paid” to NIE „PAID”
        assertThatThrownBy(() -> mapper.readValue("{\"id\":1,\"status\":\"paid\"}", Order.class))
                .isInstanceOf(InvalidFormatException.class);
    }

    @Test
    @DisplayName("4. Nieznane pole w JSON jest po cichu pomijane (domyślnie w Boot; w czystym Jacksonie 3 też)")
    void unknownFieldsIgnored() {
        String json = "{\"id\":1,\"name\":\"Kawa\",\"price\":24.90,\"color\":\"brązowy\"}";

        assertThat(mapper.readValue(json, Product.class).name()).isEqualTo("Kawa");

        JsonMapper plain = JsonMapper.builder().build();            // mapper „z pudełka”, bez Boota
        assertThat(plain.readValue(json, Product.class).name()).isEqualTo("Kawa");
    }

    @Test
    @DisplayName("5. BigDecimal zachowuje skalę i dokładność; double daje błąd zaokrągleń")
    void bigDecimalVersusDouble() {
        // double: 0.1 + 0.2 nie jest 0.3
        assertThat(0.1 + 0.2).isNotEqualTo(0.3);
        assertThat(0.1 + 0.2).isEqualTo(0.30000000000000004);

        // BigDecimal odczytany z JSON jako liczba dziesiętna: 0.1 + 0.2 = 0.3
        BigDecimal a = mapper.readValue("0.1", BigDecimal.class);
        BigDecimal b = mapper.readValue("0.2", BigDecimal.class);
        assertThat(a.add(b)).isEqualByComparingTo("0.3");

        // 24.90 zostaje 24.90 (dwa miejsca po przecinku); jako double stałoby się 24.9
        assertThat(mapper.writeValueAsString(new BigDecimal("24.90"))).isEqualTo("24.90");
        assertThat(mapper.writeValueAsString(24.90d)).isEqualTo("24.9");
    }

    @Test
    @DisplayName("5. Enum w JSON to tekst z nazwą stałej; cena może przyjść jako liczba lub jako tekst")
    void enumAndPriceFormats() {
        Product fromNumber = mapper.readValue("{\"id\":1,\"price\":24.90,\"category\":\"SWEETS\"}", Product.class);
        Product fromText = mapper.readValue("{\"id\":1,\"price\":\"24.90\"}", Product.class);

        assertThat(fromNumber.category()).isEqualTo(Category.SWEETS);
        assertThat(fromNumber.price()).isEqualTo(new BigDecimal("24.90"));
        assertThat(fromText.price()).isEqualTo(new BigDecimal("24.90"));
    }

    @Test
    @DisplayName("5. PUŁAPKA: cykl referencji (szef ↔ podwładny) — Jackson nie kończy zapisu i rzuca wyjątek")
    void referenceCycle() {
        Employee boss = new Employee("Szef");
        boss.addToTeam(new Employee("Jan"));   // Jan.boss == Szef, Szef.team zawiera Jana

        assertThatThrownBy(() -> mapper.writeValueAsString(boss))
                .isInstanceOf(JacksonException.class)
                .hasMessageContaining("nesting depth");
    }
}
