package s12_security.sec02_pitfalls.role_prefix;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** ZŁA konfiguracja (pułapka z sekcji 2 lekcji Sec02Authorization): {@code hasRole("ROLE_ADMIN")} w regułach adresów. */
@SpringBootApplication
public class RolePrefixApplication {

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/sec02/pitfall/admin/**").hasRole("ROLE_ADMIN")    // prefiks ROLE_ podany drugi raz
                .anyRequest().authenticated());
        return http.build();
    }
}
