package s12_security.sec04_pitfalls.advice;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ZŁE założenie (pułapka z sekcji 4 lekcji Sec04StatelessApi): "mój {@code @RestControllerAdvice} zamieni każdy błąd
 * bezpieczeństwa w ProblemDetail". Advice ma handlery {@code AuthenticationException} i {@code AccessDeniedException}, ale
 * odmowy z reguł ADRESÓW zapadają w filtrach — zanim żądanie dotrze do DispatcherServlet — więc advice nigdy ich nie widzi.
 */
@SpringBootApplication
public class AdviceApplication {

    /** Licznik wywołań advice — test sprawdza, że pozostał zerem. */
    static final AtomicInteger ADVICE_CALLS = new AtomicInteger();

    @RestController
    static class StatsController {

        @GetMapping("/sec04/pitfall/admin/stats")
        public String stats() {
            return "Statystyki";
        }
    }

    @RestControllerAdvice
    static class SecurityAdvice {

        @ExceptionHandler(AuthenticationException.class)
        public ProblemDetail unauthorized(AuthenticationException exception) {
            ADVICE_CALLS.incrementAndGet();
            return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Z advice: zaloguj się");
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ProblemDetail forbidden(AccessDeniedException exception) {
            ADVICE_CALLS.incrementAndGet();
            return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Z advice: brak uprawnień");
        }
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec04/pitfall/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    UserDetailsService pitfallUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());   // TYLKO DO NAUKI
    }
}
