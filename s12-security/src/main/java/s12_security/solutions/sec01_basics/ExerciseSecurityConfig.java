package s12_security.solutions.sec01_basics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Rozwiązanie wzorcowe ćwiczenia 5 lekcji Sec01Basics: reguły adresów {@code /sec01/cw/**}. */
@Configuration
public class ExerciseSecurityConfig {

    @Bean
    SecurityFilterChain exerciseFilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sec01/cw/public/**", "/error").permitAll()
                        .requestMatchers("/sec01/cw/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
