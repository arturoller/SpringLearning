package s19_deploy.dep03_production_config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Dep03ProductionConfig. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Dep03ProductionConfigExercisesTest {

    @Test
    @DisplayName("Ćwiczenie 1: nazwa zmiennej środowiska dla klucza ustawienia")
    void exercise1() {
        assertThat(Dep03ProductionConfig.exercise1("shop.payment.api-key")).isEqualTo("SHOP_PAYMENT_APIKEY");
        assertThat(Dep03ProductionConfig.exercise1("server.port")).isEqualTo("SERVER_PORT");
        assertThat(Dep03ProductionConfig.exercise1("spring.datasource.url")).isEqualTo("SPRING_DATASOURCE_URL");
    }

    @Test
    @DisplayName("Ćwiczenie 2: maskowanie sekretów")
    void exercise2() {
        Map<String, String> input = Map.of(
                "shop.payment.api-key", "abc",
                "server.port", "8080",
                "DB.PASSWORD", "tajne",
                "app.auth.Token", "xyz");

        assertThat(Dep03ProductionConfig.exercise2(input)).containsExactly(
                Map.entry("DB.PASSWORD", "******"),
                Map.entry("app.auth.Token", "******"),
                Map.entry("server.port", "8080"),
                Map.entry("shop.payment.api-key", "******"));
        assertThat(input).containsEntry("shop.payment.api-key", "abc");   // oryginał bez zmian
    }

    @Test
    @DisplayName("Ćwiczenie 3: strażnik ustawień produkcyjnych")
    void exercise3() {
        Map<String, String> good = Map.of(
                "spring.profiles.active", "prod",
                "spring.datasource.password", "${DB_PASSWORD}",
                "management.endpoint.health.show-details", "never");
        assertThat(Dep03ProductionConfig.exercise3(good)).isEmpty();

        Map<String, String> bad = Map.of(
                "debug", "true",
                "shop.payment.api-key", "tajne123",
                "spring.datasource.password", "haslo",
                "spring.profiles.active", "dev",
                "management.endpoint.health.show-details", "always");
        assertThat(Dep03ProductionConfig.exercise3(bad)).containsExactly(
                "DEBUG_WLACZONY",
                "SEKRET_W_PLIKU:shop.payment.api-key",
                "SEKRET_W_PLIKU:spring.datasource.password",
                "PROFIL_PROD_NIEAKTYWNY",
                "SZCZEGOLY_ZDROWIA_PUBLICZNE");

        // brak spring.profiles.active = profil prod nieaktywny; pusta wartość sekretu to nie sekret w pliku
        assertThat(Dep03ProductionConfig.exercise3(Map.of("shop.payment.api-key", "")))
                .containsExactly("PROFIL_PROD_NIEAKTYWNY");
    }
}
