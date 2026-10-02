# SpringLearning — kurs Springa po polsku

Samodzielny kurs Spring Boot po polsku — kontynuacja kursu [JavaLearning](https://github.com/arturoller/JavaLearning). Ta sama metoda nauki: uruchamialne przykłady, szczegółowe komentarze po polsku (przy każdej angielskiej nazwie jest tłumaczenie), ćwiczenia sprawdzane automatycznie, pytania kontrolne, ściągi do powtórek (ŚCIĄGA) oraz powtórki rozłożone w czasie (spaced repetition — uczenie się przez przypominanie sobie materiału w rosnących odstępach czasu: po 1, 3, 7, 14 i 30 dniach).

> **Najpierw Java:** [https://github.com/arturoller/JavaLearning](https://github.com/arturoller/JavaLearning). Ten kurs zakłada znajomość Javy z tamtego repozytorium — zobacz sekcję „Wymagania” niżej.

Kurs jest w budowie (status ⏳ przy każdej lekcji). Działy będą pojawiać się kolejno, zaczynając od `s00-start`.

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

- **Java 21** jako minimum, zalecana **Java 25** (obie to wersje LTS — Long-Term Support, czyli z długim wsparciem).
- **Spring Boot 4.1** (najnowsza stabilna 4.x w chwili planowania kursu; w środku: Spring Framework 7, Spring Security 7,
  Hibernate 7, Jackson 3, JUnit 6).
- **Maven** w wersji wielomodułowej (multi-module — jeden projekt złożony z wielu podprojektów): jeden moduł = jeden dział.
  W repozytorium jest **Maven Wrapper** (`mvnw`/`mvnw.cmd` — skrypt, który sam pobiera właściwą wersję Mavena), więc nie
  trzeba niczego instalować.
- **H2** — baza danych w pamięci (in-memory database), nie wymaga instalacji; dane znikają po zamknięciu aplikacji.
- **Flyway** — wersjonowane migracje bazy danych (pliki SQL `V1__…`, `V2__…`, które zmieniają strukturę bazy krok po kroku).
- **Docker** (opcjonalnie) — kontener to lekki, odizolowany „mini-komputer” z zainstalowanym programem, np. bazą PostgreSQL.
  Używamy go przez **Testcontainers** (prawdziwa baza na czas testów) i **Docker Compose** (baza startuje razem z aplikacją).
  Bez Dockera kurs też działa: testy wymagające Dockera są wtedy automatycznie pomijane.
- **IntelliJ IDEA** — wystarczy Community; Ultimate ma dodatkowe wsparcie dla Springa (podświetlanie beanów, nawigacja po endpointach).
- **Spring Initializr** ([start.spring.io](https://start.spring.io)) — generator szkieletu projektu Spring Boot.

## Czym różnią się lekcje od JavaLearning

- Lekcja to nie jeden plik z `main`, tylko mała aplikacja albo konfiguracja (kilka klas: kontroler, serwis, repozytorium, testy).
- Każdy dział to **osobny moduł Mavena** z własnymi zależnościami. Dlaczego? Startery Springa (np. Security, JPA, Actuator)
  same zmieniają konfigurację całej aplikacji — gdyby wszystkie lekcje były w jednym projekcie, dodanie Spring Security
  w dziale 12 zablokowałoby (zabezpieczyło hasłem) wszystkie wcześniejsze lekcje webowe. Osobne moduły tego unikają.
- Większość lekcji sprawdzana jest **testami**, nie metodą `main` + komentarzem `// WYNIK:`:
  - `@SpringBootTest` — uruchamia cały kontekst aplikacji,
  - `@WebMvcTest` + MockMvc (narzędzie do testowania kontrolerów bez uruchamiania prawdziwego serwera HTTP),
  - `@DataJpaTest` — testuje samą warstwę bazy danych.

  Jak czytać takie testy, wyjaśnia już pierwszy dział (`s00-start`), więc nie musisz czekać do działu o testowaniu.
- Niektóre lekcje uruchamiają prawdziwą aplikację na porcie 8080 i dają gotowe zapytania HTTP do wypróbowania: pliki `.http`
  w katalogu `http/` modułu (IntelliJ HTTP Client — wbudowane narzędzie do wysyłania zapytań HTTP bez przeglądarki) albo
  polecenia `curl`.
- Tagi zostają te same co w JavaLearning (patrz tabela niżej) — to ten sam system wyszukiwania i powtórek.
- Nazwy działów mają ten sam format co w JavaLearning (numer + temat), tylko z literą `s` zamiast `t` (s jak Spring).
  Moduł nazywa się z myślnikami (`s04-web-rest`), a pakiet Javy w nim — z podkreślnikami (`s04_web_rest`), bo Java nie
  pozwala na myślniki w nazwach pakietów.

## Szybki start

Tak będzie to wyglądać, gdy pojawią się pierwsze lekcje:

1. Zainstaluj **JDK 25** (albo co najmniej 21). Najprościej z IntelliJ: File → Project Structure → Project → SDK →
   Download JDK → wersja 25 (dowolny dostawca, np. Eclipse Temurin). Wybierz ją potem jako SDK projektu.
2. Sklonuj repozytorium i otwórz w IntelliJ IDEA **główny** `pom.xml` (File → Open → wskaż `pom.xml` w korzeniu → Open as
   Project). IntelliJ sam rozpozna wszystkie moduły (działy).
3. Poczekaj, aż Maven pobierze zależności (przy pierwszym otwarciu potrzebne jest połączenie z internetem).
4. Uruchom testy:
   - cały kurs: `./mvnw test` (Windows: `mvnw.cmd test`),
   - jeden dział: `./mvnw -pl s04-web-rest test`,
   - albo zielony trójkąt ▶ obok klasy/metody testowej w IntelliJ.
5. Dla lekcji uruchamiających aplikację: kliknij ▶ obok `main` klasy z `@SpringBootApplication` w pakiecie lekcji, poczekaj
   na start na porcie 8080, a potem wykonaj gotowe zapytania z pliku `.http` w katalogu `http/` modułu.

## Struktura

```
pom.xml                         główny (parent — rodzic) pom: wersje, lista modułów, wspólne ustawienia
mvnw, mvnw.cmd, .mvn/           Maven Wrapper — budowanie bez instalowania Mavena
s00-start/                      moduł = dział
├── pom.xml                     zależności tylko tego działu
├── http/                       gotowe zapytania HTTP (.http) do lekcji-aplikacji
└── src/
    ├── main/java/s00_start/    lekcje — każda lekcja w swoim podpakiecie
    ├── main/resources/         application.properties, migracje Flyway
    └── test/java/s00_start/    testy sprawdzające lekcje i ćwiczenia (te same pakiety co w main)
s01-core-ioc/
├── …
└── src/main/java/s01_core_ioc/
    ├── ioc01_first_bean/               lekcja 1 (plik lekcji + klasy pomocnicze + ewentualnie własna klasa startowa)
    ├── ioc02_constructor_injection/    lekcja 2
    └── solutions/                      rozwiązania wzorcowe ćwiczeń
…
s22-extras/                     ostatni dział — dodatki
```
To standardowy układ Mavena — taki sam, jaki wygeneruje Spring Initializr i jaki zobaczysz w dokumentacji i tutorialach.
Numer `sNN` ustawia kolejność nauki. Lekcja, która potrzebuje działającego Springa, ma **własną** klasę startową
`@SpringBootApplication` w swoim podpakiecie — dzięki temu lekcje w jednym dziale nie widzą nawzajem swoich beanów.

## Jak uczyć się z jednej lekcji

1. Przeczytaj nagłówek i ŚCIĄGĘ na końcu pliku.
2. Przed uruchomieniem testu **przewidź wynik** (co powinna sprawdzić asercja). Uruchom testy (▶ albo `./mvnw -pl <moduł> test`) i porównaj z przewidywaniem.
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

| Dział (moduł) | Temat | Stan |
|---|---|---|
| `s00-start` | Jak korzystać z kursu, Spring Initializr (generator szkieletu projektu Spring Boot), budowa projektu wielomodułowego, uruchamianie aplikacji i testów, **wprowadzenie do testów** (jak czytać checkery: `@SpringBootTest`, MockMvc, asercje AssertJ), pliki `.http` | ⏳ |
| `s01-core-ioc` | Kontener IoC (odwrócenie sterowania — Inversion of Control: to Spring, nie Ty, tworzy obiekty i zarządza zależnościami), beany (bean — ziarno, czyli obiekt zarządzany przez Springa), `@Component`/`@Service`/`@Repository`, `@Bean`, wstrzykiwanie przez konstruktor, zasięgi (scope: singleton — jeden obiekt na całą aplikację, prototype — nowy obiekt przy każdym pobraniu), cykl życia beana | ⏳ |
| `s02-configuration` | `@Configuration`, `@Value`, `@ConfigurationProperties`, profile (profile — zestawy konfiguracji na różne środowiska, np. dev/test/prod), `application.properties`/`.yml` | ⏳ |
| `s03-boot-basics` | Autokonfiguracja (auto-configuration — Spring Boot sam dobiera konfigurację na podstawie zależności w projekcie), startery (starter — gotowy zestaw zależności Maven pod jedną nazwą; w Boot 4 podzielone na mniejsze moduły), `SpringApplication`, DevTools (automatyczne przeładowanie aplikacji po zmianie kodu), jak Boot „zgaduje” konfigurację | ⏳ |
| `s04-web-rest` | `@RestController`, mapowania (mapping — przypisanie adresu URL i metody HTTP do metody Javy), `@PathVariable`/`@RequestParam`/`@RequestBody`, `ResponseEntity` (typ opakowujący odpowiedź HTTP razem z kodem statusu i nagłówkami), kody HTTP, JSON (format wymiany danych, konwertowany automatycznie biblioteką Jackson), **CORS** (Cross-Origin Resource Sharing — zasady, które pozwalają stronie z innej domeny, np. frontendowi, wołać Twoje API), **wersjonowanie API** (wbudowane w Spring Framework 7 — np. `/api/v1` kontra `/api/v2` albo nagłówek z wersją) | ⏳ |
| `s05-validation` | Bean Validation (walidacja danych przez adnotacje: `@NotBlank`, `@Size`, `@Valid`), własne walidatory (validator — klasa sprawdzająca poprawność danych) | ⏳ |
| `s06-error-handling` | `@RestControllerAdvice` (globalna obsługa wyjątków dla wszystkich kontrolerów), `ProblemDetail` (standardowy format błędu HTTP, RFC 9457), własne wyjątki | ⏳ |
| `s07-data-jdbc` | `JdbcClient`/`JdbcTemplate` (klasy Springa upraszczające pracę z JDBC), `schema.sql`, `data.sql` (skrypty tworzące strukturę bazy i wypełniające ją danymi startowymi), **Flyway** — migracje bazy (migration — wersjonowana zmiana struktury bazy: `V1__create_products.sql`, `V2__add_price.sql`…), dlaczego w prawdziwych projektach zastępuje `schema.sql`; od tej lekcji wszystkie działy z bazą korzystają z Flyway | ⏳ |
| `s08-data-jpa` | Encje (entity — klasa reprezentująca wiersz w tabeli bazy danych), Spring Data JPA (warstwa automatyzująca dostęp do bazy przez JPA — Java Persistence API), zapytania z nazw metod (query methods), `@Query`, paginacja (podział wyników na strony) i sortowanie, Flyway + JPA (`ddl-auto=validate` — Hibernate tylko sprawdza schemat, zamiast go tworzyć) | ⏳ |
| `s09-jpa-relations` | Relacje między encjami, LAZY/EAGER (leniwe kontra łapczywe ładowanie powiązanych danych), problem N+1 (nadmiarowa liczba zapytań SQL przy ładowaniu relacji), **mapowanie encja ↔ DTO** (ręcznie, przez rekordy; dlaczego nie zwracamy encji z kontrolera; Lombok — tylko odesłanie do [JavaLearning `t20_lombok`](https://github.com/arturoller/JavaLearning)) | ⏳ |
| `s10-transactions` | `@Transactional`, propagacja (propagation — jak zagnieżdżone transakcje wpływają na siebie), rollback (wycofanie zmian po błędzie), pułapki (np. wywołanie metody transakcyjnej z tej samej klasy — wtedy proxy Springa nie działa) | ⏳ |
| `s11-testing` | `@SpringBootTest`, `@WebMvcTest`, MockMvc, `@DataJpaTest`, `@MockitoBean` (zastępowanie prawdziwego beana atrapą w teście), **Testcontainers** (prawdziwa baza PostgreSQL w kontenerze Docker na czas testów) i **`@ServiceConnection`** (Boot sam podpina aplikację do kontenera), **Docker Compose** (baza startuje razem z aplikacją przy uruchamianiu lokalnym); testy z Dockerem są pomijane, gdy Docker nie działa | ⏳ |
| `s12-security` | Spring Security: logowanie, role, BCrypt (algorytm hashowania haseł), podstawy JWT (JSON Web Token — token przenoszący informacje o zalogowanym użytkowniku), CORS a Spring Security | ⏳ |
| `s13-rest-client` | `RestClient` (klasa Springa do wywoływania zewnętrznych API przez HTTP), **HTTP Interfaces** (`@HttpExchange` — klient HTTP opisany samym interfejsem Javy, bez pisania implementacji), obsługa błędów i timeouty (limit czasu oczekiwania na odpowiedź), **odporność** (resilience) wbudowana w Spring Framework 7: `@Retryable` (ponawianie nieudanych wywołań) i `@ConcurrencyLimit` (limit jednoczesnych wywołań) | ⏳ |
| `s14-openapi` | springdoc-openapi, Swagger UI (interaktywna dokumentacja API w przeglądarce), dokumentowanie API (także wersji API) | ⏳ |
| `s15-actuator-logging` | Actuator (moduł do monitorowania aplikacji: health — stan aplikacji, metrics — liczniki i statystyki), logowanie (SLF4J/Logback — standardowe biblioteki do zapisywania logów), obserwowalność (observability — możliwość sprawdzenia, co dzieje się w działającej aplikacji) | ⏳ |
| `s16-cache-scheduling-async` | `@Cacheable` (pamięć podręczna — zapamiętywanie wyników, żeby nie liczyć/pobierać ich ponownie), `@Scheduled` (zadania cykliczne uruchamiane automatycznie), `@Async` (metody wykonywane w tle, bez blokowania wątku wywołującego), **wątki wirtualne** (virtual threads z Javy 21 — bardzo lekkie wątki; w Springu włączane jedną właściwością `spring.threads.virtual.enabled=true`: co zmieniają, kiedy pomagają, a kiedy nie) | ⏳ |
| `s17-events` | Zdarzenia aplikacji (`ApplicationEvent`, `@EventListener` — powiadamianie innych części aplikacji o tym, co się stało), `@TransactionalEventListener`, zapowiedź kolejek (queue — kolejka wiadomości; Kafka/RabbitMQ w dodatkach) | ⏳ |
| `s18-aop` | Aspekty (`@Aspect` — kod, który dokleja się do wielu metod naraz, np. logowanie czasu wykonania), jak działają proxy Springa (proxy — obiekt pośredniczący, przez który przechodzi wywołanie metody) | ⏳ |
| `s19-deploy` | Budowanie JAR (spakowana, gotowa do uruchomienia aplikacja), Docker (obraz aplikacji), profile produkcyjne, zmienne środowiskowe (environment variables — konfiguracja przekazywana z zewnątrz, poza kodem) | ⏳ |
| `s20-modulith` | **Spring Modulith**: podział jednej aplikacji na moduły biznesowe (np. produkty, zamówienia, klienci) z jasnymi granicami, test, który pilnuje, żeby moduły nie sięgały sobie „do środka”, komunikacja modułów przez zdarzenia, dokumentacja architektury generowana z kodu — przygotowanie do projektów końcowych | ⏳ |
| `s21-capstone` | Projekty: REST API sklepu (produkty, klienci, zamówienia — ten sam „świat” danych co w JavaLearning), system rezerwacji, biblioteka z uwierzytelnianiem (authentication — sprawdzanie tożsamości użytkownika) | ⏳ |
| `s22-extras` | **Dodatki (opcjonalne)**: Spring AI (wywoływanie modelu językowego — LLM — z aplikacji Springa), Kafka/RabbitMQ (kolejki wiadomości naprawdę, w kontenerze), OAuth2 i Keycloak (logowanie przez zewnętrzny serwer tożsamości, Resource Server), tracing (Micrometer Tracing — śledzenie jednego zapytania przez wiele serwisów), przegląd WebFlux (programowanie reaktywne — czym jest i kiedy go NIE potrzebujesz, zwłaszcza przy wątkach wirtualnych), GraalVM native image (kompilacja aplikacji do natywnego pliku wykonywalnego — szybki start, mniej pamięci) | ⏳ |

## Uruchamianie i ustawienia IntelliJ

- **Otwieranie projektu:** otwórz główny `pom.xml` (w korzeniu repozytorium). Każdy dział pojawi się w oknie Maven jako
  osobny moduł. Po dodaniu nowego działu kliknij „Reload All Maven Projects” (ikona odświeżania w oknie Maven).
- **Uruchomienie lekcji:** dla lekcji-testów kliknij zielony trójkąt ▶ obok metody lub klasy testowej. Dla lekcji-aplikacji
  kliknij ▶ obok metody `main` klasy z `@SpringBootApplication` w pakiecie lekcji i poczekaj na komunikat w stylu
  „Tomcat started on port 8080”. Z linii poleceń: `./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=<pełna nazwa klasy>`.
- **Ćwiczenia:** testy ćwiczeń (z tagiem `cwiczenie`) są wyłączone z `./mvnw test`, żeby nierozwiązane zadania nie
  psuły budowania. Uruchamiasz je sam: ▶ w IntelliJ albo `./mvnw -pl <moduł> test -Dgroups=cwiczenie`.
- **Docker:** testy z Testcontainers (dział `s11` i dalej) uruchomią się tylko wtedy, gdy działa Docker (np. Docker Desktop).
  Bez niego są pomijane (żółty, a nie czerwony wynik) — reszta kursu działa normalnie.
- **Błąd kompilacji w innym pliku:** tak jak w JavaLearning, IntelliJ przed uruchomieniem kompiluje moduł. Błąd w jednej
  lekcji może zablokować uruchamianie pozostałych lekcji tego działu — najlepiej go poprawić. Inne działy (moduły) nie są dotknięte.
  - Awaryjnie: Run → Edit Configurations → Modify options → Before launch → zamień „Build” na „Build, no error check”.
- **Wsparcie dla Springa w IntelliJ:** Community wystarczy na cały kurs. Ultimate dodaje podświetlanie nazw beanów, nawigację po endpointach i generator zapytań HTTP — wygodne, ale nieobowiązkowe.
- **Kodowanie polskich znaków:** pliki kursu są w UTF-8, tak jak w JavaLearning. File → Settings → Editor → File Encodings → Project Encoding.
- **Wymagania:** JDK 21 albo nowszy (zalecany 25), Docker opcjonalnie, połączenie z internetem przy pierwszym budowaniu
  (pobranie Mavena przez wrapper i zależności Spring Boot). Mavena nie trzeba instalować — wystarczy `mvnw`.

## Powiązane repozytoria

- [JavaLearning](https://github.com/arturoller/JavaLearning) — repozytorium kursu Javy — zacznij od niego.

---

Ten plik będzie rósł razem z kursem: wraz z każdym nowym działem (modułem `sNN-…`) w spisie treści zmieni się stan z ⏳ na 🔶, a potem na ✅.
