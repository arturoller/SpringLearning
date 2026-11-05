package s12_security.sec02_pitfalls.rule_order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcje 1 i 5 lekcji Sec02Authorization — pułapki kolejności reguł i "zabezpieczenia tylko w interfejsie". */
@SpringBootTest
@AutoConfigureMockMvc
class Sec02RuleOrderPitfallTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 1 (pułapka): ogólna reguła przed szczegółową — klient czyta raport tylko dla admina (200 zamiast 403)")
    void generalRuleFirstWins() {
        assertThat(mvc.get().uri("/sec02/pitfall/admin/reports/salaries")).hasStatusOk();
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 5 (pułapka): adres ukryty tylko w interfejsie — klient i tak usuwa produkt (204), bo serwer go wpuszcza")
    void hiddenInUiOnly() {
        assertThat(mvc.delete().uri("/sec02/pitfall/products/1")).hasStatus(HttpStatus.NO_CONTENT);
    }
}
