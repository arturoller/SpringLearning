package s12_security.sec03_users_passwords;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Sec03SecurityConfig = łańcuch filtrów i koder haseł lekcji Sec03UsersPasswords.
 * Użytkownicy pochodzą z beana ShopUserDetailsService — Spring Security znajduje go sam (jedyny bean tego typu).
 */
@Configuration
public class Sec03SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated());
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Koder haseł. {@code createDelegatingPasswordEncoder()} = koder „delegujący”: koduje BCryptem i dopisuje prefiks
     * {@code {bcrypt}}, a przy sprawdzaniu czyta prefiks i wybiera właściwy algorytm. Spring Security użyje TEGO
     * beana do porównywania haseł przy logowaniu.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();   // factories = fabryki
    }
}
