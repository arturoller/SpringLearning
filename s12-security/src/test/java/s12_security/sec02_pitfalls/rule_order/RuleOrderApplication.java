package s12_security.sec02_pitfalls.rule_order;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ZŁA konfiguracja (pułapki z sekcji 1 i 5 lekcji Sec02Authorization):
 * <ol>
 *   <li>ogólna reguła {@code /sec02/pitfall/admin/**} stoi PRZED szczegółową {@code /sec02/pitfall/admin/reports/**} —
 *       szczegółowa nigdy nie zostanie sprawdzona;</li>
 *   <li>usuwanie produktu jest "ukryte w interfejsie" (przycisk widzi tylko admin), ale adres nie ma własnej reguły,
 *       a na końcu jest {@code anyRequest().authenticated()} — więc może go wywołać każdy zalogowany.</li>
 * </ol>
 */
@SpringBootApplication
@RestController
public class RuleOrderApplication {

    @GetMapping("/sec02/pitfall/admin/reports/salaries")
    public String salaries() {
        return "Pensje pracowników";
    }

    @DeleteMapping("/sec02/pitfall/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        // "przycisk Usuń widzi tylko admin" — ale serwer tego nie sprawdza
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec02/pitfall/admin/**").authenticated()                // ogólna — za wcześnie!
                        .requestMatchers("/sec02/pitfall/admin/reports/**").hasRole("ADMIN")      // nigdy nie zadziała
                        .anyRequest().authenticated())                                              // "wystarczy być zalogowanym"
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
