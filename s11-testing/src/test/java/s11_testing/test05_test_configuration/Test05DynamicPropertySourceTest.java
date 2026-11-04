package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcja 3 lekcji Test05TestConfiguration: {@code @DynamicPropertySource} — właściwość, której wartość powstaje
 * dopiero w czasie uruchomienia testu (tu: adres serwera-atrapy na losowym wolnym porcie). Metoda musi być
 * STATYCZNA, bo Spring woła ją, zanim powstanie kontekst (i zanim powstanie obiekt testu).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test05")
class Test05DynamicPropertySourceTest {

    // Serwer startuje razem z klasą testową — przed kontekstem Springa, który będzie potrzebował jego adresu.
    static final PriceStubServer STUB = PriceStubServer.start(Map.of("KAWA", "24.90", "HERBATA", "15.99"));

    @DynamicPropertySource // dynamic = dynamiczny, obliczany w czasie działania
    static void priceServiceUrl(DynamicPropertyRegistry registry) { // registry = rejestr
        registry.add("test05.price-service.url", STUB::baseUrl); // Supplier: wartość pobierana przy pierwszym odczycie
    }

    @AfterAll
    static void stopStub() {
        STUB.close();
    }

    @Autowired
    PriceClient prices;

    @Test
    @DisplayName("Sekcja 3: @DynamicPropertySource podał adres atrapy z losowym portem — PriceClient pobiera z niej cenę Kawy")
    void dynamicUrlPointsToStub() {
        assertThat(prices.baseUrl()).isEqualTo("http://127.0.0.1:" + STUB.port()).doesNotContain("localhost:9");
        assertThat(prices.currentPrice("KAWA")).isEqualByComparingTo("24.90");
        assertThat(prices.currentPrice("HERBATA")).isEqualByComparingTo("15.99");
    }

    @Test
    @DisplayName("Sekcja 3: atrapa zwraca 404 dla nieznanego kodu — RestClient rzuca HttpClientErrorException.NotFound")
    void unknownProductIs404() {
        assertThatThrownBy(() -> prices.currentPrice("CIASTKA"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }
}
