package s12_security.sec05_jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * SZKIELET ĆWICZENIA 3 lekcji Sec05Jwt: własny konwerter (converter = przelicznik) twierdzenia {@code roles} na uprawnienia —
 * to samo, co w lekcji robi skonfigurowany {@code JwtGrantedAuthoritiesConverter}, tylko napisane samodzielnie.
 */
public class ExerciseRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /**
     * Ćwiczenie 3: {@code roles: ["ADMIN", "EMPLOYEE"]} → {@code ROLE_ADMIN}, {@code ROLE_EMPLOYEE}. Brak twierdzenia
     * {@code roles} → pusta lista (nie {@code null}, nie wyjątek).
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }
}
