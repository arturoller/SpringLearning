package s12_security.solutions.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import s12_security.sec02_authorization.MethodSecurityOnly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy rozwiązań wzorcowych ćwiczeń 1–3 lekcji Sec02Authorization. @Tag("wzorzec") — biegną w domyślnym ./mvnw test.
 * Ten sam mały kontekst co w teście ćwiczeń: serwis rozwiązania + samo bezpieczeństwo metod.
 */
@Tag("wzorzec")
@SpringJUnitConfig(classes = {ExerciseOrderService.class, MethodSecurityOnly.class})
class Sec02SolutionsTest {

    @Autowired
    ExerciseOrderService service;

    private static String[] reachable(RoleHierarchy hierarchy, String authority) {
        return hierarchy.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList(authority)).stream()
                .map(GrantedAuthority::getAuthority).sorted().toArray(String[]::new);
    }

    @Test
    @DisplayName("Wzorzec 1: roleToAuthority dopisuje ROLE_, a nazwę z prefiksem odrzuca")
    void exercise1() {
        assertThat(AuthorityNames.roleToAuthority("ADMIN")).isEqualTo("ROLE_ADMIN");
        assertThatThrownBy(() -> AuthorityNames.roleToAuthority("ROLE_ADMIN")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: hierarchia ADMIN > EMPLOYEE, AUDITOR; EMPLOYEE > CUSTOMER")
    void exercise2() {
        RoleHierarchy hierarchy = ExerciseRoleHierarchy.create();
        assertThat(reachable(hierarchy, "ROLE_ADMIN"))
                .containsExactly("ROLE_ADMIN", "ROLE_AUDITOR", "ROLE_CUSTOMER", "ROLE_EMPLOYEE");
        assertThat(reachable(hierarchy, "ROLE_EMPLOYEE")).containsExactly("ROLE_CUSTOMER", "ROLE_EMPLOYEE");
        assertThat(reachable(hierarchy, "ROLE_AUDITOR")).containsExactly("ROLE_AUDITOR");
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Wzorzec 3a: właścicielka anuluje swoje zamówienie, cudzego — nie")
    void exercise3Owner() {
        assertThat(service.cancel("anna", 5)).isEqualTo("Anulowano zamówienie 5 klienta anna");
        assertThatThrownBy(() -> service.cancel("piotr", 6)).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "boss", roles = "ADMIN")
    @DisplayName("Wzorzec 3a: ADMIN anuluje każde zamówienie, ale zwrotu nie zrobi (tu nie ma hierarchii ról)")
    void exercise3Admin() {
        assertThat(service.cancel("piotr", 6)).isEqualTo("Anulowano zamówienie 6 klienta piotr");
        assertThatThrownBy(() -> service.refund(10)).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    @WithMockUser(username = "ewa", roles = "EMPLOYEE")
    @DisplayName("Wzorzec 3b: zwrot robi tylko EMPLOYEE")
    void exercise3Employee() {
        assertThat(service.refund(10)).isEqualTo("Zwrot 10 zł");
    }

    @Test
    @WithMockUser(username = "anna", roles = "CUSTOMER")
    @DisplayName("Wzorzec 3b: klient nie zrobi zwrotu")
    void exercise3CustomerRefund() {
        assertThatThrownBy(() -> service.refund(10)).isInstanceOf(AuthorizationDeniedException.class);
    }
}
