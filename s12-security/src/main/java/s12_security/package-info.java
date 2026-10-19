/**
 * Dział s12_security — SPRING SECURITY 7 (security = bezpieczeństwo): kto może wejść do aplikacji (uwierzytelnianie)
 * i co mu wolno (autoryzacja), jak przechowywać hasła, jak zabezpieczyć API bez sesji i tokenami JWT, i jak to testować.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code sec01_basics} — Sec01Basics: co robi sam starter (hasło w logu, /login, HTTP Basic), łańcuch filtrów
 *       ({@code DelegatingFilterProxy}, {@code FilterChainProxy}, {@code SecurityFilterChain}), własny łańcuch
 *       w DSL z lambdami, 401 a 403, strona {@code /error}.</li>
 *   <li>{@code sec02_authorization} — Sec02Authorization: role i uprawnienia (prefiks {@code ROLE_}), kolejność reguł,
 *       metody HTTP w regułach, {@code @EnableMethodSecurity} i {@code @PreAuthorize}, domyślne „zabronione”.</li>
 *   <li>{@code sec03_users_passwords} — Sec03UsersPasswords: {@code UserDetailsService}, użytkownicy z „bazy”,
 *       {@code PasswordEncoder} (BCrypt, {@code DelegatingPasswordEncoder}, {@code {noop}}), zalogowany użytkownik
 *       ({@code @AuthenticationPrincipal}, {@code SecurityContextHolder}).</li>
 *   <li>{@code sec04_stateless_api} — Sec04StatelessApi: API bez sesji, CSRF (kiedy wolno wyłączyć), CORS
 *       w Security, błędy 401/403 jako JSON ({@code ProblemDetail}), dwa łańcuchy filtrów.</li>
 *   <li>{@code sec05_jwt} — Sec05Jwt: budowa tokenu JWT, wystawianie ({@code JwtEncoder}) i sprawdzanie
 *       (Resource Server, {@code JwtDecoder}), {@code scope} a uprawnienia, token przeterminowany.</li>
 *   <li>{@code sec06_security_testing} — Sec06SecurityTesting: {@code @WebMvcTest} z bezpieczeństwem,
 *       {@code @WithMockUser}, {@code @WithUserDetails}, {@code httpBasic()}, {@code user()}, {@code jwt()},
 *       {@code csrf()}.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową i własną konfigurację bezpieczeństwa w swoim podpakiecie, więc łańcuchy filtrów lekcji się nie mieszają.
 * Spring Security jest TYLKO w tym module — w innych działach zablokowałby wszystkie punkty końcowe.
 * Wszystkie hasła i klucze w tym dziale są TESTOWE; klucze do JWT są generowane w czasie działania programu.</p>
 */
package s12_security;
