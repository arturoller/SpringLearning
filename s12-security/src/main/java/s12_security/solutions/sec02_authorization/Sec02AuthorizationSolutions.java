package s12_security.solutions.sec02_authorization;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;

/** Rozwiązania wzorcowe ćwiczeń 1–3 lekcji Sec02Authorization (ćwiczenie 4: SolutionSalaryService). */
public final class Sec02AuthorizationSolutions {

    private Sec02AuthorizationSolutions() {
    }

    static String solution1(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Rola nie może być pusta");
        }
        if (role.startsWith("ROLE_")) {
            throw new IllegalArgumentException(role + " — prefiks ROLE_ dokleja się sam");
        }
        return "ROLE_" + role;
    }

    static UserDetails solution2() {
        return User.withUsername("tomasz")
                .password("{noop}tomasz-test-123")
                .authorities("ROLE_SALES", "report:read")   // JEDNO wywołanie — roles() i authorities() się nadpisują
                .build();
    }

    static SecurityFilterChain solution3(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                .requestMatchers("/api/hr/**").hasAnyRole("HR", "ADMIN")
                .anyRequest().denyAll());
        http.httpBasic(Customizer.withDefaults());
        http.csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
