# SpringLearning — kurs Springa po polsku

Samodzielny kurs Spring Boot po polsku — kontynuacja kursu [JavaLearning](https://github.com/arturoller/JavaLearning). Ta sama metoda nauki: uruchamialne przykłady, szczegółowe komentarze po polsku (przy każdej angielskiej nazwie jest tłumaczenie), ćwiczenia sprawdzane automatycznie, pytania kontrolne, ściągi do powtórek (ŚCIĄGA) oraz powtórki rozłożone w czasie (spaced repetition — uczenie się przez przypominanie sobie materiału w rosnących odstępach czasu: po 1, 3, 7, 14 i 30 dniach).

> **Najpierw Java:** [https://github.com/arturoller/JavaLearning](https://github.com/arturoller/JavaLearning). Ten kurs zakłada znajomość Javy z tamtego repozytorium — zobacz sekcję „Wymagania” niżej.

Kurs jest w budowie (status ⏳ przy każdej lekcji). Działy będą pojawiać się kolejno, zaczynając od `s00_start`.

## Dla kogo jest ten kurs

Dla każdego, kto skończył (albo prawie skończył) JavaLearning i chce zobaczyć, jak te same pojęcia — obiekty, interfejsy, wyjątki, streamy, JDBC, testy — wyglądają w prawdziwej aplikacji webowej zbudowanej na Springu. Nie zakładamy wcześniejszej znajomości Springa. Zakładamy solidną Javę.

## Wymagania — co musisz umieć z JavaLearning

Zanim zaczniesz, przerób (albo przynajmniej dobrze znaj) te działy z [JavaLearning](https://github.com/arturoller/JavaLearning):

| Dział | Dlaczego jest potrzebny w Springu |
|---|---|
| `t06_oop_basics` | Spring zarządza obiektami (beanami) — musisz rozumieć klasy, konstruktory i hermetyzację, żeby wiedzieć, czym właściwie zarządza kontener. |
| `t07_inheritance_polymorphism` | Spring wstrzykuje zależności przez interfejsy, a kontrolery i serwisy korzystają z polimorfizmu (jeden interfejs, wiele implementacji). |
| `t09_records` | DTO (Data Transfer Object — obiekt do przenoszenia danych między warstwami) w Springu najczęściej piszemy jako rekordy. |
| `t10_exceptions` | Obsługa błędów HTTP w Springu opiera się na własnych wyjątkach i ich przechwytywaniu w jednym miejscu. |
| `t11_generics` | Repozytoria Spring Data JPA i typy takie jak `ResponseEntity<T>` są generyczne. |
| `t12_collections` | Listy i mapy pojawiają się w niemal każdym kontrolerze, serwisie i repozytorium. |
| `t13_lambdas` | Konfiguracja Springa (`@Bean`, filtry, callbacki) często korzysta z lambd i interfejsów funkcyjnych. |
| `t14_optional` | Repozytoria Spring Data JPA zwracają `Optional<T>` zamiast `null`. |
| `t16_streams` | Przetwarzanie danych z bazy (mapowanie encji na DTO, filtrowanie, grupowanie) korzysta ze streamów. |
| `t19_annotations_reflection` | Spring jest zbudowany na adnotacjach, refleksji i dynamicznych proxy — to fundament działania całego kontenera. |
| `t22_design_patterns` | Wstrzykiwanie zależności (dependency injection), strategia i dekorator to podstawowe wzorce architektury Springa. |
| `t28_networking_http` | Spring Web jest zbudowany na protokole HTTP — musisz znać metody, kody odpowiedzi i nagłówki. |
| `t29_jdbc_databases` | Spring Data JPA i `JdbcClient` to nakładki na JDBC — warto rozumieć, co jest pod spodem. |
| `t32_junit_mockito` | Większość lekcji Springa sprawdzana jest testami (JUnit, Mockito), a nie metodą `main`. |
| `t34_toward_spring` | To bezpośredni most: własny kontener IoC/DI, warstwy controller–service–repository, REST i HTTP — to samo, co teraz robi za Ciebie Spring. |

## Stack technologiczny

- **Java 17+** (zalecana Java 21).
- **Spring Boot 3.x**.
- **Maven** — zarządzanie zależnościami i budowanie projektu.
- **H2** — baza danych w pamięci (in-memory database), nie wymaga instalacji; dane znikają po zamknięciu aplikacji.
- opcjonalnie **PostgreSQL w Dockerze** (kontener — lekki, odizolowany „mini-komputer” z zainstalowaną bazą) — później, gdy H2 przestanie wystarczać.
- **IntelliJ IDEA** — wystarczy Community; Ultimate ma dodatkowe wsparcie dla Springa (podświetlanie beanów, nawigacja po endpointach).
- **Spring Initializr** ([start.spring.io](https://start.spring.io)) — generator szkieletu projektu Spring Boot.

## Czym różnią się lekcje od JavaLearning

- Lekcja to nie jeden plik z `main`, tylko mała aplikacja albo konfiguracja (kilka klas: kontroler, serwis, repozytorium, testy).
- Większość lekcji sprawdzana jest **testami**, nie metodą `main` + komentarzem `// WYNIK:`:
  - `@SpringBootTest` — uruchamia cały kontekst aplikacji,
  - `@WebMvcTest` + MockMvc (narzędzie do testowania kontrolerów bez uruchamiania prawdziwego serwera HTTP),
  - `@DataJpaTest` — testuje samą warstwę bazy danych.
- Niektóre lekcje uruchamiają prawdziwą aplikację na porcie 8080 i dają gotowe zapytania HTTP do wypróbowania: pliki `.http` (IntelliJ HTTP Client — wbudowane narzędzie do wysyłania zapytań HTTP bez przeglądarki) albo polecenia `curl`.
- Tagi zostają te same co w JavaLearning (patrz tabela niżej) — to ten sam system wyszukiwania i powtórek.
- Nazwy pakietów mają ten sam format co w JavaLearning (numer + temat), tylko z literą `s` zamiast `t` (s jak Spring): `s04_web_rest` zamiast `t16_streams` — dzięki temu oba kursy przegląda się tą samą metodą.

## Szybki start

Tak będzie to wyglądać, gdy pojawią się pierwsze lekcje:

1. Zainstaluj **JDK 17** (albo nowszy; zalecana 21).
2. Sklonuj repozytorium i otwórz folder projektu w IntelliJ IDEA (File → Open → wskaż `pom.xml` → Open as Project).
3. Poczekaj, aż Maven pobierze zależności (przy pierwszym otwarciu potrzebne jest połączenie z internetem).
4. Uruchom testy lekcji: `mvn test` z linii poleceń albo zielony trójkąt ▶ obok klasy/metody testowej w IntelliJ.
5. Dla lekcji uruchamiających aplikację: kliknij ▶ obok `main` klasy z `@SpringBootApplication`, poczekaj na start na porcie 8080, a potem wykonaj gotowe zapytania z dołączonego pliku `.http`.

## Struktura

```
src/                        lekcje — każdy dział to osobny pakiet
├── s00_start/              jak korzystać z kursu, Spring Initializr, uruchamianie aplikacji i testów
├── s01_core_ioc/           kontener IoC, beany, @Component/@Service/@Repository, @Bean, wstrzykiwanie przez konstruktor
├── s02_configuration/      @Configuration, @Value, @ConfigurationProperties, profile, application.properties/yml
├── ...
└── s20_capstone/           projekty: REST API sklepu, system rezerwacji, biblioteka z uwierzytelnianiem
test/                       testy sprawdzające lekcje i ćwiczenia (ten sam układ pakietów co w src/)
resources/                  application.properties/yml, schema.sql, data.sql, pliki .http z gotowymi zapytaniami
```
Każdy dział to osobny folder (= pakiet) bezpośrednio w `src/`, tak jak w JavaLearning. Testy leżą w `test/`, a konfiguracja i dane startowe w `resources/` (Maven dostanie te trzy katalogi w `pom.xml`). Numer `sNN_` (s jak Spring) ustawia kolejność nauki w drzewie projektu. Każda lekcja Springa to zwykle osobna, mała aplikacja Spring Boot albo konfiguracja, uruchamiana niezależnie od pozostałych.

## Jak uczyć się z jednej lekcji

1. Przeczytaj nagłówek i ŚCIĄGĘ na końcu pliku.
2. Przed uruchomieniem testu **przewidź wynik** (co powinna sprawdzić asercja). Uruchom testy (▶ albo `mvn test`) i porównaj z przewidywaniem.
3. Jeśli lekcja uruchamia aplikację na porcie 8080, wykonaj gotowe zapytania z pliku `.http` i porównaj odpowiedź z opisem w komentarzu.
4. Zmień coś w kodzie i sprawdź, czy rozumiesz, dlaczego test się psuje albo dalej przechodzi.
5. Odpowiedz na PYTANIA KONTROLNE bez patrzenia w kod. Odpowiedzi są w zwiniętym bloku na końcu pliku.
6. Rozwiąż ćwiczenia (`exerciseN`). Checker (test) pokazuje czerwony pasek, dopóki rozwiązanie nie jest poprawne.
7. Rób powtórki po 1, 3, 7, 14 i 30 dniach: najpierw pytania z pamięci, dopiero potem ściąga — tak jak w JavaLearning.

## Tagi do wyszukiwania (Ctrl+Shift+F)

Tagi zawsze mają dokładnie tę postać (wielkie litery + dwukropek), więc wyszukiwanie znajduje wszystkie wystąpienia. To ten sam zestaw tagów co w JavaLearning.

| Tag | Co znajdziesz |
|---|---|
| `TEMAT:` | nagłówki wszystkich lekcji |
| `W SKRÓCIE:` | streszczenia lekcji |
| `ANALOGIA:` | porównania z życia |
| `JAK TO DZIAŁA:` | mechanizm krok po kroku |
| `SŁÓWKA:` | angielskie słówka z tłumaczeniem |
| `ZOBACZ TEŻ:` | powiązania między lekcjami |
| `PUŁAPKA:` | typowe błędy |
| `DOBRA PRAKTYKA:` | zasady „jak robić dobrze” i dlaczego |
| `WYNIK:` | co wypisze/zwróci dany fragment kodu |
| `ŚCIĄGA:` | podsumowania do powtórek |
| `PYTANIA KONTROLNE:` | pytania do samosprawdzenia (także „co się stanie?” i „znajdź błąd”) |
| `ĆWICZENIE` | zadania; bez dwukropka, bo mają numery: `ĆWICZENIE 1:` |

## Spis treści

Legenda: ✅ gotowe · 🔶 w trakcie · ⏳ zaplanowane — tutaj wszystko jest jeszcze ⏳.

| Dział | Temat | Stan |
|---|---|---|
| `s00_start` | Jak korzystać z kursu, Spring Initializr (generator szkieletu projektu Spring Boot), uruchamianie aplikacji i testów | ⏳ |
| `s01_core_ioc` | Kontener IoC (odwrócenie sterowania — Inversion of Control: to Spring, nie Ty, tworzy obiekty i zarządza zależnościami), beany (bean — ziarno, czyli obiekt zarządzany przez Springa), `@Component`/`@Service`/`@Repository`, `@Bean`, wstrzykiwanie przez konstruktor, zasięgi (scope: singleton — jeden obiekt na całą aplikację, prototype — nowy obiekt przy każdym pobraniu), cykl życia beana | ⏳ |
| `s02_configuration` | `@Configuration`, `@Value`, `@ConfigurationProperties`, profile (profile — zestawy konfiguracji na różne środowiska, np. dev/test/prod), `application.properties`/`.yml` | ⏳ |
| `s03_boot_basics` | Autokonfiguracja (auto-configuration — Spring Boot sam dobiera konfigurację na podstawie zależności w projekcie), startery (starter — gotowy zestaw zależności Maven pod jedną nazwą), `SpringApplication`, DevTools (automatyczne przeładowanie aplikacji po zmianie kodu), jak Boot „zgaduje” konfigurację | ⏳ |
| `s04_web_rest` | `@RestController`, mapowania (mapping — przypisanie adresu URL i metody HTTP do metody Javy), `@PathVariable`/`@RequestParam`/`@RequestBody`, `ResponseEntity` (typ opakowujący odpowiedź HTTP razem z kodem statusu i nagłówkami), kody HTTP, JSON (format wymiany danych, konwertowany automatycznie biblioteką Jackson) | ⏳ |
| `s05_validation` | Bean Validation (walidacja danych przez adnotacje: `@NotBlank`, `@Size`, `@Valid`), własne walidatory (validator — klasa sprawdzająca poprawność danych) | ⏳ |
| `s06_error_handling` | `@RestControllerAdvice` (globalna obsługa wyjątków dla wszystkich kontrolerów), `ProblemDetail` (standardowy format błędu HTTP, RFC 7807), własne wyjątki | ⏳ |
| `s07_data_jdbc` | `JdbcClient`/`JdbcTemplate` (klasy Springa upraszczające pracę z JDBC), `schema.sql`, `data.sql` (skrypty tworzące strukturę bazy i wypełniające ją danymi startowymi) | ⏳ |
| `s08_data_jpa` | Encje (entity — klasa reprezentująca wiersz w tabeli bazy danych), Spring Data JPA (warstwa automatyzująca dostęp do bazy przez JPA — Java Persistence API), zapytania z nazw metod (query methods), `@Query`, paginacja (podział wyników na strony) i sortowanie | ⏳ |
| `s09_jpa_relations` | Relacje między encjami, LAZY/EAGER (leniwe kontra łapczywe ładowanie powiązanych danych), problem N+1 (nadmiarowa liczba zapytań SQL przy ładowaniu relacji), DTO kontra encja | ⏳ |
| `s10_transactions` | `@Transactional`, propagacja (propagation — jak zagnieżdżone transakcje wpływają na siebie), rollback (wycofanie zmian po błędzie), pułapki (np. wywołanie metody transakcyjnej z tej samej klasy — wtedy proxy Springa nie działa) | ⏳ |
| `s11_testing` | `@SpringBootTest`, `@WebMvcTest`, MockMvc, `@DataJpaTest`, `@MockBean`/`@MockitoBean` (zastępowanie prawdziwego beana atrapą w teście), Testcontainers (opcjonalnie — prawdziwa baza danych w kontenerze Docker na czas testów) | ⏳ |
| `s12_security` | Spring Security: logowanie, role, BCrypt (algorytm hashowania haseł), podstawy JWT (JSON Web Token — token przenoszący informacje o zalogowanym użytkowniku) | ⏳ |
| `s13_rest_client` | `RestClient` (klasa Springa do wywoływania zewnętrznych API przez HTTP), wywoływanie zewnętrznych API, obsługa błędów i timeouty (limit czasu oczekiwania na odpowiedź) | ⏳ |
| `s14_openapi` | springdoc-openapi, Swagger UI (interaktywna dokumentacja API w przeglądarce), dokumentowanie API | ⏳ |
| `s15_actuator_logging` | Actuator (moduł do monitorowania aplikacji: health — stan aplikacji, metrics — liczniki i statystyki), logowanie (SLF4J/Logback — standardowe biblioteki do zapisywania logów), obserwowalność (observability — możliwość sprawdzenia, co dzieje się w działającej aplikacji) | ⏳ |
| `s16_cache_scheduling_async` | `@Cacheable` (pamięć podręczna — zapamiętywanie wyników, żeby nie liczyć/pobierać ich ponownie), `@Scheduled` (zadania cykliczne uruchamiane automatycznie), `@Async` (metody wykonywane w tle, bez blokowania wątku wywołującego) | ⏳ |
| `s17_events` | Zdarzenia aplikacji (`ApplicationEvent`, `@EventListener` — powiadamianie innych części aplikacji o tym, co się stało), zapowiedź kolejek (queue — kolejka wiadomości, np. Kafka/RabbitMQ) | ⏳ |
| `s18_aop` | Aspekty (`@Aspect` — kod, który dokleja się do wielu metod naraz, np. logowanie czasu wykonania), jak działają proxy Springa (proxy — obiekt pośredniczący, przez który przechodzi wywołanie metody) | ⏳ |
| `s19_deploy` | Budowanie JAR (spakowana, gotowa do uruchomienia aplikacja), Docker, profile produkcyjne, zmienne środowiskowe (environment variables — konfiguracja przekazywana z zewnątrz, poza kodem) | ⏳ |
| `s20_capstone` | Projekty: REST API sklepu (produkty, klienci, zamówienia — ten sam „świat” danych co w JavaLearning), system rezerwacji, biblioteka z uwierzytelnianiem (authentication — sprawdzanie tożsamości użytkownika) | ⏳ |

## Uruchamianie i ustawienia IntelliJ

- **Uruchomienie lekcji:** dla lekcji-testów kliknij zielony trójkąt ▶ obok metody lub klasy testowej. Dla lekcji-aplikacji kliknij ▶ obok metody `main` klasy z `@SpringBootApplication` i poczekaj na komunikat w stylu „Tomcat started on port 8080”.
- **Błąd kompilacji w innym pliku:** tak jak w JavaLearning, IntelliJ przed uruchomieniem kompiluje cały projekt. Błąd w jednej lekcji może zablokować uruchamianie pozostałych — najlepiej go poprawić.
  - Awaryjnie: Run → Edit Configurations → Modify options → Before launch → zamień „Build” na „Build, no error check”.
- **Wsparcie dla Springa w IntelliJ:** Community wystarczy na cały kurs. Ultimate dodaje podświetlanie nazw beanów, nawigację po endpointach i generator zapytań HTTP — wygodne, ale nieobowiązkowe.
- **Kodowanie polskich znaków:** pliki kursu są w UTF-8, tak jak w JavaLearning. File → Settings → Editor → File Encodings → Project Encoding.
- **Wymagania:** JDK 17 (zalecana 21), Maven (IntelliJ pobiera zależności sam), połączenie z internetem przy pierwszym budowaniu (pobranie zależności Spring Boot).

## Powiązane repozytoria

- [JavaLearning](https://github.com/arturoller/JavaLearning) — repozytorium kursu Javy — zacznij od niego.

---

Ten plik będzie rósł razem z kursem: wraz z każdym nowym działem `sNN_` w spisie treści zmieni się stan z ⏳ na 🔶, a potem na ✅.
