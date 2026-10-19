package s22_extras.ext03_oauth2_keycloak;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Konwerter ról Keycloaka na uprawnienia Springa (lekcja Ext03OAuth2Keycloak, sekcja 4).
 *
 * <p>Keycloak zapisuje role DZIEDZINY (realm roles) w tokenie tak:</p>
 * <pre>
 *   "realm_access": { "roles": [ "klient", "offline_access", "default-roles-sklep" ] }
 * </pre>
 * Spring domyślnie tego pola NIE czyta (zna tylko „scope” → SCOPE_...). Ten konwerter zamienia każdą rolę na
 * uprawnienie {@code ROLE_<ROLA WIELKIMI LITERAMI>}, np. klient → ROLE_KLIENT, więc działa {@code hasRole("KLIENT")}.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");   // claim jako mapa (albo null)
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();                                                 // brak ról = brak uprawnień, nie błąd
        }
        return roles.stream()
                .map(role -> "ROLE_" + role.toString().toUpperCase(Locale.ROOT))
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
