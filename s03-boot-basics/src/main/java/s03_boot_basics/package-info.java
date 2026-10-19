/**
 * Dział s03_boot_basics — PODSTAWY SPRING BOOTA: co Boot robi „sam”, skąd to wie i jak to kontrolować.
 * (auto-configuration = autokonfiguracja; starter = zestaw startowy zależności)
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code boot01_auto_configuration} — Boot01AutoConfiguration: {@code @SpringBootApplication}, pliki
 *       {@code AutoConfiguration.imports}, warunki {@code @ConditionalOn…}, „wycofanie się” autokonfiguracji przed Twoim
 *       beanem, raport warunków ({@code debug=true}) i wyłączanie autokonfiguracji.</li>
 *   <li>{@code boot02_writing_auto_configuration} — Boot02WritingAutoConfiguration: własna autokonfiguracja
 *       „biblioteki sklepu” — {@code @AutoConfiguration}, warunki, rekord ustawień, rejestracja w pliku {@code .imports},
 *       kolejność ({@code after}) i testy z {@code ApplicationContextRunner}.</li>
 *   <li>{@code boot03_spring_application} — Boot03SpringApplication: co robi {@code SpringApplication.run} krok po kroku,
 *       dostosowanie startu, runnery ({@code ApplicationRunner}, {@code CommandLineRunner}), zdarzenia cyklu życia
 *       aplikacji i kod wyjścia.</li>
 *   <li>{@code boot04_starters} — Boot04Starters: startery Boota 4 (moduły technologii, nowe nazwy), BOM i wersje,
 *       {@code dependency:tree}, dlaczego kurs ma osobny moduł na dział, DevTools.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie. Wyjątek, który warto zrozumieć: plik
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} z lekcji Boot02 działa na
 * CAŁĄ ścieżkę klas (classpath) modułu, czyli na aplikacje wszystkich lekcji tego działu. Dlatego autokonfiguracja
 * z Boot02 jest domyślnie wyłączona i włącza ją tylko profil lekcji Boot02 ({@code application-boot02.properties}).
 * W module nie ma serwera WWW — aplikacje lekcji budują kontekst, robią swoje i kończą działanie.</p>
 */
package s03_boot_basics;
