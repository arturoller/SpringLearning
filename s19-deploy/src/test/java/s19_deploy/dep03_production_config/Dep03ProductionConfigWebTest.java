package s19_deploy.dep03_production_config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Dep03ProductionConfig od strony HTTP: odpowiedź aplikacji nie może zdradzać klucza API.
 * Wybrany wycinek: pełny kontekst + MockMvc (kontroler potrzebuje beana PaymentSettings z konfiguracji).
 * Ustawienia (w tym sztuczny klucz) podajemy w {@code properties} — tak jak zrobiłaby to platforma przy wdrożeniu.
 */
@SpringBootTest(properties = {
        "shop.payment.url=http://bramka.test",
        "shop.payment.api-key=sekretny-klucz-123"   // klucz UDAWANY, tylko do testu
})
@AutoConfigureMockMvc
class Dep03ProductionConfigWebTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("5. GET /api/payment-settings pokazuje adres i limit czasu, a klucz API tylko w postaci zamaskowanej")
    void responseDoesNotLeakApiKey() {
        assertThat(mvc.get().uri("/api/payment-settings"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.url", url -> assertThat(url).isEqualTo("http://bramka.test"))
                .hasPathSatisfying("$.apiKey", key -> assertThat(key).isEqualTo("****************23"))
                .hasPathSatisfying("$.timeout", timeout -> assertThat(timeout).isEqualTo("PT5S"));

        assertThat(mvc.get().uri("/api/payment-settings")).body().asString().doesNotContain("sekretny-klucz-123");
    }
}
