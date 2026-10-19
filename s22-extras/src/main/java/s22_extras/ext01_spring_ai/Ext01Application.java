package s22_extras.ext01_spring_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Klasa startowa lekcji Ext01SpringAi. Włącza profil ext01 (application-ext01.properties: Ollama na localhost:11434).
 *
 * <p>Najpierw uruchom Ollamę z modelem (s22-extras/compose/ext01/compose.yaml — opis w pliku), potem ▶ przy main
 * i zapytania z s22-extras/http/ext01_spring_ai.http. Bez Ollamy aplikacja WSTAJE (Spring AI nie łączy się z modelem
 * przy starcie), a dopiero zapytanie kończy się błędem 500 (Connection refused).</p>
 */
@SpringBootApplication
public class Ext01Application {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Ext01Application.class);
        application.setAdditionalProfiles("ext01");
        application.run(args);
    }

    /**
     * IZOLACJA: starter oauth2-resource-server jest na classpath całego modułu, więc bez tego beana Spring Security
     * odrzuciłby KAŻDE zapytanie (401). Ta lekcja nie uczy zabezpieczeń — jawnie otwieramy wszystko.
     */
    @Bean
    SecurityFilterChain ext01OpenChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())   // permit all = wpuść wszystkich
                .csrf(csrf -> csrf.disable())                                    // API bez sesji — bez ochrony CSRF
                .build();
    }
}
