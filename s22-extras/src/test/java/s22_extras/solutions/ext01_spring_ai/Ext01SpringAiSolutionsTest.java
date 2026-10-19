package s22_extras.solutions.ext01_spring_ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import s22_extras.ext01_spring_ai.CatalogProduct;
import s22_extras.ext01_spring_ai.Ext01SpringAi.PriceCheck;
import s22_extras.ext01_spring_ai.FakeChatModel;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Ext01SpringAiExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Ext01SpringAiSolutionsTest {

    static final CatalogProduct SLUCHAWKI =
            new CatalogProduct("ELE-003", "Słuchawki BT", "ELEKTRONIKA", new BigDecimal("349.90"));

    @Test
    @DisplayName("Wzorzec 1: slogan — wiadomość systemowa, użytkownika i odpowiedź bez spacji na brzegach")
    void solution1() {
        FakeChatModel fake = new FakeChatModel().willAnswer("  Kawa, która budzi!  \n");

        String slogan = Ext01SpringAiSolutions.solution1(ChatClient.create(fake), "Kawa ziarnista 1kg");

        assertThat(slogan).isEqualTo("Kawa, która budzi!");
        assertThat(fake.lastSystemText()).isEqualTo("Jesteś copywriterem sklepu. Odpowiadasz po polsku, jednym zdaniem.");
        assertThat(fake.lastUserText()).isEqualTo("Napisz slogan dla produktu: Kawa ziarnista 1kg");
    }

    @Test
    @DisplayName("Wzorzec 2: propozycja ceny jako rekord + walidacja odpowiedzi modelu")
    void solution2() {
        FakeChatModel fake = new FakeChatModel().willAnswer(
                "{\"sku\": \"ELE-003\", \"suggestedPrice\": 329.00, \"reason\": \"Konkurencja jest tańsza.\"}",
                "{\"sku\": \"ELE-003\", \"suggestedPrice\": -5, \"reason\": \"?\"}",
                "{\"sku\": \"ELE-003\", \"reason\": \"brak ceny\"}",
                "{\"sku\": \"KSI-001\", \"suggestedPrice\": 79.00, \"reason\": \"inny produkt\"}");
        ChatClient client = ChatClient.create(fake);

        assertThat(Ext01SpringAiSolutions.solution2(client, SLUCHAWKI))
                .isEqualTo(new PriceCheck("ELE-003", new BigDecimal("329.00"), "Konkurencja jest tańsza."));
        assertThat(fake.lastUserText()).contains("ELE-003", "Słuchawki BT", "349.90", "Your response should be in JSON format.");

        assertThatThrownBy(() -> Ext01SpringAiSolutions.solution2(client, SLUCHAWKI))
                .isInstanceOf(IllegalStateException.class).hasMessage("Model zwrócił niepoprawną cenę");
        assertThatThrownBy(() -> Ext01SpringAiSolutions.solution2(client, SLUCHAWKI))
                .isInstanceOf(IllegalStateException.class).hasMessage("Model zwrócił niepoprawną cenę");
        assertThatThrownBy(() -> Ext01SpringAiSolutions.solution2(client, SLUCHAWKI))
                .isInstanceOf(IllegalStateException.class).hasMessage("Model pomylił produkt");
    }

    @Test
    @DisplayName("Wzorzec 3: maskowanie e-maila, PESEL-u i telefonu przed wysłaniem do modelu")
    void solution3() {
        assertThat(Ext01SpringAiSolutions.solution3("Jan, jan@example.com, tel. +48 600-700-800, PESEL 90010112345"))
                .isEqualTo("Jan, [EMAIL], tel. [TELEFON], PESEL [PESEL]");
        assertThat(Ext01SpringAiSolutions.solution3("Dzwoń: 600700800 albo 600 700 800"))
                .isEqualTo("Dzwoń: [TELEFON] albo [TELEFON]");
        assertThat(Ext01SpringAiSolutions.solution3("Zamówienie ZAM-001 na 2 sztuki, kod 12345"))
                .as("krótkie liczby i numery zamówień zostają")
                .isEqualTo("Zamówienie ZAM-001 na 2 sztuki, kod 12345");
    }
}
