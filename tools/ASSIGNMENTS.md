# ASSIGNMENTS — konspekty działów dla agentów piszących lekcje

Każda sekcja = jedna paczka lekcji dla JEDNEGO agenta. Agent dostaje `tools/AGENT_KIT.md` + swoją sekcję. Nazwy lekcji są już
w `tools/lessons.txt`. Fakty oznaczone „SPRAWDŹ” agent potwierdza w jarze (`~/.m2/repository`, javap) albo uruchomieniem,
zanim je napisze. Gotowe sekcje zostają w pliku jako historia (stan na końcu nagłówka).

---

## A. s00_start — dodatki: słowniczek i debugowanie (2 lekcje) — stan: do napisania

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

## B. s03_boot_basics — podstawy Spring Boota (4 lekcje) — stan: do napisania

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
