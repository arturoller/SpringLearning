package s12_security.sec02_authorization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authorization.AuthorityAuthorizationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sekcje 2 i 4 lekcji Sec02Authorization: role kontra uprawnienia i hierarchia ról — zwykłe testy jednostkowe, bez Springa.
 */
class Sec02RoleHierarchyTest {

    @Test
    @DisplayName("Sekcja 4: z ROLE_ADMIN hierarchia \"wylicza\" też ROLE_EMPLOYEE i ROLE_CUSTOMER")
    void reachableAuthorities() {
        RoleHierarchy hierarchy = Sec02SecurityConfig.roleHierarchy();
        assertThat(hierarchy.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList("ROLE_ADMIN")))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_EMPLOYEE", "ROLE_CUSTOMER");
        assertThat(hierarchy.getReachableGrantedAuthorities(AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Sekcja 2 (pułapka): hasRole(\"ROLE_ADMIN\") w regułach adresów to wyjątek — prefiks ROLE_ dopisuje się sam")
    void hasRoleWithPrefixFails() {
        assertThatThrownBy(() -> AuthorityAuthorizationManager.hasRole("ROLE_ADMIN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("should not start with ROLE_");
    }
}
