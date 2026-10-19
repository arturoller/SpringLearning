package s17_events.evt02_transactional_events;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.math.BigDecimal;

/**
 * Klasa startowa lekcji Evt02TransactionalEvents (własna, bo lekcje działu nie dzielą kontekstu). Włącza profil
 * {@code evt02} (baza H2, migracje Flyway), składa jedno zamówienie zatwierdzone i jedno wycofane, i wypisuje, w jakich
 * fazach transakcji zareagowali słuchacze.
 */
@SpringBootApplication
public class Evt02Application {

    public static void main(String[] args) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(Evt02Application.class)
                .profiles("evt02").web(WebApplicationType.NONE).run(args)) {
            OrderService service = context.getBean(OrderService.class);
            EventLog log = context.getBean(EventLog.class);
            service.placeOrder("ZAM-001", "Jan Kowalski", new BigDecimal("349.90"));
            System.out.println("--- zamówienie zatwierdzone:");
            log.entries().forEach(entry -> System.out.println("  " + entry.what()));
            log.clear();
            try {
                service.placeOrderAndFail("ZAM-002", "Maria Nowak", new BigDecimal("64.99"));
            } catch (IllegalStateException e) {
                System.out.println("--- zamówienie wycofane (" + e.getMessage() + "):");
            }
            log.entries().forEach(entry -> System.out.println("  " + entry.what()));
        }
    }
}
