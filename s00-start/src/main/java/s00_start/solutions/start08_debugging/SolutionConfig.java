package s00_start.solutions.start08_debugging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import s00_start.start08_debugging.DebugShop.EmailNotifier;
import s00_start.start08_debugging.DebugShop.Notifier;
import s00_start.start08_debugging.DebugShop.OrderService;
import s00_start.start08_debugging.DebugShop.SmsNotifier;

/**
 * Rozwiązanie wzorcowe ćwiczenia 2: dwóch kandydatów typu Notifier, więc jednego oznaczamy @Primary (bean główny —
 * Spring wybiera go, gdy pasuje kilka). Konstruktor OrderService zostaje bez zmian.
 */
@Configuration(proxyBeanMethods = false)
public class SolutionConfig {

    @Bean
    EmailNotifier emailNotifier() {
        return new EmailNotifier();
    }

    @Bean
    @Primary
    SmsNotifier smsNotifier() {
        return new SmsNotifier();
    }

    @Bean
    OrderService orderService(Notifier notifier) {
        return new OrderService(notifier);
    }
}
