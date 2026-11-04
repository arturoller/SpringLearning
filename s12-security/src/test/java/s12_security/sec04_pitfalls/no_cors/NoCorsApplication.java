package s12_security.sec04_pitfalls.no_cors;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ZŁA konfiguracja (pułapka z sekcji 3 lekcji Sec04StatelessApi): CORS ustawiony tylko w Spring MVC
 * ({@code WebMvcConfigurer.addCorsMappings}, jak w s04_web_rest/Rest06Cors), a łańcuch Spring Security nie ma
 * {@code http.cors(...)}. Preflight OPTIONS nie niesie nagłówka Authorization, więc filtry bezpieczeństwa odrzucają go
 * jako anonima, zanim reguła CORS z MVC w ogóle zostanie przeczytana.
 */
@SpringBootApplication
public class NoCorsApplication {

    @RestController
    static class OrdersController {

        @GetMapping("/sec04/pitfall/orders")
        public String orders() {
            return "Zamówienia";
        }
    }

    @Bean
    WebMvcConfigurer mvcCors() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/sec04/pitfall/**").allowedOrigins("http://localhost:3000").allowedMethods("GET");
            }
        };
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        // brakuje: .cors(Customizer.withDefaults()) — wtedy Security użyłby reguł z addCorsMappings
        return http.build();
    }

    @Bean
    UserDetailsService pitfallUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());   // TYLKO DO NAUKI
    }
}
