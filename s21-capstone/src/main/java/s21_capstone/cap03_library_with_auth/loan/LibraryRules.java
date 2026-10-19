package s21_capstone.cap03_library_with_auth.loan;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Zasady biblioteki z application-cap03.properties ({@code cap03.library.*}) — rekord z
 * {@code @ConfigurationProperties} (s02_configuration/Cfg02ConfigurationProperties). Zmiana kary albo terminu
 * nie wymaga kompilacji, a test może podać własne wartości.
 *
 * @param loanDays       na ile dni wypożyczamy (14)
 * @param finePerDay     kara za każdy dzień po terminie (0.50 zł)
 * @param maxActiveLoans ile książek naraz może mieć czytelnik (3)
 */
@ConfigurationProperties(prefix = "cap03.library")
public record LibraryRules(int loanDays, BigDecimal finePerDay, int maxActiveLoans) {
}
