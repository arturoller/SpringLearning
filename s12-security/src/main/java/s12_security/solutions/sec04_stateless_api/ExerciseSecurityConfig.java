package s12_security.solutions.sec04_stateless_api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/** Rozwiązanie wzorcowe ćwiczenia 4 lekcji Sec04StatelessApi. */
@Configuration
public class ExerciseSecurityConfig {

    @Bean
    SecurityFilterChain exerciseFilterChain(HttpSecurity http) {
        ExerciseJsonEntryPoint entryPoint = new ExerciseJsonEntryPoint();
        http
                .securityMatcher("/sec04/cw/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/sec04/cw/catalog").permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(ExerciseCors.source()))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    UserDetailsService exerciseUsers() {
        return new InMemoryUserDetailsManager(
                User.withUsername("anna").password("{noop}haslo-testowe").roles("CUSTOMER").build());   // TYLKO DO NAUKI
    }
}
