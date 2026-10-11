package s01_core_ioc.solutions.ioc05_multiple_candidates;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import s01_core_ioc.ioc05_multiple_candidates.EmailSender;
import s01_core_ioc.ioc05_multiple_candidates.NotificationSender;
import s01_core_ioc.ioc05_multiple_candidates.SmsSender;

/** Rozwiązanie ćwiczenia 2: {@code @Primary} na metodzie @Bean wysyłacza e-mail. */
public class Ioc05Exercise2ConfigSolution {

    @Bean
    @Primary
    public NotificationSender emailSender() {
        return new EmailSender();
    }

    @Bean
    public NotificationSender smsSender() {
        return new SmsSender();
    }
}
