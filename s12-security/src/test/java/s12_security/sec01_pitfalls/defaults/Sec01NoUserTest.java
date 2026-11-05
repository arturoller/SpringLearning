package s12_security.sec01_pitfalls.defaults;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Sec01Basics — sam starter, wariant 1 z 3: żadnych właściwości {@code spring.security.user.*}.
 * {@code @SpringBootTest} + {@code @AutoConfigureMockMvc}, bo sprawdzamy autokonfigurację całej aplikacji (beany), a żądania
 * wystarczy wysłać przez MockMvc. Warianty 2 i 3: Sec01GeneratedPasswordTest, Sec01UserPropertiesTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class Sec01NoUserTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    MockMvcTester mvc;

    @Test
    @DisplayName("Sekcja 1 (pułapka modułu): jest resource-server na classpath i brak spring.security.user.* → Boot NIE tworzy użytkownika")
    void noDefaultUser() {
        assertThat(context.getBeanNamesForType(UserDetailsService.class)).isEmpty();
        assertThat(context.containsBean("defaultSecurityFilterChain")).isTrue();   // ale zamek i tak jest: wszystko chronione
        assertThat(mvc.get().uri("/sec01/defaults/hello")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
