package s12_security.sec05_pitfalls.default_converter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

/**
 * NIEPEŁNA konfiguracja (pułapka z sekcji 4 lekcji Sec05Jwt): tokeny mają twierdzenie {@code roles}, reguła mówi
 * {@code hasRole("ADMIN")}, ale Resource Server używa DOMYŚLNEGO konwertera — a ten czyta tylko {@code scope}/{@code scp}.
 */
@SpringBootApplication
public class DefaultConverterApplication {

    @RestController
    static class AdminController {

        @GetMapping("/sec05/pitfall/admin")
        public String admin() {
            return "Panel administratora";
        }

        @GetMapping("/sec05/pitfall/orders")
        public String orders() {
            return "Zamówienia";
        }
    }

    @Bean
    SecurityFilterChain pitfallChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec05/pitfall/admin").hasRole("ADMIN")
                        .requestMatchers("/sec05/pitfall/orders").hasAuthority("SCOPE_orders:read")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))   // domyślny konwerter
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    SecretKey pitfallKey() throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance("HmacSHA256").generateKey();    // TYLKO DO NAUKI: losowy przy starcie
    }

    @Bean
    JwtEncoder pitfallEncoder(SecretKey pitfallKey) {
        return NimbusJwtEncoder.withSecretKey(pitfallKey).build();
    }

    @Bean
    JwtDecoder pitfallDecoder(SecretKey pitfallKey) {
        return NimbusJwtDecoder.withSecretKey(pitfallKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
