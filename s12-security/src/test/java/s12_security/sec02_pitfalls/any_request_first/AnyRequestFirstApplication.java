package s12_security.sec02_pitfalls.any_request_first;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** ZŁA konfiguracja (pułapka z sekcji 1 lekcji Sec02Authorization): {@code anyRequest()} przed innymi regułami. */
@SpringBootApplication
public class AnyRequestFirstApplication {

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
                .requestMatchers("/sec02/pitfall/public/**").permitAll());   // po anyRequest() — tej reguły nikt by nie sprawdził
        return http.build();
    }
}
