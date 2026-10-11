package s03_boot_basics.solutions.boot02_writing_auto_configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import s03_boot_basics.boot02_writing_auto_configuration.LoyaltyPolicy;

/**
 * Rozwiązanie wzorcowe ćwiczenia 3: pełna autokonfiguracja programu lojalnościowego.
 * Może mieć {@code @AutoConfiguration}, bo pakiet solutions nie jest skanowany przez żadną klasę startową.
 * Nie jest wpisana do pliku .imports — test rejestruje ją przez AutoConfigurations.of(...).
 */
@AutoConfiguration
@ConditionalOnProperty(name = "boot02.loyalty.enabled", havingValue = "true")
public class Boot02LoyaltyAutoConfigurationSolution {

    @Bean
    @ConditionalOnMissingBean
    LoyaltyPolicy loyaltyPolicy(@Value("${boot02.loyalty.percent:5}") int percent) {
        return new LoyaltyPolicy(percent);
    }
}
