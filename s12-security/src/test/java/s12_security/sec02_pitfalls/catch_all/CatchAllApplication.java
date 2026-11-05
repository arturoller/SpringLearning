package s12_security.sec02_pitfalls.catch_all;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * ZŁA kombinacja (pułapka z sekcji 3 lekcji Sec02Authorization): {@code @PreAuthorize} na metodzie kontrolera i advice
 * "łap wszystko" ({@code Exception.class}). Odmowa dostępu z metody zamienia się w 500.
 */
@SpringBootApplication
@EnableMethodSecurity
public class CatchAllApplication {

    @RestController
    static class StatsController {

        @GetMapping("/sec02/pitfall/stats")
        @PreAuthorize("hasRole('ADMIN')")
        public String stats() {
            return "Statystyki";
        }
    }

    @RestControllerAdvice
    static class CatchAllAdvice {

        @ExceptionHandler(Exception.class)
        public ProblemDetail any(Exception exception) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Wystąpił nieoczekiwany błąd");
        }
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
