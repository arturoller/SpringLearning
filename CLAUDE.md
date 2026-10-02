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
```
src/          lekcje: pakiety sNN_temat (s00_start … s20_capstone), każdy z package-info.java
test/         testy lekcji i ćwiczeń (te same pakiety co w src/)
resources/    application.properties/yml, schema.sql, data.sql, pliki .http z gotowymi zapytaniami
temp/         lokalne pliki robocze Claude (konspekty, notatki, narzędzia) — w .gitignore, NIGDY w commicie
```
- Maven: `<sourceDirectory>src</sourceDirectory>`, `<testSourceDirectory>test</testSourceDirectory>`, zasoby z `resources/`.
- Dodaj **Maven Wrapper** (`mvnw`, `mvnw.cmd`), żeby budowanie działało bez instalowania Mavena (u ucznia Maven jest tylko
  wbudowany w IntelliJ).
- Nazwy klas: `<Temat><NN><Aspekt>` (jak w JavaLearning), np. `Ioc02ConstructorInjection`, test: `Ioc02ConstructorInjectionTest`.
- Nie używaj nazw kolidujących z klasami JDK/Springa (np. nie twórz własnej klasy `Component`, `Service`, `List`).
- „Świat” danych jak w JavaLearning: sklep — produkty, klienci, zamówienia, pracownicy (ten sam klimat przykładów).

## 3. Wersje (ustal na początku i zapisz tutaj)
- Java: zalecana **21** (LTS); możliwa 25 (LTS). Uczeń ma lokalnie JDK 17.0.16 — jeśli wybierzesz 21/25, napisz w README,
  jak zainstalować JDK (IntelliJ: File → Project Structure → SDK → Download JDK).
- Spring Boot: **najnowsza stabilna wersja** w chwili tworzenia szkieletu — sprawdź na https://start.spring.io i zapisz tu numer.
- Startery na start: web, validation, data-jpa, h2, test (dalsze — security, actuator, cache… — dopiero w swoich działach).
- Zapisane decyzje: Java = `____`, Spring Boot = `____` (uzupełnij przy tworzeniu szkieletu).

## 4. Format lekcji
Każda lekcja to mała, samodzielna rzecz w pakiecie `sNN_temat` (klasy z przykładami + test, który je sprawdza).

**Nagłówek** (Javadoc klasy, w `<pre>`), tagi zawsze DOKŁADNIE w tej postaci, z dwukropkiem zaraz po słowie:
`TEMAT:` · `W SKRÓCIE:` · `ANALOGIA:` · `JAK TO DZIAŁA:` · `SŁÓWKA:` · `ZOBACZ TEŻ:`
W treści: `PUŁAPKA:` i `DOBRA PRAKTYKA:` (zawsze z „dlaczego”), na końcu `ŚCIĄGA:` i `PYTANIA KONTROLNE:` (5–8 pytań, co najmniej
2 typu „Co się stanie?” / „ZNAJDŹ BŁĄD”). Ćwiczenia: `ĆWICZENIE 1:` (numerowane). Kod z `<`, `>`, `&` w Javadoc zawsze w
`{@code …}` — nigdy encje HTML (`&lt;`). Wzór formatu: dowolna lekcja JavaLearning, np. `src/t08_enums/Enums03ConstantBodies.java`.

**Weryfikacja przez testy (zamiast `main` + `// WYNIK:` z JavaLearning):**
- Każda sekcja lekcji ma test w `test/sNN_temat/…Test.java` z `@DisplayName` po polsku — test jest „wynikiem” sekcji.
- Rodzaje testów: `@SpringBootTest` (cały kontekst), `@WebMvcTest` + MockMvc (kontrolery), `@DataJpaTest` (baza),
  zwykłe testy jednostkowe tam, gdzie Spring nie jest potrzebny.
- Lekcje, które uruchamiają aplikację, dostają plik `resources/http/sNN_….http` z gotowymi zapytaniami (IntelliJ HTTP Client).

**Ćwiczenia — nie mogą psuć budowania:**
- Szkielet ćwiczenia w `src/` (metody z `// TODO` i `throw new UnsupportedOperationException("TODO")`).
- Testy ćwiczeń oznaczone `@Tag("cwiczenie")` — **wyłączone z domyślnego `mvnw test`** (konfiguracja surefire:
  `<excludedGroups>cwiczenie</excludedGroups>`). Uczeń uruchamia je sam (▶ w IntelliJ albo `mvnw test -Dgroups=cwiczenie`).
- Rozwiązania wzorcowe w podpakiecie `…solutions`, a ich testy (`@Tag("wzorzec")`) BIEGNĄ domyślnie — dowód, że ćwiczenie jest
  rozwiązywalne, a test poprawny. Odpowiedzi na pytania kontrolne: w zwiniętym bloku
  `// <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">` na końcu klasy lekcji.

## 5. Determinizm i bezpieczeństwo testów
- Testy webowe: `@SpringBootTest(webEnvironment = RANDOM_PORT)` albo MockMvc — nigdy na sztywno port 8080 w testach.
- Baza: H2 w pamięci; dane startowe w `data.sql`/w teście. Żadnych zewnętrznych usług ani internetu w testach
  (zewnętrzne API: lokalny serwer testowy / MockRestServiceServer / WireMock, jeśli dodamy zależność).
- Czas: wstrzykuj `Clock` (bean), nigdy `LocalDate.now()` w logice. Losowość: z ziarnem. Żadnych asercji na czasie wykonania.
- Kolejność: nie zakładaj kolejności z `HashMap`/`HashSet`; sortuj przed porównaniem.
- Żadnych sekretów w repozytorium (hasła, klucze JWT — tylko przykładowe, oznaczone jako testowe).

## 6. Jak pracujemy (workflow)
- **1 dział = 1 commit**, robiony dopiero gdy dział jest kompletny i `mvnw test` jest zielony. Commit robi Claude, bez pytania.
- **Push robi użytkownik** (lokalnie). Sesja w chmurze: pracuj na osobnej gałęzi, wypchnij gałąź i otwórz pull request —
  nigdy nie wypychaj bezpośrednio na `main`.
- Komunikaty commitów po polsku, np. `s04_web_rest: REST w Springu (6 lekcji)`, zakończone liniami Co-Authored-By modeli.
- Przed commitem działu: pełne `mvnw test`, sprawdzenie tagów (format `TAG:`), brak encji HTML, brak plików w `temp/`.
- Agenci (subagenci): najwyżej 2 naraz (3 tylko za zgodą użytkownika). Proste działy → model Sonnet, trudne (Security, transakcje,
  AOP, JPA od środka) → Opus. Agent dostaje KONKRETNY konspekt (plik w `temp/`), nie skanuje projektu, weryfikuje każdą lekcję
  od razu po napisaniu. Nowa paczka lekcji = nowy agent; poprawki do jego paczki = wiadomość do tego samego agenta.
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
1. Ustal wersje (sekcja 3) i wpisz je tutaj.
2. Utwórz `pom.xml` (parent spring-boot-starter-parent, startery z sekcji 3, katalogi src/test/resources, surefire z
   `excludedGroups=cwiczenie`), Maven Wrapper, uzupełnij `.gitignore` o `temp/`, `target/`, `.idea/`.
3. Klasa startowa `SpringLearningApplication` (pakiet główny ponad `sNN_`) — ustal, jak lekcje-aplikacje się uruchamiają
   (osobna klasa `@SpringBootApplication` na lekcję albo profile) i opisz to w README.
4. Lekcja `s00_start` (jak korzystać z kursu, uruchamianie testów, pliki .http, ćwiczenia z tagiem) + jej testy, `mvnw test` zielony.
5. Commit `s00_start: …` i aktualizacja spisu treści w README (⏳ → ✅).
6. Konspekty kolejnych działów zapisuj w `temp/ASSIGNMENTS.md` przed zleceniem ich agentom.
