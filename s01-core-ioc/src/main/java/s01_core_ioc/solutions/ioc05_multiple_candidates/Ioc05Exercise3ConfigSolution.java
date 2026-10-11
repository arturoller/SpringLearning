package s01_core_ioc.solutions.ioc05_multiple_candidates;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import s01_core_ioc.ioc05_multiple_candidates.AlertService;
import s01_core_ioc.ioc05_multiple_candidates.NotificationSender;

/** Rozwiązanie ćwiczenia 3: wysyłacz SMS wskazany kwalifikatorem. */
public class Ioc05Exercise3ConfigSolution {

    @Bean
    public AlertService alertService(@Qualifier("smsSender") NotificationSender sender) {
        return new AlertService(sender);
    }
}
