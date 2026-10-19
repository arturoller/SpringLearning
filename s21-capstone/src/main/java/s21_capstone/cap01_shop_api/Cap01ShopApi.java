package s21_capstone.cap01_shop_api;

/**
 * <pre>
 * TEMAT: Projekt końcowy 1 — REST API sklepu: produkty, klienci, zamówienia z pozycjami, reguły statusów,
 *        ProblemDetail, DTO, brak N+1 i testy na każdym poziomie
 *        (capstone = projekt zwieńczający; REST API = interfejs programistyczny przez HTTP; N+1 = jedno zapytanie
 *         o listę + po jednym na każdy element)
 *
 * W SKRÓCIE:
 *   Składamy w jedną aplikację wszystko z kursu: kontrolery REST (s04), walidację (s05), ProblemDetail (s06),
 *   Flyway (s07), Spring Data JPA i relacje (s08, s09), transakcje (s10), testy (s11) i — świadomie wyłączone —
 *   zabezpieczenia (s12). Najważniejsze decyzje projektu:
 *   • pakiety według FUNKCJI (product, customer, order, discount, common), a w każdym warstwy: kontroler → serwis → repozytorium;
 *   • encje nie wychodzą z serwisu — na zewnątrz idą rekordy DTO, budowane w transakcji (open-in-view=false);
 *   • reguły biznesowe w domenie: zamówienie samo przelicza sumę, enum statusów sam wie, co wolno;
 *   • każdy błąd to application/problem+json; każda reguła, której może pilnować baza, jest też ograniczeniem w bazie;
 *   • lista zamówień to projekcja DTO — 1 zapytanie SQL zamiast 10 (zmierzone testem).
 *
 * ANALOGIA: sklep z zapleczem.
 *   Klient widzi ladę (kontrolery i DTO): cennik, formularz zamówienia, paragon. Sprzedawca (serwis) pilnuje zasad:
 *   „nie wydam towaru, którego nie ma”, „zapłacone zamówienie można wysłać, ale nie można go znowu opłacić”. Zaplecze
 *   (repozytoria i baza) przechowuje towar i dokumenty, a magazynier (ograniczenia w bazie) nie przyjmie drugiego
 *   pudełka z tym samym numerem, nawet gdy dwóch sprzedawców przyniesie je w tej samej sekundzie. Paragon (pozycja
 *   zamówienia) ma cenę z dnia zakupu — zmiana cennika jutro nie zmienia wczorajszych paragonów.
 *
 * JAK TO DZIAŁA:
 *   Droga jednego żądania, np. PATCH /api/orders/3/status {"status":"OPLACONE"}:
 *   DispatcherServlet → łańcuch filtrów Security (u nas permitAll, sekcja 7) → OrderController.changeStatus
 *   → Jackson czyta ChangeStatusRequest (nieznany status = 400) → @Valid → OrderService.changeStatus
 *   [proxy @Transactional: początek transakcji] → OrderRepository.findDetailedById (1 SELECT z join fetch)
 *   → ShopOrder.changeStatus → OrderStatus.canChangeTo → NIE: ConflictException → [rollback]
 *   → ShopProblemAdvice → 409 application/problem+json {"code":"ORDER_STATUS_CHANGE","currentStatus":"WYSLANE",...}
 *   → TAK: OrderResponse.from(order) w transakcji → [commit: Hibernate wysyła UPDATE orders] → 200 + JSON.
 *
 * SŁÓWKA:
 *   capstone = projekt końcowy; specification = specyfikacja (opis wymagań); use case = przypadek użycia;
 *   aggregate root = korzeń agregatu (encja, przez którą zmienia się całą grupę obiektów); state machine = maszyna
 *   stanów; snapshot = migawka (kopia wartości z danej chwili); projection = projekcja (zapytanie zwracające tylko
 *   potrzebne kolumny); whitelist = biała lista (lista dozwolonych wartości); race condition = wyścig; constraint =
 *   ograniczenie (w bazie); slice test = test wycinka (tylko jedna warstwa aplikacji); spy = szpieg (prawdziwy obiekt
 *   z możliwością podmiany pojedynczej metody); package by feature = pakiety według funkcji.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest05CrudApi (CRUD i kody statusu), s05_validation/Val03NestedAndGroups (@Valid na liście),
 *             s06_error_handling/Err04ProblemDetail i s06_error_handling/Err05CustomExceptions (ProblemDetail, wyjątki
 *             domeny), s07_data_jdbc/Jdbc04Flyway (migracje), s08_data_jpa/Jpa05PagingSorting (strony i sortowanie),
 *             s09_jpa_relations/Rel05NPlusOne i s09_jpa_relations/Rel06EntityToDto (N+1, DTO), s10_transactions/Tx01Transactional,
 *             s11_testing/Test01TestSlices (wycinki), s12_security/Sec01Basics (domyślne zabezpieczenia),
 *             s20_modulith/Mod01Modules (pakiety według funkcji jako moduły), t34_toward_spring/Spring02Layers (warstwy),
 *             t35_capstone/Capstone04RestTodo (REST bez Springa).
 * </pre>
 */
public final class Cap01ShopApi {

    private Cap01ShopApi() {
    }

    // =================================================================================================
    // 1. SPECYFIKACJA — wymagania, przypadki użycia, punkty końcowe, model danych
    //    (testy: Cap01ShopApiTest.fullOrderLifecycle, Cap01ShopApiWebSliceTest #1)
    // =================================================================================================
    //
    // Wymagania (co sklep MA umieć):
    //   W1. Katalog: przeglądanie produktów ze stronami, filtrem kategorii i wyszukiwaniem po fragmencie nazwy; dodawanie,
    //       zmiana i usuwanie produktu (produktu z zamówień usunąć nie wolno).
    //   W2. Klienci: lista, szczegóły, rejestracja (email opcjonalny, ale unikalny).
    //   W3. Zamówienia: złożenie zamówienia z pozycjami (cena z chwili zamówienia, suma liczona przez system), lista
    //       zamówień (najnowsze pierwsze, filtr statusu), szczegóły z pozycjami, zmiana statusu według reguł.
    //   W4. Każdy błąd w jednym formacie (ProblemDetail), z polskim opisem i kodem rozpoznawalnym przez program.
    //
    // Przypadek użycia „Złożenie zamówienia” (use case):
    //   1. Klient wysyła numer klienta i listę pozycji (sku + ilość).  2. System sprawdza format (400), istnienie klienta
    //   i produktów (404).  3. System kopiuje aktualne ceny do pozycji i liczy sumę.  4. Zamówienie dostaje status NOWE
    //   i numer ZAM-xxx.  5. Odpowiedź 201 + nagłówek Location.  Wszystko albo nic: błąd w punkcie 2 = brak zamówienia.
    //
    // Punkty końcowe (endpoints):
    //   GET    /api/products?category=&q=&page=&size=&sort=   200 strona  | 400 zły parametr
    //   GET    /api/products/{sku}                            200         | 404
    //   POST   /api/products                                  201+Location| 400 walidacja | 409 SKU_TAKEN
    //   PUT    /api/products/{sku}                            200         | 400 | 404
    //   DELETE /api/products/{sku}                            204         | 404 | 409 PRODUCT_IN_USE
    //   GET    /api/customers   ·   GET /api/customers/{id}   200         | 404
    //   POST   /api/customers                                 201+Location| 400 | 409 EMAIL_TAKEN
    //   POST   /api/orders                                    201+Location| 400 | 404 (klient, sku)
    //   GET    /api/orders?status=&page=&size=                200 strona OrderSummary (najnowsze pierwsze)
    //   GET    /api/orders/{id}                               200 szczegóły z pozycjami | 404
    //   PATCH  /api/orders/{id}/status                        200 | 400 nieznany status | 404 | 409 ORDER_STATUS_CHANGE
    //
    // Model danych (tabele z migracji V1–V3):
    //   customers 1 ──── * orders 1 ──── * order_lines * ──── 1 products          discount_codes (ćwiczenie 1)
    //   orders: status, total (suma zapisana), created_at   order_lines: quantity, unit_price (KOPIA ceny)
    // Relacje JPA są JEDNOKIERUNKOWE tam, gdzie wystarczą: zamówienie → klient (klient nie ma listy zamówień),
    // pozycja → produkt. Dwukierunkowa jest tylko zamówienie ↔ pozycje, bo pozycje bez zamówienia nie istnieją.

    // =================================================================================================
    // 2. ARCHITEKTURA — pakiety, warstwy, DTO, transakcje (testy: Cap01ShopApiTest #2)
    // =================================================================================================
    //
    //   cap01_shop_api/            Cap01Application, Cap01Config (zegar + Security), Cap01Exercises
    //   ├── common/                ShopException (sealed) + NotFound/Conflict/InvalidRequest, ShopProblemAdvice, PageResponse
    //   ├── product/               Product, Category, ProductRepository, ProductService, ProductController, ProductDtos
    //   ├── customer/              Customer, CustomerRepository, CustomerService, CustomerController, CustomerDtos
    //   ├── order/                 ShopOrder, OrderLine, OrderStatus, OrderSummary, OrderRepository, OrderService,
    //   │                          OrderController, OrderDtos
    //   └── discount/              DiscountCode, DiscountCodeRepository (pod ćwiczenie 1)
    //
    // Pakiety według FUNKCJI (package by feature), a nie według warstw (controllers/, services/, repositories/).
    // Dlaczego? Zmiana „zamówień” dotyka jednego katalogu, a zależności widać na pierwszy rzut oka: order zna product
    // i customer, ale product NIE zna order. Dlatego ProductService.delete nie pyta tabeli zamówień, tylko próbuje
    // usunąć i tłumaczy naruszenie klucza obcego (sekcja 3). Ten układ to też gotowy kandydat na moduły Spring Modulith
    // (s20_modulith/Mod01Modules).
    //
    // Zadania warstw:
    //   kontroler  — HTTP: adres, metoda, parametry, @Valid, status, Location. Zero logiki biznesowej.
    //   serwis     — transakcja (@Transactional; readOnly = true na klasie, zwykła na metodach zmieniających),
    //                pobranie encji, wywołanie metod domeny, zbudowanie DTO WEWNĄTRZ transakcji.
    //   domena     — encje i enumy z regułami (ShopOrder.addLine, ShopOrder.changeStatus, OrderStatus.allowedNext).
    //   repozytorium — zapytania „pod ekran”: projekcja dla listy, join fetch dla szczegółów.
    //
    // Złożenie zamówienia jest JEDNĄ transakcją: nieznany sku w drugiej pozycji → NotFoundException → rollback → w bazie
    // nie ma ani zamówienia, ani pierwszej pozycji (test orderIsAllOrNothing liczy zamówienia przed i po).
    //
    // DOBRA PRAKTYKA: spring.jpa.open-in-view=false w profilu cap01. Dlaczego? Wtedy leniwe relacje działają tylko
    //   w transakcji serwisu — błąd „mapuję encję w kontrolerze” wychodzi od razu (LazyInitializationException,
    //   s09_jpa_relations/Rel04LazyLoading), a nie po cichu jako dodatkowe zapytania w czasie pisania JSON-a.
    //
    // PUŁAPKA: wyjątek domeny, który „zna” HTTP (status w wyjątku, ResponseStatusException w serwisie). Dlaczego to źle?
    //   Ten sam serwis wołają też testy, zadania w tle i ćwiczenia — status HTTP nic tam nie znaczy. U nas wyjątek mówi
    //   CO się stało (code + szczegóły), a JAK to pokazać przez HTTP decyduje jedna porada (sekcja 5).

    // =================================================================================================
    // 3. MODEL I MIGRACJE — encje z regułami, kopia ceny, ograniczenia w bazie
    //    (testy: Cap01ShopApiUnitTest #3, Cap01ShopApiRepositoryTest.priceSnapshot, Cap01ShopApiTest #2, #3)
    // =================================================================================================
    //
    // ShopOrder to korzeń agregatu: pozycje dodaje się tylko przez addLine (konstruktor OrderLine jest pakietowy,
    // getLines() zwraca listę tylko do odczytu), więc total ZAWSZE równa się sumie pozycji:
    //     2 × 349.90 + 1 × 64.99 = 764.79   (BigDecimal, zaokrąglenie HALF_UP do groszy — test totalIsRecalculated)
    // OrderLine kopiuje cenę produktu (unit_price). Test priceSnapshot podnosi cenę słuchawek do 399.90, a ZAM-001
    // nadal ma 349.90 i sumę 764.79.
    //
    // Schemat tworzy WYŁĄCZNIE Flyway (V1 schemat, V2 dane startowe, V3 kody rabatowe), Hibernate tylko sprawdza
    // (ddl-auto=validate). Nowa funkcja = nowa migracja (V3), nigdy edycja starej — Flyway pamięta sumy kontrolne.
    //
    // Reguła w serwisie + ograniczenie w bazie (dwie linie obrony):
    //   • sku unikalny: existsBySku (czytelny 409) + uk_products_sku (wyścig dwóch żądań);
    //   • email klienta unikalny: existsByEmail + uk_customers_email (unique dopuszcza wiele NULL — klienci bez emaila);
    //   • produkt z zamówień: fk_order_lines_product — ProductService.delete tłumaczy naruszenie na 409 PRODUCT_IN_USE;
    //   • price > 0, stock >= 0, quantity > 0: CHECK w bazie + walidacja DTO + reguły encji.
    // saveAndFlush/flush w serwisie wysyła SQL OD RAZU, więc naruszenie ograniczenia wychodzi w metodzie serwisu jako
    // DataIntegrityViolationException (Spring tłumaczy wyjątek H2 na swój) i można je zamienić na 409. Bez flush INSERT
    // poszedłby dopiero przy commicie — poza naszym try/catch — i klient dostałby 500.
    // Test databaseConstraintIsLastLineOfDefence udaje wyścig: szpieg (@MockitoSpyBean) każe existsBySku odpowiedzieć
    // „nie ma” dla istniejącego KSI-002 — mimo to klient dostaje 409 SKU_TAKEN, a stary produkt jest nietknięty.
    //
    // PUŁAPKA: FetchType domyślny dla @ManyToOne to EAGER. Dlaczego to ważne? Każde załadowanie pozycji zamówienia
    //   dociągałoby produkt i zamówienie, nawet gdy nikt ich nie potrzebuje. Wszystkie nasze relacje ToOne mają LAZY,
    //   a to, co potrzebne, dociągamy świadomie (join fetch, projekcja — sekcja 6).

    // =================================================================================================
    // 4. REGUŁY STATUSU — maszyna stanów w enumie (testy: Cap01ShopApiUnitTest #4, Cap01ShopApiWebSliceTest #4)
    // =================================================================================================
    //
    //   NOWE → OPLACONE → WYSLANE → DOSTARCZONE;  NOWE i OPLACONE → ANULOWANE;  DOSTARCZONE, ANULOWANE — koniec.
    // OrderStatus.allowedNext() zwraca dozwolone następne statusy, canChangeTo(next) — czy wolno. Encja woła to
    // w changeStatus i przy odmowie rzuca ConflictException z polami dla klienta. Prawdziwa odpowiedź (PATCH ZAM-003,
    // które jest WYSLANE, na OPLACONE):
    //     409 {"detail":"Nie można zmienić statusu zamówienia ZAM-003 z WYSLANE na OPLACONE", ...,
    //          "code":"ORDER_STATUS_CHANGE","currentStatus":"WYSLANE","requestedStatus":"OPLACONE","allowed":["DOSTARCZONE"]}
    // Szczegóły zamówienia mają pole allowedStatusChanges — aplikacja klienta wie, które przyciski pokazać, i nie
    // musi kopiować reguł.
    //
    // PUŁAPKA: dozwolone przejścia jako pole enuma ustawiane w konstruktorze stałej:
    //     NOWE(EnumSet.of(OPLACONE, ANULOWANE)), OPLACONE(EnumSet.of(WYSLANE, ANULOWANE)), ...
    //   Kompilator: „error: illegal forward reference” (sprawdzone javac). Dlaczego? Stała OPLACONE jeszcze nie istnieje,
    //   gdy tworzy się NOWE — stałe enuma powstają po kolei. Rozwiązanie: metoda ze switch (jak u nas) albo mapa
    //   wypełniana w bloku static PO deklaracji wszystkich stałych (t08_enums/Enums03ConstantBodies).
    //
    // DOBRA PRAKTYKA: zmiana statusu jako PATCH /api/orders/{id}/status z JEDNYM polem, a nie PUT całego zamówienia.
    //   Dlaczego? PUT przyjmowałby od klienta także sumę i pozycje — a tych klientowi zmieniać nie wolno (over-posting,
    //   s04_web_rest/Rest05CrudApi). Przejście statusu to operacja z regułami, nie „nadpisanie pola”.

    // =================================================================================================
    // 5. BŁĘDY I WALIDACJA — jeden kształt błędu w całym API (testy: Cap01ShopApiWebSliceTest #5, Cap01ShopApiTest #5)
    // =================================================================================================
    //
    // ShopProblemAdvice extends ResponseEntityExceptionHandler:
    //   • ShopException (sealed: NotFound → 404, Conflict → 409, InvalidRequest → 400) — switch bez default; nowy rodzaj
    //     wyjątku bez gałęzi = błąd kompilacji, a nie zapomniany status;
    //   • @Valid na ciele → 400 z mapą errors, także dla zagnieżdżonych pól: "lines[0].quantity":"ilość musi być dodatnia";
    //   • walidacja parametrów (@Max(50) int size) → 400 z errors {"size":"rozmiar strony: od 1 do 50"} — nadpisany
    //     handleHandlerMethodValidationException (bez tego tylko ogólne "Validation failure");
    //   • wyjątki Spring MVC z klasy bazowej: zła kategoria → 400 "Failed to convert 'category' with value: 'ZABAWKI'",
    //     nieznany status w JSON-ie → 400 "Failed to read request", DELETE /api/orders/1 → 405;
    //   • Exception → 500 "Wystąpił błąd wewnętrzny..." — szczegóły tylko w logu.
    //
    // PUŁAPKA: pole typu prostego w DTO żądania (int stock, boolean vip). Dlaczego to kłopot? Jackson 3 domyślnie
    //   odrzuca JSON BEZ takiego pola (FAIL_ON_NULL_FOR_PRIMITIVES) — klient dostaje 400 "Failed to read request" bez
    //   mapy errors, zanim w ogóle ruszy walidacja (sprawdzone: POST /api/customers bez "vip", gdy pole było boolean).
    //   Dlatego w żądaniach Integer/Boolean (+ @NotNull, gdy pole wymagane): brak "stock" → {"errors":{"stock":"stan jest wymagany"}}.
    //
    // PUŁAPKA: (PYTANIE REKRUTACYJNE: „czemu nie przyjąć Pageable prosto od klienta?”) Pageable z adresu przyjmuje
    //   DOWOLNE pole sortowania. sort=cena → PropertyReferenceException „No property 'cena' found for type 'Product'”
    //   → nasza siatka bezpieczeństwa → 500 (sprawdzone uruchomieniem). Gorzej: sort=purchasePrice (gdyby takie pole
    //   było w encji) pozwoliłoby klientowi zgadywać ukryte dane po kolejności wyników. Dlatego ProductController ma
    //   BIAŁĄ LISTĘ (name, price, price,desc, sku) → nieznany klucz to 400 SORT_NOT_ALLOWED z listą dozwolonych.
    //   Domyślny limit rozmiaru strony w Spring Data to 2000 (spring.data.web.pageable.max-page-size) — u nas @Max(50).
    //
    // DOBRA PRAKTYKA: komunikaty walidacji podane jawnie (message = "..."). Dlaczego? Domyślne komunikaty Hibernate
    //   Validatora zależą od języka JVM — test z „must not be blank” przejdzie na serwerze, a padnie na polskim laptopie.

    // =================================================================================================
    // 6. WYDAJNOŚĆ — lista bez N+1, szczegóły w jednym zapytaniu (testy: Cap01ShopApiRepositoryTest #6, Cap01ShopApiTest #6)
    // =================================================================================================
    //
    // PRZED (findAll + mapowanie encji): strona 5 zamówień i dla każdego klient + liczba pozycji =
    //     1 SELECT orders + 4 SELECT customers (różni klienci; Jan drugi raz jest już w kontekście utrwalania)
    //     + 5 SELECT order_lines = 10 zapytań  (zmierzone: getPrepareStatementCount() == 10). 50 zamówień → ponad 50.
    // PO (OrderRepository.findSummaries — projekcja OrderSummary z join, left join i count(l), group by):
    //     1 zapytanie na niepełną stronę, 2 na pełną (dane + count) — NIEZALEŻNIE od liczby zamówień (zmierzone).
    // Szczegóły (findDetailedById): join fetch klienta, pozycji i ich produktów — 1 zapytanie.
    //
    // countQuery podajemy jawnie: bez niego Spring Data i Hibernate policzą poprawnie, ale podzapytaniem z tymi samymi
    // złączeniami i group by (widać w logu SQL), a do policzenia zamówień wystarczy jedna tabela.
    //
    // PUŁAPKA: zwracanie Page wprost z kontrolera. Dlaczego nie? Spring Data ostrzega w logu (prawdziwy tekst):
    //   „Serializing PageImpl instances as-is is not supported, meaning that there is no guarantee about the stability
    //   of the resulting JSON structure!” — JSON ma wtedy pola pageable, sort, empty... z wnętrza biblioteki. Nasz rekord
    //   PageResponse (content, page, size, totalElements, totalPages) to stała umowa z klientem.
    //
    // Ciekawostka (sprawdzone w logu SQL): Hibernate 7.4 przy join fetch KOLEKCJI ze stronicowaniem (setMaxResults)
    // przenosi limit do podzapytania: select ... from (select ... from orders ... fetch first ? rows only) left join
    // order_lines. Starsze poradniki ostrzegają, że Hibernate 6 stronicował wtedy w pamięci (HHH90003004) — w nowych
    // wersjach sprawdzaj log SQL, zamiast ufać staremu przepisowi.

    // =================================================================================================
    // 7. SPRING SECURITY NA ŚCIEŻCE KLAS — jawne permitAll (test: Cap01ShopApiDefaultSecurityTest)
    // =================================================================================================
    //
    // Moduł s21-capstone ma starter security (dla Cap03). Sama obecność startera włącza domyślne zabezpieczenia
    // (s12_security/Sec01Basics). Bez naszego beana (sprawdzone wycinkiem bez @Import(Cap01Config.class)):
    //     GET /api/products/ELE-001 → 401;   POST /api/products → 403 (brak tokenu CSRF).
    // Cap01Config.cap01SecurityFilterChain: anyRequest().permitAll(), CSRF wyłączone (API bez ciasteczek sesji),
    // sesja STATELESS. Własny bean SecurityFilterChain wyłącza domyślny łańcuch Boota.
    //
    // PUŁAPKA: @WebMvcTest NIE ładuje naszych klas @Configuration (także łańcucha filtrów). Dlaczego to ważne? Wycinek
    //   bez @Import(Cap01Config.class) testuje DOMYŚLNE zabezpieczenia, nie nasze — każdy test dostaje 401/403
    //   i wygląda, jakby kontroler był zepsuty (s12_security/Sec06SecurityTesting).

    // =================================================================================================
    // 8. TESTY NA KAŻDYM POZIOMIE — co sprawdzać gdzie
    // =================================================================================================
    //
    //   Cap01ShopApiUnitTest            JUnit, bez Springa     reguły domeny: statusy (11 przejść), suma, kopia ceny  ~0,1 s
    //   Cap01ShopApiWebSliceTest        @WebMvcTest + atrapy    HTTP: walidacja, statusy, ProblemDetail, Location
    //   Cap01ShopApiDefaultSecurityTest @WebMvcTest bez Import  co robi Security bez naszej konfiguracji
    //   Cap01ShopApiRepositoryTest      @DataJpaTest            zapytania, liczba SQL (N+1), kopia ceny w bazie
    //   Cap01ShopApiTest                @SpringBootTest+MockMvc współpraca warstw: transakcja, ograniczenia, scenariusz
    // Im niżej, tym szybciej i dokładniej; im wyżej, tym bliżej prawdy. Większość reguł — w testach jednostkowych,
    // kilka scenariuszy — w pełnym kontekście.
    //
    // PUŁAPKA: @Transactional na teście pełnego kontekstu z MockMvc. Dlaczego nie? Żądanie MockMvc biegnie w tym samym
    //   wątku, więc dołącza do transakcji testu: leniwe relacje „działają” mimo open-in-view=false, a commit (i błędy
    //   ograniczeń przy commicie) nigdy nie następuje. Test przechodzi, aplikacja pada. Dlatego Cap01ShopApiTest nie
    //   jest transakcyjny, a każdy test tworzy własne dane (nowy sku, nowe zamówienie).
    //
    // DOBRA PRAKTYKA: w testach ćwiczeń baza wraca do stanu z migracji przed każdym testem (flyway.clean() +
    //   flyway.migrate(), czyszczenie odblokowane tylko tam: spring.flyway.clean-disabled=false, własna nazwa bazy).
    //   Dlaczego? Testy, które naprawdę zatwierdzają zmiany (stany magazynu), nie mogą zależeć od kolejności.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Pakiety według funkcji; kontroler = HTTP, serwis = transakcja + DTO, domena = reguły, repozytorium = zapytania.
     *   • DTO budowane w transakcji serwisu; open-in-view=false; encja nigdy w odpowiedzi.
     *   • Korzeń agregatu: ShopOrder.addLine przelicza sumę; OrderLine kopiuje cenę (unit_price).
     *   • Statusy: enum.allowedNext() ze switch (nie pole z konstruktora — illegal forward reference); odmowa → 409 + allowed.
     *   • Błędy: sealed ShopException + switch w poradzie; ResponseEntityExceptionHandler dla wyjątków Springa;
     *     errors dla @Valid i dla parametrów; Exception → 500 bez szczegółów.
     *   • DTO żądań: Integer/Boolean zamiast int/boolean (Jackson 3 odrzuca brak pola typu prostego).
     *   • Reguła w serwisie + ograniczenie w bazie; saveAndFlush/flush, żeby naruszenie złapać w serwisie.
     *   • Lista: projekcja DTO (select new ... count) + jawne countQuery → 1–2 zapytania; szczegóły: join fetch → 1.
     *   • Sortowanie z białej listy, rozmiar strony z limitem; własny PageResponse zamiast Page.
     *   • Security na classpath → jawny SecurityFilterChain z permitAll; w @WebMvcTest — @Import konfiguracji.
     *   • Testy: jednostkowe (reguły) → wycinki (HTTP, JPA) → pełny kontekst (scenariusze), bez @Transactional w pełnym.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego pozycja zamówienia ma własną kolumnę unit_price, skoro cenę zna produkt?
     *   2. Co się stanie? W CreateCustomerRequest pole vip jest typu boolean, a klient wyśle POST /api/customers
     *      z JSON-em {"name":"Piotr","city":"Łódź"} (bez vip). Jaki status i jakie ciało?
     *   3. Co się stanie? Kontroler przyjmuje Pageable prosto z adresu, a klient wyśle GET /api/products?sort=cena.
     *   4. ZNAJDŹ BŁĄD: enum OrderStatus { NOWE(EnumSet.of(OPLACONE, ANULOWANE)), OPLACONE(EnumSet.of(WYSLANE)), ... }
     *      z konstruktorem OrderStatus(Set<OrderStatus> next).
     *   5. ZNAJDŹ BŁĄD: @GetMapping("/api/orders") Page<ShopOrder> list(Pageable p) { return orders.findAll(p); }
     *      — wymień co najmniej trzy problemy.
     *   6. Po co saveAndFlush w ProductService.create, skoro wcześniej jest existsBySku?
     *   7. Co się stanie? PATCH /api/orders/3/status z {"status":"OPLACONE"} (ZAM-003 ma status WYSLANE).
     *   8. Dlaczego Cap01ShopApiTest NIE ma adnotacji @Transactional, choć zmienia dane?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA (rozszerzenia sklepu) — w klasie Cap01Exercises, testy: Cap01ShopApiExercisesTest (▶ sam),
    // rozwiązania: s21_capstone.solutions.cap01_shop_api.Cap01ShopApiSolutions.
    //   ĆWICZENIE 1 (łatwe): kody rabatowe — suma zamówienia po rabacie, ważność liczona z Clock, kod przeterminowany → 409.
    //   ĆWICZENIE 2 (średnie): raport sprzedaży po kategoriach — jedno zapytanie JPQL z projekcją, bez anulowanych.
    //   ĆWICZENIE 3 (trudniejsze): blokada zamówienia przy braku towaru — zdejmowanie stanów i zamówienie w JEDNEJ
    //   transakcji; brak towaru w drugiej pozycji nie może zmienić stanu pierwszej.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo cena produktu się zmienia, a zamówienie to dokument z chwili zakupu. Bez kopii podwyżka ceny laptopa
     *      zmieniłaby sumy wszystkich starych zamówień. Test priceSnapshot: cena słuchawek 399.90, a ZAM-001 nadal
     *      ma 349.90 i sumę 764.79.
     *   2. 400 Bad Request, application/problem+json z "detail":"Failed to read request" i BEZ mapy errors — Jackson 3
     *      domyślnie odrzuca brak wartości dla typu prostego (FAIL_ON_NULL_FOR_PRIMITIVES), więc walidacja nawet nie
     *      rusza (sprawdzone uruchomieniem). Z Boolean vip ten sam JSON daje 201 i vip=false.
     *   3. Spring Data wykona zapytanie dopiero w repozytorium i rzuci PropertyReferenceException „No property 'cena'
     *      found for type 'Product'” — nasza siatka bezpieczeństwa zamieni to na 500 „Wystąpił błąd wewnętrzny”
     *      (sprawdzone uruchomieniem). Błąd klienta wygląda jak awaria serwera. Stąd biała lista i 400.
     *   4. Kod się nie skompiluje: „error: illegal forward reference” — NOWE odwołuje się do OPLACONE, które jeszcze
     *      nie zostało utworzone. Poprawka: metoda allowedNext() ze switch albo mapa w bloku static po stałych.
     *   5. (a) encja w odpowiedzi — wyciek pól, sprzężenie API ze schematem, leniwe relacje (klient, pozycje) przy
     *      pisaniu JSON-a poza transakcją kończą się błędem przy open-in-view=false (s09_jpa_relations/Rel06EntityToDto);
     *      (b) Page wprost — niestabilny JSON i ostrzeżenie Spring Data; (c) Pageable od klienta — dowolne sortowanie
     *      (500 albo zgadywanie ukrytych pól) i strony do 2000 wierszy; (d) mapowanie encji z leniwymi relacjami = N+1.
     *   6. existsBySku i INSERT to dwa kroki (check-then-act): dwa równoległe żądania mogą oba przejść sprawdzenie.
     *      Wtedy ratuje nas uk_products_sku, a saveAndFlush wysyła INSERT od razu, więc naruszenie
     *      (DataIntegrityViolationException) łapiemy w serwisie i zamieniamy na ten sam 409 SKU_TAKEN zamiast 500 przy
     *      commicie. Test databaseConstraintIsLastLineOfDefence udaje taki wyścig szpiegiem.
     *   7. 409 Conflict, application/problem+json: "code":"ORDER_STATUS_CHANGE", "currentStatus":"WYSLANE",
     *      "requestedStatus":"OPLACONE", "allowed":["DOSTARCZONE"]; status w bazie się nie zmienia (rollback).
     *   8. Bo żądanie MockMvc dołączyłoby do transakcji testu: leniwe relacje działałyby mimo open-in-view=false,
     *      a commit i błędy ograniczeń przy commicie nigdy by nie wystąpiły — test ukryłby błędy, które zobaczy prawdziwy
     *      klient. Zamiast wycofywania każdy test pracuje na własnych danych.
     */
    // </editor-fold>
}
