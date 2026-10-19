package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.transaction.support.TransactionTemplate;

/**
 * <pre>
 * TEMAT: Encja czy DTO w odpowiedzi kontrolera — dlaczego kontroler NIE zwraca encji
 *        (DTO = Data Transfer Object, obiekt do przenoszenia danych; mapper = maper, tłumacz encja → DTO;
 *         serialization = serializacja, zamiana obiektu na JSON; cycle = cykl; leak = wyciek)
 *
 * W SKRÓCIE:
 *   Encja opisuje TABELĘ (wszystkie kolumny, relacje, leniwe pola). Odpowiedź API opisuje to, co chcemy pokazać
 *   KLIENTOWI. Gdy kontroler zwraca encję, Jackson (biblioteka JSON w Spring MVC) wypisuje wszystko, co ma getter:
 *   wyciekają pola wewnętrzne (cena zakupu, notatki), leniwe relacje kończą się wyjątkiem albo dodatkowymi
 *   zapytaniami w czasie pisania JSON, relacja dwukierunkowa tworzy nieskończony cykl, a każda zmiana encji
 *   (nowa kolumna, nowa nazwa pola) po cichu zmienia API. Rozwiązanie: rekord DTO z dokładnie tymi polami, które
 *   chcemy pokazać, zbudowany w SERWISIE, wewnątrz transakcji.
 *
 * ANALOGIA: karta dań kontra kuchnia.
 *   Klient restauracji dostaje kartę dań (DTO): nazwę, opis, cenę. Nie dostaje receptury, cen hurtowych od
 *   dostawcy ani zeszytu z notatkami szefa kuchni (encja). Gdy kuchnia zmienia dostawcę albo przestawia półki,
 *   karta się nie zmienia — klienta to nie obchodzi.
 *
 * JAK TO DZIAŁA:
 *   ŹLE:     EntityController → shopService.productEntity(sku) → Product (encja) → Jackson → JSON ze WSZYSTKIM
 *   DOBRZE:  DtoController → shopService.product(sku)  [@Transactional: find + ProductDto.from(encja)]
 *              → ProductDto (rekord) → Jackson → JSON z wybranymi polami
 *   Jackson zamienia obiekt na JSON PO powrocie z metody kontrolera — czyli już po zakończeniu transakcji serwisu.
 *   Profil rel06 ma spring.jpa.open-in-view=false (dobra praktyka z Rel04LazyLoading), więc sesji Hibernate
 *   w tym momencie już nie ma.
 *
 * SŁÓWKA:
 *   DTO (Data Transfer Object) = obiekt do przenoszenia danych; mapper = maper (zamienia jeden obiekt na drugi);
 *   mapping = mapowanie; serialization = serializacja (obiekt → JSON); leak = wyciek; cycle = cykl, zapętlenie;
 *   nesting depth = głębokość zagnieżdżenia; response committed = odpowiedź już wysłana (zatwierdzona);
 *   coupling = sprzężenie (zależność); factory method = metoda fabryczna (statyczna metoda tworząca obiekt).
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel04LazyLoading (open-in-view, LazyInitializationException),
 *             s09_jpa_relations/Rel05NPlusOne (projekcja DTO prosto z zapytania), s04_web_rest/Rest04Json
 *             (Jackson w Spring MVC), t09_records/Records01Basics (rekordy), t20_lombok/Lombok03DataValueBuilder
 *             (Lombok: @Data, @Value, @Builder — także pułapki @Data na encjach).
 * </pre>
 */
public final class Rel06EntityToDto {

    private Rel06EntityToDto() {
    }

    // =================================================================================================
    // 1. Wyciek pól wewnętrznych i sprzężenie API ze schematem bazy
    // =================================================================================================
    //
    // GET /api/rel06/entities/products/ELE-001 (EntityController zwraca encję Product) — odpowiedź z testu:
    //     {"id":1,"name":"Laptop Pro 14","price":5499.99,"purchasePrice":4100.00,"sku":"ELE-001"}
    // Cena zakupu (purchasePrice) i techniczne id z bazy poszły do każdego, kto zna adres. Ten sam produkt przez
    // DtoController (GET /api/rel06/products/ELE-001):
    //     {"sku":"ELE-001","name":"Laptop Pro 14","price":5499.99}
    //
    // Sprzężenie (coupling): w JSON encji nazwy pól to nazwy pól Javy. Zmień w encji purchasePrice na costPrice,
    // dodaj kolumnę „supplier” — i API zmienia się samo, bez niczyjej decyzji, psując aplikacje klientów.
    // Z DTO zmiana schematu bazy zatrzymuje się w maperze.
    //
    // PUŁAPKA: „ukryję pole adnotacją @JsonIgnore na encji” (Jackson 3 trzyma adnotacje w starym pakiecie
    //   com.fasterxml.jackson.annotation). Dlaczego to słabe rozwiązanie? Encja zaczyna znać szczegóły API, a jedna
    //   encja ma wtedy JEDEN kształt JSON dla wszystkich punktów końcowych (endpointów) — panel administratora
    //   i sklep internetowy potrzebują różnych pól. Każde nowe pole encji jest domyślnie PUBLICZNE — wystarczy
    //   zapomnieć o adnotacji.

    // =================================================================================================
    // 2. Leniwa relacja w encji + open-in-view=false → wyjątek w czasie pisania JSON
    // =================================================================================================
    //
    // GET /api/rel06/entities/customers/{id} — kontroler zwraca encję Customer z leniwą listą orders.
    // Serwis już zamknął transakcję i sesję; Jackson woła getOrders(), iteruje PersistentBag i... (z testu):
    //     500, HttpMessageNotWritableException: Could not write JSON: Cannot lazily initialize collection of role
    //     's09_jpa_relations.rel06_entity_to_dto.Customer.orders' with key '1' (no session)
    //     (przyczyny: tools.jackson.databind.DatabindException → org.hibernate.LazyInitializationException)
    // W logu serwera tylko WARN z DefaultHandlerExceptionResolver („Resolved [...]”). Błąd powstaje POZA naszym
    // kodem — w Jacksonie, po wyjściu z kontrolera — więc try/catch w kontrolerze nic tu nie pomoże.

    // =================================================================================================
    // 3. Relacja dwukierunkowa + open-in-view=true → nieskończony cykl, a klient dostaje 200
    // =================================================================================================
    //
    // Ten sam adres z open-in-view=true: sesja żyje do końca żądania, więc Jackson dociąga orders (dodatkowy
    // SELECT w czasie pisania JSON!), w zamówieniu widzi getCustomer() → ten sam klient → jego orders → ...
    // Jackson 3 przerywa przy limicie zagnieżdżenia (z testu):
    //     HttpMessageNotWritableException: Could not write JSON: Document nesting depth (501) exceeds the maximum
    //     allowed (500, from `StreamWriteConstraints.getMaxNestingDepth()`)
    //     (through reference chain: ...Customer["orders"]->...PersistentBag[0]->...ShopOrder["customer"]->...)
    // Najgorsze: część odpowiedzi jest już wysłana („Ignoring exception, response committed already” w logu),
    // więc klient dostaje status 200 i 25 705 bajtów JSON (sprawdzone na prawdziwym serwerze: składniowo poprawny,
    // 166 poziomów klient → zamówienie → klient...). Klient API nie dostaje ŻADNEGO sygnału błędu.
    //
    // PUŁAPKA: zwracanie encji z relacją dwukierunkową. Dlaczego to ważne? Zależnie od ustawień kończy się 500
    //   (sekcja 2), dodatkowymi zapytaniami w czasie serializacji albo „poprawną” odpowiedzią ze śmieciami.
    //   Plastry z Jacksona (@JsonIgnore, @JsonManagedReference/@JsonBackReference) zatrzymują cykl, ale wiążą
    //   encję z API (sekcja 1) i nie rozwiązują problemu leniwych pól.

    // =================================================================================================
    // 4. DTO: rekord z wybranymi polami
    // =================================================================================================
    //
    // public record ProductDto(String sku, String name, BigDecimal price) {
    //     public static ProductDto from(Product product) { ... }      // metoda fabryczna — mapowanie w DTO
    // }
    // public record CustomerDto(String name, String city, String email, List<OrderDto> orders) {
    //     public record OrderDto(String number, String status) { }   // zamówienie BEZ klienta → brak cyklu
    // }
    // Rekord (t09_records/Records01Basics) to idealne DTO: niezmienny, zwięzły, bez adnotacji JPA, Jackson
    // zapisuje jego komponenty w kolejności deklaracji. DTO tworzy DRZEWO danych (klient → zamówienia), a nie graf
    // z powrotami (zamówienie → klient), więc cyklu nie da się zrobić.
    //
    // Dwa sposoby mapowania (oba w lekcji):
    //   • statyczna metoda from(encja) w DTO (ProductDto.from) — proste DTO, mapowanie blisko danych;
    //   • osobna klasa maper (CustomerMapper, bean Springa) — większe mapowania, zależności, DTO bez wiedzy o encjach.
    // Lombok (@Value, @Builder — t20_lombok/Lombok03DataValueBuilder) skraca klasy DTO, gdy nie są rekordami,
    // a MapStruct (biblioteka spoza tego kursu) generuje kod maperów w czasie kompilacji. Zasada ta sama: encja
    // NIE wychodzi z serwisu.
    //
    // PUŁAPKA: Lombok @Data na ENCJI. Dlaczego? Generuje equals/hashCode/toString ze WSZYSTKICH pól, także
    //   z relacji: toString/hashCode przechodzą po relacji dwukierunkowej w kółko (StackOverflowError) albo
    //   uruchamiają leniwe ładowanie (zapytania, LazyInitializationException). Na encjach — tylko @Getter i ręczne
    //   equals/hashCode po id (Rel01ManyToOne).

    // =================================================================================================
    // 5. Gdzie mapować? W serwisie, w transakcji
    // =================================================================================================
    //
    // ShopService.customer(id):
    //     Customer c = customers.findWithOrdersById(id)   // left join fetch c.orders — 1 zapytanie (SQL z logu:
    //                                                     // ... from customers c1_0 left join orders o1_0 ...)
    //     return customerMapper.toDto(c);                 // sesja otwarta — getOrders() bezpieczne
    // Kontroler dostaje gotowy CustomerDto i nie wie nic o Hibernate. Odpowiedź (z testu):
    //     {"name":"Jan Kowalski","city":"Warszawa","email":"jan@example.com",
    //      "orders":[{"number":"ZAM-001","status":"NOWE"},{"number":"ZAM-003","status":"WYSLANE"}]}
    //
    //   • w KONTROLERZE — za późno: transakcja zamknięta, leniwe pola → LazyInitializationException
    //     (albo, z open-in-view, zapytania poza transakcją — Rel04LazyLoading);
    //   • w SERWISIE — dobrze: transakcja otwarta, a zapytanie (join fetch) dobierasz do tego, czego DTO potrzebuje;
    //   • w ZAPYTANIU — jeszcze lepiej dla odczytu płaskich danych: projekcja select new ...Dto(...)
    //     (Rel05NPlusOne) — bez encji w ogóle.
    //
    // DOBRA PRAKTYKA: serwis przyjmuje i zwraca DTO (albo typy proste), encje żyją tylko wewnątrz transakcji.
    //   Dlaczego? Granica transakcji = granica świata Hibernate. Poza nią są tylko zwykłe obiekty, które można
    //   bezpiecznie serializować, cache'ować i testować bez bazy.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kontroler zwraca DTO (rekord), nigdy encję.
     *   • Encja w JSON: wyciek pól (id, cena zakupu, notatki) + API zmienia się razem ze schematem bazy.
     *   • Leniwe pole + open-in-view=false → 500, HttpMessageNotWritableException ← LazyInitializationException.
     *   • Relacja dwukierunkowa + open-in-view=true → cykl, „Document nesting depth (501) exceeds the maximum
     *     allowed (500...)”, a klient i tak dostaje 200 z uciętym (ale poprawnym składniowo) JSON.
     *   • Mapowanie: DTO.from(encja) albo klasa maper; w SERWISIE, w transakcji, z join fetch pod potrzeby DTO.
     *   • DTO to drzewo bez powrotów (zamówienie w DTO nie zna klienta) → brak cykli.
     *   • @JsonIgnore/@JsonManagedReference na encjach i Lombok @Data na encjach — plastry i pułapki.
     *
     * PYTANIA KONTROLNE:
     *   1. Wymień cztery problemy, które powoduje zwracanie encji z kontrolera.
     *   2. Co się stanie? Profil z open-in-view=false, kontroler zwraca encję Customer z leniwą listą zamówień.
     *   3. Co się stanie? To samo, ale z open-in-view=true — jaki status HTTP dostanie klient i co będzie w treści?
     *   4. ZNAJDŹ BŁĄD: {@code @GetMapping("/products/{sku}") Product product(...) { return service.find(sku); }}
     *      — produkt nie ma żadnych relacji, test przechodzi. Co tu jest nie tak?
     *   5. Dlaczego mapowanie na DTO robimy w serwisie, a nie w kontrolerze?
     *   6. Jak CustomerDto unika cyklu, choć w encjach relacja klient–zamówienie jest dwukierunkowa?
     *   7. Czym różni się mapowanie metodą from w DTO od osobnej klasy maper? Kiedy wybrać które?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rel06EntityToDtoExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zamień encję zamówienia na rekord {@code CustomerDto.OrderDto} (numer i status).
     * Podpowiedź: {@code new CustomerDto.OrderDto(order.getNumber(), order.getStatus())}.
     */
    static CustomerDto.OrderDto exercise1(ShopOrder order) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj CustomerContactDto (imię i nazwisko + kontakt) dla działu obsługi klienta.
     * Kontakt: e-mail, a gdy klient go nie ma — tekst „brak e-maila, kontakt przez miasto: Kraków” (z miastem
     * klienta). Notatka wewnętrzna NIE może trafić do DTO. Podpowiedź: zwykły if albo operator warunkowy.
     */
    static CustomerContactDto exercise2(Customer customer) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć CustomerDto klienta o podanym imieniu i nazwisku. Test woła metodę POZA
     * transakcją i sprawdza, że całość kosztuje dokładnie 2 zapytania (znalezienie id + klient z zamówieniami).
     * Podpowiedź: {@code tx.execute(status -> ...)}: customers.findByName(name) → id → findWithOrdersById(id) →
     * mapper.toDto(...) — wszystko wewnątrz lambdy (w transakcji).
     */
    static CustomerDto exercise3(TransactionTemplate tx, CustomerRepository customers, CustomerMapper mapper,
                                 String name) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. (a) Wyciek pól wewnętrznych (id, cena zakupu, notatki); (b) leniwe pola: wyjątek albo zapytania w czasie
     *      serializacji; (c) cykle w JSON przy relacjach dwukierunkowych; (d) sprzężenie API ze schematem bazy —
     *      każda zmiana encji zmienia API.
     *   2. 500. Jackson przy zapisie JSON sięga po leniwą kolekcję po zamknięciu sesji: HttpMessageNotWritableException
     *      „Could not write JSON: Cannot lazily initialize collection of role '...Customer.orders' with key '1'
     *      (no session)”. Sprawdzone testem lazyRelationFailsDuringSerialization.
     *   3. Status 200 i około 25 KB JSON: klient → zamówienia → klient → ... aż do limitu zagnieżdżenia Jacksona
     *      (500). Wyjątek „Document nesting depth (501) exceeds the maximum allowed (500, ...)” ląduje tylko w logu
     *      („response committed already”), bo część odpowiedzi już wysłano. Sprawdzone testem
     *      Rel06EntityCycleTest.bidirectionalEntityCreatesCycle i ręcznie na serwerze (curl).
     *   4. Encja w odpowiedzi API: wyciekają id i purchasePrice (cena zakupu), a każda zmiana encji zmieni API.
     *      Sprawdzone testem entityLeaksInternalFields. Brak relacji nie czyni encji dobrym DTO.
     *   5. Bo w serwisie trwa transakcja i sesja jest otwarta — leniwe pola można czytać, a zapytanie (join fetch)
     *      dobrać do potrzeb DTO. W kontrolerze transakcja już się skończyła (open-in-view=false) → wyjątek.
     *   6. CustomerDto.OrderDto nie ma pola z klientem — DTO jest drzewem (klient → zamówienia) bez powrotów, więc
     *      Jackson nie ma po czym chodzić w kółko.
     *   7. from w DTO: krótko, mapowanie przy danych, dobre dla prostych DTO. Osobna klasa maper (bean): gdy mapowanie
     *      jest większe, potrzebuje innych beanów albo DTO ma nie znać encji (np. DTO w osobnym module API).
     */
    // </editor-fold>
}
