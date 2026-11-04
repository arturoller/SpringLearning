package s04_web_rest.rest04_json;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lekcja Rest04Json, sekcja 3: ustawienia dat w Boot 4. Właściwość zapisu dat jako liczb ma w Jacksonie 3 NOWĄ nazwę
 * ({@code spring.jackson.datatype.datetime.write-dates-as-timestamps}); stara nazwa z Boot 3
 * ({@code spring.jackson.serialization.write-dates-as-timestamps}) przestała działać.
 */
@JsonTest(properties = "spring.jackson.datatype.datetime.write-dates-as-timestamps=true")
class Rest04DateSettingsTest {

    @Autowired
    JsonMapper mapper;

    @Test
    @DisplayName("3. Nowa właściwość spring.jackson.datatype.datetime.write-dates-as-timestamps=true → daty jako liczby")
    void datesAsTimestamps() {
        Order order = new Order(1, LocalDate.of(2026, 3, 15), Instant.parse("2026-03-15T10:15:30Z"),
                new BigDecimal("49.80"), OrderStatus.PAID);

        assertThat(mapper.writeValueAsString(order)).contains("\"orderDate\":[2026,3,15]")
                .contains("\"placedAt\":1773569730.000000000");
    }

    @Test
    @DisplayName("3. PUŁAPKA: stara nazwa spring.jackson.serialization.write-dates-as-timestamps wywala start aplikacji")
    void oldPropertyNameFailsStartup() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.jackson.serialization.write-dates-as-timestamps=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("spring.jackson.serialization");
                });
    }
}
