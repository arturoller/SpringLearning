package s02_configuration.cfg05_environment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Program lojalnościowy — bez {@code havingValue}: bean powstaje, gdy klucz {@code cfg05.feature.loyalty} ISTNIEJE
 * i jego wartość nie jest tekstem "false". Wyłączony, gdy klucza brak albo jest "false".
 */
@Component
@ConditionalOnProperty("cfg05.feature.loyalty")
public class LoyaltyProgram {
}
