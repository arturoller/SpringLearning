package s17_events.evt01_application_events;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Evt01ApplicationEvents (własna, bo lekcje działu nie dzielą kontekstu). Włącza profil
 * {@code evt01}, składa dwa zamówienia i wypisuje, co zrobili słuchacze — w jakiej kolejności i w którym wątku.
 */
@SpringBootApplication
public class Evt01Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Evt01Application.class)
                .profiles("evt01").web(WebApplicationType.NONE).run(args)) {
            OrderService service = context.getBean(OrderService.class);
            EventLog log = context.getBean(EventLog.class);
            service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));
            service.placeOrder("ZAM-002", "Zofia Krawczyk", new BigDecimal("5499.99"));
            log.entries().forEach(entry -> System.out.println(entry.what() + "   [wątek: " + entry.thread() + "]"));
        }
    }
}
