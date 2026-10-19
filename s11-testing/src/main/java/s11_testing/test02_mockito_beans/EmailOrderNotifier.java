package s11_testing.test02_mockito_beans;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * „Prawdziwy” powiadamiacz: udaje wysyłkę e-maila (w kursie tylko wpis w logu — żadnej sieci).
 * W testach z {@code @MockitoBean OrderNotifier} ten bean jest ZASTĘPOWANY atrapą.
 */
@Component
public class EmailOrderNotifier implements OrderNotifier {

    private static final Logger log = LoggerFactory.getLogger(EmailOrderNotifier.class);

    @Override
    public void orderPlaced(OrderConfirmation confirmation) {
        log.info("Wysyłam e-mail do {}: zamówienie {} x {}", confirmation.customerEmail(),
                confirmation.quantity(), confirmation.sku());
    }
}
