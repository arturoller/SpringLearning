/**
 * Dział s12_security — BEZPIECZEŃSTWO (Spring Security 7): kto Ty jesteś (uwierzytelnianie — authentication) i co
 * Ci wolno (autoryzacja — authorization); hasła, sesje, API bez sesji, tokeny JWT i testy zabezpieczeń.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code sec01_basics} — Sec01Basics: co robi sam starter, łańcuch filtrów (SecurityFilterChain),
 *       {@code HttpSecurity}, 401 kontra 403, logowanie formularzem i Basic.</li>
 *   <li>{@code sec02_authorization} — Sec02Authorization: reguły dla adresów ({@code authorizeHttpRequests}), role
 *       i uprawnienia, {@code @PreAuthorize} na metodach.</li>
 *   <li>{@code sec03_users_passwords} — Sec03UsersPasswords: {@code UserDetailsService}, użytkownicy w bazie,
 *       {@code PasswordEncoder} (BCrypt), rejestracja.</li>
 *   <li>{@code sec04_stateless_api} — Sec04StatelessApi: API bez sesji, CSRF — kiedy wyłączyć, CORS w Spring Security.</li>
 *   <li>{@code sec05_jwt} — Sec05Jwt: tokeny JWT — wydawanie, sprawdzanie (Resource Server), role w tokenie.</li>
 *   <li>{@code sec06_security_testing} — Sec06SecurityTesting: {@code @WithMockUser}, {@code with(user(...))},
 *       {@code with(jwt())}, {@code csrf()} — testy zabezpieczeń.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}; każda lekcja ma własną klasę
 * startową i własną konfigurację bezpieczeństwa, więc reguły jednej lekcji nie działają w innej.</p>
 */
package s12_security;
