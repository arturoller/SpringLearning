package s17_events.evt03_async_and_outbox;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Evt03AsyncAndOutbox (własna, bo lekcje działu nie dzielą kontekstu).
 * {@code @EnableAsync} (enable = włącz; async = asynchroniczny) włącza obsługę {@code @Async} — bez niej adnotacja
 * jest po cichu ignorowana. {@code main} składa zamówienie przez outbox, pokazuje stan skrzynki przed i po wysyłce.
 */
@SpringBootApplication
@EnableAsync
public class Evt03Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Evt03Application.class)
                .profiles("evt03").web(WebApplicationType.NONE).run(args)) {
            OutboxOrderService service = context.getBean(OutboxOrderService.class);
            OrderRepository repository = context.getBean(OrderRepository.class);
            OutboxRelay relay = context.getBean(OutboxRelay.class);

            service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));
            System.out.println("Po złożeniu zamówienia: zamówień=" + repository.countOrders()
                    + ", niewysłanych wiadomości=" + repository.findUnsent().size());
            int sent = relay.sendPending();
            System.out.println("Wysłano: " + sent + ", niewysłanych wiadomości=" + repository.findUnsent().size());
        }
    }
}
