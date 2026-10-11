package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.context.annotation.Bean;

/**
 * Szkielet ĆWICZENIA 2 (patrz Ioc05MultipleCandidates, komentarz przy ćwiczeniu 2). Klasa BEZ {@code @Configuration}:
 * rejestruje ją test, aplikacja lekcji jej nie skanuje.
 */
public class Ioc05Exercise2Config {

    // TODO: dodaj adnotację @Primary na jednej z poniższych metod tak, żeby getBean(NotificationSender.class)
    //       zwracało wysyłacz e-mail (bez @Primary kontener zgłosi NoUniqueBeanDefinitionException).
    @Bean
    public NotificationSender emailSender() {
        return new EmailSender();
    }

    @Bean
    public NotificationSender smsSender() {
        return new SmsSender();
    }
}
