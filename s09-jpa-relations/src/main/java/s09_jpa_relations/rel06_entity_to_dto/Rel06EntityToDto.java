package s09_jpa_relations.rel06_entity_to_dto;

/**
 * <pre>
 * TEMAT: Encja a DTO — dlaczego encje nie wychodzą poza serwis, mapowanie encja → DTO i żądanie → encja
 *        (entity = encja; DTO, data transfer object = obiekt do przenoszenia danych; mapping = mapowanie,
 *         przepisywanie pól; request = żądanie; mass assignment = masowe przypisanie pól z żądania)
 *
 * W SKRÓCIE:
 *   Encja to model BAZY: ma leniwe relacje, cykle (zamówienie ↔ pozycja), pola tajne (skrót hasła) i reguły zapisu.
 *   Na zewnątrz (kontroler, JSON, inna usługa) oddajemy DTO — niezmienny rekord z dokładnie tymi polami, które są
 *   potrzebne odbiorcy. Do środka wpuszczamy żądanie (też rekord) z polami, które klient MOŻE ustawić; resztę
 *   (ceny, status, numer) ustala serwer. Mapowanie robi serwis, w swojej transakcji, ręcznie — metodą {@code from}
 *   w DTO albo osobną klasą-maperem.
 *
 * ANALOGIA: okienko w urzędzie.
 *   Urzędnik ma w szafie pełną teczkę sprawy (encja) — z notatkami wewnętrznymi, odnośnikami do innych teczek i
 *   danymi osobowymi. Petentowi nie daje teczki do ręki, tylko wydaje zaświadczenie (DTO) z tym, co trzeba. A wniosek
 *   (żądanie) petent wypełnia na formularzu z określonymi rubrykami — nie dopisuje sobie sam pieczątki „zatwierdzono”
 *   ani kwoty opłaty; te wpisuje urząd.
 *
 * JAK TO DZIAŁA:
 *   1. Serwis ({@code OrderService}, {@code @Transactional}) wczytuje encje — najlepiej jednym zapytaniem pod
 *      konkretne DTO ({@code join fetch}) albo od razu projekcją {@code select new} (Rel05, sekcja 5).
 *   2. W transakcji maper przepisuje pola do rekordu. Rekord nie ma relacji ani proxy — po wyjściu z serwisu nic się
 *      w nim nie doczyta i nie wybuchnie (Rel04).
 *   3. W drugą stronę serwis czyta żądanie, sam pobiera z bazy, co trzeba (klient, produkty, ceny), i buduje encję
 *      jej własnymi metodami ({@code new ShopOrder(…)}, {@code addLine(…)}) — encja pilnuje swoich reguł.
 *   4. Aktualizacja: serwis wczytuje ZARZĄDZANĄ encję i zmienia jej pola; przy zatwierdzeniu transakcji dirty checking
 *      (wykrywanie zmian) sam wysyła UPDATE.
 *
 * SŁÓWKA:
 *   DTO = obiekt do przenoszenia danych; mapper = maper (klasa tłumacząca jeden obiekt na drugi); request = żądanie;
 *   response = odpowiedź; serialization = serializacja (zamiana obiektu na JSON/tekst); cycle = cykl (A → B → A);
 *   mass assignment = masowe przypisanie (wpisanie do obiektu wszystkich pól z żądania); managed = zarządzana
 *   (encja w kontekście utrwalania); dirty checking = wykrywanie zmian; code generator = generator kodu.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest04Json (JSON, cykl referencji przy serializacji),
 *             s09_jpa_relations/Rel04LazyLoading (LazyInitializationException poza transakcją),
 *             s09_jpa_relations/Rel05NPlusOne (projekcja select new, join fetch bez N+1),
 *             s09_jpa_relations/Rel02OneToMany (addLine, kaskada zapisu pozycji),
 *             s08_data_jpa/Jpa01Entities (stany encji, dirty checking),
 *             t20_lombok/Lombok03DataValueBuilder (@Data i @Value — i dlaczego @Data nie na encje).
 * </pre>
 */
public final class Rel06EntityToDto {

    private Rel06EntityToDto() {
    }

    // =================================================================================================
    // 1. DLACZEGO NIE ZWRACAMY ENCJI na zewnątrz serwisu
    // =================================================================================================
    //
    //   PRZED (kusi, bo „mniej klas”):
    //     {@code public ShopOrder getOrder(String number) { return orders.findByOrderNumber(number).orElseThrow(); }}
    //     …a kontroler oddaje tę encję jako JSON.
    //
    // Pięć powodów, każdy sprawdzony testem (Rel06EntityToDtoTest, sekcja 1) albo w innej lekcji:
    //   a) LAZY POZA TRANSAKCJĄ: encja zwrócona z repozytorium bez transakcji → {@code getLines()} i
    //      {@code getCustomer().getName()} rzucają LazyInitializationException (test z NOT_SUPPORTED). DTO z
    //      {@code service.details(…)} w tej samej sytuacji działa. Szczegóły: s09_jpa_relations/Rel04LazyLoading.
    //   b) CYKLE przy serializacji: serializator JSON (Jackson) wypisuje wszystkie gettery. ShopOrder ma {@code lines},
    //      a OrderLine ma {@code order} typu ShopOrder (test na właściwościach JavaBeans) → zamówienie → pozycje →
    //      zamówienie → … — nieskończona rekurencja (s04_web_rest/Rest04Json). OrderLineDto nie ma pola order.
    //   c) UJAWNIANIE PÓL: Customer ma getter {@code getPasswordHash()} (potrzebny przy logowaniu) — serializator
    //      wypisałby skrót hasła (test: właściwości email, id, name, passwordHash). CustomerDto ma tylko id, name, email.
    //      To samo z wewnętrznymi id, flagami, polami audytu.
    //   d) SPRZĘŻENIE API ZE SCHEMATEM: zmiana nazwy kolumny/pola encji (albo nowa relacja) zmienia JSON i psuje
    //      klientów API (aplikację mobilną, inne usługi). DTO to osobny kontrakt — encja może się zmieniać pod spodem.
    //   e) MASS ASSIGNMENT: przyjęcie encji jako ciała żądania pozwala klientowi ustawić KAŻDE pole z setterem —
    //      status „PAID”, cenę, id. Dlatego żądanie to osobny rekord bez tych pól (CreateOrderRequest.Item ma tylko
    //      productId i quantity — test), a ShopOrder ma setter TYLKO do uwagi (test: jedyna zapisywalna właściwość = note).
    //
    // DOBRA PRAKTYKA: encje żyją WEWNĄTRZ warstwy serwisu i transakcji; przez granicę przechodzą tylko DTO.
    //   Dlaczego? Każdy z pięciu problemów znika naraz, a granica transakcji jest jasna: wszystko, co leniwe,
    //   dzieje się w serwisie.
    //   PYTANIE REKRUTACYJNE: „Dlaczego nie zwraca się encji JPA z kontrolera REST?”

    // =================================================================================================
    // 2. DTO jako rekordy; mapowanie ręczne w transakcji serwisu
    // =================================================================================================
    //
    //   {@code record OrderSummaryDto(String orderNumber, String customerName, OrderStatus status, BigDecimal total)}
    //   {@code record OrderDetailsDto(String orderNumber, String customerName, OrderStatus status, String note,}
    //   {@code                        List<OrderLineDto> lines, BigDecimal total)}
    //   {@code record OrderLineDto(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal)}
    //   {@code record CustomerDto(Long id, String name, String email)}
    //
    // Dlaczego rekordy? Są niezmienne (immutable — nie da się ich „po drodze” zepsuć), mają equals/hashCode/toString
    // z pól (łatwe asercje w testach: {@code isEqualTo(new OrderDetailsDto(…))}) i nie mają relacji ani proxy.
    //
    // Dwa style mapowania (oba w tej lekcji):
    //   • metoda statyczna w DTO: {@code CustomerDto.from(customer)}, {@code OrderLineDto.from(line)} — proste, blisko danych;
    //   • osobna klasa {@code OrderMapper.toDetails(order)} (bean @Component) — gdy mapowanie składa kilka DTO, potrzebuje
    //     innych beanów albo chcesz je trzymać z dala od rekordów. To zwykła klasa: test jednostkowy bez bazy (sekcja 5).
    //
    // Liczba zapytań jest pod kontrolą (sprawdzone testem):
    //   {@code service.details("ZAM-001")} → 1 zapytanie (findDetailsByOrderNumber):
    //     {@code select so1_0.id,…,c1_0.password_hash,l1_0.order_id,…,p1_0.name,p1_0.price,…,so1_0.status from orders so1_0}
    //     {@code join customer c1_0 on … left join order_line l1_0 on … left join product p1_0 on …}
    //     {@code where so1_0.order_number=? order by l1_0.id}
    //   {@code service.summaries()} → 1 zapytanie projekcji {@code select new …OrderSummaryDto(…) … group by …}, 0 encji.
    //   (Zauważ: zapytanie z encją klienta czyta też password_hash — wczytana encja ma wszystkie kolumny. DTO tego pola
    //   po prostu nie przepisuje; projekcja w ogóle go nie czyta.)
    //
    // PUŁAPKA: mapowanie POZA transakcją albo na encji wczytanej bez relacji. Dlaczego groźne? Poza transakcją —
    //   LazyInitializationException; w transakcji, ale bez join fetch — mapper „po cichu” robi N+1 (każde
    //   {@code line.getProduct().getName()} to SELECT). Zapytanie pod DTO + asercja na liczbie zapytań w teście.

    // =================================================================================================
    // 3. KIERUNEK ODWROTNY: CreateOrderRequest → encja (cena liczona po stronie serwera)
    // =================================================================================================
    //
    //   {@code record CreateOrderRequest(Long customerId, String note, List<Item> items)}
    //   {@code record Item(Long productId, int quantity)}          ← BEZ ceny, statusu, numeru i id
    //
    // OrderService.placeOrder (sprawdzone testem — Jan, Klawiatura ×2 + Kubek ×1):
    //     {@code select … from customer c1_0 where c1_0.id=?}
    //     {@code select p1_0.id,p1_0.name,p1_0.price from product p1_0 where p1_0.id in (?,?)}   ← findAllById: 1 zapytanie
    //     {@code select count(*) from orders so1_0}                                              ← numer ZAM-004
    //     {@code insert into orders (customer_id,note,order_number,status,id) values (?,?,?,?,default)}
    //     {@code insert into order_line (…) values (…)}  ×2                                      ← kaskada
    //   Wynik: ZAM-004, status NEW, wartość 324.30 = 2 × 149.90 + 24.50 — z cen PRODUKTÓW w bazie.
    //   Nieznany produkt → {@code IllegalArgumentException("Nieznany produkt: 99")}; ilość 0 → encja odmawia
    //   („Ilość musi być dodatnia: 0”) — oba sprawdzone testem.
    //
    // PUŁAPKA: ufanie cenie z żądania. Dlaczego groźne?
    //   PRZED: {@code record Item(Long productId, int quantity, BigDecimal unitPrice)} i
    //          {@code order.addLine(product, item.quantity(), item.unitPrice())}
    //   Każdy, kto potrafi wysłać żądanie HTTP (curl, narzędzia przeglądarki), zamówi monitor za 0.01 zł. Formularz na
    //   stronie nie chroni — żądanie da się wysłać z pominięciem formularza.
    //   PO: w żądaniu tylko id i ilość; cena = {@code product.getPrice()} z bazy (OrderService.placeOrder). To samo dotyczy
    //   rabatów, statusów, ról, „isAdmin”: wszystko, co ma wartość biznesową, ustala serwer.
    //
    // DOBRA PRAKTYKA: wszystkie produkty żądania jednym {@code findAllById} (IN) zamiast {@code findById} w pętli.
    //   Dlaczego? Pętla to N+1 przy zapisie (zamówienie na 30 pozycji = 30 SELECT-ów). Przy okazji od razu widać
    //   nieznane id: brakuje ich w mapie wyników.

    // =================================================================================================
    // 4. GDZIE MAPOWAĆ i jak AKTUALIZOWAĆ (zmieniaj zarządzaną encję, nie twórz nowej z DTO)
    // =================================================================================================
    //
    // Gdzie? W warstwie SERWISU: kontroler zna tylko DTO/żądania (HTTP ↔ rekordy), serwis zna DTO i encje (to on ma
    // transakcję), repozytorium zna encje i projekcje. Encja nie wie nic o DTO (nie ma metody {@code toDto()}) —
    // model bazy nie zależy od kształtu API.
    //
    // Aktualizacja — OrderService.changeNote (sprawdzone testem):
    //   {@code ShopOrder order = orders.findByOrderNumber(number).orElseThrow();  order.setNote(request.note());}  — bez save!
    //     {@code select so1_0.id,so1_0.customer_id,so1_0.note,so1_0.order_number,so1_0.status from orders so1_0 where so1_0.order_number=?}
    //     {@code update orders set customer_id=?,note=?,order_number=?,status=? where id=?}
    //   Encja jest zarządzana (managed): Hibernate przy wczytaniu zapamiętał jej stan, a przy flush porównał go z obecnym
    //   (dirty checking) i wysłał UPDATE. Domyślnie UPDATE zawiera WSZYSTKIE kolumny (jedno polecenie na encję, łatwe
    //   do zbuforowania); {@code @DynamicUpdate} (Hibernate) wysyłałby tylko zmienione.
    //
    // PUŁAPKA: budowanie NOWEJ encji z DTO i {@code save}. Dlaczego groźne?
    //   {@code orders.save(new ShopOrder("ZAM-001", customers.getReferenceById(1L), "nowa uwaga"))} — nowy obiekt nie ma id,
    //   więc save = persist = INSERT, nie UPDATE. Sprawdzone testem: INSERT od razu przy save (IDENTITY — id nadaje
    //   baza), naruszenie UNIQUE → DataIntegrityViolationException „… UK_ORDERS_ORDER_NUMBER …”. Bez ograniczenia
    //   UNIQUE powstałby po cichu DUPLIKAT zamówienia. A gdyby nowa encja miała id (konstruktor z id, merge) —
    //   merge nadpisałby wszystkie kolumny wartościami z DTO, także te, których DTO nie zawiera (null w statusie,
    //   zgubione pozycje przy orphanRemoval).
    //
    // DOBRA PRAKTYKA: aktualizacja = {@code findById} + metody encji ({@code setNote}, {@code setQuantity}) w transakcji
    //   serwisu. Dlaczego? Zmieniasz tylko to, co żądanie pozwala zmienić; reszta stanu (status, pozycje, klient)
    //   zostaje nietknięta, a reguły encji (np. ilość > 0) działają także przy aktualizacji.

    // =================================================================================================
    // 5. BIBLIOTEKI: MapStruct i Lombok — krótko
    // =================================================================================================
    //
    // MapStruct (generator mapperów w czasie KOMPILACJI): piszesz interfejs
    //   {@code @Mapper interface OrderMapStruct { OrderLineDto toDto(OrderLine line); }}
    // a procesor adnotacji generuje klasę z ręcznym kodem przepisywania pól (bez refleksji — szybko i widać w target/).
    // Opłaca się przy wielu dużych DTO; przy kilku małych ręczne {@code from} jest czytelniejsze i nie wymaga zależności.
    // Ręczny maper ma jeszcze jedną zaletę: to zwykła klasa — test bez bazy i bez Springa (test sekcji 5: 0 zapytań).
    // W tym dziale MapStruct tylko wspominamy i nie dodajemy go do pom.xml. Zapowiedź: w projektach działu s21_capstone
// (REST API sklepu) cały ten podział — encje w środku, DTO i żądania na zewnątrz — złożysz w jedną aplikację.
    //
    // Lombok (t20_lombok/Lombok01Accessors, t20_lombok/Lombok03DataValueBuilder):
    //   • DTO — nie potrzeba: rekord Javy daje konstruktor, gettery, equals/hashCode i toString;
    //   • encje — najwyżej {@code @Getter} (i ewentualnie {@code @NoArgsConstructor(access = PROTECTED)}).
    // PUŁAPKA: {@code @Data} na encji. Dlaczego groźne? Generuje toString, equals i hashCode ze WSZYSTKICH pól, także
    //   relacji: toString inicjalizuje leniwe relacje (zapytania, LazyInitializationException) i przy relacji
    //   dwukierunkowej kończy się StackOverflowError (Rel02, sekcja 1); hashCode zależny od pól zmienia się po zapisie
    //   i gubi encje w HashSet (s08_data_jpa/Jpa01Entities); a settery do wszystkiego otwierają drogę mass assignment.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Encje nie wychodzą poza serwis: lazy poza transakcją, cykle w JSON, tajne pola, sprzężenie API ze schematem,
     *     mass assignment.
     *   • DTO = rekord z polami pod odbiorcę; żądanie = rekord z polami, które klient może ustawić (bez ceny/statusu/id).
     *   • Mapowanie w transakcji serwisu: {@code Dto.from(encja)} albo klasa {@code XxxMapper}; encja nie zna DTO.
     *   • Zapytanie pod DTO: {@code join fetch} relacji, których użyje maper, albo od razu {@code select new Dto(…)}.
     *   • Żądanie → encja: serwis pobiera z bazy (findAllById — 1 zapytanie), cenę bierze z produktu, numer/status nadaje sam.
     *   • Aktualizacja: findById + setter/metoda encji, bez save (dirty checking → UPDATE). Nie {@code save(new Encja(…z DTO…))}.
     *   • MapStruct — generator mapperów w czasie kompilacji (wzmianka); Lombok na encjach: @Getter tak, @Data nigdy.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień pięć powodów, dla których kontroler nie powinien zwracać encji JPA.
     *   2. Co się stanie? Serwis bez transakcji zwraca {@code orders.findByOrderNumber("ZAM-001").orElseThrow()}, a kontroler
     *      oddaje ją jako JSON (Jackson wypisuje wszystkie gettery).
     *   3. ZNAJDŹ BŁĄD: {@code record Item(Long productId, int quantity, BigDecimal unitPrice)} i w serwisie
     *      {@code order.addLine(product, item.quantity(), item.unitPrice())}.
     *   4. Co się stanie? {@code orders.save(new ShopOrder("ZAM-001", customers.getReferenceById(1L), "nowa uwaga"))} —
     *      z zamiarem zmiany uwagi w istniejącym ZAM-001 (order_number ma UNIQUE).
     *   5. Jakie polecenia SQL wyśle {@code changeNote} (findByOrderNumber + setNote, bez save) i dlaczego UPDATE zawiera
     *      wszystkie kolumny?
     *   6. Dlaczego rekordy dobrze nadają się na DTO, a słabo na encje?
     *   7. Gdzie w aplikacji mapować encje na DTO i dlaczego tam?
     *   8. Kiedy sięgnąć po MapStruct, a kiedy wystarczy ręczne {@code from}? Co jest nie tak z {@code @Data} na encji?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku DtoExercises.java (metody z TODO), testy: Rel06EntityToDtoExercisesTest.
    // Potrzebne zapytania dopisujesz w ExerciseShopOrderRepository.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) leniwe relacje poza transakcją → LazyInitializationException; (b) cykle referencji (zamówienie ↔ pozycja)
     *      → nieskończona rekurencja przy serializacji; (c) ujawnianie pól (skrót hasła, wewnętrzne dane); (d) API
     *      sprzężone ze schematem bazy — zmiana encji psuje klientów; (e) mass assignment — klient ustawia pola,
     *      których nie powinien (status, cena, id).
     *   2. Serializator zacznie od getterów: getLines() i getCustomer().getName() poza transakcją rzucą
     *      LazyInitializationException (sprawdzone testem z NOT_SUPPORTED). Gdyby sesja była otwarta (np. Open Session
     *      in View), wypisałby passwordHash klienta i wpadł w cykl zamówienie → pozycje → zamówienie…
     *   3. Cena pochodzi od klienta — każdy może wysłać żądanie z ceną 0.01. Cena musi pochodzić z bazy
     *      ({@code product.getPrice()}), a w rekordzie żądania nie powinno być pola unitPrice.
     *   4. Nowy obiekt bez id → save robi persist → INSERT (od razu, bo id nadaje IDENTITY). Duplikat numeru łamie
     *      UNIQUE → DataIntegrityViolationException „… UK_ORDERS_ORDER_NUMBER …” (sprawdzone testem); bez UNIQUE
     *      powstałby drugi ZAM-001. Poprawnie: findByOrderNumber + setNote w transakcji.
     *   5. SELECT zamówienia po numerze i przy flush {@code update orders set customer_id=?,note=?,order_number=?,status=? where id=?}
     *      (sprawdzone testem). Hibernate domyślnie aktualizuje wszystkie kolumny — jedno, stałe polecenie na encję
     *      (dobre do buforowania); tylko zmienione kolumny wysyła z @DynamicUpdate.
     *   6. DTO: niezmienne, equals/hashCode/toString z pól, zwięzłe — idealne do przenoszenia danych i asercji. Encje
     *      JPA muszą być zmienne (dirty checking), mieć konstruktor bez argumentów i nie mogą być final (proxy) —
     *      rekord tego nie spełnia, więc rekord nie może być encją.
     *   7. W warstwie serwisu, w jego transakcji: tam encje są zarządzane, leniwe relacje mogą się doczytać, a na
     *      zewnątrz wychodzi gotowy rekord. Kontroler zna tylko DTO, encja nie zna DTO.
     *   8. MapStruct — przy wielu i dużych DTO (mniej ręcznego, powtarzalnego kodu, generowany w czasie kompilacji);
     *      ręczne from — przy kilku małych DTO (czytelne, bez zależności). @Data na encji: toString/equals/hashCode ze
     *      wszystkich pól łącznie z relacjami (zapytania, LazyInitializationException, StackOverflowError przy relacji
     *      dwukierunkowej, zmienny hashCode) i settery do wszystkiego.
     */
    // </editor-fold>
}
