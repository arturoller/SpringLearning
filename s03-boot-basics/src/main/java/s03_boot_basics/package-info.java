/**
 * Dział s03_boot_basics — PODSTAWY SPRING BOOTA: co Boot robi „sam z siebie” i jak to kontrolować.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code boot01_auto_configuration} — Boot01AutoConfiguration: autokonfiguracja (auto-configuration),
 *       warunki {@code @ConditionalOnClass}/{@code @ConditionalOnMissingBean}, „ustępowanie” (back-off) przed Twoim
 *       beanem, raport warunków (condition evaluation report) i wyłączanie autokonfiguracji.</li>
 *   <li>{@code boot02_writing_auto_configuration} — Boot02WritingAutoConfiguration: własna klasa
 *       {@code @AutoConfiguration}, plik {@code AutoConfiguration.imports}, testowanie przez
 *       {@code ApplicationContextRunner}.</li>
 *   <li>{@code boot03_spring_application} — Boot03SpringApplication: {@code SpringApplication} i
 *       {@code SpringApplicationBuilder}, typ aplikacji, {@code ApplicationRunner}/{@code CommandLineRunner},
 *       zdarzenia startu, leniwa inicjalizacja, kody wyjścia.</li>
 *   <li>{@code boot04_starters} — Boot04Starters: startery w Boot 4 (zmodularyzowane), co jest w środku startera,
 *       jak Boot „zgaduje” konfigurację z classpath i DevTools.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, a klucze ustawień mają prefiks lekcji ({@code boot01.…}, {@code boot02.…}).</p>
 */
package s03_boot_basics;
