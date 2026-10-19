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

## C. s04_web_rest — REST w Spring MVC (7 lekcji) — stan: gotowe (2026-10-19)
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

## D. s05_validation — walidacja (5 lekcji) — stan: gotowe (2026-10-19)
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

## E. s06_error_handling — obsługa błędów (5 lekcji) — stan: gotowe (2026-10-19)
Startery: `webmvc`, `validation` (+ test `-webmvc-test`).
Uwaga po napisaniu: w Boot 4 właściwości błędów mają prefiks `spring.web.error.*`; `server.error.*` (jak niżej w konspekcie)
jest wycofane z poziomem `error` i po cichu ignorowane — lekcja Err01 to pokazuje.
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

## F. s07_data_jdbc — JDBC w Springu (5 lekcji) — stan: gotowe (2026-10-19)
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

## G. s08_data_jpa — Spring Data JPA (6 lekcji) — stan: gotowe (2026-10-19)
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

---

## Zasady dla sekcji H–I (działy z częściową pracą z WIP)
Sesja w chmurze (2026-10-11) zostawiła niescalony WIP na gałęzi `claude/funny-bell-pmq611`. Jego moduły s09 i s10 są już
skopiowane do drzewa roboczego (`s09-jpa-relations/`, `s10-transactions/`), budują się i przechodzą LessonLint. Ocena: lekcje
Rel01, Rel02, Tx01, Tx02 są kompletne i dobrej jakości, Rel03 i Tx03 to szkice (klasy/encje bez lekcji), reszty brak.
Kolejność pracy agenta:
1. PRZEGLĄD gotowych lekcji z WIP według `tools/AGENT_KIT.md` (szczególnie „Mistakes found in reviews”): każda PUŁAPKA i
   odpowiedź „Co się stanie?” ma być sprawdzona uruchomieniem/testem — popraw albo usuń twierdzenia bez dowodu (np. w Tx02
   komentarz o `NESTED` przy JPA nie ma testu — SPRAWDŹ). Testy lekcji nie mogą używać szkieletów ćwiczeń.
2. Dokończenie szkiców (Rel03, Tx03) do pełnych lekcji.
3. Nowe lekcje według konspektu.
Zasady wspólne z sekcji C–G obowiązują (Maven tylko `-pl <moduł>`, H2 i Flyway per lekcja, bez portu 8080).

## H. s09_jpa_relations — relacje JPA (6 lekcji) — stan: gotowe (2026-10-19; Rel01, Rel02 z WIP po przeglądzie)
Startery: `data-jpa`, `flyway`, `h2` (runtime), test `data-jpa-test`; dodatkowo `webmvc` (+ test `-webmvc-test`) tylko dla Rel06.
- **Rel01ManyToOne**, **Rel02OneToMany** — z WIP: przegląd i weryfikacja (patrz zasady H–I).
- **Rel03ManyToMany** (`rel03_many_to_many`, szkic w WIP): `@ManyToMany` (produkty ↔ tagi), tabela łącząca, strona właściciela,
  `Set` zamiast `List` (SPRAWDŹ, co robi Hibernate 7 przy `List` — usuwanie i ponowne wstawianie wierszy), metody pomocnicze
  utrzymujące obie strony, kiedy zamienić `@ManyToMany` na encję pośrednią z własnymi polami (pozycja zamówienia z ilością).
- **Rel04LazyLoading** (`rel04_lazy_loading`): `FetchType.LAZY`/`EAGER`, wartości domyślne (`@ManyToOne`/`@OneToOne` EAGER,
  kolekcje LAZY — SPRAWDŹ w JPA 3.2), proxy Hibernate (klasa obiektu), `LazyInitializationException` poza transakcją
  (dokładny komunikat z uruchomienia), `spring.jpa.open-in-view` (domyślnie włączone + ostrzeżenie w logu — SPRAWDŹ treść
  w Boot 4.1) i dlaczego warto je wyłączać; DOBRA PRAKTYKA: wszystko LAZY, a dociąganie jawne.
- **Rel05NPlusOne** (`rel05_n_plus_one`): problem N+1 pokazany LICZBĄ zapytań (statystyki Hibernate:
  `hibernate.generate_statistics` + `Statistics.getPrepareStatementCount()` — SPRAWDŹ nazwy w Hibernate 7.4), rozwiązania:
  `JOIN FETCH`, `@EntityGraph`, `@BatchSize`/`default_batch_fetch_size`, projekcja DTO; PUŁAPKA: `JOIN FETCH` kolekcji +
  stronicowanie (ostrzeżenie/wyjątek Hibernate 7 — SPRAWDŹ dokładnie) i duplikaty wierszy.
- **Rel06EntityToDto** (`rel06_entity_to_dto`): dlaczego kontroler NIE zwraca encji (cykle w JSON przy relacjach
  dwukierunkowych, leniwe pola → wyjątek albo zapytania przy serializacji, wyciek pól wewnętrznych, sprzężenie API ze
  schematem bazy) — pokaż na prawdziwym kontrolerze (MockMvc) z encją, potem z rekordem DTO; mapowanie ręczne (metoda `from`,
  osobna klasa mapper), gdzie mapować (serwis w transakcji); Lombok i MapStruct — tylko odesłanie
  (`t20_lombok/Lombok03DataValueBuilder` jeśli istnieje w rejestrze JavaLearning). Plik `.http` dla aplikacji lekcji
  (sprawdzanie ręczne na porcie 8089).

## I. s10_transactions — transakcje (5 lekcji) — stan: gotowe (2026-10-19; Tx01, Tx02 z WIP po przeglądzie)
Startery: `data-jpa`, `flyway`, `h2` (runtime), test `data-jpa-test`. Dział TRUDNY — każde twierdzenie z testem.
- **Tx01Transactional**, **Tx02Propagation** — z WIP: przegląd i weryfikacja (patrz zasady H–I).
- **Tx03ProxyPitfalls** (`tx03_proxy_pitfalls`, klasy pomocnicze w WIP): wywołanie metody `@Transactional` z tej samej klasy
  (self-invocation) — brak transakcji, metoda prywatna/`final` (SPRAWDŹ, co Spring 7 robi z `@Transactional` na metodzie
  niepublicznej — od Framework 6.0 działa dla protected/package-private przy proxy klasowym?), wyjątek kontrolowany vs
  niekontrolowany (rollback), złapany wyjątek w środku, `rollbackFor`; jak to naprawić (osobny bean, `TransactionTemplate`);
  sprawdzanie `TransactionSynchronizationManager.isActualTransactionActive()` w testach.
- **Tx04ReadOnlyIsolation** (`tx04_read_only_isolation`): `readOnly = true` — co naprawdę robi z Hibernate (tryb flush, brak
  dirty checking — SPRAWDŹ), poziomy izolacji (`Isolation.*`) i zjawiska (brudny odczyt, niepowtarzalny odczyt, fantomy) —
  pokaż tylko to, co da się DETERMINISTYCZNIE odtworzyć na H2 (dwie transakcje sterowane krok po kroku, bez `sleep`);
  `timeout`.
- **Tx05ProgrammaticTransactions** (`tx05_programmatic_transactions`): `TransactionTemplate` (`execute`,
  `executeWithoutResult`, `setRollbackOnly`), `PlatformTransactionManager` niskopoziomowo, kiedy programowo zamiast adnotacji;
  `@TransactionalEventListener` — tylko zapowiedź `s17_events`.

## J. s11_testing — testowanie w Spring Boot (7 lekcji) — stan: gotowe (2026-10-19; Test01 z WIP po przeglądzie)
Startery (pom gotowy, z WIP): `webmvc`, `data-jpa`, `flyway`, `h2` + `postgresql` (runtime), `spring-boot-docker-compose`
(runtime), test: `-webmvc-test`, `-data-jpa-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`,
`testcontainers-postgresql` (Testcontainers 2.0.5). Docker JEST dostępny na komputerze autora (28.3.3) — lekcje 06/07 sprawdzaj
naprawdę; obrazy z jawnym tagiem (nigdy `latest`). U ucznia bez Dockera testy z kontenerami mają być POMIJANE
(`@Testcontainers(disabledWithoutDocker = true)` albo równoważny warunek), a `./mvnw test` zielony.
W WIP są tylko klasy domenowe i 5 testów Test01 (slices, cache kontekstu, webEnvironment) — bez klasy lekcji; przejrzyj je
według AGENT_KIT i dopisz lekcję.
- **Test01TestSlices** (`test01_test_slices`): piramida testów, wycinki (`@WebMvcTest`, `@DataJpaTest`, `@JdbcTest`, `@JsonTest`)
  a `@SpringBootTest` (webEnvironment MOCK/RANDOM_PORT/DEFINED_PORT/NONE), co ładuje który wycinek (SPRAWDŹ pakiety Boot 4),
  pamięć podręczna kontekstów testowych (ten sam klucz konfiguracji = ten sam kontekst; co tworzy nowy: `@MockitoBean`,
  inne właściwości, `@DirtiesContext`) — pokaż licząc starty kontekstu.
- **Test02MockitoBeans** (`test02_mockito_beans`): czysty Mockito (`@Mock`, `@InjectMocks`, `when/verify`, `ArgumentCaptor`)
  w teście jednostkowym bez Springa vs `@MockitoBean`/`@MockitoSpyBean` w kontekście (pakiet
  `org.springframework.test.context.bean.override.mockito`); `@MockBean` usunięte w Boot 4; `@TestBean` (podmiana beana metodą
  fabryczną — SPRAWDŹ); reset atrap między testami; PUŁAPKA: atrapa wszystkiego = test niczego. Odesłanie do
  `t32_junit_mockito/…` (tylko nazwy z rejestru JavaLearning).
- **Test03MockMvcTester** (`test03_mock_mvc_tester`): `MockMvcTester` (API w stylu AssertJ, Framework 6.2+) — jak go dostać
  w `@WebMvcTest` (SPRAWDŹ, czy Boot 4 wstrzykuje go automatycznie), `assertThat(mvc.get().uri(...)).hasStatusOk()`,
  `bodyJson()`, porównanie z klasycznym `perform().andExpect()`; `RestTestClient` (Framework 7) — SPRAWDŹ, czy jest dostępny
  z zależności modułu; jeśli wymaga innej zależności, tylko wzmianka.
- **Test04DataJpaTest** (`test04_data_jpa_test`): `@DataJpaTest` dokładniej — transakcja i wycofanie po teście, `TestEntityManager`,
  `flush()`/`clear()`, `@Sql`, zastępowanie bazy (`@AutoConfigureTestDatabase`, `replace = NONE` — zapowiedź Test06),
  Flyway w wycinku, dane testowe niezależne od kolejności testów.
- **Test05TestConfiguration** (`test05_test_configuration`): `@TestConfiguration` (kiedy jest skanowana, a kiedy trzeba
  `@Import` — SPRAWDŹ), `@TestPropertySource`, `@DynamicPropertySource`, `@ActiveProfiles`, podmiana `Clock` w teście
  (deterministyczny czas — CLAUDE.md sekcja 5), wpływ na pamięć podręczną kontekstów.
- **Test06Testcontainers** (`test06_testcontainers`): prawdziwy PostgreSQL w kontenerze na czas testu: `@Testcontainers`,
  `@Container` (statyczny — jeden na klasę), `@ServiceConnection` (Boot sam ustawia `DataSource`), klasa kontenera
  w Testcontainers 2.0 (SPRAWDŹ pakiet — zmienił się względem 1.x), Flyway na PostgreSQL, różnice H2 vs PostgreSQL, które test na
  H2 by przeoczył (pokaż jedną prawdziwą — SPRAWDŹ uruchomieniem), uruchamianie aplikacji lokalnie z kontenerami
  (`SpringApplication.from(...).with(...)` — SPRAWDŹ). Testy pomijane bez Dockera.
- **Test07DockerCompose** (`test07_docker_compose`): `spring-boot-docker-compose` — plik `compose.yaml` lekcji, Boot uruchamia
  usługi przy starcie aplikacji i tworzy połączenia (service connection), cykl życia (`spring.docker.compose.lifecycle-management`),
  wskazanie pliku lekcji (`spring.docker.compose.file`), domyślne pomijanie w testach (SPRAWDŹ właściwość), Docker Compose vs
  Testcontainers — kiedy co. IZOLACJA: obsługa Compose musi być WYŁĄCZONA dla wszystkich innych lekcji modułu
  (`spring.docker.compose.enabled=false` we wspólnym `application.properties`, włączona tylko w profilu Test07). Plik `.http`
  dla aplikacji lekcji (port 8091 przy sprawdzaniu).

## K. s12_security — Spring Security 7 (6 lekcji) — stan: gotowe (2026-10-19)
Startery: `webmvc`, `security`, `oauth2-resource-server` (+ test `-webmvc-test`, `-security-test`). Spring Security 7.1.1.
Dział TRUDNY — stare tutoriale (Security 5: `WebSecurityConfigurerAdapter`, `antMatchers`, `.and()`) NIE działają; mów o tym
wprost i SPRAWDŹ każde API w jarach 7.1.1. Hasła i klucze tylko przykładowe, oznaczone jako testowe; klucze JWT generuj w kodzie
w czasie działania (para RSA albo tajny klucz HMAC z jawnie testową wartością) — żadnych plików z kluczami w repozytorium.
Port do ręcznego sprawdzania `.http`: 8092.
- **Sec01Basics** (`sec01_basics`): co się dzieje po dodaniu startera (wszystko zablokowane, wygenerowane hasło w logu —
  SPRAWDŹ dokładną treść, użytkownik `user`, formularz logowania i HTTP Basic), łańcuch filtrów (`SecurityFilterChain`,
  `DelegatingFilterProxy`/`FilterChainProxy` — po co), własny bean `SecurityFilterChain` z DSL lambda
  (`authorizeHttpRequests`, `requestMatchers`, `permitAll`, `authenticated`), 401 vs 403. Odesłanie do
  `s04_web_rest/Rest06Cors` (CORS) i do `s06_error_handling/…`.
- **Sec02Authorization** (`sec02_authorization`): role i uprawnienia (`hasRole("ADMIN")` = `ROLE_ADMIN` — PUŁAPKA prefiksu),
  kolejność reguł (pierwsza pasująca wygrywa — PUŁAPKA `anyRequest()` na początku), metody HTTP w regułach, zabezpieczenia
  metod: `@EnableMethodSecurity` + `@PreAuthorize` (SpEL, `#id`, `authentication.name`), domyślny deny (`anyRequest().denyAll()`
  vs `authenticated()`).
- **Sec03UsersPasswords** (`sec03_users_passwords`): `UserDetailsService`, `InMemoryUserDetailsManager`, użytkownicy z „bazy”
  (własna implementacja na mapie — bez JPA w tym module), `PasswordEncoder`: `BCryptPasswordEncoder` (sól, koszt, ten sam
  tekst → różne hashe), `DelegatingPasswordEncoder` i prefiks `{bcrypt}` (SPRAWDŹ domyślny encoder z
  `PasswordEncoderFactories`), PUŁAPKA `{noop}` i hasła w kodzie; aktualnie zalogowany użytkownik (`@AuthenticationPrincipal`,
  `SecurityContextHolder`).
- **Sec04StatelessApi** (`sec04_stateless_api`): API bez sesji (`SessionCreationPolicy.STATELESS`), CSRF — czym jest, dlaczego
  można wyłączyć dla czysto bezstanowego API z tokenem w nagłówku, a nie wolno dla aplikacji z ciasteczkiem sesji (SPRAWDŹ
  domyślne zachowanie CSRF w Security 7), CORS w Security (`http.cors(...)` + `CorsConfigurationSource`), obsługa błędów
  401/403 jako JSON (`AuthenticationEntryPoint`, `AccessDeniedHandler`, ewentualnie `ProblemDetail`).
- **Sec05Jwt** (`sec05_jwt`): JWT — budowa (nagłówek, ładunek, podpis), czego NIE trzymać w ładunku, ważność (`exp`), Resource
  Server: `http.oauth2ResourceServer(o -> o.jwt(...))`, `JwtDecoder` z kluczem testowym, wystawianie tokenu w lekcji
  (`JwtEncoder`/`NimbusJwtEncoder` — SPRAWDŹ API) przez prosty punkt końcowy logowania, mapowanie claimów na uprawnienia
  (`JwtAuthenticationConverter`, prefiks `SCOPE_`), token przeterminowany → 401 (SPRAWDŹ nagłówek `WWW-Authenticate`).
  DOBRA PRAKTYKA: w prawdziwym systemie tokeny wydaje serwer tożsamości (zapowiedź OAuth2/Keycloak w `s22_extras`).
- **Sec06SecurityTesting** (`sec06_security_testing`): `@WebMvcTest` + bezpieczeństwo (co jest ładowane — SPRAWDŹ, czy własny
  `SecurityFilterChain` trzeba `@Import`), `@WithMockUser`, `@WithUserDetails`, `SecurityMockMvcRequestPostProcessors`
  (`httpBasic()`, `user()`, `jwt()`, `csrf()`), testy 401/403/200 dla każdej reguły, test, że hasło nie wycieka w odpowiedzi.

---

## Zasady dla sekcji L i dalszych
Jak w „Zasadach wspólnych dla sekcji C–G”. Moduł istnieje (pom gotowy, w parent pom, zależności pobrane). Porty do ręcznego
sprawdzania `.http`: s13 → 8093, s14 → 8094, s15 → 8095, s16 → 8096. Żadnego internetu w testach: „zewnętrzne API” to lokalny
serwer w teście (RANDOM_PORT) albo `MockRestServiceServer`.

## L. s13_rest_client — klient HTTP (4 lekcje) — stan: gotowe (2026-10-19; limity czasu: spring.http.clients.*, nie spring.http.client.*)
Startery: `restclient`, `webmvc` (udawany serwer dostawcy), test: `restclient-test`, `webmvc-test`. Scenariusz: sklep pyta
zewnętrzne API dostawcy o ceny/stan magazynu.
- **Rc01RestClient** (`rc01_rest_client`): `RestClient` (od Framework 6.1) — budowanie (`RestClient.Builder` od Boota,
  `baseUrl`, nagłówki domyślne), `get().uri(...).retrieve().body(...)`, `toEntity`, `post().body(...)`, mapowanie JSON na rekord
  (Jackson 3), `ParameterizedTypeReference` dla list; test `@RestClientTest` + `MockRestServiceServer` (SPRAWDŹ pakiety Boot 4)
  i test z prawdziwym lokalnym serwerem (RANDOM_PORT). Porównanie: `RestTemplate` (stary, w trybie utrzymania), `WebClient`
  (reaktywny — nie potrzebujesz go do zwykłych wywołań).
- **Rc02ErrorsAndTimeouts** (`rc02_errors_and_timeouts`): domyślne wyjątki dla 4xx/5xx (`HttpClientErrorException`,
  `HttpServerErrorException` — SPRAWDŹ dokładne typy), `onStatus(...)` i własne wyjątki, `exchange(...)` dla pełnej kontroli,
  limity czasu połączenia i odczytu (SPRAWDŹ, jak w Boot 4 — właściwości `spring.http.client.*`/fabryka żądań), wyjątek przy
  przekroczeniu (`ResourceAccessException` — test z wolnym udawanym serwerem, deterministycznie: serwer czeka na zatrzask, nie `sleep`
  w asercji), logowanie wywołań.
- **Rc03HttpInterfaces** (`rc03_http_interfaces`): klient opisany interfejsem (`@HttpExchange`, `@GetExchange`, `@PostExchange`),
  tworzenie przez `HttpServiceProxyFactory` + `RestClientAdapter`, w Framework 7/Boot 4 rejestracja grup klientów
  (`@ImportHttpServices` i właściwości — SPRAWDŹ dokładne nazwy i działanie), test z `MockRestServiceServer`.
- **Rc04Resilience** (`rc04_resilience`): odporność wbudowana w Spring Framework 7: `@Retryable` (liczba prób, opóźnienie,
  które wyjątki), `@ConcurrencyLimit`, włączenie (`@EnableResilientMethods` — SPRAWDŹ), `RetryTemplate`/`RetryPolicy` z core
  (SPRAWDŹ pakiety `org.springframework.core.retry`, `org.springframework.resilience`); PUŁAPKA: ponawianie operacji nie
  idempotentnej (POST zamówienia), ponawianie przez proxy (wywołanie z tej samej klasy — bez ponowień); testy liczące próby
  na udawanym serwerze, bez zależności od czasu (minimalne opóźnienia, asercje na liczbie wywołań).

## M. s14_openapi — dokumentacja API (3 lekcje) — stan: gotowe (2026-10-19)
Startery: `webmvc`, `validation`, `springdoc-openapi-starter-webmvc-ui` (wersja z parent pom: `${springdoc.version}` = 3.1.1),
test `webmvc-test`. SPRAWDŹ zgodność springdoc 3.1.1 z Boot 4.1.1 (start aplikacji, `/v3/api-docs`, `/swagger-ui.html`).
- **Api01Springdoc** (`api01_springdoc`): czym jest OpenAPI (opis API jako dane), co springdoc generuje z kontrolerów bez żadnych
  adnotacji, `/v3/api-docs` (JSON) i Swagger UI; test MockMvc czytający dokument i sprawdzający ścieżki/operacje/schematy (JsonPath);
  właściwości `springdoc.*` (ścieżki, wyłączenie UI na produkcji).
- **Api02DocumentingApi** (`api02_documenting_api`): `@Operation`, `@ApiResponse`, `@Parameter`, `@Schema` (opis, przykład),
  `@Tag`; jak ograniczenia Bean Validation trafiają do schematu (SPRAWDŹ, co springdoc 3 odwzorowuje); rekordy DTO, odpowiedzi
  błędów (`ProblemDetail`); PUŁAPKA: dokumentacja rozjeżdża się z kodem — test, który pilnuje kluczowych elementów dokumentu.
- **Api03GroupsAndVersions** (`api03_groups_and_versions`): `GroupedOpenApi` (publiczne vs administracyjne), informacje ogólne
  (`OpenAPI` bean: tytuł, wersja, kontakt), schemat zabezpieczeń (tylko opis, bez Security w module), dokumentowanie wersji API
  z `s04_web_rest/Rest07ApiVersioning` (SPRAWDŹ, czy springdoc 3.1 rozumie atrybut `version` w mapowaniach Framework 7 — jeśli
  nie, pokaż obejście i powiedz o tym uczciwie).

## N. s15_actuator_logging — Actuator, metryki, logi (5 lekcji) — stan: gotowe (2026-10-19)
Startery: `webmvc`, `actuator`, test `webmvc-test`, `actuator-test`.
- **Obs01Actuator** (`obs01_actuator`): punkty końcowe (health, info, metrics, loggers, env, beans, conditions — odesłanie do
  `s03_boot_basics/Boot01AutoConfiguration`), domyślnie wystawione przez HTTP (SPRAWDŹ w Boot 4.1), `management.endpoints.web.
  exposure.include`, osobny port zarządzania, PUŁAPKA bezpieczeństwa (`env`, `heapdump` wystawione publicznie), `info` z danymi
  aplikacji. Testy MockMvc albo RANDOM_PORT.
- **Obs02HealthIndicators** (`obs02_health_indicators`): własny `HealthIndicator` (np. „magazyn dostępny”), szczegóły
  (`show-details`), grupy zdrowia, sondy liveness/readiness (kiedy włączone — SPRAWDŹ), `AvailabilityChangeEvent`.
- **Obs03Metrics** (`obs03_metrics`): Micrometer — `MeterRegistry`, `Counter`, `Timer`, `Gauge`, tagi (PUŁAPKA wysokiej
  kardynalności — np. id klienta jako tag), metryki HTTP od Boota (`http.server.requests`), `/actuator/metrics/{nazwa}`; testy
  bez zależności od czasu (sprawdzaj liczniki, nie czasy).
- **Obs04Logging** (`obs04_logging`): SLF4J + Logback, poziomy, `logging.level.*`, grupy logów, zapis do pliku, wzorzec,
  logowanie strukturalne (`logging.structured.format.console` — SPRAWDŹ wartości w Boot 4.1), MDC (identyfikator żądania),
  zmiana poziomu w czasie działania przez `/actuator/loggers`; PUŁAPKA: łączenie napisów zamiast `{}` i logowanie danych
  osobowych/haseł. Testy z `OutputCaptureExtension`.
- **Obs05Observations** (`obs05_observations`): Observation API (Micrometer) — jedna obserwacja = metryka + (opcjonalnie) ślad,
  `ObservationRegistry`, `Observation.createNotStarted(...).observe(...)`, `@Observed` (SPRAWDŹ, co trzeba włączyć), co
  obserwuje Boot sam; zapowiedź śledzenia rozproszonego (`s22_extras/Ext04Tracing`).

## O. s16_cache_scheduling_async — pamięć podręczna, harmonogram, asynchroniczność (5 lekcji) — stan: gotowe (2026-10-19)
Startery: `cache`, `webmvc`, test `webmvc-test`. Determinizm: żadnych asercji na czasie; zadania cykliczne testuj przez
wywołanie metody i sprawdzenie konfiguracji harmonogramu, asynchroniczność przez `CompletableFuture`/Awaitility (SPRAWDŹ, czy
Awaitility jest w `spring-boot-starter-test`).
- **Cache01Cacheable** (`cache01_cacheable`): `@EnableCaching`, `@Cacheable` (nazwa, klucz, `condition`, `unless`), licznik
  wywołań prawdziwej metody w teście, domyślny `CacheManager` (SPRAWDŹ, co Boot wybiera bez dodatkowych bibliotek), PUŁAPKI:
  wywołanie z tej samej klasy (proxy), obiekty zmienne w pamięci podręcznej, klucz z wielu parametrów.
- **Cache02CacheManagement** (`cache02_cache_management`): `@CachePut`, `@CacheEvict` (`allEntries`), `@Caching`, unieważnianie
  przy zmianie danych, ograniczenie rozmiaru i czasu życia (tylko opis — Caffeine/Redis wymagają zależności; wyraźnie powiedz),
  `spring.cache.*`, testy z `CacheManager`.
- **Sched03Scheduled** (`sched03_scheduled`): `@EnableScheduling`, `@Scheduled` (`fixedRate`, `fixedDelay`, `initialDelay`, `cron`
  ze strefą), wyrażenia cron Springa (6 pól — PUŁAPKA vs cron Linuksa 5 pól), `CronExpression.next(...)` w teście z ustalonym
  czasem, pula wątków harmonogramu (domyślnie jeden wątek — SPRAWDŹ), `ScheduledTaskHolder` do sprawdzenia zarejestrowanych zadań,
  `Clock` w logice zadania.
- **Async04Async** (`async04_async`): `@EnableAsync`, `@Async` z `CompletableFuture`, wykonawca (`TaskExecutor` od Boota —
  `spring.task.execution.*`), wyjątki w metodach `void` (`AsyncUncaughtExceptionHandler`), PUŁAPKI: wywołanie z tej samej klasy,
  `@Async` na metodzie prywatnej, utrata kontekstu (MDC, transakcja) w innym wątku.
- **Async05VirtualThreads** (`async05_virtual_threads`): wątki wirtualne (Java 21) — `spring.threads.virtual.enabled=true`: co
  zmienia w Boot 4.1 (Tomcat, wykonawcy zadań, harmonogram — SPRAWDŹ), test pokazujący `Thread.currentThread().isVirtual()` w
  kontrolerze/metodzie `@Async`, kiedy pomagają (dużo czekania na I/O) a kiedy nie (obliczenia CPU), przypinanie (pinning) —
  `synchronized` w JDK 24+ już nie przypina (JEP 491 — SPRAWDŹ, opisz dla Javy 21 i 25), PUŁAPKA: pule połączeń z bazą
  nadal ograniczają współbieżność. Odesłanie do `t21_concurrency/…` (tylko nazwy z rejestru JavaLearning).

## P. s17_events — zdarzenia aplikacji (3 lekcje) — stan: gotowe (2026-10-19)
Startery: `jdbc`, `h2` (runtime), `flyway`. Bez weba. Scenariusz: złożenie zamówienia → powiadomienie, punkty lojalnościowe,
rezerwacja magazynu. Odesłanie do `t22_design_patterns/…` (obserwator — tylko nazwy z rejestru JavaLearning).
- **Evt01ApplicationEvents** (`evt01_application_events`): `ApplicationEventPublisher.publishEvent(rekord)` (zdarzenie nie musi
  dziedziczyć po `ApplicationEvent`), `@EventListener`, domyślnie SYNCHRONICZNIE w tym samym wątku (wyjątek słuchacza wraca do
  publikującego — SPRAWDŹ), kolejność `@Order`, warunek `condition` (SpEL), słuchacz zwracający nowe zdarzenie; po co zdarzenia
  (luźne powiązanie modułów) i kiedy NIE (ukryty przepływ sterowania).
- **Evt02TransactionalEvents** (`evt02_transactional_events`): `@TransactionalEventListener` (fazy `AFTER_COMMIT` domyślnie,
  `AFTER_ROLLBACK`, `BEFORE_COMMIT`), PUŁAPKA: publikacja bez transakcji → słuchacz się nie wykona (`fallbackExecution` —
  SPRAWDŹ), PUŁAPKA: zapis do bazy w słuchaczu `AFTER_COMMIT` (transakcja już zatwierdzona — SPRAWDŹ, co się dzieje i jak to
  zrobić poprawnie, np. `REQUIRES_NEW`); testy z prawdziwą transakcją (`TransactionTemplate`) i wycofaniem. Odesłanie do
  `s10_transactions/Tx01Transactional`.
- **Evt03AsyncAndOutbox** (`evt03_async_and_outbox`): słuchacz `@Async` (zdarzenie obsłużone w innym wątku, utrata kontekstu
  transakcji), co jeśli aplikacja padnie po zatwierdzeniu, a przed obsłużeniem zdarzenia (zdarzenie ginie) → wzorzec outbox
  (zapis zdarzenia w tej samej transakcji i osobne wysyłanie) — prosta własna implementacja na tabeli + zapowiedź rejestru
  publikacji Spring Modulith (`s20_modulith/Mod02ModuleEvents`) i kolejek (Kafka/RabbitMQ w `s22_extras/Ext02Messaging`).
  Testy deterministyczne (Awaitility albo jawne wywołanie wysyłacza).

## Q. s18_aop — programowanie aspektowe (3 lekcje) — stan: gotowe (2026-10-19)
Starter: `spring-boot-starter-aspectj` (w Boot 4 zamiast `-aop`). Dział TRUDNY (proxy od środka).
- **Aop01Aspects** (`aop01_aspects`): po co aspekty (sprawy przekrojowe: logowanie, pomiar, uprawnienia), `@Aspect` + `@Component`,
  porady `@Before`, `@AfterReturning`, `@AfterThrowing`, `@After`, `@Around` (`ProceedingJoinPoint.proceed()` — PUŁAPKA: brak
  `proceed()` = metoda się nie wykona), wyrażenia punktów przecięcia (`execution(...)`, `within(...)`, `@annotation(...)`,
  `bean(...)`), `@Pointcut` wielokrotnego użytku; testy z licznikami/zapisem kolejności wywołań.
- **Aop02Proxies** (`aop02_proxies`): jak Spring tworzy proxy — JDK dynamic proxy (interfejs) vs CGLIB (podklasa), co Boot ustawia
  domyślnie (`spring.aop.proxy-target-class` — SPRAWDŹ), `AopUtils.isAopProxy/isCglibProxy`, klasa obiektu w teście,
  PUŁAPKI: wywołanie z tej samej klasy omija proxy (dlatego `@Transactional`, `@Cacheable`, `@Async`, `@Retryable`
  „nie działają”), metody `final`/prywatne, rzutowanie na klasę przy proxy JDK (`ClassCastException`/`BeanNotOfRequiredTypeException`
  — SPRAWDŹ); obejścia (osobny bean, wstrzyknięcie samego siebie, `AopContext` — z ostrzeżeniem). Odesłanie do
  `t19_annotations_reflection/…` (dynamiczne proxy w czystej Javie — tylko nazwy z rejestru).
- **Aop03CustomAnnotations** (`aop03_custom_annotations`): własna adnotacja (np. `@MeasureTime`, `@AuditLog`) + aspekt
  `@Around("@annotation(...)")` z dostępem do atrybutów adnotacji, kolejność kilku aspektów (`@Order`) i kolejność względem
  transakcji, PUŁAPKA: aspekt łapiący i połykający wyjątki; kiedy aspekt to przesada (jawne wywołanie jest czytelniejsze).

## R. s19_deploy — wdrożenie (4 lekcje) — stan: gotowe (2026-10-19)
Startery: `webmvc`, `actuator`, test `webmvc-test`. W tym dziale agent MOŻE dodać do `s19-deploy/pom.xml` sekcję `<build>` z
`spring-boot-maven-plugin` (to temat lekcji) — z jedną klasą główną (`mainClass`), żeby `./mvnw -pl s19-deploy package` dał
działający JAR. Docker jest dostępny u autora — budowanie obrazu sprawdź ręcznie, ale testy (`mvnw test`) NIE mogą wymagać
Dockera ani zbudowanego JAR-a (faza test jest przed package).
- **Dep01ExecutableJar** (`dep01_executable_jar`): JAR wykonywalny („fat jar”) — `repackage`, budowa (`BOOT-INF/classes`,
  `BOOT-INF/lib`, `META-INF/MANIFEST.MF`, `JarLauncher` — SPRAWDŹ nazwę klasy w Boot 4), `java -jar`, argumenty i właściwości
  z linii poleceń, warstwy (layers) i `java -Djarmode=tools -jar … extract` (SPRAWDŹ tryb w Boot 4), dlaczego nie „uber-jar”
  z rozpakowanymi klasami. Pokaż prawdziwy wynik `jar tf` (z uruchomienia) w komentarzu.
- **Dep02Docker** (`dep02_docker`): `Dockerfile` wielostopniowy (budowa + obraz z samym JRE, warstwy z `extract`), obraz
  bazowy z jawnym tagiem (np. Temurin 25 JRE), uruchomienie jako nie-root, pamięć JVM w kontenerze (`-XX:MaxRAMPercentage`),
  `./mvnw spring-boot:build-image` (Buildpacks) — porównanie; `.dockerignore`. Plik `Dockerfile` w module; instrukcja ręczna,
  sprawdzona przez autora (`docker build`, `docker run`, zapytanie do `/actuator/health`).
- **Dep03ProductionConfig** (`dep03_production_config`): konfiguracja z zewnątrz — zmienne środowiskowe (relaxed binding:
  `SPRING_DATASOURCE_URL`… — odesłanie do `s02_configuration/Cfg05Environment`), `spring.config.import` (plik spoza JAR-a,
  `optional:`), profil `prod`, sekrety NIGDY w repozytorium (zmienne środowiskowe, menedżer sekretów — opis), `--debug` wyłączony;
  testy wiązania konfiguracji z „udawanego” środowiska (prawdziwa nazwa źródła `systemEnvironment`!).
- **Dep04StartupAndShutdown** (`dep04_startup_and_shutdown`): łagodne zamykanie (`server.shutdown=graceful` — SPRAWDŹ wartość
  domyślną w Boot 4.1, `spring.lifecycle.timeout-per-shutdown-phase`), sondy liveness/readiness dla Kubernetesa (odesłanie do
  `s15_actuator_logging/Obs02HealthIndicators`), czas startu, leniwa inicjalizacja (wady), CDS / pamięć podręczna AOT w JDK 25
  (tylko opis + SPRAWDŹ, co Boot 4.1 wspiera), zapowiedź GraalVM (`s22_extras/Ext06NativeImage`).

## S. s20_modulith — Spring Modulith (3 lekcje) — stan: do napisania
Zależności: `spring-modulith-starter-core`, `-starter-jdbc`, `spring-modulith-docs` (BOM Modulith 2.1.1 z parent pom), `jdbc`,
`h2`, `flyway`, test `spring-modulith-starter-test`. UWAGA na izolację: Modulith traktuje pakiety pod klasą
`@SpringBootApplication` jako moduły — każda lekcja ma WŁASNĄ aplikację w swoim pakiecie, a moduły biznesowe są jej podpakietami
(np. `mod01_modules.products`, `mod01_modules.orders`). SPRAWDŹ, jak `ApplicationModules.of(Mod01Application.class)` wyznacza
moduły i czy pakiet `solutions` nie zostanie potraktowany jako moduł innej lekcji.
- **Mod01Modules** (`mod01_modules`): monolit modułowy — moduł = bezpośredni podpakiet aplikacji, API modułu vs pakiet
  `internal`, `ApplicationModules.of(...).verify()` w teście (błąd przy sięganiu do środka innego modułu — SPRAWDŹ komunikat,
  pokaż na celowo złej aplikacji w osobnym pakiecie testowym), cykle między modułami, `@ApplicationModule(allowedDependencies=…)`.
- **Mod02ModuleEvents** (`mod02_module_events`): komunikacja modułów przez zdarzenia — `@ApplicationModuleListener` (asynchronicznie,
  po zatwierdzeniu transakcji, we własnej transakcji — SPRAWDŹ), rejestr publikacji zdarzeń w bazie (tabela `EVENT_PUBLICATION`
  — skąd schemat: SPRAWDŹ `spring.modulith.events.jdbc.schema-initialization.enabled` albo migracja Flyway), ponawianie
  nieukończonych publikacji, test `@ApplicationModuleTest` + `Scenario` (`publish(...).andWaitForEventOfType(...)`). Odesłanie
  do `s17_events/Evt03AsyncAndOutbox`.
- **Mod03Documentation** (`mod03_documentation`): `Documenter` — diagramy modułów (PlantUML/C4) i „kanwa” modułu generowane do
  `target/spring-modulith-docs` w teście (pokaż fragment prawdziwego wyniku), nazwane interfejsy (`@NamedInterface`),
  obserwowalność modułów (tylko wzmianka); kiedy monolit modułowy, a kiedy mikroserwisy.

## T. s21_capstone — projekty końcowe (3 lekcje) — stan: do napisania
Zależności: `webmvc`, `validation`, `data-jpa`, `h2`, `flyway`, `security`, `oauth2-resource-server`, testy webmvc/data-jpa/security.
Projekt = lekcja: specyfikacja (wymagania, przypadki użycia, punkty końcowe, model danych), architektura (warstwy, pakiety, DTO,
obsługa błędów), PEŁNA implementacja wzorcowa w pakiecie lekcji + testy na każdym poziomie (jednostkowe, wycinki, pełny kontekst),
plik `.http` ze scenariuszem, a ćwiczenia = ROZSZERZENIA projektu (nowa funkcja z testem ćwiczenia; szkielet + TODO).
Każda lekcja odsyła do lekcji kursu, z których korzysta (tylko nazwy z rejestru). IZOLACJA: Security jest na classpath całego
modułu — projekty bez logowania (Cap01, Cap02) mają jawny `SecurityFilterChain` z `permitAll()` (z komentarzem DLACZEGO);
H2 i Flyway osobno na projekt (`db/migration/cap0N`). Port do ręcznego sprawdzania: 8121.
- **Cap01ShopApi** (`cap01_shop_api`): REST API sklepu — produkty (CRUD, wyszukiwanie, stronicowanie), klienci, zamówienia
  z pozycjami (relacje JPA, przeliczanie sumy, zmiana statusu z regułami: NOWE → OPLACONE → WYSLANE → DOSTARCZONE, ANULOWANE),
  walidacja, `ProblemDetail`, DTO zamiast encji, brak N+1 na liście zamówień (test liczący zapytania), dane startowe z
  `tools/AGENT_KIT.md` 5a w migracji. Ćwiczenia: kody rabatowe, raport sprzedaży po kategoriach, blokada zamówienia przy braku towaru.
- **Cap02Reservations** (`cap02_reservations`): system rezerwacji (np. stanowiska serwisowe sklepu) — sloty czasowe, rezerwacja
  bez nakładania się (reguła w serwisie + ograniczenie w bazie; współbieżność: dwie rezerwacje tego samego slotu — test
  deterministyczny, blokada optymistyczna `@Version` albo ograniczenie unikalności — SPRAWDŹ zachowanie na H2), `Clock`
  wstrzykiwany (rezerwacje w przeszłości odrzucane), anulowanie z terminem, zdarzenie „potwierdzono” + słuchacz po zatwierdzeniu
  transakcji. Ćwiczenia: lista oczekujących, limit rezerwacji na klienta, przypomnienia (metoda wołana przez `@Scheduled`).
- **Cap03LibraryWithAuth** (`cap03_library_with_auth`): biblioteka — książki, czytelnicy, wypożyczenia (termin zwrotu, kara za
  spóźnienie liczona z `Clock`), Spring Security: role BIBLIOTEKARZ i CZYTELNIK, logowanie zwracające JWT (klucz testowy
  generowany w kodzie), `@PreAuthorize` (czytelnik widzi tylko swoje wypożyczenia), hasła BCrypt, testy 401/403/200.
  Ćwiczenia: rezerwacja książki wypożyczonej, przedłużenie terminu (maks. 1 raz), raport zaległości tylko dla bibliotekarza.

## U. s22_extras — dodatki (6 lekcji, opcjonalne) — stan: do napisania
Zależności (pom gotowy, agent tego działu MOŻE go zmieniać, jeśli coś nie działa — z uzasadnieniem w raporcie): `webmvc`,
`spring-ai-starter-model-ollama` (BOM Spring AI 2.0.1), `kafka`, `oauth2-resource-server` (Security!), `opentelemetry`, `webflux`,
testy: webmvc/webflux/security, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-kafka`.
IZOLACJA (najtrudniejsza w kursie — moduł łączy kłócące się startery): każda lekcja ma własną aplikację i musi jawnie wyłączyć to,
czego nie dotyczy (np. lekcja Spring AI: Security z `permitAll` albo wyłączenie autokonfiguracji — SPRAWDŹ nazwy klas w Boot 4;
eksport śladów OpenTelemetry wyłączony poza lekcją o tracingu; Kafka nie łączy się przy starcie, jeśli nikt jej nie używa —
SPRAWDŹ). Testy bez internetu: model AI = atrapa `ChatModel` w teście (żadnych prawdziwych wywołań), Kafka = Testcontainers
(pomijane bez Dockera; Docker jest u autora), Keycloak tylko w `compose.yaml` do uruchomienia ręcznego (testy z `jwt()`).
Każda lekcja to PRZEGLĄD: kiedy po to sięgnąć, minimalny działający przykład, pułapki, co czytać dalej. 2–3 ćwiczenia na lekcję.
Port do ręcznego sprawdzania: 8122.
- **Ext01SpringAi** (`ext01_spring_ai`): `ChatClient` (prompt, rola systemowa, odpowiedź jako rekord — structured output),
  model lokalny Ollama (konfiguracja `spring.ai.ollama.*`, uruchomienie modelu w Dockerze — opis), atrapa `ChatModel` w testach,
  PUŁAPKI: niedeterministyczne odpowiedzi, koszty/limity, dane osobowe w promptach, „prompt injection”. SPRAWDŹ API Spring AI 2.0.1.
- **Ext02Messaging** (`ext02_messaging`): kolejki wiadomości — Kafka: `KafkaTemplate.send`, `@KafkaListener`, grupa konsumentów,
  serializacja JSON (Jackson 3 — SPRAWDŹ serializery Spring Kafka dla Boot 4), test z `@ServiceConnection` + kontener Kafki
  (jawny tag obrazu), RabbitMQ — tylko porównanie (kiedy który); powiązanie z outbox (`s17_events/Evt03AsyncAndOutbox`).
- **Ext03OAuth2Keycloak** (`ext03_oauth2_keycloak`): OAuth2/OIDC w skrócie (serwer autoryzacji wydaje tokeny, Twoja aplikacja to
  Resource Server), konfiguracja `spring.security.oauth2.resourceserver.jwt.issuer-uri`, mapowanie ról Keycloak na uprawnienia
  Springa (konwerter claimów), `compose.yaml` z Keycloakiem do ręcznego uruchomienia (realm do zaimportowania — dane testowe),
  testy z `jwt()`. Odesłanie do `s12_security/Sec05Jwt`.
- **Ext04Tracing** (`ext04_tracing`): śledzenie rozproszone — ślad (trace) i odcinek (span), propagacja nagłówka `traceparent`
  między usługami (dwie aplikacje albo RestClient do samej siebie), identyfikator śladu w logach, eksport (OpenTelemetry —
  w testach eksporter w pamięci, SPRAWDŹ jak w Boot 4.1), próbkowanie. Odesłanie do `s15_actuator_logging/Obs05Observations`.
- **Ext05WebFluxOverview** (`ext05_webflux_overview`): czym jest programowanie reaktywne (`Mono`, `Flux`, nieblokujące I/O),
  minimalny kontroler WebFlux i `WebTestClient` (aplikacja lekcji z `WebApplicationType.REACTIVE`, bo MVC też jest na classpath —
  SPRAWDŹ), PUŁAPKA: blokujący kod w reaktywnym potoku, i najważniejsze: kiedy NIE potrzebujesz WebFlux (wątki wirtualne —
  `s16_cache_scheduling_async/Async05VirtualThreads`).
- **Ext06NativeImage** (`ext06_native_image`): GraalVM native image — co daje (start w milisekundach, mniej pamięci) i co kosztuje
  (długa kompilacja, świat zamknięty: refleksja wymaga podpowiedzi), przetwarzanie AOT Springa (`spring-boot:process-aot`),
  `RuntimeHints`/`RuntimeHintsRegistrar` z testem `RuntimeHintsPredicates` (bez GraalVM), opis budowy obrazu natywnego
  (`native:compile`, Buildpacks) bez wymogu instalacji GraalVM.
