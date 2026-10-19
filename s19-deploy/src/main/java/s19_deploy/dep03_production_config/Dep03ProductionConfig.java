package s19_deploy.dep03_production_config;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Konfiguracja produkcyjna — ustawienia z zewnątrz, zmienne środowiska, profil prod i sekrety
 *        (production = produkcja; secret = sekret, czyli hasło lub klucz; environment variable = zmienna środowiska;
 *        profile = profil; fail fast = „szybka porażka”: błąd od razu przy starcie, nie w połowie pracy)
 *
 * W SKRÓCIE:
 *   Ten sam JAR (Dep01ExecutableJar) działa na laptopie, w testach i na produkcji — różni je TYLKO konfiguracja
 *   podana z zewnątrz. Zasady: (1) w repozytorium i w JAR-ze trzymasz wartości domyślne i bezpieczne (adresy, limity);
 *   (2) wszystko, co zależy od środowiska, przychodzi z zewnątrz (zmienne środowiska, argumenty, plik obok JAR-a);
 *   (3) SEKRETY (hasła, klucze) nigdy nie trafiają do repozytorium ani do obrazu — tylko z zewnątrz;
 *   (4) aplikacja bez wymaganego ustawienia nie startuje, zamiast działać „na pół gwizdka”.
 *
 * ANALOGIA: hotel i klucze do pokoi.
 *   Budynek (JAR) jest taki sam dla wszystkich gości. Klucz do pokoju (sekret) wydaje recepcja (platforma:
 *   zmienna środowiska, menedżer sekretów) — nie jest przymocowany do drzwi (plik w repozytorium). Gdy gość nie ma
 *   klucza, drzwi się nie otwierają od razu (fail fast), a nie dopiero w środku nocy.
 *
 * JAK TO DZIAŁA:
 *   Priorytet źródeł ustawień (od NAJWYŻSZEGO; skrót — pełna lista w dokumentacji i w s02_configuration/Cfg01Properties):
 *     1. argumenty z linii poleceń               java -jar app.jar --shop.payment.timeout=9s
 *     2. właściwości systemowe JVM               java -Dshop.payment.timeout=9s -jar app.jar
 *     3. zmienne środowiska systemu              SHOP_PAYMENT_TIMEOUT=7s
 *     4. pliki application-{profil}.properties   na zewnątrz JAR-a, potem w JAR-ze
 *     5. pliki application.properties            na zewnątrz JAR-a (./config/, ./), potem w JAR-ze
 *   Wyższe źródło wygrywa z niższym. Dzięki temu wartość z pliku w JAR-ze jest tylko „wartością domyślną”,
 *   a operator zmienia ją zmienną środowiska bez przebudowy. Wynik tego priorytetu sprawdza test lekcji (sekcja 2).
 *
 * SŁÓWKA:
 *   relaxed binding = luźne wiązanie (jedno ustawienie można zapisać na kilka sposobów); import = dołączenie
 *   pliku konfiguracji; optional = opcjonalny (brak pliku nie jest błędem); config tree = drzewo plików
 *   (katalog, w którym nazwa pliku to klucz, a zawartość to wartość); secret manager = menedżer sekretów;
 *   mask = zamaskować (ukryć wartość gwiazdkami); placeholder = symbol zastępczy (${...}); debug = tryb diagnostyczny.
 *
 * ZOBACZ TEŻ: s02_configuration/Cfg05Environment (nazwy zmiennych środowiska), s02_configuration/Cfg03Profiles (profile),
 *             s02_configuration/Cfg02ConfigurationProperties (rekordy z @ConfigurationProperties),
 *             s19_deploy/Dep02Docker (zmienne środowiska w kontenerze).
 * </pre>
 */
public final class Dep03ProductionConfig {

    private Dep03ProductionConfig() {
    }

    // =================================================================================================
    // 1. ZMIENNE ŚRODOWISKA — SHOP_PAYMENT_APIKEY ⇄ klucz shop.payment.api-key
    // =================================================================================================
    //
    // Lekcja ma rekord PaymentSettings (@ConfigurationProperties("shop.payment")) z polami url, apiKey i timeout.
    // Pole apiKey to klucz shop.payment.api-key. Zmienna środowiska ma wielkie litery i podkreślniki, więc Boot stosuje
    // regułę (relaxed binding): kropki → podkreślniki, myślniki USUŃ, wielkie litery:
    //     shop.payment.api-key   →   SHOP_PAYMENT_APIKEY
    // Starszy zapis z podkreślnikiem w miejscu myślnika też działa: SHOP_PAYMENT_API_KEY. Test sekcji 1 sprawdza OBA
    // (prawdziwy wynik: apiKey wiązany w obu przypadkach).
    //
    // Przykłady w praktyce:
    //     Windows (PowerShell):   $env:SHOP_PAYMENT_APIKEY = "..."; java -jar app.jar
    //     Linux / Git Bash:       SHOP_PAYMENT_APIKEY=... java -jar app.jar
    //     Docker:                 docker run -e SHOP_PAYMENT_APIKEY=... -p 8080:8080 obraz
    // Sprawdzone uruchomieniem prawdziwego JAR-a: zmienna SHOP_NAME="Z env" zmieniła odpowiedź /api/shop z Dep01
    // (a argument --shop.greeting=Hej zadziałał obok niej).
    //
    // PUŁAPKA: test, który podstawia „sztuczne” zmienne środowiska pod własną nazwą źródła (np. "sztuczne-env").
    //   Dlaczego? Reguły nazw zmiennych Boot stosuje tylko do źródła o nazwie "systemEnvironment" (stała
    //   StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME); przy innej nazwie SHOP_PAYMENT_APIKEY
    //   „nie działa” w teście, choć w prawdziwym systemie działa. Testy tej lekcji używają prawdziwej nazwy
    //   (szczegóły i dowód: s02_configuration/Cfg05Environment, sekcja 3).
    //
    // DOBRA PRAKTYKA: ustawienia środowiskowe (adresy baz, klucze) podawaj zmiennymi środowiska, a w pliku trzymaj
    //   tylko wartości domyślne. Dlaczego? Zmienne ustawia platforma (Docker, Kubernetes, chmura) bez dotykania
    //   JAR-a czy obrazu — jeden artefakt, wiele środowisk.

    // =================================================================================================
    // 2. PRIORYTET: plik profilu < zmienna środowiska < argument
    // =================================================================================================
    //
    // Profil prod (plik s19-deploy/src/main/resources/application-prod.properties) ustawia timeout=3s.
    // Prawdziwy wynik testu sekcji 2 (ta sama aplikacja, trzy uruchomienia):
    //     tylko profil prod:                                 timeout = PT3S
    //     + zmienna SHOP_PAYMENT_TIMEOUT=7s:                 timeout = PT7S   (zmienna wygrała z plikiem)
    //     + argument --shop.payment.timeout=9s:              timeout = PT9S   (argument wygrał ze zmienną)
    //
    // Profil prod włączasz: --spring.profiles.active=prod albo zmienną SPRING_PROFILES_ACTIVE=prod. Plik profilu
    // zawiera tylko to, co bezpieczne w repozytorium: adres bramki, limit czasu, poziom logów (root=WARN),
    // wystawienie wyłącznie /actuator/health bez szczegółów (management.endpoint.health.show-details=never).
    //
    // PUŁAPKA: zostawienie na produkcji szerokiego wystawienia Actuatora (management.endpoints.web.exposure.include=*)
    //   albo szczegółów zdrowia (show-details=always). Dlaczego? Punkty beans, mappings czy heapdump zdradzają budowę aplikacji,
    //   a heapdump nawet zawartość pamięci (z hasłami). Na produkcji wystawiaj tylko to, co potrzebne
    //   (health), i zabezpiecz resztę (s12_security). Domyślnie w Boot 4.1 przez HTTP jest tylko health (sprawdzone).

    // =================================================================================================
    // 3. PLIKI POZA JAR-em: ./config, spring.config.import, katalog sekretów
    // =================================================================================================
    //
    // (a) Plik obok JAR-a. Boot sam czyta application.properties z katalogu ./config/ i z bieżącego katalogu (obok
    //     miejsca, w którym uruchomiono java -jar) — z wyższym priorytetem niż plik w JAR-ze. Sprawdzone uruchomieniem:
    //     config/application.properties z linią shop.name=Z pliku config zmienił odpowiedź /api/shop na
    //     {"name":"Z pliku config","greeting":"Witamy"} bez przebudowy JAR-a.
    // (b) Jawny import: --spring.config.import=optional:file:/etc/sklep/secrets.properties
    //     „optional:” — brak pliku NIE jest błędem. Bez „optional:” brak pliku przerywa start. Prawdziwy komunikat
    //     (test sekcji 3, ConfigDataResourceNotFoundException):
    //       Config data resource 'file [...\brak.properties]' via location 'file:...\brak.properties' cannot be found
    // (c) Katalog sekretów (configtree) — tak Kubernetes montuje Secret jako pliki: --spring.config.import=
    //     optional:configtree:/run/secrets/ . Plik /run/secrets/shop/payment/api-key z zawartością „tajny” daje
    //     właściwość shop.payment.api-key=tajny (nazwa katalogów i pliku → klucz po kropkach). Test sekcji 3 to potwierdza.
    //
    // DOBRA PRAKTYKA: sekrety dostarczaj z zewnątrz w jeden z trzech sposobów: zmienna środowiska, plik spoza JAR-a
    //   (z uprawnieniami tylko dla użytkownika aplikacji) albo katalog sekretów z menedżera sekretów (Kubernetes Secret,
    //   Vault, usługi chmurowe). Dlaczego? Wszystko, co jest w repozytorium lub w obrazie, widzi każdy, kto
    //   ma dostęp do kodu lub rejestru obrazów — także po wycofaniu zmiany (historia git zostaje na zawsze).
    //
    // PUŁAPKA: sekret wpisany do application.properties „tylko na chwilę”. Dlaczego? Wystarczy jeden commit —
    //   klucz jest w historii repozytorium na zawsze i trzeba go unieważnić u dostawcy (rotacja). Pliki z lokalnymi
    //   sekretami dodaj do .gitignore, a w repozytorium trzymaj co najwyżej przykład z fałszywą wartością.

    // =================================================================================================
    // 4. FAIL FAST — brak wymaganego ustawienia = aplikacja nie startuje
    // =================================================================================================
    //
    // Rekord PaymentSettings sprawdza w konstruktorze, że url i apiKey są podane. Bez klucza start kończy się wyjątkiem
    // (prawdziwy łańcuch przyczyn z testu sekcji 4, od zewnętrznego):
    //     UnsatisfiedDependencyException ... → ConfigurationPropertiesBindException → BindException
    //     → BeanInstantiationException: Constructor threw exception
    //     → IllegalArgumentException: Brak ustawienia shop.payment.api-key (klucz API bramki płatności) — podaj je ...
    // (test sprawdza PRZYCZYNĘ ŹRÓDŁOWĄ, bo opakowujące wyjątki zależą od tego, który bean powstaje pierwszy).
    //
    // DOBRA PRAKTYKA: wymagane ustawienia waliduj przy starcie (konstruktor rekordu albo Bean Validation w s05).
    //   Dlaczego? Błąd w pierwszej sekundzie po wdrożeniu (kontener się restartuje, Kubernetes nie włącza ruchu)
    //   jest tańszy niż pierwsze płatności odrzucane w środku nocy przez pusty klucz.

    // =================================================================================================
    // 5. SEKRETY W LOGACH I ODPOWIEDZIACH
    // =================================================================================================
    //
    // Rekord w Javie ma automatyczny toString, który wypisuje WSZYSTKIE pola — także klucz API. Wystarczy
    // log.info("Ustawienia: {}", settings), żeby sekret trafił do logów (a stamtąd do narzędzi, kopii zapasowych,
    // zgłoszeń błędów). PaymentSettings nadpisuje toString i udostępnia maskedApiKey(): dla klucza
    // „sekretny-klucz-123” widać „****************23”. Test sekcji 5 sprawdza toString i odpowiedź
    // GET /api/payment-settings (Dep03ProductionConfigWebTest).
    //
    // PUŁAPKA: logowanie całego rekordu z konfiguracją, mapy zmiennych środowiska albo adresu z hasłem
    //   (jdbc:postgresql://host/baza?password=…). Dlaczego? Logi czyta wielu ludzi i wiele systemów; sekret
    //   w logu jest wyciekiem nawet bez ataku.
    //
    // WSKAZÓWKA: punkt /actuator/env i /actuator/configprops domyślnie NIE pokazują wartości (właściwość
    // management.endpoint.env.show-values ma domyślnie wartość never — sprawdzone w metadanych Boota 4.1.1), a
    // /actuator/env nie jest nawet wystawiony przez HTTP (domyślnie tylko health; sprawdzone: odpowiedź 404).
    // W starszych tutorialach (Boot 2.x) wartości były widoczne z maskowaniem po nazwie klucza — to się zmieniło.
    // Nie traktuj tego jako powodu do wystawiania env: to ostatnia linia obrony.

    // =================================================================================================
    // 6. --debug i inne przełączniki diagnostyczne
    // =================================================================================================
    //
    // Argument --debug (albo debug=true w ustawieniach) włącza raport „CONDITIONS EVALUATION REPORT” (raport oceny
    // warunków autokonfiguracji — s03_boot_basics/Boot01AutoConfiguration) i podnosi poziom logów wybranych bibliotek.
    // Test sekcji 6 pokazuje, że raport pojawia się w konsoli tylko z --debug.
    //
    // PUŁAPKA: debug=true w pliku, który trafia na produkcję. Dlaczego? Ogromne logi (spowolnienie, koszt
    //   magazynowania) i ujawnianie szczegółów konfiguracji. Włączaj tryb diagnostyczny argumentem na czas
    //   badania problemu; w plikach profilu prod nie ma klucza debug (test sekcji 3 to sprawdza).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Jeden JAR, konfiguracja z zewnątrz. Priorytet: argumenty > -D > zmienne środowiska > pliki profilu > pliki.
     *   • Zmienna środowiska: kropki → _, myślniki usunąć, WIELKIE: shop.payment.api-key → SHOP_PAYMENT_APIKEY
     *     (działa też SHOP_PAYMENT_API_KEY). Testy podstawiają źródło o nazwie "systemEnvironment".
     *   • Profil: --spring.profiles.active=prod albo SPRING_PROFILES_ACTIVE=prod; plik application-prod.properties.
     *   • Pliki poza JAR-em: ./config/application.properties, --spring.config.import=optional:file:…,
     *     optional:configtree:/run/secrets/. Bez „optional:” brak pliku przerywa start.
     *   • Sekrety NIGDY w repozytorium ani w obrazie; z zewnątrz; nie w logach (toString rekordu wypisuje wszystko!).
     *   • Wymagane ustawienia waliduj przy starcie (fail fast). Na produkcji: bez debug, Actuator tylko health.
     *
     * PYTANIA KONTROLNE:
     *   1. Jak brzmi nazwa zmiennej środowiska dla klucza {@code shop.payment.api-key} i czy zadziała zapis z _ w środku?
     *   2. Co się stanie? W application-prod.properties jest {@code shop.payment.timeout=3s}, a uruchamiasz aplikację
     *      z zmienną SHOP_PAYMENT_TIMEOUT=7s i argumentem {@code --shop.payment.timeout=9s} — jaki będzie timeout?
     *   3. Co się stanie? Uruchamiasz aplikację z {@code --spring.config.import=file:/brak/pliku.properties}
     *      (bez słowa optional:), a pliku nie ma.
     *   4. ZNAJDŹ BŁĄD: w repozytorium leży application.properties z linią {@code shop.payment.api-key=prawdziwy-klucz}
     *      „bo w repozytorium prywatnym”. Co jest złego, mimo że repozytorium jest prywatne?
     *   5. ZNAJDŹ BŁĄD: w teście ustawiasz sztuczne zmienne środowiska jako SystemEnvironmentPropertySource
     *      o nazwie "moje-zmienne" i zmienna SHOP_PAYMENT_APIKEY „nie działa”. Dlaczego?
     *   6. Dlaczego rekord z ustawieniami nadpisuje toString?
     *   7. Po co sprawdzać wymagane ustawienia w konstruktorze rekordu zamiast czekać na pierwsze użycie?
     *   8. Co robi {@code optional:configtree:/run/secrets/} i skąd się tam biorą pliki w Kubernetesie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Dep03ProductionConfigExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zamień klucz ustawienia na nazwę zmiennej środowiska według reguły z dokumentacji
     * Boota: kropki → podkreślniki, myślniki usuń, wielkie litery.
     * Przykład: "shop.payment.api-key" → "SHOP_PAYMENT_APIKEY"; "server.port" → "SERVER_PORT".
     * Podpowiedź: replace(), toUpperCase() — pamiętaj o Locale.ROOT (t04_strings/Strings01Basics).
     */
    static String exercise1(String propertyKey) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zamaskuj sekrety przed wypisaniem do logu. Dla kluczy zawierających (bez względu na
     * wielkość liter) jedno ze słów: password, secret, token, key — wartość zamień na "******". Pozostałe wartości
     * zostaw. Zwróć NOWĄ mapę posortowaną po kluczach (oryginału nie zmieniaj).
     * Przykład: {"shop.payment.api-key": "abc", "server.port": "8080"} → {"server.port": "8080", "shop.payment.api-key": "******"}.
     * Podpowiedź: TreeMap, toLowerCase(Locale.ROOT), contains().
     */
    static Map<String, String> exercise2(Map<String, String> properties) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): „strażnik produkcji” — sprawdź mapę ustawień przeznaczonych na produkcję i zwróć listę
     * problemów (w tej kolejności; pusta = wszystko dobrze):
     *   1. "DEBUG_WLACZONY"                   — klucz debug ma wartość true;
     *   2. "SEKRET_W_PLIKU:" + klucz          — klucz z słowem password/secret/token/key (jak w ćwiczeniu 2), którego
     *                                            wartość jest niepusta i NIE jest symbolem zastępczym ${...}; klucze
     *                                            posortowane alfabetycznie;
     *   3. "PROFIL_PROD_NIEAKTYWNY"           — spring.profiles.active nie zawiera słowa prod (brak klucza też);
     *   4. "SZCZEGOLY_ZDROWIA_PUBLICZNE"      — management.endpoint.health.show-details ma wartość always.
     * Podpowiedź: wartość ${DB_PASSWORD} jest dobra (sekret przyjdzie ze zmiennej), a "tajne123" — błędem.
     */
    static List<String> exercise3(Map<String, String> properties) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. SHOP_PAYMENT_APIKEY (kropki → _, myślnik usunięty, wielkie litery). Zapis SHOP_PAYMENT_API_KEY też działa.
     *      Sprawdzone testem sekcji 1.
     *   2. PT9S, czyli 9 sekund: argument z linii poleceń ma najwyższy priorytet, potem zmienna środowiska (7s), a plik
     *      profilu (3s) najniższy. Sprawdzone testem sekcji 2.
     *   3. Start się przerwie wyjątkiem ConfigDataResourceNotFoundException („Config data resource 'file […]' …
     *      cannot be found”). Z „optional:” aplikacja ruszyłaby bez tego pliku. Sprawdzone testem sekcji 3.
     *   4. Sekret trafia do historii repozytorium, kopii zapasowych, forków i klonów — widzi go każdy z dostępem do kodu
     *      (dziś i w przyszłości) i nie da się go „cofnąć” zwykłym commitem; trzeba go unieważnić u dostawcy.
     *      Prywatność repozytorium nie zmienia tego, że sekret jest rozproszony po wielu miejscach.
     *   5. Boot stosuje reguły nazw zmiennych tylko do źródła o nazwie "systemEnvironment"; źródło o innej nazwie
     *      traktuje klucze dosłownie. Użyj StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME.
     *      (Dowód: s02_configuration/Cfg05Environment.)
     *   6. Domyślny toString rekordu wypisuje wszystkie pola, więc klucz API trafiłby do logów przy wypisaniu rekordu.
     *      Sprawdzone testem sekcji 5.
     *   7. Aplikacja zgłasza błąd w pierwszej sekundzie po starcie (a platforma nie włącza do niej ruchu), zamiast
     *      działać pozornie poprawnie i zawieść dopiero przy pierwszej płatności.
     *   8. Importuje ustawienia z katalogu, w którym nazwa pliku (z katalogami po kropkach) jest kluczem, a zawartość
     *      wartością. Kubernetes montuje Secret jako katalog plików (jeden plik na klucz Secretu). Sprawdzone
     *      testem sekcji 3 na tymczasowym katalogu.
     */
    // </editor-fold>
}
