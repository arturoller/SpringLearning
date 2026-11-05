package s04_web_rest.rest05_crud_api;

/**
 * <pre>
 * TEMAT: Kompletne API CRUD — kontroler, serwis i testy na różnych wysokościach
 *        (CRUD = Create, Read, Update, Delete — utwórz, odczytaj, zmień, usuń; API = interfejs programistyczny;
 *         controller = kontroler, warstwa przyjmująca HTTP; service = serwis, warstwa logiki; DTO = Data Transfer
 *         Object, obiekt do przesyłania danych; idempotent = idempotentny, wielokrotne wykonanie daje ten sam skutek)
 *
 * W SKRÓCIE:
 *   Większość API REST to cztery czynności na zasobie (tu: produkt): utworzyć, odczytać, zmienić, usunąć. Każdej
 *   odpowiada metoda HTTP i kod odpowiedzi: POST → 201, GET → 200 lub 404, PUT i PATCH → 200 lub 404, DELETE → 204 lub 404.
 *   Ta lekcja składa wszystko z Rest01–Rest04 w jedno kompletne API pod adresem /rest05/products i dzieli je na dwie
 *   warstwy: KONTROLER (zna HTTP: statusy, nagłówki, JSON) i SERWIS (zna sklep: id, sortowanie, zmiana ceny; o HTTP
 *   nie wie nic). Dzięki temu każdą warstwę testujesz osobno i najtańszym możliwym testem.
 *
 * ANALOGIA: recepcja i zaplecze w hotelu.
 *   Recepcjonista (kontroler) rozmawia z gościem: przyjmuje prośbę, mówi „załatwione”, „nie ma takiego pokoju”,
 *   „proszę wypełnić poprawnie formularz”. Sam jednak nie sprząta pokoi ani nie prowadzi księgi — to robi zaplecze
 *   (serwis). Zaplecze nie wie, jak brzmi uprzejme „dzień dobry” (kody HTTP); odpowiada krótko: „jest”, „nie ma”,
 *   „usunięte”. Gdy zmienisz recepcjonistę (inny protokół), zaplecze zostaje; gdy zmienisz zasady księgi, recepcja
 *   się nie zmienia.
 *
 * JAK TO DZIAŁA:
 *   1. Zapytanie wchodzi do ProductController. @RequestBody zamienia JSON na DTO wejściowe (ProductRequest, PricePatch).
 *   2. Kontroler sprawdza dane (400), woła ProductService i zamienia wynik na odpowiedź: Optional → 200/404,
 *      boolean z delete → 204/404, utworzony produkt → 201 + Location.
 *   3. Serwis trzyma dane w pamięci: ConcurrentHashMap (bezpieczna przy wielu wątkach naraz) i AtomicLong
 *      (kolejne id bez powtórek). Zwraca model wewnętrzny Product (z kodem wewnętrznym).
 *   4. Kontroler zamienia Product na DTO wyjściowe ProductResponse (bez kodu wewnętrznego) i Jackson zapisuje JSON
 *      (Rest04Json).
 *   5. Serwis jest zwykłym beanem (@Service) wstrzykniętym do kontrolera przez konstruktor — pełne wyjaśnienie
 *      wstrzykiwania: s01_core_ioc.
 *
 * SŁÓWKA:
 *   resource = zasób (rzecz, którą API udostępnia, tu: produkt); layer = warstwa; request = żądanie, zapytanie;
 *   response = odpowiedź; replace = zastąpić; patch = łatka, częściowa zmiana; safe = bezpieczny (nie zmienia
 *   stanu); retry = ponowienie próby; mock = atrapa (udawany obiekt do testów); verify = zweryfikować,
 *   sprawdzić (np. czy metodę wywołano); atomic = niepodzielny (wykonuje się w całości albo wcale).
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest01Controllers (mapowania), s04_web_rest/Rest02RequestData (@PathVariable, @RequestBody),
 *             s04_web_rest/Rest03Responses (ResponseEntity, kody), s04_web_rest/Rest04Json (JSON, DTO),
 *             s05_validation/Val02ValidInControllers (walidacja danych deklaratywnie, zamiast ręcznego isValid),
 *             s06_error_handling/Err03ControllerAdvice (jedno miejsce na obsługę błędów),
 *             s09_jpa_relations/Rel06EntityToDto (DTO przy encjach z bazy),
 *             s11_testing/Test01TestSlices (wycinki testowe), s11_testing/Test03MockMvcTester (pełna lekcja o MockMvcTester),
 *             s11_testing/Test02MockitoBeans (@MockitoBean w szczegółach),
 *             t34_toward_spring/Spring02Layers (warstwy aplikacji z perspektywy czystej Javy),
 *             t34_toward_spring/Spring03RestConcepts (pojęcia REST).
 * </pre>
 */
public final class Rest05CrudApi {

    private Rest05CrudApi() {
    }

    // =================================================================================================
    // 1. CRUD: SZEŚĆ PUNKTÓW KOŃCOWYCH (endpoint) W JEDNEJ TABELI (ProductController)
    // =================================================================================================
    //
    //   Czynność | Metoda i adres             | Wejście            | Sukces            | Błąd
    //   ---------+----------------------------+--------------------+-------------------+----------------------------------
    //   Lista    | GET    /rest05/products    | —                  | 200 + tablica     | —
    //   Szczegóły| GET    /rest05/products/1  | —                  | 200 + produkt     | 404 (nie ma)
    //   Create   | POST   /rest05/products    | ProductRequest     | 201 + Location    | 400 (złe dane)
    //   Update   | PUT    /rest05/products/1  | ProductRequest     | 200 + produkt     | 404, 400
    //   Update   | PATCH  /rest05/products/1  | PricePatch (cena)  | 200 + produkt     | 404, 400
    //   Delete   | DELETE /rest05/products/1  | —                  | 204 (bez ciała)   | 404 (nie ma)
    //
    // Konwencje REST, które tu widać:
    //   • Adres to RZECZOWNIK w liczbie mnogiej (/products), nigdy czasownik (/getProducts, /deleteProduct).
    //     Czynność mówi metoda HTTP, a nie adres.
    //   • Po utworzeniu: 201 + nagłówek Location z adresem nowego zasobu (Rest03Responses, sekcja 1).
    //   • Lista zawsze w stałej kolejności — tu rosnąco po id. ConcurrentHashMap NIE gwarantuje kolejności,
    //     więc serwis sortuje w findAll(); inaczej klient (i test) dostałby raz taką, raz inną kolejność.
    //   • PUT = pełna zamiana zasobu (klient podaje WSZYSTKIE pola edytowalne), PATCH = zmiana części (tu: tylko cena).
    //
    // Testy sekcji 1: Rest05CrudApiTest (każdy wiersz tabeli: kod odpowiedzi, JSON, Location) i
    // Rest05ProductServiceTest (id 3, 4…, sortowanie, Optional, delete true/false).
    //
    // PUŁAPKA: dwa wątki (dwa zapytania naraz) i zwykły HashMap albo licznik {@code long nextId; nextId++}.
    //   Dlaczego? Serwer obsługuje zapytania RÓWNOLEGLE. Zwykły HashMap przy jednoczesnym zapisie może zgubić dane
    //   albo się „zaciąć”, a dwa wątki mogą odczytać ten sam nextId i nadać dwóm produktom jedno id. ConcurrentHashMap
    //   i AtomicLong są na to odporne; computeIfPresent (użyte w replace i changePrice) jest NIEPODZIELNE — nikt nie
    //   usunie produktu między sprawdzeniem „czy istnieje” a podmianą. Z bazą danych (od działu s07) tę odpowiedzialność
    //   przejmują transakcje i klucze generowane przez bazę.

    // =================================================================================================
    // 2. DWIE WARSTWY: KONTROLER I SERWIS (ProductController, ProductService)
    // =================================================================================================
    //
    // KONTROLER odpowiada za HTTP i NIC więcej: mapowania, odczyt @RequestBody/@PathVariable, statusy, nagłówki.
    // SERWIS odpowiada za sprawy sklepu i NIC nie wie o HTTP: nie zwraca ResponseEntity ani kodów, tylko wartości
    // Javy — Optional (jest/nie ma), boolean (czy usunięto), Product. Kontroler tłumaczy je na odpowiedzi:
    //
    //   service.find(id)            → Optional<Product>  ──►  ResponseEntity.of(optional.map(ProductResponse::from))
    //   service.delete(id)          → boolean             ──►  true ? 204 : 404
    //   service.create(request)     → Product             ──►  201 + Location
    //
    // Po co ten podział?
    //   • Testy: serwis sprawdzasz zwykłym testem jednostkowym BEZ Springa (Rest05ProductServiceTest — milisekundy);
    //     kontroler — z atrapą serwisu (sekcja 5). Każda warstwa ma własny, szybki test.
    //   • Wymiana: serwis w pamięci zamienisz na serwis z bazą danych (dział s07), a kontroler zostanie nietknięty.
    //   • Ponowne użycie: tej samej logiki użyjesz z innego miejsca niż HTTP (zadanie cykliczne, kolejka wiadomości).
    //   (Ten sam podział warstw w „czystej Javie”: t34_toward_spring/Spring02Layers.)
    //
    // DOBRA PRAKTYKA: kontroler cienki, serwis gruby. Dlaczego? Kod w kontrolerze da się wykonać tylko przez HTTP i
    //   testować tylko przez MockMvc; kod w serwisie to zwykła klasa Javy, którą testujesz jednym zdaniem w JUnit.
    //   Gdy w kontrolerze pojawia się pętla albo obliczenia biznesowe — przenieś je do serwisu.
    //
    // PUŁAPKA: serwis zwracający ResponseEntity albo rzucający ResponseStatusException „bo wygodniej”. Dlaczego? Serwis
    //   zaczyna zależeć od HTTP: nie użyjesz go poza kontrolerem, a jego test wymaga znajomości kodów HTTP.
    //   (Wyjątki domenowe — np. własny ProductNotFoundException — zamienia na kod osobna warstwa błędów: s06.)

    // =================================================================================================
    // 3. DTO WEJŚCIOWE I WYJŚCIOWE (ProductRequest, PricePatch, ProductResponse, Product)
    // =================================================================================================
    //
    //   ProductRequest(name, price)             ← klient WYSYŁA (POST, PUT)
    //   PricePatch(price)                       ← klient WYSYŁA (PATCH)
    //   ProductResponse(id, name, price)        ← klient DOSTAJE
    //   Product(id, name, price, internalCode)  ← model wewnętrzny serwisu (kod "SKU-1" jest tajny dla klienta)
    //
    // Cztery rekordy zamiast jednego — celowo:
    //   • Wejście nie ma pola id: id nadaje serwer. Gdyby klient mógł wysłać id w POST, nadpisałby cudzy produkt
    //     albo wymusiłby własny numer. (To częsty błąd, zwany „mass assignment” — masowe przypisanie.)
    //   • Wyjście nie ma internalCode: test list() z Rest05CrudApiTest sprawdza, że w JSON tego pola NIE ma.
    //   • Zmiana modelu wewnętrznego (np. nowe pole w bazie) nie zmienia umowy z klientem, dopóki DTO jest takie samo.
    //   • Różne wejścia dla różnych czynności: PUT wymaga nazwy i ceny, PATCH tylko ceny.
    //
    // PUŁAPKA: jeden rekord/klasa „Product” używany i na wejściu, i na wyjściu, i w bazie. Dlaczego? Klient może
    //   ustawić pola, których nie powinien (id, kod wewnętrzny), a każda zmiana w bazie psuje JSON i odwrotnie.
    //   W większych aplikacjach to kolejny powód, by nie zwracać encji (Rest04Json, sekcja 5 oraz
    //   s09_jpa_relations/Rel06EntityToDto).
    //
    // PUŁAPKA: ręczna kontrola isValid() w kontrolerze to tylko zaślepka na potrzeby lekcji. Dlaczego? Powtarza się w
    //   każdej metodzie i łatwo ją pominąć. Docelowo: adnotacje @Valid i @NotBlank/@PositiveOrZero z działu
    //   s05_validation (Val02ValidInControllers) i jedna wspólna obsługa błędów (s06_error_handling).

    // =================================================================================================
    // 4. IDEMPOTENCJA METOD HTTP (Rest05IdempotencyTest)
    // =================================================================================================
    //
    // Metoda jest IDEMPOTENTNA, gdy wielokrotne wysłanie TEGO SAMEGO zapytania daje ten sam skutek (stan serwera)
    // co jedno wysłanie. Kod odpowiedzi może się różnić — liczy się stan.
    //
    //   Metoda | Bezpieczna (nic nie zmienia)? | Idempotentna? | Przykład z tego API
    //   -------+-------------------------------+---------------+--------------------------------------------------
    //   GET    | tak                           | tak           | odczyt listy — ile razy chcesz
    //   PUT    | nie                           | tak           | to samo PUT dwa razy → ten sam stan, wciąż 2 produkty
    //   DELETE | nie                           | tak           | 1. raz 204, 2. raz 404, ale stan taki sam: produktu nie ma
    //   POST   | nie                           | NIE           | dwa razy POST → dwa nowe produkty (id 3 i 4)
    //   PATCH  | nie                           | zależy        | „ustaw cenę 19.90” — tak; „obniż cenę o 10%” — nie
    //
    // Po co to wiedzieć? Sieć bywa zawodna: klient nie dostał odpowiedzi i ponawia (retry). Powtórzenie GET, PUT i
    // DELETE jest bezpieczne. Powtórzenie POST może utworzyć duplikat (podwójne zamówienie, podwójna płatność!) —
    // dlatego API płatnicze dodają „klucz idempotentności” w nagłówku (jedno zamówienie = jeden klucz). Tu tego nie
    // robimy; wiedz, że taki wzorzec istnieje.
    //
    // Test sekcji 4 (Rest05IdempotencyTest, prawdziwy serwis): GET×2 bez zmian; PUT×2 → wciąż 2 produkty i ta sama
    // nazwa; DELETE → 204 potem 404, na liście 1 produkt; POST×2 → id 3 i 4, razem 4 produkty; PATCH „ustaw 19.90” ×2
    // → cena 19.90 za każdym razem.
    //
    // PYTANIE REKRUTACYJNE: które metody HTTP są idempotentne i dlaczego POST nie jest? (GET, PUT, DELETE — oraz
    //   HEAD i OPTIONS — są; POST tworzy nowy zasób przy każdym wywołaniu, więc powtórzenie zmienia stan za każdym
    //   razem. PATCH zależy od tego, co zmienia.) Dodatkowo: czym idempotentny różni się od bezpiecznego? (Bezpieczna
    //   w ogóle nie zmienia stanu — GET; idempotentna może zmieniać, ale tylko raz — PUT, DELETE.)
    //
    // PUŁAPKA: DELETE zwracające 404 za drugim razem traktowane jako błąd przez klienta. Dlaczego? Cel („produktu ma nie
    //   być”) jest osiągnięty, więc dobrze napisany klient przy ponowieniu DELETE uznaje 404 za sukces. Niektóre API
    //   zwracają wtedy zawsze 204 — to też poprawny wybór; ważne, by opisać go w dokumentacji i trzymać się go.

    // =================================================================================================
    // 5. TESTY NA TRZECH WYSOKOŚCIACH + MockMvcTester
    // =================================================================================================
    //
    //   Wysokość                      | Test                    | Co sprawdza                              | Koszt
    //   ------------------------------+-------------------------+------------------------------------------+---------
    //   Zwykły JUnit (bez Springa)    | Rest05ProductServiceTest| logika serwisu                           | ms
    //   @WebMvcTest + @MockitoBean    | Rest05CrudApiTest       | kontroler: kody, JSON, wołanie serwisu   | ~sekunda
    //   @WebMvcTest + prawdziwy serwis| Rest05IdempotencyTest   | skutek na danych w wielu zapytaniach     | ~sekunda
    //   @SpringBootTest(RANDOM_PORT)  | Rest05ServerTest        | całość przez prawdziwy HTTP (RestClient) | najdroższy
    //
    // • @MockitoBean (Rest05CrudApiTest). Zamiast prawdziwego ProductService Spring wstawia atrapę Mockito. Ustawiasz jej
    //   odpowiedzi (given(service.find(1)).willReturn(Optional.of(...))) i po żądaniu SPRAWDZASZ wywołania:
    //   verify(service).create(new ProductRequest("Ciastka", new BigDecimal("12.50"))) — że kontroler przekazał serwisowi
    //   dokładnie to, co wysłał klient; verifyNoInteractions(service) — że przy złych danych serwis nie został w ogóle
    //   zawołany. Zawsze @MockitoBean (nie @MockBean — usunięty w Boot 4; pełna lekcja: s11_testing/Test02MockitoBeans).
    //
    // • @SpringBootTest(webEnvironment = RANDOM_PORT) (Rest05ServerTest). Startuje całą aplikację z Tomcatem na LOSOWYM
    //   porcie (@LocalServerPort podaje, na jakim), a RestClient wysyła prawdziwe zapytania HTTP. W jednym teście cały
    //   scenariusz: POST (201 + Location) → GET pod tym Location → PUT → PATCH → DELETE (204) → GET (404). RestClient
    //   zamienia odpowiedzi 4xx na wyjątek (HttpClientErrorException.NotFound, .BadRequest) — dlatego 404 sprawdzamy
    //   przez assertThatThrownBy. Boot 4 ma też TestRestTemplate (pakiet org.springframework.boot.resttestclient,
    //   z adnotacją @AutoConfigureTestRestTemplate); w nowym kodzie przydaje się RestClient.
    //
    // • MockMvcTester (Rest05MockMvcTesterTest) — ten sam MockMvc, ale z asercjami AssertJ, jak reszta testów:
    //
    //     assertThat(mvc.get().uri("/rest05/products/1"))
    //             .hasStatusOk()
    //             .bodyJson().extractingPath("$.name").asString().isEqualTo("Kawa");
    //
    //   @WebMvcTest tworzy go sam; wstrzykujesz @Autowired MockMvcTester. Zamiast andExpect(status().isOk()) piszesz
    //   .hasStatusOk() — podpowiedzi IDE działają lepiej, a komunikaty błędów są czytelniejsze. Pełna lekcja:
    //   s11_testing/Test03MockMvcTester. W działach s04–s05 zostajemy przy klasycznym MockMvc.
    //
    // DOBRA PRAKTYKA: większość testów na najniższej wysokości, kilka na środkowej, jeden-dwa na najwyższej.
    //   Dlaczego? Wyższe testy są wolniejsze i trudniejsze do zdiagnozowania (coś nie działa — ale gdzie?); tanie
    //   testy dają szybką informację zwrotną. Ten kształt nazywa się „piramidą testów”.
    //
    // PUŁAPKA: test zależny od stanu zostawionego przez inny test (serwis w pamięci jest JEDEN na cały kontekst).
    //   Dlaczego? JUnit nie gwarantuje kolejności testów: jedna kolejność przejdzie, inna wywali — i nie wiesz czemu.
    //   Dlatego testy z prawdziwym serwisem wołają service.reset() w @BeforeEach i nie zakładają niczego o poprzednich.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • CRUD: GET lista 200; GET {id} 200/404; POST 201 + Location; PUT 200/404; PATCH 200/404; DELETE 204/404;
     *     złe dane 400. Adresy: rzeczownik w liczbie mnogiej, czynność mówi metoda HTTP.
     *   • Kontroler (HTTP, statusy) → serwis (logika, zwraca Optional/boolean/obiekty, nie zna HTTP).
     *   • DTO wejściowe bez id (id nadaje serwer), DTO wyjściowe bez pól wewnętrznych, model wewnętrzny osobno.
     *   • PUT = cały zasób, PATCH = część. Dane w pamięci: ConcurrentHashMap + AtomicLong, lista sortowana po id.
     *   • Idempotentne: GET, PUT, DELETE (skutek ten sam przy powtórzeniu). POST — nie. PATCH — zależy.
     *   • Testy: zwykły JUnit dla serwisu; @WebMvcTest + @MockitoBean dla kontrolera (given/verify);
     *     @SpringBootTest(RANDOM_PORT) + RestClient dla całości; MockMvcTester jako asercje AssertJ.
     *
     * PYTANIA KONTROLNE:
     *   1. Jaki kod i jaki nagłówek zwróci udany POST tworzący produkt? A jaki kod zwróci udane DELETE i czy ma ciało?
     *   2. Które z metod GET, POST, PUT, DELETE są idempotentne? Podaj przykład z tego API, który to pokazuje.
     *   3. Co się stanie? Klient dwa razy pod rząd wysyła to samo zapytanie DELETE /rest05/products/1.
     *   4. Co się stanie? Klient dwa razy pod rząd wysyła to samo zapytanie POST /rest05/products z tym samym JSON.
     *   5. ZNAJDŹ BŁĄD: kontroler w POST przyjmuje jako {@code @RequestBody} model wewnętrzny {@code Product} (z polami
     *      id i internalCode) zamiast {@code ProductRequest}. Co złego może zrobić klient?
     *   6. ZNAJDŹ BŁĄD: serwis przechowuje produkty w zwykłym {@code HashMap}, a licznik id to {@code long nextId}
     *      zwiększany przez {@code nextId++}. Co może pójść nie tak przy ruchu równoległym?
     *   7. Dlaczego serwis ma zwracać {@code Optional<Product>}, a nie ResponseEntity? Co zyskujesz w testach?
     *   8. Czym różni się test z {@code @MockitoBean ProductService} od testu z {@code @Import(ProductService.class)}
     *      i kiedy użyjesz którego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Rest05CrudApiExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Budujesz CRUD dla klientów (/rest05/cw/customers) na GOTOWYM serwisie CustomerBook (find, add, rename,
    // changeEmail, remove). Mapowania są gotowe. Rozwiązania wzorcowe: Rest05SolutionsController
    // (s04_web_rest.solutions.rest05_crud_api).
    //
    // ĆWICZENIE 1 (łatwe): get(id) — 200 z klientem albo 404 bez ciała. Podpowiedź: ResponseEntity.of(optional).
    //
    // ĆWICZENIE 2 (łatwe): delete(id) — 204, gdy klient został usunięty, 404, gdy go nie było.
    //   Podpowiedź: book.remove(id) zwraca boolean.
    //
    // ĆWICZENIE 3 (średnie): create(request, uriBuilder) — dodaj klienta (book.add), odpowiedz 201, nagłówkiem Location
    //   z adresem /rest05/cw/customers/{id} i klientem w ciele. Podpowiedź: uriBuilder.path(...).buildAndExpand(id).
    //
    // ĆWICZENIE 4 (średnie): rename(id, request) — zmień nazwę (book.rename, e-mail zostaje); 200 z klientem albo 404.
    //
    // ĆWICZENIE 5 (trudniejsze): changeEmail(id, patch) — zmień sam e-mail (book.changeEmail). Pusty e-mail albo taki
    //   bez znaku '@' → 400 i NIC nie zmieniaj; w pozostałych przypadkach 200 albo 404, gdy klienta nie ma.
    //   Podpowiedź: najpierw sprawdź dane (400), dopiero potem wołaj serwis.

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. POST: 201 Created i nagłówek Location z adresem nowego zasobu (np. http://localhost/rest05/products/3).
     *      DELETE: 204 No Content — bez ciała. Sprawdzone w Rest05CrudApiTest (create, deleteOneAndNotFound).
     *   2. GET, PUT i DELETE są idempotentne; POST nie. Przykład: dwa razy to samo POST tworzy produkty o id 3 i 4
     *      (razem 4 na liście), a dwa razy to samo PUT zostawia 2 produkty i tę samą nazwę.
     *      Sprawdzone w Rest05IdempotencyTest.
     *   3. Pierwszy DELETE daje 204, drugi 404 — ale stan jest taki sam (produktu nie ma), więc metoda jest
     *      idempotentna w skutku, choć kod odpowiedzi się zmienił. Sprawdzone w deleteIsIdempotent (sekcja 4).
     *   4. Powstają dwa osobne produkty (id 3 i 4), oba z kodem 201; lista ma wtedy 4 pozycje. POST nie jest
     *      idempotentny — dlatego ponawianie POST bez klucza idempotentności grozi duplikatami.
     *      Sprawdzone w postIsNotIdempotent (sekcja 4).
     *   5. Klient mógłby ustawić pola, które ma ustawiać serwer (id — nadpisanie cudzego produktu, internalCode —
     *      własny kod wewnętrzny). To „mass assignment”. Dlatego wejście to osobne DTO (ProductRequest) bez tych pól.
     *   6. HashMap przy jednoczesnym zapisie z wielu wątków może zgubić wpisy; a nextId++ nie jest niepodzielne:
     *      dwa wątki mogą odczytać tę samą wartość i nadać dwóm produktom to samo id (jeden nadpisze drugi).
     *      Rozwiązanie: ConcurrentHashMap i AtomicLong (a docelowo baza danych).
     *   7. Serwis nie powinien zależeć od HTTP: Optional znaczy „jest/nie ma” i działa w każdym kontekście. W testach
     *      sprawdzasz go zwykłym JUnit bez Springa (Rest05ProductServiceTest) i nie musisz znać kodów HTTP.
     *   8. @MockitoBean podmienia serwis atrapą: test sprawdza TYLKO kontroler (kody, JSON, przekazanie argumentów
     *      przez verify) i jest niezależny od logiki serwisu. @Import(ProductService.class) dodaje PRAWDZIWY serwis:
     *      użyj go, gdy chcesz sprawdzić skutek na danych w wielu zapytaniach (np. idempotencję).
     */
    // </editor-fold>
}
