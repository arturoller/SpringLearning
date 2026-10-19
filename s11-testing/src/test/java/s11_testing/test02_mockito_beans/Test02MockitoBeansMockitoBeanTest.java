package s11_testing.test02_mockito_beans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;

/**
 * Sekcja 2 lekcji Test02MockitoBeans: {@code @MockitoBean} — atrapa jako bean w kontekście Springa.
 * Serwis i katalog są PRAWDZIWE (z kontekstu), zastąpiony jest tylko powiadamiacz (efekt uboczny: e-mail).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)   // bez weba — testujemy serwis
class Test02MockitoBeansMockitoBeanTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    OrderService service;            // prawdziwy serwis z kontekstu

    @MockitoBean
    OrderNotifier notifier;          // bean typu OrderNotifier w kontekście ZASTĄPIONY atrapą

    @Test
    @DisplayName("2. @MockitoBean: bean powiadamiacza zastąpiony atrapą (ta sama nazwa), katalog prawdziwy")
    void beanReplacedInContext() {
        assertThat(context.getBeanNamesForType(OrderNotifier.class)).containsExactly("emailOrderNotifier");
        assertThat(mockingDetails(context.getBean(OrderNotifier.class)).isMock()).isTrue();
        assertThat(context.getBean(OrderNotifier.class)).isSameAs(notifier);
        assertThat(mockingDetails(context.getBean(ProductCatalog.class)).isMock()).isFalse();
    }

    @Test
    @DisplayName("2. @MockitoBean: prawdziwy serwis + prawdziwy katalog, a e-mail „wysyła” atrapa")
    void realServiceWithMockedSideEffect() {
        OrderConfirmation confirmation = service.placeOrder("zofia@example.com", "KSI-001", 2);

        assertThat(confirmation.totalGrosze()).isEqualTo(15800);   // 2 × 7900 z PRAWDZIWEGO katalogu
        verify(notifier).orderPlaced(confirmation);
    }
}
