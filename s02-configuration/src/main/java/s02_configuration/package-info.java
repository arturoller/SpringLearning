/**
 * Dział s02_configuration — KONFIGURACJA APLIKACJI (configuration = konfiguracja): jak podać aplikacji ustawienia
 * (adresy, limity, nazwy, przełączniki funkcji) bez zmiany kodu i bez ponownej kompilacji.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code cfg01_properties} — Cfg01Properties: application.properties, {@code @Value}, wartości domyślne,
 *       konwersja typów i PRIORYTET źródeł ustawień (argumenty, właściwości JVM, zmienne środowiska, plik).</li>
 *   <li>{@code cfg02_configuration_properties} — Cfg02ConfigurationProperties: {@code @ConfigurationProperties}
 *       i rekord Javy zamiast wielu {@code @Value} — typy, zagnieżdżenia, listy, mapy, Duration, DataSize.</li>
 *   <li>{@code cfg03_profiles} — Cfg03Profiles: {@code @Profile}, pliki {@code application-{profil}.properties},
 *       {@code spring.profiles.active}, {@code @ActiveProfiles} w testach, grupy profili.</li>
 *   <li>{@code cfg04_yaml} — Cfg04Yaml: ten sam plik konfiguracji w formacie YAML, listy, wiele dokumentów
 *       w jednym pliku i pułapki wcięć.</li>
 *   <li>{@code cfg05_environment} — Cfg05Environment: interfejs Environment, {@code @ConditionalOnProperty}
 *       (przełączniki funkcji) i nazwy zmiennych środowiskowych (relaxed binding).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, a klucze ustawień mają prefiks lekcji ({@code cfg01.…}, {@code cfg02.…}) —
 * dzięki temu lekcje, które dzielą jedną ścieżkę klas (classpath), nie nadpisują sobie nawzajem ustawień.
 * Wspólny {@code application.properties} modułu jest prawie pusty.</p>
 */
package s02_configuration;
