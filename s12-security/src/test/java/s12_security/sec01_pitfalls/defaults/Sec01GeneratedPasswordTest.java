package s12_security.sec01_pitfalls.defaults;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Sec01Basics — sam starter, wariant 2 z 3: podana tylko nazwa użytkownika.
 * {@code OutputCaptureExtension} (przechwytywanie wyjścia) łapie to, co aplikacja wypisała przy starcie.
 */
@SpringBootTest(properties = "spring.security.user.name=user")
@ExtendWith(OutputCaptureExtension.class)
class Sec01GeneratedPasswordTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("Sekcja 1: podana tylko nazwa → użytkownik w pamięci i losowe hasło wypisane w logu przy starcie")
    void generatedPassword(CapturedOutput output) {
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).containsExactly("inMemoryUserDetailsManager");
        assertThat(output.getOut()).contains("Using generated security password: ");
    }
}
