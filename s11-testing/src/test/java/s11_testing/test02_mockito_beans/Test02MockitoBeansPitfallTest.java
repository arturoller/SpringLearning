package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Sekcja 6 lekcji Test02MockitoBeans: PUŁAPKA „atrapa wszystkiego = test niczego”. Atrapą jest tu sama KLASA
 * TESTOWANA (OrderService). Test przechodzi — choć sprawdza wyłącznie to, czego sami nauczyliśmy atrapę.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Test02MockitoBeansPitfallTest {

    @MockitoBean
    OrderService service;   // ŹLE: atrapa tego, co chcemy sprawdzić

    @Test
    @DisplayName("6. PUŁAPKA: test atrapy serwisu „potwierdza” kwotę 99999, a prawdziwy kod liczy 12998")
    void mockingTheClassUnderTestProvesNothing() {
        given(service.placeOrder("jan@example.com", "SPO-001", 2))
                .willReturn(new OrderConfirmation("jan@example.com", "SPO-001", 2, 99999));

        // „Test” przechodzi — sprawdza tylko, że atrapa oddaje to, czego ją nauczyliśmy (tautologia):
        assertThat(service.placeOrder("jan@example.com", "SPO-001", 2).totalGrosze()).isEqualTo(99999);

        // Prawdziwy serwis (zbudowany ręcznie, z prawdziwym katalogiem) liczy zupełnie co innego:
        var real = new OrderService(new InMemoryProductCatalog(), confirmation -> { });
        assertThat(real.placeOrder("jan@example.com", "SPO-001", 2).totalGrosze()).isEqualTo(12998);
    }
}
