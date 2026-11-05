package s12_security.sec06_security_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 1 lekcji Sec06SecurityTesting: adnotacje {@code @WithMockUser}, {@code @WithAnonymousUser}, {@code @WithUserDetails}.
 * {@code @WebMvcTest} + {@code @Import}: łańcuchy filtrów (Sec06SecurityConfig — wycinek sam ich nie ładuje) i prawdziwy
 * OrderService (z proxy bezpieczeństwa metod; {@code @WebMvcTest} nie ładuje {@code @Service}).
 * <p>
 * Adnotacja na KLASIE działa w każdym teście; adnotacja na METODZIE ją zastępuje.
 */
@WebMvcTest
@Import({Sec06SecurityConfig.class, OrderService.class})
@WithMockUser(username = "anna", roles = "CUSTOMER")      // domyślny użytkownik wszystkich testów tej klasy
class Sec06AnnotationsTest {

    @Autowired
    MockMvcTester mvc;

    private static Authentication current() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    @DisplayName("Sekcja 1: @WithMockUser na klasie — anna (CUSTOMER) widzi swoje zamówienia, panel admina 403")
    void classLevelUser() {
        assertThat(mvc.get().uri("/sec06/orders")).hasStatusOk()
                .bodyJson().extractingPath("$[*].id").asArray().containsExactly(1, 2);
        assertThat(mvc.get().uri("/sec06/admin/stats")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    @DisplayName("Sekcja 1: @WithMockUser na metodzie zastępuje ten z klasy — boss (ADMIN) wchodzi do panelu admina")
    void methodLevelOverrides() {
        assertThat(current().getName()).isEqualTo("boss");
        assertThat(mvc.get().uri("/sec06/admin/stats")).hasStatusOk();
    }

    @Test
    @WithAnonymousUser
    @DisplayName("Sekcja 1: @WithAnonymousUser — anonim mimo adnotacji na klasie: zamówienia 401, katalog 200")
    void anonymous() {
        assertThat(mvc.get().uri("/sec06/orders")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/sec06/products")).hasStatusOk();
    }

    @Test
    @WithMockUser(username = "nikt", roles = "EMPLOYEE")
    @DisplayName("Sekcja 1: @WithMockUser nie pyta UserDetailsService — użytkownik \"nikt\" nie istnieje, a test przechodzi")
    void mockUserNeedNotExist() {
        // Kontekst czytamy PRZED żądaniem: MockMvc działa w tym samym wątku, a filtry czyszczą SecurityContextHolder na końcu
        // żądania — po mvc.get()/post() current() zwróciłoby null.
        assertThat(((UserDetails) current().getPrincipal()).getPassword()).isEqualTo("password");   // hasło-atrapa
        assertThat(mvc.post().uri("/sec06/products")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    @WithUserDetails("ewa")
    @DisplayName("Sekcja 1: @WithUserDetails(\"ewa\") ładuje PRAWDZIWEGO użytkownika z UserDetailsService (role z serwisu)")
    void realUserDetails() {
        assertThat(current().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_EMPLOYEE");                          // bez FACTOR_PASSWORD — nie było logowania hasłem
        // Prawdziwy zapisany hash: "{noop}haslo-testowe" albo — gdy inny test zalogował już ewę hasłem — "{bcrypt}…"
        // (automatyczny upgrade hasła, Sec03UsersPasswords). Na pewno nie atrapa "password" z @WithMockUser.
        assertThat(((UserDetails) current().getPrincipal()).getPassword()).startsWith("{").isNotEqualTo("password");
        assertThat(mvc.post().uri("/sec06/products")).hasStatus(HttpStatus.CREATED);
        assertThat(mvc.get().uri("/sec06/orders/of/piotr")).hasStatusOk();  // reguła metody: EMPLOYEE widzi cudze
    }
}
