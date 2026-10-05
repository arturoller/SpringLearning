/**
 * Dział s00_start — START, czyli jak korzystać z kursu SpringLearning.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code start01_how_to_use} — Start01HowToUse: budowa lekcji, tagi, nazwy, metoda nauki i powtórek.</li>
 *   <li>{@code start02_multi_module} — Start02MultiModule: projekt wielomodułowy, rodzic i moduły, classpath, startery.</li>
 *   <li>{@code start03_first_application} — Start03FirstApplication: pierwsza aplikacja Spring Boot i jej uruchamianie.</li>
 *   <li>{@code start04_testing_intro} — Start04TestingIntro: wprowadzenie do testów (JUnit, AssertJ, MockMvc,
 *       {@code @SpringBootTest}, {@code @WebMvcTest}, {@code @MockitoBean}).</li>
 *   <li>{@code start05_http_files} — Start05HttpFiles: pliki .http i prawdziwe zapytania HTTP do działającej aplikacji.</li>
 *   <li>{@code start06_exercises} — Start06Exercises: jak działają ćwiczenia, tagi testów i rozwiązania wzorcowe.</li>
 * </ol>
 *
 * <p>Czym jest ten plik? package-info.java opisuje CAŁY pakiet (package = pakiet, info = informacja). Nie zawiera
 * klasy — tylko komentarz dokumentacyjny i deklarację pakietu. IntelliJ pokazuje ten opis po najechaniu na nazwę pakietu.</p>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja, która potrzebuje Springa, ma
 * własną klasę startową w swoim podpakiecie — dzięki temu lekcje nie widzą nawzajem swoich beanów.</p>
 */
package s00_start;
