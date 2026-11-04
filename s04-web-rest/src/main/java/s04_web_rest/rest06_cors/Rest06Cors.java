package s04_web_rest.rest06_cors;

/**
 * <pre>
 * TEMAT: CORS — kiedy przeglądarka blokuje zapytania do API z innej domeny i jak na to pozwolić
 *        (CORS — Cross-Origin Resource Sharing = współdzielenie zasobów między różnymi pochodzeniami;
 *         origin = pochodzenie: schemat + host + port)
 *
 * W SKRÓCIE:
 *   Przeglądarka pilnuje, żeby skrypt ze strony A nie czytał po cichu odpowiedzi z serwera B. To polityka tego samego
 *   pochodzenia (same-origin policy). Gdy frontend sklepu (np. http://localhost:3000) woła nasze API
 *   (http://localhost:8080), to są DWA różne pochodzenia, więc przeglądarka pyta serwer: „czy pozwalasz?”.
 *   Serwer odpowiada nagłówkami Access-Control-Allow-*. W Springu ustawiasz je adnotacją @CrossOrigin albo globalnie
 *   w WebMvcConfigurer.addCorsMappings. CORS NIE jest zabezpieczeniem serwera — chroni użytkownika przeglądarki.
 *
 * ANALOGIA: bramkarz w klubie, który pracuje dla GOŚCIA, a nie dla klubu.
 *   Twój znajomy (skrypt na stronie) chce w Twoim imieniu wejść do klubu (API) i przynieść Ci, co tam jest. Bramkarz
 *   (przeglądarka) najpierw dzwoni do klubu: „czy wpuszczacie ludzi z firmy X?” (preflight). Klub mówi „tak, firma X —
 *   tak; metodą GET i POST — tak” (nagłówki Access-Control-Allow-*). Kto przychodzi BEZ bramkarza (curl, Postman,
 *   inny serwer), wchodzi tak czy inaczej — bramkarz chroni tylko tych, którzy z niego korzystają.
 *
 * JAK TO DZIAŁA:
 *   1. Skrypt ze strony http://localhost:3000 wysyła fetch("http://localhost:8080/rest06/api/products").
 *   2. Przeglądarka dokłada nagłówek Origin: http://localhost:3000 (skrypt nie może go podrobić).
 *   3. Zapytanie „nie-proste” (PUT, DELETE, JSON, własny nagłówek) poprzedza zapytanie wstępne — PREFLIGHT:
 *      OPTIONS z nagłówkami Origin, Access-Control-Request-Method, Access-Control-Request-Headers.
 *   4. Spring MVC: HandlerMapping (mapowanie zapytań na metody) znajduje metodę kontrolera i jej regułę CORS
 *      (z @CrossOrigin albo z CorsRegistry). DefaultCorsProcessor (domyślny procesor CORS) porównuje Origin, metodę
 *      i nagłówki z regułą. Pasuje → dopisuje Access-Control-Allow-Origin itd.; nie pasuje → 403 „Invalid CORS request”.
 *   5. Przeglądarka czyta nagłówki odpowiedzi i DOPIERO WTEDY decyduje, czy wysłać właściwe zapytanie i czy pokazać
 *      skryptowi odpowiedź.
 *
 * SŁÓWKA:
 *   origin = pochodzenie; same-origin policy = polityka tego samego pochodzenia; cross-origin = między pochodzeniami;
 *   preflight = zapytanie wstępne („rozpoznanie przed lotem”); allow = pozwalać; expose = ujawniać;
 *   credentials = poświadczenia (ciasteczka, nagłówek Authorization); max age = maksymalny wiek (czas pamiętania);
 *   pattern = wzorzec; registry = rejestr; frontend = część aplikacji działająca w przeglądarce; invalid = nieprawidłowy.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (DispatcherServlet i HandlerMapping, 403/405),
 *             s04_web_rest/Rest03Responses (nagłówki odpowiedzi, własny X-Total-Count),
 *             s04_web_rest/Rest05CrudApi (API, które zwykle woła frontend z innego pochodzenia),
 *             s12_security/Sec04StatelessApi (API z Spring Security — tam CORS konfiguruje się w łańcuchu filtrów),
 *             s12_security/Sec01Basics (czym jest łańcuch filtrów bezpieczeństwa).
 * </pre>
 */
public final class Rest06Cors {

    private Rest06Cors() {
    }

    // =================================================================================================
    // 1. POLITYKA TEGO SAMEGO POCHODZENIA — CO TO JEST ORIGIN I KTO BLOKUJE (PlainController)
    // =================================================================================================
    //
    // Origin (pochodzenie) = SCHEMAT + HOST + PORT. Ścieżka się nie liczy.
    //
    //   Adres A                         | Adres B                          | To samo pochodzenie?
    //   --------------------------------+----------------------------------+------------------------------------------
    //   https://sklep.example.com/a     | https://sklep.example.com/b?x=1  | TAK (różni się tylko ścieżka)
    //   http://sklep.example.com        | https://sklep.example.com        | NIE (inny schemat: http i https)
    //   http://localhost:3000           | http://localhost:8080            | NIE (inny port — typowy frontend + API!)
    //   https://sklep.example.com       | https://api.sklep.example.com    | NIE (inny host — poddomena to inny host)
    //
    // Kto blokuje? PRZEGLĄDARKA. Serwer dostaje zapytanie i odpowiada jak zwykle; to przeglądarka nie pokazuje odpowiedzi
    // skryptowi, jeśli w odpowiedzi brak zgody (Access-Control-Allow-Origin). Test serverDoesNotBlock (uruchomiony):
    //   • GET /rest06/plain/hello bez nagłówka Origin (tak pyta curl i IntelliJ HTTP Client) → 200 „cześć”;
    //   • ten sam GET z Origin: https://evil.example.com → TEŻ 200 „cześć” — kontroler się wykonał! — tylko bez nagłówka
    //     Access-Control-Allow-Origin. Przeglądarka dostałaby dane, ale nie pokaże ich skryptowi (w konsoli: błąd CORS).
    // Test sameOriginIsNotCors: Origin równy adresowi serwera (http://localhost w MockMvc) to NIE jest zapytanie CORS —
    // Spring nie sprawdza reguł i odpowiada 200, choć http://localhost nie jest na liście dozwolonych pochodzeń.
    //
    // PUŁAPKA: „w Postmanie/curl działa, a w przeglądarce błąd CORS, więc serwer jest zepsuty”. Dlaczego to złe wnioskowanie?
    //   Narzędzia spoza przeglądarki w ogóle nie stosują polityki tego samego pochodzenia. Błąd CORS to brak ZGODY
    //   w nagłówkach odpowiedzi, a nie awaria endpointu. Szukaj w konfiguracji CORS, nie w logice kontrolera.
    //
    // PUŁAPKA: traktowanie CORS jak zabezpieczenia API. Dlaczego groźne? Każdy, kto nie używa przeglądarki (skrypt, bot,
    //   inny serwer), ominie CORS — po prostu nie wyśle nagłówka Origin. Dostęp do danych chroni uwierzytelnianie
    //   i autoryzacja (dział s12_security), a CORS chroni UŻYTKOWNIKA przed cudzą stroną, która działa w jego przeglądarce
    //   (z jego ciasteczkami). PYTANIE REKRUTACYJNE: „Czy CORS chroni serwer przed atakami?” — Nie; chroni użytkownika
    //   przeglądarki, a o tym, kto dostaje dane, decyduje uwierzytelnianie i autoryzacja.

    // =================================================================================================
    // 2. ZAPYTANIE PROSTE KONTRA PREFLIGHT (ProductApiController)
    // =================================================================================================
    //
    // Zapytanie PROSTE (simple request): GET, HEAD albo POST, tylko „zwykłe” nagłówki, a Content-Type co najwyżej
    // text/plain, application/x-www-form-urlencoded albo multipart/form-data. Przeglądarka wysyła je od razu (z Origin)
    // i patrzy na Access-Control-Allow-Origin w odpowiedzi.
    //
    // Każde inne (PUT, DELETE, PATCH, POST z application/json, własny nagłówek, np. X-Shop-Client) poprzedza PREFLIGHT:
    //
    //   OPTIONS /rest06/api/products/1                 ← przeglądarka pyta „czy mogę?”
    //   Origin: http://localhost:3000
    //   Access-Control-Request-Method: PUT
    //   Access-Control-Request-Headers: Content-Type
    //
    //   200 OK                                          ← serwer: „tak, pod tymi warunkami”
    //   Access-Control-Allow-Origin: http://localhost:3000
    //   Access-Control-Allow-Methods: GET,POST,PUT
    //   Access-Control-Allow-Headers: Content-Type
    //   Access-Control-Allow-Credentials: true
    //   Access-Control-Max-Age: 3600                    ← przez godzinę nie pytaj ponownie
    //
    // Dopiero po takiej odpowiedzi przeglądarka wysyła właściwe PUT. Test preflightAllowed (uruchomiony) sprawdza
    // dokładnie te nagłówki oraz to, że ciało jest puste — preflight obsługuje Spring, metoda update() się NIE wykonuje.
    // Test simpleRequestFromAllowedOrigin: GET z dozwolonego pochodzenia → 200, Access-Control-Allow-Origin z KONKRETNYM
    // adresem, Access-Control-Allow-Credentials: true i Access-Control-Expose-Headers: X-Total-Count.
    // (Spring dokłada też nagłówek Vary: Origin, … — informacja dla pamięci podręcznych, że odpowiedź zależy od Origin.)
    //
    // Exposed headers (ujawnione nagłówki). Skrypt z innego pochodzenia widzi tylko kilka „bezpiecznych” nagłówków
    // odpowiedzi (np. Content-Type). Własny X-Total-Count z Rest03Responses zobaczy dopiero wtedy, gdy serwer go ujawni:
    // exposedHeaders("X-Total-Count") → Access-Control-Expose-Headers: X-Total-Count.
    //
    // DOBRA PRAKTYKA: ustaw maxAge (np. 3600 s). Dlaczego? Bez niego przeglądarka może wysyłać preflight przed wieloma
    //   zapytaniami — podwaja to liczbę zapytań i opóźnienie. (Przeglądarki mają też własne górne limity tego czasu.)
    //
    // PUŁAPKA: test MockMvc „bez Origin” przechodzi, a frontend i tak dostaje błąd CORS. Dlaczego? Bez nagłówka Origin
    //   Spring w ogóle nie stosuje reguł CORS. Testy CORS muszą wysyłać Origin (i przy preflight
    //   Access-Control-Request-Method) — tak jak testy tej lekcji.

    // =================================================================================================
    // 3. @CrossOrigin I GLOBALNIE WebMvcConfigurer.addCorsMappings (PartnerController, CorsConfig)
    // =================================================================================================
    //
    // a) Globalnie — jedno miejsce dla całego API (CorsConfig):
    //
    //   @Configuration
    //   public class CorsConfig implements WebMvcConfigurer {
    //       public void addCorsMappings(CorsRegistry registry) {
    //           registry.addMapping("/rest06/api/**")
    //                   .allowedOrigins("http://localhost:3000", "https://sklep.example.com")
    //                   .allowedMethods("GET", "POST", "PUT")
    //                   .allowedHeaders("Content-Type", "X-Shop-Client")
    //                   .exposedHeaders("X-Total-Count")
    //                   .allowCredentials(true)
    //                   .maxAge(3600);
    //       }
    //   }
    //
    // b) Adnotacją przy kontrolerze (PartnerController):
    //
    //   @CrossOrigin(origins = "https://partner.example.com", maxAge = 600)   ← na klasie: dla wszystkich metod
    //   public class PartnerController {
    //       @GetMapping("/news")
    //       @CrossOrigin(origins = "https://blog.example.com")                 ← na metodzie: DOKŁADA do reguły z klasy
    //       public String news() { … }
    //
    // Co sprawdziły testy (uruchomione):
    //   • preflightRejected: obce pochodzenie, metoda DELETE (nie ma jej w allowedMethods) albo nagłówek Authorization
    //     (nie ma go w allowedHeaders) → preflight 403 Forbidden z ciałem „Invalid CORS request” i bez
    //     Access-Control-Allow-Origin.
    //   • actualRequestRejected: gdy ścieżka MA regułę CORS, Spring sprawdza też WŁAŚCIWE zapytanie: GET z obcym Origin
    //     → 403 i kontroler się nie wykonuje; DELETE z dozwolonego pochodzenia → 403. (Porównaj z sekcją 1: ścieżka BEZ
    //     reguły odpowiada 200 bez nagłówków zgody.)
    //   • crossOriginAnnotation: /offers — tylko partner.example.com (blog → 403); /news — oba pochodzenia (reguły z klasy
    //     i metody się łączą); preflight dla /offers: Access-Control-Allow-Methods: GET (domyślnie @CrossOrigin pozwala
    //     na metody z mapowania), Access-Control-Max-Age: 600; bez allowCredentials nie ma nagłówka Allow-Credentials.
    //
    //   Sposób                         | Zalety                                   | Wady
    //   -------------------------------+------------------------------------------+-------------------------------------
    //   @CrossOrigin na klasie/metodzie | reguła widoczna przy kodzie endpointu   | rozsiana po kontrolerach, łatwo o różnice
    //   WebMvcConfigurer.addCorsMappings| jedno miejsce, wzorce ścieżek (/api/**) | trzeba pamiętać, że istnieje
    //
    // DOBRA PRAKTYKA: dla całego API — jedna globalna reguła z DOKŁADNĄ listą pochodzeń (najlepiej z ustawień, np.
    //   właściwości rest06.… różne dla dev i produkcji). Dlaczego? Lista zaufanych frontendów to decyzja o bezpieczeństwie
    //   użytkowników — powinna być w jednym miejscu, łatwa do przejrzenia, a nie rozsiana po adnotacjach.
    //
    // PUŁAPKA: @CrossOrigin bez parametrów. Dlaczego ryzykowne? Pozwala WSZYSTKIM pochodzeniom („*”) — każda strona
    //   w internecie może z przeglądarki użytkownika czytać odpowiedzi tego endpointu. Zawsze podawaj origins.
    //
    // Pod maską: obie drogi kończą się obiektem CorsConfiguration (konfiguracja CORS) przypiętym do ścieżki/metody.
    // Te same obiekty można zbudować ręcznie i podać filtrowi CorsFilter przez CorsConfigurationSource (źródło
    // konfiguracji CORS) — to robisz w ćwiczeniu 5 i tego samego mechanizmu używa Spring Security (sekcja 5).

    // =================================================================================================
    // 4. PUŁAPKA: allowedOrigins("*") + allowCredentials(true) (Rest06CredentialsPitfallTest)
    // =================================================================================================
    //
    // Standard CORS zabrania odpowiedzi Access-Control-Allow-Origin: * razem z Access-Control-Allow-Credentials: true
    // (inaczej KAŻDA strona mogłaby czytać dane zalogowanego użytkownika). Spring pilnuje tego sam i rzuca
    // IllegalArgumentException z komunikatem (sprawdzone testem validationMessage):
    //   When allowCredentials is true, allowedOrigins cannot contain the special value "*" since that cannot be set on
    //   the "Access-Control-Allow-Origin" response header. To allow credentials to a set of origins, list them explicitly
    //   or consider using "allowedOriginPatterns" instead.
    //
    // KIEDY wybucha? To zależy od sposobu konfiguracji (sprawdzone testami):
    //   • @CrossOrigin(origins = "*", allowCredentials = "true") → aplikacja NIE STARTUJE (błąd tworzenia beana
    //     requestMappingHandlerMapping) — test annotationFailsAtStartup;
    //   • addCorsMappings z allowedOrigins("*").allowCredentials(true) → start się UDAJE, a wyjątek leci przy KAŻDYM
    //     zapytaniu na objętą ścieżkę (także bez Origin; na prawdziwym serwerze klient dostaje 500) —
    //     test globalMappingFailsAtRequestTime. Gorszy przypadek: błąd wychodzi dopiero „na produkcji”.
    //
    // Naprawa: wypisz pochodzenia dokładnie (najlepiej) albo użyj WZORCÓW: allowedOriginPatterns("https://*.example.com").
    // Z wzorcem Spring odsyła KONKRETNE pochodzenie z zapytania, nie „*”, więc standard jest spełniony
    // (test originPatternsFix: Access-Control-Allow-Origin: https://a.example.com + Allow-Credentials: true).
    //
    // PUŁAPKA: allowedOriginPatterns("*") „żeby działało”. Dlaczego złe? Technicznie przechodzi walidację, ale
    //   z poświadczeniami oznacza: każda strona w internecie może czytać dane zalogowanego użytkownika. To dokładnie to,
    //   przed czym CORS miał chronić. Wzorzec zawężaj do własnych domen (https://*.sklep.example.com).
    //
    // Stare tutoriale: do Spring 5.2 przy allowCredentials(true) można było podać "*" (Spring odsyłał wtedy pochodzenie
    // z zapytania). Od Spring 5.3 to błąd, a do „gwiazdek” służy allowedOriginPatterns — w Spring 7 tak samo.

    // =================================================================================================
    // 5. ZAPOWIEDŹ: CORS ZE SPRING SECURITY
    // =================================================================================================
    //
    // Gdy w dziale s12_security dojdzie spring-boot-starter-security, zapytania przechodzą najpierw przez łańcuch filtrów
    // bezpieczeństwa — PRZED DispatcherServletem i regułami z @CrossOrigin/CorsRegistry. Preflight nie niesie danych
    // logowania, więc jeśli Security nie obsłuży CORS sam, odrzuci preflight jako niezalogowany (401/403), zanim Spring MVC
    // w ogóle zobaczy regułę CORS. Dlatego tam CORS włącza się w konfiguracji Security (http.cors(...)), najczęściej
    // z beanem CorsConfigurationSource — tym samym typem, który budujesz w ćwiczeniu 5. Szczegóły:
    // s12_security/Sec04StatelessApi.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Origin = schemat + host + port; inny port (localhost:3000 kontra :8080) = inne pochodzenie.
     *   • Blokuje PRZEGLĄDARKA, nie serwer; curl/HTTP Client/Postman nie stosują CORS. CORS to nie uwierzytelnianie.
     *   • Proste zapytanie: od razu + Origin. Inne (PUT/DELETE/JSON/własne nagłówki): najpierw preflight OPTIONS
     *     z Origin, Access-Control-Request-Method, Access-Control-Request-Headers.
     *   • Odpowiedź: Access-Control-Allow-Origin/-Methods/-Headers/-Credentials, -Expose-Headers, -Max-Age.
     *   • Ścieżka z regułą, zapytanie spoza reguły → 403 „Invalid CORS request”; ścieżka bez reguły → zwykła odpowiedź
     *     bez nagłówków zgody (przeglądarka ją ukryje).
     *   • @CrossOrigin(origins = …) na klasie/metodzie (reguły się łączą); globalnie: WebMvcConfigurer.addCorsMappings.
     *   • allowCredentials(true) + allowedOrigins("*") = błąd (adnotacja: przy starcie; CorsRegistry: przy zapytaniu);
     *     użyj dokładnych adresów albo allowedOriginPatterns.
     *   • Testy CORS: zawsze z nagłówkiem Origin. Ze Spring Security: http.cors(...) + bean CorsConfigurationSource.
     *
     * PYTANIA KONTROLNE:
     *   1. Które z par mają to samo pochodzenie: (http://localhost:3000, http://localhost:8080),
     *      (https://sklep.example.com/a, https://sklep.example.com/b), (http://sklep.example.com, https://sklep.example.com)?
     *   2. Dlaczego zapytanie z curl działa, a ten sam fetch ze strony w przeglądarce kończy się błędem CORS?
     *   3. Co się stanie? Strona https://evil.example.com wysyła z przeglądarki GET na /rest06/plain/hello (kontroler bez
     *      żadnej reguły CORS). Czy kontroler się wykona? Jaki będzie status i czy skrypt zobaczy odpowiedź?
     *   4. Co się stanie? Frontend z http://localhost:3000 chce wysłać DELETE /rest06/api/products/1, a globalna reguła ma
     *      allowedMethods("GET", "POST", "PUT").
     *   5. ZNAJDŹ BŁĄD: {@code registry.addMapping("/api/**").allowedOrigins("*").allowCredentials(true);} — aplikacja
     *      startuje, ale coś jest nie tak. Co i kiedy to wyjdzie? Jak to naprawić?
     *   6. Kiedy przeglądarka wysyła preflight? Co oznacza nagłówek Access-Control-Max-Age?
     *   7. ZNAJDŹ BŁĄD: frontend czyta {@code response.headers.get("X-Total-Count")} i dostaje null, choć w HTTP Client
     *      nagłówek jest widoczny. Czego brakuje w konfiguracji CORS?
     *   8. Dlaczego po dodaniu Spring Security sama adnotacja @CrossOrigin może przestać wystarczać?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseCors (+ gotowy ExerciseController), testy: Rest06CorsExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // ExerciseCors to zwykła klasa ze statycznymi metodami (nie bean), więc niedokończone ćwiczenia nie psują aplikacji.
    // Rozwiązania wzorcowe: Rest06SolutionsCors (s04_web_rest.solutions.rest06_cors).
    //
    // ĆWICZENIE 1 (łatwe): originOf(url) ma zwrócić pochodzenie adresu: "https://sklep.example.com/koszyk?id=1" →
    //   "https://sklep.example.com", "http://localhost:3000/app/" → "http://localhost:3000".
    //   Podpowiedź: URI.create(url) i getScheme(), getHost(), getPort() (port -1 = nie podano jawnie) — sekcja 1.
    //
    // ĆWICZENIE 2 (łatwe): sameOrigin(a, b) — czy dwa adresy mają to samo pochodzenie. Podpowiedź: porównaj originOf.
    //
    // ĆWICZENIE 3 (średnie): shopFrontConfig() — CorsConfiguration dla frontendu http://localhost:5173: metody GET i POST,
    //   nagłówek Content-Type, maxAge 1800 s. Podpowiedź: new CorsConfiguration(), addAllowedOrigin, addAllowedMethod,
    //   addAllowedHeader, setMaxAge(1800L). Test sprawdza wynik metodami checkOrigin/checkHttpMethod/checkHeaders —
    //   tymi samymi, których używa DefaultCorsProcessor.
    //
    // ĆWICZENIE 4 (średnie): partnersConfig() — wszystkie poddomeny "https://*.partner.example.com", z poświadczeniami,
    //   metoda GET, ujawniony nagłówek X-Total-Count. Podpowiedź: sekcja 4 — który z dwóch sposobów podawania pochodzeń
    //   przyjmie gwiazdkę razem z allowCredentials? (addAllowedOriginPattern, setAllowCredentials, addExposedHeader.)
    //
    // ĆWICZENIE 5 (trudniejsze): source() — CorsConfigurationSource, które dla "/rest06/cw/**" zwraca shopFrontConfig().
    //   Test podłącza je do filtra CorsFilter przed ExerciseController: preflight z http://localhost:5173 → 200
    //   z Access-Control-Allow-Origin, z obcego pochodzenia → 403. Podpowiedź: new UrlBasedCorsConfigurationSource()
    //   i registerCorsConfiguration(wzorzecŚcieżki, konfiguracja) — sekcje 3 i 5.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tylko (https://sklep.example.com/a, https://sklep.example.com/b) — ścieżka się nie liczy. Pierwsza para różni
     *      się portem, trzecia schematem (http/https). Sprawdzone testem wzorcowym ćwiczenia 2.
     *   2. Politykę tego samego pochodzenia stosuje PRZEGLĄDARKA. curl nie wysyła Origin i nie sprawdza nagłówków zgody;
     *      przeglądarka wysyła Origin i ukrywa odpowiedź (albo nie wysyła zapytania po preflight), gdy serwer nie odeśle
     *      Access-Control-Allow-Origin dla tego pochodzenia.
     *   3. Kontroler SIĘ WYKONA, status 200, ciało „cześć” — ale bez nagłówka Access-Control-Allow-Origin, więc
     *      przeglądarka nie pokaże odpowiedzi skryptowi (błąd CORS w konsoli). Sprawdzone testem serverDoesNotBlock.
     *      Wniosek: CORS nie chroni serwera przed wykonaniem zapytania.
     *   4. DELETE nie jest zapytaniem prostym, więc przeglądarka wyśle preflight OPTIONS z Access-Control-Request-Method:
     *      DELETE. Spring odpowie 403 „Invalid CORS request” i przeglądarka nie wyśle DELETE. Gdyby ktoś wysłał DELETE
     *      z tym Origin bez preflightu, Spring też odpowie 403 (reguła istnieje dla ścieżki). Sprawdzone testami
     *      preflightRejected i actualRequestRejected.
     *   5. „*” w allowedOrigins z allowCredentials(true) jest zabronione. Przy CorsRegistry start się udaje, ale każde
     *      zapytanie na /api/** kończy się IllegalArgumentException (na serwerze 500). Naprawa: dokładna lista
     *      allowedOrigins albo allowedOriginPatterns z wzorcem własnych domen. Sprawdzone testami
     *      globalMappingFailsAtRequestTime i originPatternsFix.
     *   6. Przed zapytaniem, które nie jest proste: metody inne niż GET/HEAD/POST, Content-Type inny niż trzy „formularzowe”
     *      (np. application/json), własne nagłówki. Access-Control-Max-Age mówi, ile sekund przeglądarka może pamiętać
     *      wynik preflightu i nie pytać ponownie (test preflightAllowed: 3600).
     *   7. Brakuje exposedHeaders("X-Total-Count"). Skrypt z innego pochodzenia widzi tylko kilka bezpiecznych nagłówków;
     *      resztę serwer musi ujawnić przez Access-Control-Expose-Headers (test simpleRequestFromAllowedOrigin).
     *   8. Filtry Spring Security działają przed Spring MVC. Preflight nie ma danych logowania, więc jeśli Security nie
     *      obsługuje CORS, odrzuci go, zanim reguła z @CrossOrigin zadziała. CORS trzeba włączyć w Security
     *      (http.cors(...), zwykle z beanem CorsConfigurationSource) — s12_security/Sec04StatelessApi.
     */
    // </editor-fold>
}
