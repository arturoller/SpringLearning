package s12_security.solutions.sec01_basics;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Rozwiązania wzorcowe ćwiczeń lekcji Sec01Basics — zajrzyj dopiero po własnej próbie! */
public final class Sec01BasicsSolutions {

    private Sec01BasicsSolutions() {
    }

    static String solution1(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    static SecurityFilterChain solution2(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/error").permitAll()   // kilka wzorców w jednej regule
                .anyRequest().authenticated());
        http.httpBasic(Customizer.withDefaults());
        return http.build();
    }

    static SecurityFilterChain solution3(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());
        http.formLogin(Customizer.withDefaults());   // form login = logowanie formularzem (strona /login)
        http.httpBasic(Customizer.withDefaults());   // oba naraz: przeglądarka → /login, program → 401 + Basic
        return http.build();
    }
}
