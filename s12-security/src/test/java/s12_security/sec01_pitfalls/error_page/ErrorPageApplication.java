package s12_security.sec01_pitfalls.error_page;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ZŁA konfiguracja (pułapka z sekcji 5 lekcji Sec01Basics): publiczne adresy są dozwolone, ale {@code /error} — nie.
 * Gdy publiczny adres kończy się błędem, Tomcat przekazuje żądanie na {@code /error} (dispatch ERROR), a filtry bezpieczeństwa
 * sprawdzają je jeszcze raz: anonim nie ma tam wstępu, więc zamiast 500/404 dostaje 401.
 */
@SpringBootApplication
@RestController
public class ErrorPageApplication {

    @GetMapping("/sec01/pitfall/public/crash")
    public String crash() {
        throw new IllegalStateException("awaria");
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec01/pitfall/public/**").permitAll()   // brak: .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
