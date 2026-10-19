/**
 * Dział s19_deploy — WDROŻENIE (deployment = wdrożenie): jak z aplikacji w IntelliJ zrobić coś, co działa na serwerze.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code dep01_executable_jar} — Dep01ExecutableJar: wykonywalny JAR, {@code java -jar}, warstwy i tryb
 *       {@code -Djarmode=tools}.</li>
 *   <li>{@code dep02_docker} — Dep02Docker: Dockerfile wielostopniowy, użytkownik nie-root, pamięć JVM w kontenerze,
 *       Buildpacks ({@code spring-boot:build-image}).</li>
 *   <li>{@code dep03_production_config} — Dep03ProductionConfig: konfiguracja z zewnątrz, zmienne środowiska, profil
 *       {@code prod}, sekrety.</li>
 *   <li>{@code dep04_startup_and_shutdown} — Dep04StartupAndShutdown: łagodne zamykanie, sondy liveness/readiness,
 *       leniwa inicjalizacja, pamięć podręczna AOT.</li>
 * </ol>
 *
 * <p>Pliki poza kodem: {@code s19-deploy/Dockerfile} i {@code s19-deploy/.dockerignore} (lekcja Dep02Docker),
 * {@code s19-deploy/http/dep01_executable_jar.http}. Moduł ma w {@code pom.xml} sekcję {@code build} z
 * {@code spring-boot-maven-plugin} (jedyny moduł kursu z tym pluginem) — klasą główną JAR-a jest aplikacja lekcji
 * Dep01Application.</p>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową, a ustawienia lekcji mają prefiks (np. {@code shop.payment.*} w lekcji Dep03). Rozwiązania ćwiczeń leżą
 * w pakiecie {@code solutions} obok lekcji, nie w środku.</p>
 */
package s19_deploy;
