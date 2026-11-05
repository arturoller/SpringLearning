package s12_security.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 3 i 4 lekcji Sec02Authorization: bezpieczeństwo metod BEZ HTTP — wołamy bean serwisu wprost.
 * {@code @SpringBootTest}, bo potrzebny jest prawdziwy bean z proxy (pośrednikiem) bezpieczeństwa.
 * {@code @WithMockUser} ustawia użytkownika w SecurityContextHolder na czas jednego testu.
 */
@SpringBootTest
class Sec02MethodSecurityTest {

    @Autowired
    OrderService orders;

    @Test
    @DisplayName("Sekcja 3: bean z @PreAuthorize to proxy CGLIB — podklasa wygenerowana przez Springa")
    void serviceIsProxy() {
        assertThat(orders.getClass().getName()).contains("$$SpringCGLIB$$");
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 3: @PreAuthorize z #username == authentication.name — własne zamówienia tak, cudze: AuthorizationDeniedException")
    void preAuthorizeOwnOrders() {
        assertThat(orders.ordersOf("anna")).hasSize(2);
        assertThatThrownBy(() -> orders.ordersOf("piotr"))
                .isInstanceOf(AuthorizationDeniedException.class)       // Security 6.3+: konkretny typ odmowy…
                .isInstanceOf(AccessDeniedException.class)              // …który dziedziczy po starym AccessDeniedException
                .hasMessage("Access Denied");
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Sekcja 3: @PostAuthorize sprawdza WYNIK — metoda się wykonała, ale cudze zamówienie nie wychodzi z serwisu")
    void postAuthorize() {
        assertThat(orders.byId(1).owner()).isEqualTo("anna");
        assertThatThrownBy(() -> orders.byId(3)).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "piotr", roles = "CUSTOMER")
    @DisplayName("Sekcja 3: @PostFilter usuwa z listy elementy, których użytkownik nie może zobaczyć")
    void postFilter() {
        assertThat(orders.visibleOrders()).extracting(ShopOrder::id).containsExactly(3L);
    }

    @Test
    @DisplayName("Sekcja 3: bez żadnego użytkownika w kontekście — AuthenticationCredentialsNotFoundException (a przez HTTP: anonim → 401)")
    void noAuthentication() {
        assertThatThrownBy(() -> orders.countAll()).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    @DisplayName("Sekcja 4: hierarchia ról działa w @PreAuthorize — ADMIN przechodzi hasRole('EMPLOYEE')")
    void roleHierarchyForMethods() {
        assertThat(orders.ordersOf("piotr")).hasSize(1);    // reguła: właściciel albo EMPLOYEE
        assertThat(orders.countAll()).isEqualTo(3);
    }

    @Test
    @WithMockUser(username = "ewa", roles = "EMPLOYEE")
    @DisplayName("Sekcja 4: hierarchia działa tylko w dół — EMPLOYEE nie przechodzi hasRole('ADMIN')")
    void hierarchyGoesDownOnly() {
        assertThatThrownBy(() -> orders.countAll()).isInstanceOf(AuthorizationDeniedException.class);
    }
}
