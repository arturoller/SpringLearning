# ASSIGNMENTS — konspekty działów dla agentów piszących lekcje

Każda sekcja = jedna paczka lekcji dla JEDNEGO agenta. Agent dostaje `tools/AGENT_KIT.md` + swoją sekcję. Nazwy lekcji są już
w `tools/lessons.txt`. Fakty oznaczone „SPRAWDŹ” agent potwierdza w jarze (`~/.m2/repository`, javap) albo uruchomieniem,
zanim je napisze. Gotowe sekcje zostają w pliku jako historia (stan na końcu nagłówka).

---

## A. s00_start — dodatki: słowniczek i debugowanie (2 lekcje) — stan: gotowe (2026-10-19)

Moduł `s00-start` już istnieje (startery: `spring-boot-starter-webmvc`, test: `spring-boot-starter-webmvc-test`). Dopisz obie
lekcje do `s00-start/src/main/java/s00_start/package-info.java` (kolejność czytania). Nie zmieniaj istniejących lekcji 01–06.

### Start07Glossary — pakiet `s00_start.start07_glossary`
Odpowiednik `t00_start/Start02Glossary` z JavaLearning, ale dla Springa: słowniczek pojęć całego kursu.
- Dane jako kod: rekord `Term(String english, String polish, String definition, String lesson)` i niezmienna lista haseł
  (`List.of(...)`) w klasie pomocniczej `SpringGlossary` z metodami wyszukiwania. `lesson` = odesłanie `sNN_dzial/Klasa`
  (tylko nazwy z `tools/lessons.txt`; dla działów bez nazw lekcji — sam dział `sNN_dzial`) albo `tNN_pakiet/Klasa`
  (JavaLearning, rejestr `tools/javalearning-lessons.txt`).
- 80–120 haseł z całego planu kursu (README, spis treści): kontener IoC, kontekst aplikacji (ApplicationContext), bean, definicja
  beana, zasięg (scope), cykl życia, wstrzykiwanie zależności, stereotyp, konfiguracja, właściwość (property), profil,
  Environment, autokonfiguracja, warunek (condition), starter, BOM, kontroler, punkt końcowy (endpoint), mapowanie, DTO,
  serializacja JSON, walidacja, ProblemDetail, encja, repozytorium, JPA, Hibernate, migracja (Flyway), transakcja, propagacja,
  proxy, aspekt (AOP), test wycinkowy (slice), mock/stub/spy (dubler testowy), MockMvc, Testcontainers, Actuator, pamięć
  podręczna (cache), zdarzenie (event), wątek wirtualny, JWT, CORS, OpenAPI, moduł (Modulith) itd. Definicje 1–2 zdania,
  prostym językiem; prawdziwe dla Boot 4.1 / Framework 7.
- Sekcje lekcji: 1) jak korzystać ze słowniczka (Ctrl+F, wyszukiwanie w teście), 2) TABELA TERMINOLOGII KURSU — jednolite
  polskie odpowiedniki, których używa cały kurs (bean = bean/ziarno, endpoint = punkt końcowy, dependency injection =
  wstrzykiwanie zależności, immutable = niezmienny, side effect = efekt uboczny… — spójnie z CLAUDE.md sekcja 1),
  3) pojęcia, które uczeń myli (bean a obiekt, kontener a kontekst, starter a autokonfiguracja, mock a stub, profil a
  środowisko) — jako PUŁAPKI z „dlaczego”.
- Test lekcji: brak duplikatów haseł (bez względu na wielkość liter), każde hasło ma niepuste pola, lista posortowana
  alfabetycznie po `english` (kolejność ustala `Comparator`, nie zakładaj nic o `HashMap`), wyszukiwanie działa, każde odesłanie
  `lesson` ma poprawny format.
- Ćwiczenia (3): wyszukiwanie po fragmencie polskiej nazwy (bez względu na wielkość liter i polskie znaki? — tylko wielkość
  liter, `Locale.ROOT`), grupowanie haseł po dziale (`sNN_…` → lista, `TreeMap`), „fiszka” — losowanie hasła z ziarnem
  (`new Random(seed)`, wynik deterministyczny).

### Start08Debugging — pakiet `s00_start.start08_debugging`
Odpowiednik `t00_start/Start05Debugging` z JavaLearning, ale dla aplikacji Springa.
- Sekcje:
  1. Czytanie błędu startu: blok „APPLICATION FAILED TO START” (Description / Action — to wynik FailureAnalyzer, analizatora
     błędu), łańcuch `Caused by:` — czytaj od dołu (pierwotna przyczyna). Typowe: brak beana (UnsatisfiedDependencyException
     → NoSuchBeanDefinitionException), dwa kandydaty (NoUniqueBeanDefinitionException), brak właściwości, zajęty port.
  2. Błędy kontekstu w TESTACH: „Failed to load ApplicationContext” i drugi komunikat „ApplicationContext failure threshold (1)
     exceeded: skipping repeated attempt…” — to NIE nowy błąd, tylko pominięcie kolejnych prób; szukaj PIERWSZEGO błędu w logu.
  3. Debugger IntelliJ: Debug zamiast Run (przy teście i przy `main`), breakpoint w serwisie/kontrolerze, Evaluate Expression,
     breakpoint warunkowy, breakpoint na wyjątku (Spring opakowuje wyjątki — ustaw na klasie przyczyny), ramki stosu pełne
     klas Springa/proxy (np. `$$SpringCGLIB$$`) — jak je czytać.
  4. Logi: `logging.level.<pakiet>=DEBUG`, `logging.level.org.springframework=DEBUG` (dużo!), `debug=true` / `--debug` →
     raport warunków autokonfiguracji (szczegóły: `s03_boot_basics/Boot01AutoConfiguration`).
  5. Błąd ŚRODOWISKA a błąd kodu: np. „release version 21 not supported” (Maven na JDK 17 — JAVA_HOME), „Could not self-attach
     to current VM” (agent Mockito — kurs konfiguruje go w parent pom.xml), „Port 8080 was already in use”.
- Testy (MockMvc niepotrzebny): `ApplicationContextRunner` z celowo zepsutą konfiguracją → `assertThat(ctx).hasFailed()` i
  sprawdzenie pierwotnej przyczyny (`getStartupFailure()` + przejście do najgłębszej przyczyny); `SpringApplication` uruchomione
  w teście z `WebApplicationType.NONE` i zepsutą konfiguracją + `OutputCaptureExtension` → w wyjściu jest „APPLICATION FAILED TO
  START” i „Description:” (SPRAWDŹ uruchomieniem, gdzie Boot to wypisuje i czy przechwytywanie działa); zmiana poziomu logowania
  w teście (`LoggingSystem` albo `@SpringBootTest(properties = "logging.level...")`) i obserwacja wpisu DEBUG.
- Ćwiczenia (3): znajdź pierwotną przyczynę w łańcuchu wyjątków (metoda zwraca najgłębszą przyczynę), napraw celowo zepsutą
  konfigurację (brakujący bean / dwa kandydaty — szkielet w klasie BEZ adnotacji skanowania, używany przez runner), napisz
  własny `FailureAnalyzer` dla własnego wyjątku (rejestracja: `META-INF/spring.factories` — SPRAWDŹ w Boot 4.1, jaki klucz
  i czy da się go zastosować tylko w teście; jeśli rejestracja dotknęłaby innych lekcji modułu, zamiast tego wywołaj
  analizator bezpośrednio w teście).
- Bez pliku `.http` (lekcja nie wystawia API).

---

## B. s03_boot_basics — podstawy Spring Boota (4 lekcje) — stan: gotowe (2026-10-19)

Moduł `s03-boot-basics` już istnieje (pusty, wpisany do parent `pom.xml`). Startery: `spring-boot-starter` i
`spring-boot-starter-jackson` (Jackson 3 + jego autokonfiguracja). Dodaj `package-info.java` działu (kolejność czytania, uwaga
o izolacji jak w `s02_configuration/package-info.java`). Każda lekcja-aplikacja: własna `@SpringBootApplication` w podpakiecie,
bez serwera WWW. Plików `.http` brak (nie ma weba).

Fakty Boot 4 do potwierdzenia (SPRAWDŹ, zanim napiszesz):
- Boot 4 jest zmodularyzowany: autokonfiguracje technologii leżą w modułach `spring-boot-<technologia>` (np. Jackson:
  `org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration` w jarze `spring-boot-jackson`), a nie w jednym
  `spring-boot-autoconfigure` jak w Boot 2/3. Starter = pom bez kodu, który dociąga moduł + biblioteki.
- Jackson 3: pakiety `tools.jackson.*` (np. `tools.jackson.databind.json.JsonMapper`), adnotacje nadal
  `com.fasterxml.jackson.annotation`. Jaki bean tworzy autokonfiguracja (typ, nazwa) i przy jakim warunku się wycofuje.
- Spis istniejących starterów = `~/.m2/repository/org/springframework/boot/spring-boot-dependencies/4.1.1/*.pom`
  (np. jest `spring-boot-starter-webmvc`, `-aspectj`, `-classic`, `-test-classic`, `spring-boot-devtools`; NIE ma `-aop`;
  `spring-boot-starter-web` istnieje, ale jest przestarzały na rzecz `-webmvc`).

### Boot01AutoConfiguration — pakiet `s03_boot_basics.boot01_auto_configuration`
- `@SpringBootApplication` = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan` (z filtrami — m.in.
  `AutoConfigurationExcludeFilter`; SPRAWDŹ).
- Skąd Boot bierze listę autokonfiguracji: pliki `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  w jarach (w Boot 4 w wielu jarach modułów). Dawniej (Boot 2) `spring.factories` — powiedz o zmianie.
- Warunki: `@ConditionalOnClass`, `@ConditionalOnMissingBean` („wycofanie się” — Twój bean wygrywa), `@ConditionalOnProperty`,
  `@ConditionalOnBooleanProperty` (SPRAWDŹ, od której wersji), `@ConditionalOnWebApplication` (tu: niespełniony, bo brak weba).
- Raport warunków (condition evaluation report): `debug=true` / `--debug` przy starcie (Positive matches / Negative matches /
  Exclusions); w teście: `ConditionEvaluationReport.get(beanFactory)`. To główne narzędzie do odpowiedzi „dlaczego Boot
  utworzył / nie utworzył beana”. Zapowiedź: `/actuator/conditions` w `s15_actuator_logging`.
- Wyłączanie: `@SpringBootApplication(exclude = …)`, `spring.autoconfigure.exclude=…`.
- Testy: `ApplicationContextRunner` + `AutoConfigurations.of(JacksonAutoConfiguration.class)` — bez własnego beana jest
  `JsonMapper` od Boota; z własnym (`withUserConfiguration`) Boot się wycofuje (ten sam obiekt co Twój);
  `withPropertyValues` zmienia ustawienia mappera; `FilteredClassLoader` udaje brak klasy → autokonfiguracja się nie
  włącza; prawdziwy kontekst `Boot01Application` (bez serwera) i odczyt raportu warunków (jest pozytywne dopasowanie dla
  Jacksona, negatywne dla czegoś webowego — SPRAWDŹ nazwy kluczy w raporcie).
- PUŁAPKI: „Boot zrobił coś magicznie” — zawsze da się sprawdzić w raporcie; własny bean o innym TYPIE niż ten, na którym
  stoi `@ConditionalOnMissingBean`, NIE wyłącza autokonfiguracji (SPRAWDŹ na Jacksonie); `exclude` klasy, której nie ma na
  ścieżce klas — co się dzieje (SPRAWDŹ: błąd czy ignorowanie).
- Ćwiczenia (3): napisz test, który dowodzi wycofania się autokonfiguracji; ustaw właściwość tak, żeby mapper zmienił
  zachowanie (np. formatowanie/wcięcia — SPRAWDŹ nazwę właściwości w Boot 4); wyłącz autokonfigurację i sprawdź brak beana.

### Boot02WritingAutoConfiguration — pakiet `s03_boot_basics.boot02_writing_auto_configuration`
- Własna autokonfiguracja „biblioteki sklepu”, np. `ShopGreeterAutoConfiguration` (`@AutoConfiguration`) tworząca bean
  `ShopGreeter` z ustawieniami `@ConfigurationProperties(prefix = "s03.greeter")` (rekord), z warunkami:
  `@ConditionalOnMissingBean` (użytkownik może podać własny), `@ConditionalOnBooleanProperty`/`@ConditionalOnProperty`
  (`s03.greeter.enabled`, DOMYŚLNIE WYŁĄCZONA), `@ConditionalOnClass` (na klasie z tej samej „biblioteki”).
- REJESTRACJA NAPRAWDĘ: plik `s03-boot-basics/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  z pełną nazwą klasy. Ponieważ ten plik działa na CAŁĄ ścieżkę klas modułu (wszystkie lekcje s03), autokonfiguracja jest
  domyślnie wyłączona właściwością — włącza ją tylko profil/plik lekcji Boot02 (`application-boot02.properties`). Wytłumacz to
  w lekcji jako DOBRĄ PRAKTYKĘ bibliotek (autokonfiguracja nie może psuć cudzych aplikacji) i dodaj test w innej lekcji
  (np. Boot01), że bez właściwości bean `ShopGreeter` nie istnieje, a w raporcie warunków jest negatywne dopasowanie.
- Klasa autokonfiguracji leży w pakiecie lekcji, a mimo to `@SpringBootApplication` jej nie skanuje dzięki
  `AutoConfigurationExcludeFilter` (klasa z listy `.imports` jest wyłączona ze skanowania) — SPRAWDŹ testem; jeśli nie
  działa, przenieś ją do podpakietu i wyłącz ze skanowania jawnie. Wyjaśnij, dlaczego autokonfiguracji nie wolno ładować
  skanowaniem (kolejność, warunki `@ConditionalOnMissingBean` liczone za wcześnie).
- Kolejność: `@AutoConfiguration(after = …)` / `before` — kiedy potrzebne.
- Jak to wygląda w prawdziwej bibliotece: moduł `xxx-spring-boot-autoconfigure` + moduł `xxx-spring-boot-starter` (pom) —
  tylko opis, bez tworzenia nowych modułów. Konwencja nazw: startery firm trzecich NIE zaczynają się od `spring-boot`.
- Testy: `ApplicationContextRunner` + `AutoConfigurations.of(ShopGreeterAutoConfiguration.class)`: wyłączona domyślnie, włączona
  właściwością, właściwości trafiają do beana, bean użytkownika wygrywa, `FilteredClassLoader` → brak beana; pełny kontekst
  `Boot02Application` z profilem → bean jest (dowód, że plik `.imports` działa).
- Ćwiczenia (3): dodaj drugi bean z warunkiem; dodaj walidację/wartość domyślną w rekordzie właściwości (bez startera
  validation — tylko `@DefaultValue`); napisz test „bean użytkownika wygrywa”.

### Boot03SpringApplication — pakiet `s03_boot_basics.boot03_spring_application`
- Co robi `SpringApplication.run(...)` krok po kroku (środowisko → banner → kontekst → odświeżenie (refresh) → runnery →
  zdarzenia gotowości). Typ aplikacji (`WebApplicationType`: NONE/SERVLET/REACTIVE) wybierany z classpathu.
- Dostosowanie: `new SpringApplication(...)` + `setBannerMode`, `setWebApplicationType`, `setAdditionalProfiles`,
  `setDefaultProperties`, `setLazyInitialization`; `SpringApplicationBuilder` (płynne API). `spring.main.*` w properties.
- `CommandLineRunner` a `ApplicationRunner` (`ApplicationArguments`: `--opcja=wartość` vs argumenty bez opcji), kolejność
  `@Order`. Zastosowanie: zadanie przy starcie (np. wczytanie danych sklepu); PUŁAPKA: długie zadanie w runnerze opóźnia
  gotowość aplikacji; wyjątek w runnerze zatrzymuje start (SPRAWDŹ).
- Zdarzenia cyklu życia aplikacji: `ApplicationStartingEvent`, `ApplicationEnvironmentPreparedEvent`,
  `ApplicationContextInitializedEvent`, `ApplicationPreparedEvent`, `ContextRefreshedEvent`, `ApplicationStartedEvent`,
  `ApplicationReadyEvent`, `ApplicationFailedEvent` — kolejność SPRAWDŹ testem. PUŁAPKA: `@EventListener` w beanie nie
  zobaczy zdarzeń sprzed utworzenia kontekstu — trzeba `SpringApplication.addListeners(...)`.
- Kod wyjścia: `SpringApplication.exit(ctx, ...)`, `ExitCodeGenerator`.
- Testy: uruchamianie `SpringApplication` w teście z `WebApplicationType.NONE` (i `close()` kontekstu!), lista zdarzeń
  z listenera dodanego przez `addListeners`, `ApplicationArguments` z tablicy argumentów, `OutputCaptureExtension` dla bannera
  (włączony vs `Banner.Mode.OFF`), kolejność runnerów.
- Ćwiczenia (3): runner liczący produkty z argumentu `--limit=`, listener zapisujący nazwy zdarzeń, `ExitCodeGenerator`
  zwracający kod zależny od stanu.

### Boot04Starters — pakiet `s03_boot_basics.boot04_starters`
- Starter = pom z zależnościami (bez kodu). Boot 4: osobne moduły technologii (`spring-boot-<tech>`) + startery
  (`spring-boot-starter-<tech>`) + startery testowe (`spring-boot-starter-<tech>-test`). Zmiany nazw względem Boot 3
  (`-web` → `-webmvc`, `-aop` → `-aspectj`), startery `-classic` do migracji (co dają i dlaczego kurs ich nie używa).
- BOM i zarządzanie wersjami: dlaczego w pom modułów nie ma wersji (`spring-boot-starter-parent` → `spring-boot-dependencies`),
  nadpisanie wersji właściwością (np. `<mockito.version>`) i PUŁAPKA mieszania wersji; `./mvnw -pl s03-boot-basics dependency:tree`
  (opisz, co zobaczy uczeń — SPRAWDŹ uruchomieniem), okno Maven w IntelliJ.
- Dlaczego kurs ma moduł na dział (powtórz krótko z `s00_start/Start02MultiModule`): starter zmienia classpath → zmienia
  autokonfigurację.
- DevTools (`spring-boot-devtools`): automatyczny restart po zmianie klas, `<optional>true</optional>`, wyłączony w spakowanym
  JAR i w testach, w IntelliJ wymaga budowania projektu (Build → Build Project albo auto-build). Tylko opis — NIE dodawaj
  zależności do modułu.
- Testy: `ClassUtils.isPresent(...)` — klasy dociągnięte przez startery tego modułu są (np. `tools.jackson.databind.json.JsonMapper`),
  klasy webowe nie (`org.springframework.web.servlet.DispatcherServlet`); w kontekście jest bean od autokonfiguracji Jacksona,
  a nie ma beanów webowych; wersja Boota w czasie działania (`SpringBootVersion.getVersion()` — SPRAWDŹ, czy zwraca 4.1.1).
- Ćwiczenia (3): metoda sprawdzająca, czy „technologia jest na classpathu” dla listy nazw klas (wynik posortowany);
  dopasuj starter do potrzeby (mapa potrzeba → nazwa startera Boot 4, `TreeMap`); znajdź przestarzałą nazwę startera na liście.

---

## Zasady wspólne dla sekcji C–G (działy pisane równolegle)
- Moduł już istnieje (`sNN-…/pom.xml` gotowy, wpisany do parent pom, zależności pobrane). Nie zmieniaj pom.xml; jeśli
  brakuje zależności — opisz to w raporcie. Dodaj `package-info.java` działu (kolejność czytania + uwaga o izolacji, wzór:
  `s02_configuration/package-info.java`).
- Równolegle pracuje do 6 agentów, każdy w SWOIM module. Uruchamiaj Mavena tylko z `-pl <twój-moduł>`.
- Aplikacje uruchamiane ręcznie (sprawdzanie plików `.http`) startuj na porcie swojego działu, NIE na 8080:
  `./mvnw -pl <moduł> spring-boot:run -Dspring-boot.run.main-class=<klasa> -Dspring-boot.run.arguments=--server.port=80NN`
  (s04 → 8084, s05 → 8085, s06 → 8086). Plik `.http` dla ucznia nadal używa `http://localhost:8080`. Po sprawdzeniu zatrzymaj
  aplikację (zabij proces Javy nasłuchujący na Twoim porcie).
- Testy webowe: MockMvc (`@WebMvcTest` albo `@SpringBootTest` + `@AutoConfigureMockMvc`) albo `RANDOM_PORT`; nigdy stały port.
  Gdzie test musi przejść przez prawdziwy serwer (np. strona błędu `/error` Boota — MockMvc jej NIE renderuje), użyj
  `RANDOM_PORT` i klienta HTTP (SPRAWDŹ, jakiego narzędzia testowego Boot 4.1 dostarcza — `TestRestTemplate`/`RestTestClient`
  i czy wymaga dodatkowego startera/adnotacji; jeśli wymaga zależności spoza pom, użyj `java.net.http.HttpClient`).
- Dane: sklep z `tools/AGENT_KIT.md` sekcja 5a. Każda lekcja ma własne rekordy/klasy.
- Bazy (s07, s08): H2 z nazwą lekcji (`jdbc:h2:mem:<lekcja>`), Flyway lekcji w `db/migration/<lekcja>`; lekcje, które nie
  uczą Flyway, mają `spring.flyway.enabled=false` (starter Flyway jest na classpath modułu i inaczej uruchomiłby się wszędzie).
  Ustawienia lekcji — w profilu `application-<lekcja>.properties` włączanym w jej klasie startowej albo w `@SpringBootTest(properties=…)`.

## C. s04_web_rest — REST w Spring MVC (7 lekcji) — stan: do napisania
Startery: `spring-boot-starter-webmvc` (+ test `-webmvc-test`). Model: produkty sklepu. Każda lekcja-aplikacja ma plik
`s04-web-rest/http/restNN_….http`.
- **Rest01Controllers** (`rest01_controllers`): `@RestController` = `@Controller` + `@ResponseBody`; `@RequestMapping` na klasie,
  `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping/@PatchMapping`; droga żądania: Tomcat → `DispatcherServlet` → handler
  mapping → metoda → konwerter wiadomości (JSON). Co się stanie przy 2 metodach na ten sam adres i metodę HTTP (błąd startu —
  SPRAWDŹ komunikat), przy złej metodzie HTTP (405), nieznanym adresie (404). Odesłanie do `s01_core_ioc/Ioc03Stereotypes`
  (`@Controller` jako znacznik). Zapowiedź `MockMvcTester` w `s11_testing/Test03MockMvcTester`.
- **Rest02RequestData** (`rest02_request_data`): `@PathVariable`, `@RequestParam` (required, defaultValue, `Optional`), `@RequestBody`
  (rekord), `@RequestHeader`; konwersja typów i błąd 400 przy `?limit=abc` (SPRAWDŹ wyjątek); nazwy parametrów a `-parameters`
  (ustawia spring-boot-starter-parent); obiekt z parametrów zapytania (filtr wyszukiwania, `@ModelAttribute`).
- **Rest03Responses** (`rest03_responses`): `ResponseEntity` (status, nagłówki, ciało), 200/201 + `Location`
  (`ServletUriComponentsBuilder`)/204/404, `@ResponseStatus`, `ResponseStatusException`; `produces`/`consumes`, 406 i 415;
  PUŁAPKA: zwracanie `null` z metody kontrolera (SPRAWDŹ status i ciało).
- **Rest04Json** (`rest04_json`): Jackson 3 (`tools.jackson.*`, adnotacje nadal `com.fasterxml.jackson.annotation`) — rekordy,
  `@JsonProperty`, `@JsonIgnore`, `@JsonFormat`, `LocalDate`/`Instant` (SPRAWDŹ domyślny format dat w Jackson 3 + Boot 4),
  `BigDecimal`, pola null (SPRAWDŹ nazwę właściwości Boota sterującej pomijaniem null), nieznane pola w żądaniu (SPRAWDŹ
  domyślne zachowanie Boota 4), dostosowanie mappera przez customizer (SPRAWDŹ nazwę interfejsu w Boot 4); `@JsonTest` tylko
  jeśli jest dostępny z zależności modułu (inaczej zwykły test z wstrzykniętym mapperem). Wyraźnie: zmiany Jackson 2 → 3
  (stare tutoriale: `com.fasterxml.jackson.databind.ObjectMapper`).
- **Rest05CrudApi** (`rest05_crud_api`): pełne CRUD produktów (repozytorium w pamięci: `ConcurrentHashMap` + sortowanie przy
  odczycie), DTO żądania/odpowiedzi (rekordy) oddzielone od modelu, kody statusu każdej operacji, idempotentność PUT/DELETE,
  PATCH (częściowa zmiana); plik `.http` ze scenariuszem całego cyklu.
- **Rest06Cors** (`rest06_cors`): zasada tego samego pochodzenia (same-origin policy) w przeglądarce, zapytanie wstępne
  (preflight, OPTIONS), `@CrossOrigin`, globalnie `WebMvcConfigurer.addCorsMappings`, `allowedOrigins("*")` +
  `allowCredentials(true)` (SPRAWDŹ, co się dzieje i jaki komunikat), `allowedOriginPatterns`; CORS egzekwuje PRZEGLĄDARKA,
  nie serwer — curl/.http go nie widzą. Testy MockMvc z nagłówkami `Origin`, `Access-Control-Request-Method`. Zapowiedź:
  CORS a Spring Security (`s12_security`).
- **Rest07ApiVersioning** (`rest07_api_versioning`): wersjonowanie API wbudowane w Spring Framework 7 (SPRAWDŹ wszystko w jarach
  spring-webmvc 7.0.9 i spring-boot-webmvc 4.1.1): atrybut `version` w `@RequestMapping`/`@GetMapping`, konfiguracja
  `WebMvcConfigurer.configureApiVersioning(ApiVersionConfigurer)` — wersja z nagłówka / segmentu ścieżki / parametru / typu
  mediów; właściwości Boota `spring.mvc.apiversion.*`; wersja bazowa „1.2+”, wersja domyślna, wspierane wersje, brak wersji
  i nieznana wersja (kody i wyjątki), oznaczanie wersji jako przestarzałej (deprecation). Porównanie z ręcznym `/api/v1/...`.

## D. s05_validation — walidacja (5 lekcji) — stan: do napisania
Startery: `webmvc`, `validation` (+ test `-webmvc-test`). PUŁAPKA ogólna: domyślne komunikaty Hibernate Validatora zależą od
Locale JVM (są polskie tłumaczenia) — testy NIE porównują treści domyślnych komunikatów bez ustalonego Locale; sprawdzają pole
(property path) i typ ograniczenia albo własny komunikat (`message = "…"`).
- **Val01BeanValidation** (`val01_bean_validation`): adnotacje `jakarta.validation.constraints` (`@NotNull`, `@NotBlank`,
  `@NotEmpty` — różnice!, `@Size`, `@Min/@Max`, `@Positive`, `@DecimalMin`, `@Email`, `@Pattern`, `@Past/@Future`) na rekordzie;
  `Validator` (bean od Boota) i `ConstraintViolation`; `null` a `@Size` (null jest poprawny — PUŁAPKA); komunikaty własne
  i `ValidationMessages.properties` (SPRAWDŹ kodowanie pliku w Hibernate Validator 9).
- **Val02ValidInControllers** (`val02_valid_in_controllers`): `@Valid @RequestBody` → 400 i `MethodArgumentNotValidException`;
  ograniczenia na `@RequestParam`/`@PathVariable` — wbudowana walidacja metod Spring MVC (od Framework 6.1;
  `HandlerMethodValidationException` — SPRAWDŹ, kiedy który wyjątek); domyślna treść odpowiedzi 400 (SPRAWDŹ); zapowiedź
  ładnych błędów w `s06_error_handling/Err03ControllerAdvice`. Plik `.http`.
- **Val03NestedAndGroups** (`val03_nested_and_groups`): kaskada `@Valid` (zamówienie → pozycje), ograniczenia elementów
  kontenera `List<@Valid OrderLine>`, `List<@NotBlank String>`; grupy (`OnCreate`/`OnUpdate`) z `@Validated(Group.class)`,
  grupa `Default`; PUŁAPKA: brak `@Valid` na polu zagnieżdżonym = brak walidacji zagnieżdżonej.
- **Val04CustomValidators** (`val04_custom_validators`): własna adnotacja + `ConstraintValidator` (np. `@Sku` — format `ELE-001`),
  ograniczenie na poziomie klasy (zakres dat od–do), wstrzykiwanie beana Springa do walidatora (np. słownik kategorii) —
  SPRAWDŹ, że działa z walidatorem Boota; `isValid(null)` → zwracaj `true` (null to sprawa `@NotNull`).
- **Val05ServiceValidation** (`val05_service_validation`): `@Validated` na klasie serwisu + ograniczenia parametrów/wyniku →
  `ConstraintViolationException`; działa przez proxy (`MethodValidationPostProcessor`) — PUŁAPKA: wywołanie z tej samej klasy
  nie jest walidowane (SPRAWDŹ); walidacja w kontrolerze czy w serwisie — kiedy co.

## E. s06_error_handling — obsługa błędów (5 lekcji) — stan: do napisania
Startery: `webmvc`, `validation` (+ test `-webmvc-test`).
- **Err01DefaultErrors** (`err01_default_errors`): domyślna obsługa Boota: przekierowanie do `/error` (`BasicErrorController`),
  ciało JSON (timestamp, status, error, path), właściwości `server.error.include-message`/`include-binding-errors`/
  `include-stacktrace`/`include-exception` (wartości domyślne — SPRAWDŹ), strona Whitelabel dla przeglądarki; PUŁAPKA:
  MockMvc nie przechodzi przez `/error` — test przez `RANDOM_PORT`; PUŁAPKA bezpieczeństwa: stos wywołań w odpowiedzi.
- **Err02ExceptionHandler** (`err02_exception_handler`): `@ExceptionHandler` w kontrolerze, `@ResponseStatus` na wyjątku,
  `ResponseStatusException`; który handler wygrywa (najbardziej szczegółowy typ); co z wyjątkami innych kontrolerów.
- **Err03ControllerAdvice** (`err03_controller_advice`): `@RestControllerAdvice` globalnie, zawężanie (`basePackages`,
  `assignableTypes`), kolejność kilku porad (`@Order`), mapowanie `MethodArgumentNotValidException` na mapę pole → komunikat
  (powiązanie z `s05_validation/Val02ValidInControllers`), logowanie błędu raz, w jednym miejscu.
- **Err04ProblemDetail** (`err04_problem_detail`): RFC 9457, `ProblemDetail` (type, title, status, detail, instance, własne
  pola), typ treści `application/problem+json`, `ErrorResponse`/`ErrorResponseException`, `ResponseEntityExceptionHandler`
  jako baza porady, `spring.mvc.problemdetails.enabled` (SPRAWDŹ, co zmienia w Boot 4.1 i jaka jest wartość domyślna);
  komunikaty z `MessageSource` (opcjonalnie).
- **Err05CustomExceptions** (`err05_custom_exceptions`): hierarchia wyjątków domeny sklepu (`ProductNotFoundException` → 404,
  `OutOfStockException` → 409, błędne zamówienie → 400 albo 422 — uzasadnij wybór), wyjątki niekontrolowane, nie ujawniaj
  szczegółów (SQL, stos) klientowi, identyfikator błędu w logu i odpowiedzi; odesłanie do `t10_exceptions/…` (tylko nazwy
  z rejestru JavaLearning).

## F. s07_data_jdbc — JDBC w Springu (5 lekcji) — stan: do napisania
Startery: `jdbc`, `h2` (runtime), `flyway` (+ test `-jdbc-test`). Bez weba, bez plików `.http`. Odesłania do
`t29_jdbc_databases/…` (czysty JDBC w JavaLearning — tylko nazwy z rejestru).
- **Jdbc01JdbcClient** (`jdbc01_jdbc_client`): `JdbcClient` (od Framework 6.1): `sql(...).param(...).query(Product.class).list()`,
  parametry nazwane i pozycyjne, mapowanie na rekord, `single()` vs `optional()`, `update()` → liczba wierszy, klucz
  generowany (`KeyHolder`); porównanie z czystym JDBC (ile kodu znika). Schemat w pliku SQL lekcji (SPRAWDŹ, jak wskazać
  plik lekcji: `spring.sql.init.schema-locations`).
- **Jdbc02JdbcTemplate** (`jdbc02_jdbc_template`): `JdbcTemplate`/`NamedParameterJdbcTemplate` (starsze API z tutoriali),
  `RowMapper`, `queryForObject` i `EmptyResultDataAccessException` (PUŁAPKA), `batchUpdate`; hierarchia `DataAccessException`
  (tłumaczenie wyjątków SQL — powiązanie z `s01_core_ioc/Ioc03Stereotypes`).
- **Jdbc03SqlInit** (`jdbc03_sql_init`): `schema.sql`/`data.sql`, `spring.sql.init.mode` (domyślnie tylko baza wbudowana —
  SPRAWDŹ), kolejność, `continue-on-error`, pliki dla platformy (`schema-h2.sql`); dlaczego to nie wystarcza w produkcji
  (brak historii zmian) → Flyway.
- **Jdbc04Flyway** (`jdbc04_flyway`): migracje `V1__…sql`, `V2__…sql`, reguły nazw, tabela `flyway_schema_history`, suma
  kontrolna — zmiana zastosowanej migracji → błąd walidacji przy starcie (SPRAWDŹ komunikat Flyway 12), migracje powtarzalne
  `R__`, `spring.flyway.locations`, `clean` domyślnie wyłączony; obsługa H2 w Flyway 12 (SPRAWDŹ, czy potrzebny osobny moduł).
  Od tej lekcji działy z bazą używają Flyway.
- **Jdbc05Repository** (`jdbc05_repository`): repozytorium produktów na `JdbcClient` z `@Repository`, błąd unikalności →
  `DuplicateKeyException`/`DataIntegrityViolationException` (SPRAWDŹ), test wycinkowy `@JdbcTest` (SPRAWDŹ pakiet w Boot 4,
  domyślne wycofanie transakcji po teście, zastępowanie bazy — `@AutoConfigureTestDatabase`).

## G. s08_data_jpa — Spring Data JPA (6 lekcji) — stan: do napisania
Startery: `data-jpa`, `h2` (runtime), `flyway` (+ test `-data-jpa-test`). Bez weba. Hibernate 7.4, Jakarta Persistence 3.2.
SPRAWDŹ pakiety Boot 4 (`@DataJpaTest`, `TestEntityManager`) w jarach.
- **Jpa01Entities** (`jpa01_entities`): `@Entity`, `@Id`, `@GeneratedValue` (IDENTITY vs SEQUENCE — co wybiera domyślnie
  Hibernate 7 dla H2, SPRAWDŹ), `@Column`, `@Table`, wymagania encji (konstruktor bez argumentów, nie `final`, NIE rekord —
  dlaczego), `equals/hashCode` encji (PUŁAPKA), `spring.jpa.hibernate.ddl-auto` (domyślnie dla bazy wbudowanej — SPRAWDŹ),
  podgląd SQL (`spring.jpa.show-sql` vs logger `org.hibernate.SQL`), kontekst utrwalania (persistence context) w skrócie.
- **Jpa02Repositories** (`jpa02_repositories`): `JpaRepository`/`ListCrudRepository`, `save` (nowa vs istniejąca encja),
  `findById` → `Optional`, `deleteById`, `existsById`, `count`; `@DataJpaTest` (transakcja wycofywana, `TestEntityManager`,
  `flush()`/`clear()` żeby zobaczyć prawdziwy SQL).
- **Jpa03QueryMethods** (`jpa03_query_methods`): zapytania z nazw metod (`findByCategory`, `findByPriceLessThan`,
  `…IgnoreCase`, `…Containing`, `OrderBy…`, `findTop3By…`, `existsBy`, `countBy`); literówka w nazwie → błąd startu
  (SPRAWDŹ wyjątek i komunikat) — „Co się stanie?”.
- **Jpa04CustomQueries** (`jpa04_custom_queries`): `@Query` JPQL i natywne SQL, parametry nazwane, projekcje (interfejs i rekord
  DTO — SPRAWDŹ, co Spring Data 2026.0 obsługuje bez wyrażenia `new`), `@Modifying` + transakcja przy UPDATE/DELETE
  (`clearAutomatically` — PUŁAPKA nieaktualnych encji w kontekście).
- **Jpa05PagingSorting** (`jpa05_paging_sorting`): `Pageable`, `PageRequest.of(...)`, `Sort`, `Page` vs `Slice` (zapytanie
  liczące), stała kolejność przy stronicowaniu (PUŁAPKA: sortowanie po kolumnie z duplikatami bez drugiego klucza).
- **Jpa06FlywayWithJpa** (`jpa06_flyway_with_jpa`): schemat z migracji Flyway + `ddl-auto=validate` (Hibernate tylko sprawdza);
  niezgodność encji i tabeli → błąd startu (SPRAWDŹ wyjątek i komunikat) — „Co się stanie?”; dlaczego nie `update` na
  produkcji. Zapowiedź relacji: `s09_jpa_relations/Rel01ManyToOne`.
