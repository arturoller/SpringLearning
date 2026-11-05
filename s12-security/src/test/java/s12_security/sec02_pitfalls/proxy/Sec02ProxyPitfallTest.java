package s12_security.sec02_pitfalls.proxy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 2 i 5 lekcji Sec02Authorization — pułapki bezpieczeństwa metod. {@code @SpringBootTest}, bo potrzebny jest
 * prawdziwy bean z proxy.
 */
@SpringBootTest
class Sec02ProxyPitfallTest {

    @Autowired
    PayrollService payroll;

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 5 (pułapka): wywołanie zabezpieczonej metody z tej samej klasy omija proxy — klient widzi pensje")
    void selfInvocationBypassesSecurity() {
        assertThatThrownBy(() -> payroll.salaries()).isInstanceOf(AuthorizationDeniedException.class);   // przez proxy: odmowa
        assertThat(payroll.report()).isEqualTo("Raport: Pensje: tajne");                                // przez this: przechodzi!
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    @DisplayName("Sekcja 2: w @PreAuthorize hasRole('ROLE_ADMIN') działa (prefiks nie jest dopisywany drugi raz) — inaczej niż w regułach adresów")
    void prefixInExpressionWorks() {
        assertThat(payroll.salariesWithPrefix()).isEqualTo("Pensje: tajne");
    }
}
