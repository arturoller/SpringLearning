/**
 * Dział s03_boot_basics — jak działa Spring Boot od środka: autokonfiguracja i jej warunki, własna autokonfiguracja,
 * SpringApplication (runnery, kod wyjścia, zdarzenia startu, DevTools) oraz startery i zarządzanie wersjami.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code boot01_auto_configuration} — Boot01AutoConfiguration: {@code @EnableAutoConfiguration}, plik .imports,
 *       {@code @ConditionalOnClass}, {@code @ConditionalOnMissingBean}, {@code @ConditionalOnProperty}, raport warunków, exclude.</li>
 *   <li>{@code boot02_writing_auto_configuration} — Boot02WritingAutoConfiguration: własna {@code @AutoConfiguration},
 *       rejestracja w pliku .imports, bezpiecznik właściwością, testy z ApplicationContextRunner.</li>
 *   <li>{@code boot03_spring_application} — Boot03SpringApplication: ustawienia SpringApplication, CommandLineRunner
 *       i ApplicationRunner, ExitCodeGenerator, zdarzenia startu, DevTools (teoria).</li>
 *   <li>{@code boot04_starters} — Boot04Starters: czym jest starter, startery Boot 4, BOM i nadpisywanie wersji,
 *       sprawdzanie classpath.</li>
 * </ol>
 *
 * <p>Moduł ma tylko spring-boot-starter — bez serwera WWW. Każda lekcja ze Springiem ma własną klasę startową
 * ({@code Boot01Application}, {@code Boot02Application}, {@code Boot03Application}); lekcja Boot04 obywa się bez kontekstu.</p>
 *
 * <p>Uwaga: plik {@code src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 * (lekcja Boot02) działa dla WSZYSTKICH aplikacji tego modułu. Dlatego nasza autokonfiguracja jest wyłączona, dopóki
 * lekcja nie ustawi {@code boot02.greeting.enabled=true}. Klasy-przykłady błędów i szkielety ćwiczeń nie mają
 * {@code @Configuration} — testy rejestrują je ręcznie.</p>
 */
package s03_boot_basics;
