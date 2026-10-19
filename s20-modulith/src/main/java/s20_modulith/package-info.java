/**
 * Dział s20_modulith — SPRING MODULITH (modulith = monolit modułowy): jak podzielić jedną aplikację na moduły
 * biznesowe z jawnymi granicami, sprawdzać te granice testem, łączyć moduły zdarzeniami zamiast wywołań i generować
 * z kodu dokumentację architektury. Na bazie H2 w pamięci, bez weba.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet z własną klasą startową):</p>
 * <ol>
 *   <li>{@code mod01_modules} — Mod01Modules: monolit modułowy, moduł = bezpośredni podpakiet aplikacji, API modułu
 *       a pakiet {@code internal}, test {@code ApplicationModules.verify()}, cykle między modułami,
 *       {@code @ApplicationModule(allowedDependencies = …)}.</li>
 *   <li>{@code mod02_module_events} — Mod02ModuleEvents: moduły rozmawiają zdarzeniami,
 *       {@code @ApplicationModuleListener} (asynchronicznie, po zatwierdzeniu transakcji, we własnej transakcji),
 *       rejestr publikacji zdarzeń w bazie (tabela EVENT_PUBLICATION), ponawianie nieukończonych publikacji, test
 *       modułu {@code @ApplicationModuleTest} i {@code Scenario}.</li>
 *   <li>{@code mod03_documentation} — Mod03Documentation: {@code Documenter} (diagramy PlantUML/C4 i kanwy modułów
 *       w {@code target/spring-modulith-docs}), nazwane interfejsy {@code @NamedInterface}, kiedy monolit modułowy,
 *       a kiedy mikroserwisy.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: Spring Modulith traktuje KAŻDY bezpośredni podpakiet pakietu klasy
 * {@code @SpringBootApplication} jako moduł. Dlatego w pakiecie działu NIE ma klasy {@code @SpringBootApplication}
 * (wszystkie lekcje stałyby się „modułami” jednej aplikacji). Każda lekcja ma własną klasę startową, a jej moduły
 * biznesowe są podpakietami lekcji (np. {@code mod01_modules.products}, {@code mod01_modules.orders}). Rozwiązania
 * ćwiczeń leżą w pakiecie {@code solutions} — poza pakietami lekcji, więc nie są modułami żadnej z nich. Celowo złe
 * aplikacje do testów weryfikacji leżą w {@code src/test/java} (pakiety {@code mod01_broken} i {@code mod03_broken});
 * Modulith domyślnie pomija klasy testowe, więc nie psują analizy prawdziwych lekcji. Lekcje z bazą mają własny
 * profil ({@code application-modNN.properties}) i własną bazę H2 ({@code jdbc:h2:mem:modNN}).</p>
 */
package s20_modulith;
