package s00_start.start08_debugging;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import s00_start.start08_debugging.DebugShop.EmailNotifier;
import s00_start.start08_debugging.DebugShop.Notifier;
import s00_start.start08_debugging.DebugShop.OrderService;
import s00_start.start08_debugging.DebugShop.SmsNotifier;

/**
 * SZKIELET do ĆWICZENIA 2 lekcji Start08Debugging: ta konfiguracja jest CELOWO zepsuta — kontekst się nie uruchomi.
 * Napraw ją (patrz opis ćwiczenia w Start08Debugging). Nie ma tu skanowania ani @SpringBootApplication: klasę
 * uruchamia tylko test ćwiczenia, jawnie, przez ApplicationContextRunner.
 */
@Configuration(proxyBeanMethods = false)
public class ExerciseConfig {

    @Bean
    EmailNotifier emailNotifier() {
        return new EmailNotifier();
    }

    @Bean
    SmsNotifier smsNotifier() {
        return new SmsNotifier();
    }

    @Bean
    OrderService orderService(Notifier notifier) {
        return new OrderService(notifier);
    }
}
