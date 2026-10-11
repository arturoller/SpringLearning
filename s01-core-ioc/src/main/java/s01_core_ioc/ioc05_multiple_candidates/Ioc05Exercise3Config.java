package s01_core_ioc.ioc05_multiple_candidates;

import org.springframework.context.annotation.Bean;

/** Szkielet ĆWICZENIA 3 (patrz Ioc05MultipleCandidates). Klasa BEZ {@code @Configuration} — rejestruje ją test. */
public class Ioc05Exercise3Config {

    @Bean
    public AlertService alertService() {
        // TODO: poproś o parametr NotificationSender oznaczony @Qualifier("smsSender") i zwróć new AlertService(sender)
        throw new UnsupportedOperationException("TODO");
    }
}
