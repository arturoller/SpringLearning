package s22_extras.ext04_tracing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Klasa startowa lekcji Ext04Tracing. Włącza profil ext04 (application-ext04.properties: próbkowanie 100%, eksport
 * śladów OTLP do Jaegera na localhost:4318).
 *
 * <p>Najpierw uruchom Jaegera: {@code docker compose -f s22-extras/compose/ext04/compose.yaml up -d}, potem ▶ przy
 * main, zapytania z s22-extras/http/ext04_tracing.http i ślad w przeglądarce: http://localhost:16686.
 * Bez Jaegera aplikacja działa, a eksporter (po kilku ponowieniach) loguje ERROR „Failed to export spans. The request
 * could not be executed.” z przyczyną „Failed to connect to localhost/127.0.0.1:4318” — sprawdzone uruchomieniem.</p>
 */
@SpringBootApplication
public class Ext04Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Ext04Application.class);
        application.setAdditionalProfiles("ext04");
        application.run(args);
    }

    /** IZOLACJA: starter resource-server jest w module — ta lekcja nie uczy zabezpieczeń, więc otwieramy wszystko. */
    @Bean
    SecurityFilterChain ext04OpenChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
