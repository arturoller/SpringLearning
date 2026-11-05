package s12_security.solutions.sec02_authorization;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec02Authorization: szczegółowa reguła PRZED ogólną, na końcu denyAll(). */
@Configuration
public class ExerciseSecurityConfig {

    @Bean
    SecurityFilterChain exerciseFilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/sec02/cw/catalog/hidden/**").hasRole("EMPLOYEE")   // najpierw szczegół
                        .requestMatchers(HttpMethod.GET, "/sec02/cw/catalog/**").permitAll()                  // potem ogół
                        .requestMatchers(HttpMethod.POST, "/sec02/cw/catalog").hasRole("EMPLOYEE")
                        .requestMatchers("/error").permitAll()
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
