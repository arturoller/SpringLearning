# SpringLearning — instrukcje dla Claude (autor i opiekun kursu)

Ten plik czyta każda sesja Claude Code w tym repozytorium. Opisuje, JAK tworzymy kurs. Opis kursu dla ucznia jest w `README.md`.

## 1. Cel i odbiorca
- Kurs Springa i Spring Boota **po polsku** dla osoby, która uczy się programowania i utrwala wiedzę. Kontynuacja kursu
  **JavaLearning**: https://github.com/arturoller/JavaLearning (ten sam styl, te same tagi, ta sama metoda nauki).
- Uczeń słabo zna angielski: **wszystkie komentarze po polsku**, a każda angielska nazwa (adnotacja, klasa, metoda, pojęcie)
  dostaje polskie tłumaczenie przy pierwszym użyciu w pliku, np. `bean (ziarno — obiekt zarządzany przez Springa)`.
- Polszczyzna naturalna, bez kalk: immutable = „niezmienny” (NIE „niemutowalny”), mutable = „zmienny”, hiding/shadowing =
  „ukrywanie/przesłanianie”, side effect = „efekt uboczny”. Terminologia jak w JavaLearning (`t00_start/Start02Glossary`).
- Treść tworzymy od zera, skupioną na zasadach, dobrych praktykach i pułapkach. Nie odwołujemy się do prywatnych projektów ucznia.
- Szczegółowość jest celem: wyjaśniaj „dlaczego”, nie tylko „jak”.

## 2. Struktura projektu
Projekt **wielomodułowy Mavena: jeden moduł = jeden dział**. Powód rozstrzygający: startery (security, data-jpa,
actuator, flyway…) zmieniają autokonfigurację CAŁEJ aplikacji — w jednym module np. Spring Security zablokowałby
wszystkie wcześniejsze lekcje webowe. Osobny moduł = osobny classpath (ścieżka klas) = dział ma tylko swoje startery.
```
pom.xml                    parent (rodzic) — spring-boot-starter-parent, lista modułów, wspólne ustawienia, surefire
mvnw, mvnw.cmd, .mvn/      Maven Wrapper — budowanie bez instalowania Mavena
s00-start/                 moduł działu (nazwa modułu: sNN-temat, z myślnikami)
├── pom.xml                tylko startery potrzebne w tym dziale
├── http/                  pliki .http z gotowymi zapytaniami (IntelliJ HTTP Client), np. rest01_hello.http
└── src/
    ├── main/java/s00_start/…        lekcje (pakiet bazowy: sNN_temat, z podkreślnikami), package-info.java
    ├── main/resources/              application.properties, migracje Flyway (db/migration/…)
    └── test/java/s00_start/…        testy lekcji i ćwiczeń (te same pakiety co w main)
s01-core-ioc/ … s22-extras/        kolejne działy, ten sam układ
temp/                      lokalne pliki robocze Claude (konspekty, notatki, narzędzia) — w .gitignore, NIGDY w commicie
```
- Moduły dopisujemy do `<modules>` w parent `pom.xml` dopiero, gdy dział powstaje (pusty moduł nie istnieje).
- Standardowy układ Mavena (`src/main/java`, `src/main/resources`, `src/test/java`) — taki jak ze Spring Initializr,
  żeby uczeń rozpoznawał go w dokumentacji i tutorialach. Żadnych `<sourceDirectory>` w pom.xml.
- **Lekcja = podpakiet** w pakiecie działu: `sNN_temat.<skrótNN>_<aspekt>`, np. `s01_core_ioc.ioc02_constructor_injection`.
  W nim plik lekcji z nagłówkiem (`Ioc02ConstructorInjection.java`) i klasy pomocnicze.
- **Izolacja lekcji w module:** lekcja, która potrzebuje kontekstu Springa, ma WŁASNĄ klasę `@SpringBootApplication`
  (`Ioc02Application`) w swoim podpakiecie. Nigdy nie ma klasy `@SpringBootApplication` w pakiecie bazowym działu — skanowałaby
  wszystkie lekcje naraz. Podpakiety lekcji są rodzeństwem (nie zagnieżdżamy jednej lekcji w drugiej), więc
  `@SpringBootTest` w pakiecie lekcji znajduje tylko jej własną konfigurację.
- Ustawienia lekcji: wspólne `application.properties` modułu jest minimalne; to, co dotyczy jednej lekcji, idzie do
  `@SpringBootTest(properties = …)`/`@TestPropertySource` albo do profilu `application-<lekcja>.properties` włączanego w jej
  klasie startowej. Baza H2 z unikalną nazwą na lekcję (`jdbc:h2:mem:<lekcja>`), migracje Flyway lekcji w osobnym katalogu
  (`spring.flyway.locations=classpath:db/migration/<lekcja>`).
- Nazwy klas: `<Temat><NN><Aspekt>` (jak w JavaLearning), np. `Ioc02ConstructorInjection`, test: `Ioc02ConstructorInjectionTest`.
- Nie używaj nazw kolidujących z klasami JDK/Springa (np. nie twórz własnej klasy `Component`, `Service`, `List`).
- „Świat” danych jak w JavaLearning: sklep — produkty, klienci, zamówienia, pracownicy (ten sam klimat przykładów).

## 3. Wersje
- **Zapisane decyzje: Java = 21 (minimum, `<java.version>21</java.version>` w parent pom), zalecana 25 (LTS);
  Spring Boot = 4.1.1** (najnowsza stabilna 4.x — sprawdzone w Maven Central 2026-10-05 przy tworzeniu szkieletu);
  Maven Wrapper 3.3.4 z Mavenem 3.9.16.
  Boot 4.1.1 wymaga Javy 17+ i jest zgodny do Javy 26 włącznie, więc 21 i 25 działają.
- W Boot 4.1.1 przychodzą m.in.: Spring Framework 7.0, Spring Security 7.1, Spring Data 2026.0, Hibernate 7.4, Jackson 3.1,
  JUnit 6.0, Testcontainers 2.0, Flyway 12. Przy tworzeniu szkieletu sprawdź, czy nie ma nowszej 4.x z poprawkami
  (Maven Central: `org.springframework.boot:spring-boot-starter-parent`) i zaktualizuj ten numer.
- Uczeń ma lokalnie JDK 17.0.16 — README opisuje instalację JDK 25 (IntelliJ: File → Project Structure → SDK → Download JDK).
- Boot 4 jest **zmodularyzowany**: startery nazywają się jak technologia, a testy mają własne startery. Używaj:
  `spring-boot-starter-webmvc` (zamiast dawnego `-web`), `-validation`, `-data-jpa`, `-jdbc`, `-flyway`, `-restclient`,
  `-security`, `-actuator`; w testach `spring-boot-starter-test` + odpowiedniki `-webmvc-test`, `-data-jpa-test` itd.
  Przed użyciem startera sprawdź, czy taki artefakt istnieje w wersji Boota z parent pom.
- Każdy moduł dołącza **tylko startery swojego działu** (np. security dopiero w `s12-security`). Wspólny dla wszystkich jest
  tylko `spring-boot-starter-test` (w parent pom, scope test).
- Biblioteki spoza BOM Boota (springdoc-openapi, Spring Modulith, Spring AI) — wersje w `<properties>` parent pom, zgodne
  z Boot 4 (stan na 2026-10: springdoc 3.1.x, Spring Modulith 2.1.x, Spring AI 2.0.x).
- W testach podmieniamy beany wyłącznie przez **`@MockitoBean`/`@MockitoSpyBean`** — nigdy `@MockBean` (przestarzałe od Boot 3.4, usunięte
  w Boot 4 — sprawdzone w źródłach 4.1.1).

## 4. Format lekcji
Każda lekcja to mała, samodzielna rzecz w swoim podpakiecie modułu działu (klasy z przykładami + test, który je sprawdza;
układ — sekcja 2).

**Nagłówek** (Javadoc klasy, w `<pre>`), tagi zawsze DOKŁADNIE w tej postaci, z dwukropkiem zaraz po słowie:
`TEMAT:` · `W SKRÓCIE:` · `ANALOGIA:` · `JAK TO DZIAŁA:` · `SŁÓWKA:` · `ZOBACZ TEŻ:`
W treści: `PUŁAPKA:` i `DOBRA PRAKTYKA:` (zawsze z „dlaczego”), na końcu `ŚCIĄGA:` i `PYTANIA KONTROLNE:` (5–8 pytań, co najmniej
2 typu „Co się stanie?” / „ZNAJDŹ BŁĄD”). Ćwiczenia: `ĆWICZENIE 1:` (numerowane). Kod z `<`, `>`, `&` w Javadoc zawsze w
`{@code …}` — nigdy encje HTML (`&lt;`). Wzór formatu: dowolna lekcja JavaLearning, np. `src/t08_enums/Enums03ConstantBodies.java`.

**Weryfikacja przez testy (zamiast `main` + `// WYNIK:` z JavaLearning):**
- Każda sekcja lekcji ma test w `sNN-temat/src/test/java/sNN_temat/<lekcja>/…Test.java` z `@DisplayName` po polsku —
  test jest „wynikiem” sekcji.
- Rodzaje testów: `@SpringBootTest` (cały kontekst), `@WebMvcTest` + MockMvc (kontrolery), `@DataJpaTest` (baza),
  zwykłe testy jednostkowe tam, gdzie Spring nie jest potrzebny. Podmiana beanów: tylko `@MockitoBean`.
- Testy wymagające Dockera (Testcontainers, `@ServiceConnection`): klasa z `@Testcontainers(disabledWithoutDocker = true)` —
  bez Dockera są POMIJANE, więc `./mvnw test` przechodzi u każdego. Docker Compose (`spring-boot-docker-compose`) służy
  tylko do uruchamiania aplikacji lokalnie (w testach Boot go domyślnie pomija).
- Lekcje, które uruchamiają aplikację, dostają plik `sNN-temat/http/<lekcja>.http` z gotowymi zapytaniami (IntelliJ HTTP Client).
- Uruchamianie: cały kurs `./mvnw test`, jeden dział `./mvnw -pl s04-web-rest test`, jedna lekcja-aplikacja ▶ przy `main` jej
  klasy `@SpringBootApplication` albo `./mvnw -pl s04-web-rest spring-boot:run -Dspring-boot.run.main-class=…`.
- Format lekcji sprawdza `tools/LessonLint.java` (sekcja 9) — wymagane `PROBLEMS: 0`.

**Ćwiczenia — nie mogą psuć budowania:**
- Szkielet ćwiczenia w `src/` (metody z `// TODO` i `throw new UnsupportedOperationException("TODO")`).
- Testy ćwiczeń oznaczone `@Tag("cwiczenie")` — **wyłączone z domyślnego `mvnw test`** (WŁAŚCIWOŚĆ `<excludedGroups>cwiczenie`
  w `<properties>` parent pom, nie konfiguracja pluginu — dzięki temu da się ją nadpisać). Uczeń uruchamia je sam: ▶ w IntelliJ
  albo `./mvnw -pl <moduł> test -Dgroups=cwiczenie -DexcludedGroups=none` (samo `-Dgroups=cwiczenie` daje 0 testów).
- Rozwiązania wzorcowe w pakiecie `sNN_temat.solutions.<lekcja>` (rodzeństwo lekcji, NIE jej podpakiet — inaczej klasa
  startowa lekcji zeskanowałaby rozwiązanie razem ze szkieletem i miałaby dwa beany tego samego typu), a ich testy (`@Tag("wzorzec")`) BIEGNĄ domyślnie — dowód, że ćwiczenie jest
  rozwiązywalne, a test poprawny. Odpowiedzi na pytania kontrolne: w zwiniętym bloku
  `// <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">` na końcu klasy lekcji.

## 5. Determinizm i bezpieczeństwo testów
- Testy webowe: `@SpringBootTest(webEnvironment = RANDOM_PORT)` albo MockMvc — nigdy na sztywno port 8080 w testach.
- Baza: H2 w pamięci; schemat i dane startowe w `schema.sql`/`data.sql` (do `s07`), od lekcji o Flyway — w migracjach Flyway
  albo w teście. Żadnych zewnętrznych usług ani internetu w testach
  (zewnętrzne API: lokalny serwer testowy / MockRestServiceServer / WireMock, jeśli dodamy zależność). Jedyny wyjątek:
  kontenery Testcontainers, i to tylko w klasach z `@Testcontainers(disabledWithoutDocker = true)` (sekcja 4).
- Czas: wstrzykuj `Clock` (bean), nigdy `LocalDate.now()` w logice. Losowość: z ziarnem. Żadnych asercji na czasie wykonania.
- Kolejność: nie zakładaj kolejności z `HashMap`/`HashSet`; sortuj przed porównaniem.
- Żadnych sekretów w repozytorium (hasła, klucze JWT — tylko przykładowe, oznaczone jako testowe).

## 6. Jak pracujemy (workflow)
- **1 dział = 1 commit**, robiony dopiero gdy dział jest kompletny i `mvnw test` jest zielony. Commit robi Claude, bez pytania.
- **Push robi użytkownik** (lokalnie). Sesja w chmurze: pracuj na osobnej gałęzi, wypchnij gałąź i otwórz pull request —
  nigdy nie wypychaj bezpośrednio na `main`.
- Komunikaty commitów po polsku, np. `s04_web_rest: REST w Springu (6 lekcji)`, zakończone liniami Co-Authored-By modeli.
- Przed commitem działu: pełne `mvnw test` (zielone) + `java tools/LessonLint.java` z `PROBLEMS: 0` (tagi, encje HTML,
  niewidoczne znaki, odesłania do lekcji obu kursów, rejestr, tagi testów ćwiczeń/rozwiązań), brak plików w `temp/`.
- Agenci (subagenci): najwyżej 2 naraz (3 tylko za zgodą użytkownika). Proste działy → model Sonnet, trudne (Security, transakcje,
  AOP, JPA od środka) → Opus. Agent dostaje `tools/AGENT_KIT.md` + KONKRETNY konspekt (plik w `temp/`), nie skanuje projektu,
  weryfikuje każdą lekcję od razu po napisaniu. Nowa paczka lekcji = nowy agent; poprawki do jego paczki = wiadomość do tego samego agenta.
- Każdą paczkę po agencie weryfikuje główna sesja: testy + wyrywkowo merytoryka (PUŁAPKA, ŚCIĄGA, ODPOWIEDZI).
- Odpowiedzi „Co się stanie?” sprawdzaj uruchomieniem, nie liczeniem w głowie.

## 7. Pułapki środowiska (komputer ucznia: Windows 10, PowerShell 5.1, IntelliJ IDEA 2026.1)
- Bitdefender blokuje pliki `.ps1` (i `.txt` z treścią skryptów PowerShell) — nie twórz ich; narzędzia pisz w Javie
  (uruchamiane jako `java Plik.java`) albo jako polecenia inline.
- PowerShell traktuje polskie cudzysłowy „ ” jak zwykłe `"` — nie używaj ich w stringach skryptów (np. komunikatach commitów).
- Narzędzie Write/Edit zamienia `\uXXXX` na prawdziwe znaki — unikaj takich escape'ów w kodzie.
- Pliki: UTF-8 bez BOM. PowerShell 5.1 czyta pliki bez BOM jako ANSI — przy odczycie podawaj kodowanie UTF-8.
- Maven nie jest w PATH — używaj `mvnw`/`mvnw.cmd`.

## 8. Pierwsze kroki (checklista, jeśli projekt jest jeszcze pusty)
1. Sprawdź, czy wersje z sekcji 3 są nadal aktualne (najnowsza stabilna 4.x), popraw je tutaj.
2. Utwórz parent `pom.xml` w korzeniu: `<packaging>pom</packaging>`, parent `spring-boot-starter-parent`, `<java.version>21`,
   `<modules>`, `spring-boot-starter-test` dla wszystkich, surefire z `<excludedGroups>cwiczenie</excludedGroups>`, kodowanie
   UTF-8, wersje bibliotek spoza BOM Boota w `<properties>`. Dodaj Maven Wrapper (`mvnw`, `mvnw.cmd`, `.mvn/`) i uzupełnij
   `.gitignore` o `temp/`.
3. Moduł `s00-start` (standardowy układ, tylko potrzebne startery) — lekcje: jak korzystać z kursu, Spring Initializr,
   uruchamianie aplikacji i testów, **wprowadzenie do testów** (co robią `@SpringBootTest`, MockMvc, asercje — żeby uczeń
   rozumiał checkery od pierwszej lekcji), pliki `.http`, ćwiczenia z tagiem. `./mvnw test` zielony.
4. Sprawdź w IntelliJ (opis w README), że projekt otwiera się jako wielomodułowy i ▶ działa przy teście i przy `main`.
5. Commit `s00_start: …` i aktualizacja spisu treści w README (⏳ → ✅).
6. Konspekty kolejnych działów zapisuj w `temp/ASSIGNMENTS.md` przed zleceniem ich agentom i od razu dopisz nazwy lekcji do
   `tools/lessons.txt` (`sNN_dzial/KlasaLekcji`). Nowy dział = nowy moduł (`sNN-temat/pom.xml` + wpis w `<modules>`).

## 9. Narzędzia (`tools/`, w repozytorium)
| Plik | Do czego |
|---|---|
| `tools/AGENT_KIT.md` | Zasady pisania lekcji dla podagentów — przekazuj go każdemu agentowi razem z konspektem. |
| `tools/LessonLint.java` | Sprawdzacz formatu: `java -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 tools/LessonLint.java ["sNN-moduł/**"]`. |
| `tools/lessons.txt` | Rejestr lekcji (`sNN_dzial/KlasaLekcji`; linia z `/*` na końcu = sam dział). Uzupełniany razem z konspektem. |
| `tools/javalearning-lessons.txt` | Rejestr lekcji JavaLearning — do odesłań `tNN_pakiet/Klasa`. Aktualizuj, gdy w JavaLearning dojdą lekcje. |
| `tools/tags.txt` | Wymagane tagi i tagi sprawdzane pod kątem formatu (wspólne z JavaLearning). |
