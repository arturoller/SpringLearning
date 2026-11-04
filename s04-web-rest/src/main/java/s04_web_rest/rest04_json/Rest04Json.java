package s04_web_rest.rest04_json;

/**
 * <pre>
 * TEMAT: JSON w Springu — Jackson 3: rekordy, nazwy pól, daty, nieznane pola, pieniądze
 *        (JSON = JavaScript Object Notation, tekstowy format danych; serialization = serializacja, zamiana obiektu
 *         na tekst; deserialization = deserializacja, zamiana tekstu na obiekt; mapper = „tłumacz” obiekt ↔ JSON;
 *         Jackson = biblioteka Javy do JSON, której Spring używa pod spodem)
 *
 * W SKRÓCIE:
 *   Gdy metoda kontrolera zwraca rekord, a Ty widzisz w odpowiedzi JSON, to robi to Jackson: zamienia obiekt na tekst
 *   (serializacja). Gdy metoda ma parametr z @RequestBody, Jackson robi odwrotnie — czyta tekst z ciała zapytania
 *   i buduje obiekt (deserializacja). Domyślne zasady są rozsądne (nazwa pola = nazwa komponentu rekordu, daty jako
 *   tekst ISO-8601, nieznane pola pomijane), a kilka adnotacji (@JsonProperty, @JsonIgnore, @JsonInclude, @JsonFormat)
 *   pozwala zmienić kształt JSON bez zmiany kodu Javy. Ta lekcja pokazuje, co dokładnie dostaje klient — bo JSON to
 *   „umowa” z klientem i każda zmiana nazwy pola jest zmianą tej umowy.
 *
 * ANALOGIA: tłumacz na spotkaniu dwóch firm.
 *   Twoja aplikacja mówi „po javowemu” (obiekty, rekordy, BigDecimal, LocalDate), klient mówi „po jsonowemu” (teksty,
 *   liczby, listy). Jackson to tłumacz między nimi. Dobry tłumacz ma ustalony słownik: „fullName” po jsonowemu mówi
 *   się „full_name”, „hasło” nie jest tłumaczone wcale (tajemnica firmy), a data zawsze jest zapisywana tak samo.
 *   Gdy tłumacz dostanie zdanie, którego nie rozumie (zła data, nieznana kategoria), przerywa i mówi „błędne zapytanie” (400).
 *
 * JAK TO DZIAŁA:
 *   1. Spring MVC ma listę konwerterów wiadomości (HttpMessageConverter — Rest01Controllers, sekcja 3).
 *      Dla application/json używa konwertera opartego na Jacksonie.
 *   2. Boot (autokonfiguracja JacksonAutoConfiguration) tworzy JEDEN bean typu JsonMapper i ustawia go według
 *      właściwości {@code spring.jackson.*}. Ten sam mapper obsługuje wszystkie kontrolery — dlatego ustawienie
 *      w jednym miejscu działa w całej aplikacji. Możesz go wstrzyknąć (@Autowired JsonMapper mapper).
 *   3. Odpowiedź: kontroler zwraca obiekt → mapper.writeValueAsString(obiekt) → ciało odpowiedzi.
 *      Żądanie: ciało → mapper.readValue(tekst, Klasa.class) → parametr @RequestBody.
 *   4. Gdy JSON nie pasuje do klasy (zły JSON, zła data, nieznana stała enuma, brak pola typu prostego), Jackson rzuca
 *      wyjątek, a Spring zamienia go na 400 Bad Request (HttpMessageNotReadableException).
 *
 * JACKSON 3 W BOOT 4 — CO SIĘ ZMIENIŁO (uczeń spotka stare tutoriale!):
 *   • Klasy Jacksona mają NOWY pakiet: {@code tools.jackson.databind.json.JsonMapper},
 *     {@code tools.jackson.databind.ObjectMapper}, {@code tools.jackson.core.JacksonException}.
 *     W starych tutorialach: {@code com.fasterxml.jackson.databind.ObjectMapper}. Kod ze starego pakietu się nie skompiluje.
 *   • ADNOTACJE zostały w starym pakiecie: {@code com.fasterxml.jackson.annotation.JsonProperty} itd. — tu nic
 *     się nie zmieniło. (Dziwne, ale prawdziwe: Jackson 3 dzieli adnotacje z wersją 2.)
 *   • Boot 4 tworzy bean {@code JsonMapper} (w Boot 3 był {@code ObjectMapper} z Jacksona 2).
 *   • Wyjątki Jacksona (JacksonException) są niekontrolowane — dziedziczą po RuntimeException, więc nie trzeba
 *     {@code throws} ani try/catch (w Jacksonie 2 był to IOException).
 *   • JsonMapper jest NIEZMIENNY (immutable): ustawienia dajesz przy budowie ({@code JsonMapper.builder()...build()}),
 *     a nie setterami po fakcie. W Boot dostosowujesz go właściwościami {@code spring.jackson.*} albo beanem
 *     {@code JsonMapperBuilderCustomizer}.
 *   • Moduł dat (java.time) jest wbudowany — nie trzeba osobnej zależności ani rejestracji.
 *   • Nowe nazwy niektórych właściwości (sekcja 3!). Brak starych nazw = błąd przy starcie aplikacji.
 *
 * SŁÓWKA:
 *   property = właściwość (pole w JSON; ale też ustawienie w application.properties — kontekst rozstrzyga);
 *   ignore = ignorować; include = dołączać; format = format; null = brak wartości;
 *   unknown = nieznany; timestamp = znacznik czasu (data zapisana liczbą); round trip = „tam i z powrotem”
 *   (obiekt → JSON → ten sam obiekt); cycle = cykl; customizer = dostosowywacz (kod, który zmienia domyślne ustawienia);
 *   scale = skala (liczba miejsc po przecinku w BigDecimal); precision = dokładność.
 *
 * ZOBACZ TEŻ: s04_web_rest/Rest02RequestData (@RequestBody, 400 przy złym JSON),
 *             s04_web_rest/Rest03Responses (kody odpowiedzi),
 *             s04_web_rest/Rest05CrudApi (JSON w pełnym API produktów),
 *             s05_validation/Val02ValidInControllers (sprawdzanie treści danych po deserializacji),
 *             s06_error_handling/Err01DefaultErrors (jak wygląda ciało odpowiedzi 400),
 *             s09_jpa_relations/Rel06EntityToDto (po co DTO zamiast encji — cykle referencji),
 *             s11_testing/Test01TestSlices (wycinki testowe, także @JsonTest),
 *             t09_records/Records01Basics (rekordy), t15_numbers/Numbers01BigDecimal (pieniądze),
 *             t17_datetime/DateTime03Formatting (formaty dat), t08_enums/Enums01Basics (enumy).
 * </pre>
 */
public final class Rest04Json {

    private Rest04Json() {
    }

    // =================================================================================================
    // 1. REKORD ↔ JSON I JSONMAPPER JAKO BEAN (Product, Rest04JsonMapperTest, Rest04JsonTest)
    // =================================================================================================
    //
    //   public record Product(long id, String name, BigDecimal price, Category category) { }
    //
    //   new Product(1, "Kawa", new BigDecimal("24.90"), Category.DRINKS)
    //     ⇄  {"id":1,"name":"Kawa","price":24.90,"category":"DRINKS"}
    //
    // Nazwy pól w JSON to nazwy komponentów rekordu, w kolejności z rekordu. Rekord ma konstruktor kanoniczny, więc
    // Jackson odczytuje go bez żadnych adnotacji (do klasy z setterami potrzebny byłby pusty konstruktor).
    //
    // Dwa testy tej samej rzeczy, na różnej wysokości:
    //   • Rest04JsonMapperTest — @JsonTest: najwęższy wycinek. Startuje tylko autokonfigurację Jacksona (bez Tomcata
    //     i kontrolerów). Dostajesz bean JsonMapper oraz JacksonTester<T> — pomocnika z asercjami na JSON
    //     (np. extractingJsonPathStringValue("$.name")). Szybki test „jak ten obiekt wygląda w JSON?”.
    //     W Boot 4 adnotacja nazywa się tak samo jak dawniej (org.springframework.boot.test.autoconfigure.json.JsonTest),
    //     ale JacksonTester używa Jacksona 3 (Jackson2Tester — dla Jacksona 2).
    //   • Rest04JsonTest — @WebMvcTest + MockMvc: cała droga przez HTTP (kontroler → konwerter → JSON → asercja
    //     content().json(...) albo jsonPath).
    //
    // Test sekcji 1 (uruchomiony): rekord → JSON jest dokładnie {"id":1,"name":"Kawa","price":24.90,"category":"DRINKS"},
    // a odczyt tego tekstu daje rekord równy oryginałowi. Mapper z Boota to JsonMapper z pakietu tools.jackson.databind.json.
    // Zły JSON ({id:1) i brak pola typu prostego (long id) to MismatchedInputException/StreamReadException, czyli
    // JacksonException — niekontrolowany wyjątek. Przez HTTP oba przypadki dają 400.
    //
    // PUŁAPKA: brak pola typu prostego (long, int, boolean) w JSON. Dlaczego? W Jacksonie 3 (Boot 4) to błąd
    //   (FAIL_ON_NULL_FOR_PRIMITIVES): Jackson odmawia wpisania „braku” w liczbę prostą, zamiast po cichu wstawić 0
    //   (tak było w Jacksonie 2). Jeśli pole ma być opcjonalne, zrób je opakowanym typem (Long, Integer) — wtedy
    //   brak = null. Pole opcjonalne nie może też mieć typu prostego z wartością domyślną „0”, która udaje dane.
    //
    // DOBRA PRAKTYKA: w kontrolerze nie twórz własnego mappera ({@code JsonMapper.builder().build()}); wstrzykuj bean.
    //   Dlaczego? Własny mapper nie zna ustawień {@code spring.jackson.*} ani modułów z autokonfiguracji — JSON
    //   zapisywany ręcznie wyglądałby inaczej niż JSON odsyłany przez kontrolery.

    // =================================================================================================
    // 2. ADNOTACJE: @JsonProperty, @JsonIgnore, @JsonInclude, @JsonFormat (Customer)
    // =================================================================================================
    //
    //   public record Customer(
    //           long id,
    //           @JsonProperty("full_name") String fullName,              // inna nazwa pola w JSON
    //           @JsonIgnore String passwordHash,                         // nigdy w JSON (ani w odpowiedzi, ani z żądania)
    //           @JsonInclude(JsonInclude.Include.NON_NULL) String phone, // null → pole znika
    //           @JsonFormat(pattern = "dd.MM.yyyy") LocalDate birthDate  // własny format daty
    //   ) { }
    //
    // Jedna adnotacja na komponencie rekordu działa dla serializacji i deserializacji. Test sekcji 2 (uruchomiony):
    //   • Odpowiedź dla klienta Anny: {"id":7,"full_name":"Anna Nowak","birthDate":"17.05.1990"} — nazwa zmieniona,
    //     hasła i telefonu (null) nie ma. Po ustawieniu telefonu pole "phone" się pojawia.
    //   • Odczyt: pole "full_name" trafia do fullName; STARA nazwa "fullName" w JSON jest teraz polem nieznanym
    //     (sekcja 4) — fullName zostaje null. Klient musi więc używać nowej nazwy!
    //   • @JsonIgnore działa też na WEJŚCIU: klient wysyła "passwordHash":"wstrzyknięty", a rekord dostaje null.
    //     Przez HTTP: POST /rest04/customers/inspect odpowiada "passwordHash=null".
    //
    // PRZED/PO (Plain bez adnotacji, Renamed z adnotacją):
    //   record Plain(String fullName)                           → {"fullName":"Jan"}
    //   record Renamed(@JsonProperty("full_name") String fullName) → {"full_name":"Jan"}
    //
    // @JsonInclude: NON_NULL pomija null; NON_EMPTY pomija też puste teksty i kolekcje; można ustawić dla całej klasy
    // (nad rekordem) lub globalnie: {@code spring.jackson.default-property-inclusion=non_null}.
    //
    // PUŁAPKA: zmiana nazwy pola w rekordzie wysyłanym do klienta (refaktor „fullName” → „name”). Dlaczego? Nazwa pola
    //   w JSON jest umową z klientem: każda przeglądarka, aplikacja mobilna i test, które czytają "fullName", od razu
    //   się psują — a kompilator Javy tego nie wykryje. Gdy musisz zmienić kod, a nie umowę, zostaw
    //   {@code @JsonProperty("fullName")} na nowej nazwie komponentu.
    //
    // PUŁAPKA: wysyłanie encji/rekordu z polem na sekret (hasło, token, hash) bez @JsonIgnore. Dlaczego groźne?
    //   Serializacja bierze WSZYSTKIE pola. Wystarczy, że ktoś doda pole do klasy, a trafi ono do klientów.
    //   Najpewniejsze jest osobne DTO wyjściowe bez sekretów (Rest05CrudApi: DTO wejściowe vs wyjściowe);
    //   @JsonIgnore to druga linia obrony.
    //
    // DOBRA PRAKTYKA: ustaw pole z hasłem tylko w klasie wewnętrznej (encji), a do kontrolera zwracaj DTO bez niego.
    //   Dlaczego? Czego nie ma w DTO, nie może wyciec — nawet gdy ktoś zapomni o adnotacji.

    // =================================================================================================
    // 3. DATY: LocalDate, Instant, FORMAT ISO-8601 I USTAWIENIA (Order, Rest04DateSettingsTest)
    // =================================================================================================
    //
    //   public record Order(long id, LocalDate orderDate, Instant placedAt, BigDecimal total, OrderStatus status) { }
    //
    //   {"id":100,"orderDate":"2026-03-15","placedAt":"2026-03-15T10:15:30Z","total":49.80,"status":"PAID"}
    //
    // Domyślnie (Jackson 3 i Boot 4) daty są TEKSTEM w formacie ISO-8601, nie liczbami:
    //   • LocalDate (data bez godziny)          → "2026-03-15"
    //   • Instant (punkt na osi czasu, UTC)     → "2026-03-15T10:15:30Z" (litera Z = strefa UTC)
    //   • LocalDateTime (data i godzina bez strefy) → "2026-03-15T10:15:00"
    // Test sekcji 3 sprawdza dokładnie ten tekst. Przy odczycie Instant akceptuje też przesunięcie strefy:
    // "2026-03-15T11:15:30+01:00" daje ten sam moment co 10:15:30Z (test instantWithOffset). Zła data
    // ("15-03-2026") i zła stała enuma ("LOST", ale też "paid" małymi literami) to InvalidFormatException → 400.
    //
    // USTAWIENIA GLOBALNE (application.properties):
    //   spring.jackson.datatype.datetime.write-dates-as-timestamps=true   → daty jako liczby: [2026,3,15] i
    //                                                                       1773569730.000000000
    //   spring.jackson.date-format=…                                       → format dla java.util.Date
    // Test Rest04DateSettingsTest: z tą właściwością LocalDate daje [2026,3,15], a Instant — liczbę sekund.
    //
    // PUŁAPKA: stara nazwa właściwości z Boot 3 — {@code spring.jackson.serialization.write-dates-as-timestamps}.
    //   Dlaczego groźne? W Jacksonie 3 ta opcja przeniosła się do innej klasy (DateTimeFeature, a nie
    //   SerializationFeature), więc pod starą nazwą Boot 4 nie znajduje stałej enuma i APLIKACJA NIE STARTUJE
    //   (błąd wiązania właściwości). Test oldPropertyNameFailsStartup to potwierdza. Dobrze, że głośno — ale kopiując
    //   ustawienia ze starych tutoriali sprawdzaj nazwy w dokumentacji Boot 4 (spring.jackson.datatype.datetime.*,
    //   spring.jackson.datatype.enum.*, spring.jackson.deserialization.*, spring.jackson.serialization.*).
    //
    // DOBRA PRAKTYKA: w API zostaw daty jako tekst ISO-8601 i wysyłaj momenty (Instant) w UTC. Dlaczego? Format ISO jest
    //   czytelny dla człowieka, sortuje się alfabetycznie jak chronologicznie, a każdy język programowania go parsuje.
    //   Liczby (timestamp) zmuszają klienta do zgadywania, czy to sekundy, czy milisekundy. Własny format
    //   ({@code @JsonFormat(pattern = "dd.MM.yyyy")} jak w Customer) rób tylko, gdy umowa z klientem tego wymaga.

    // =================================================================================================
    // 4. NIEZNANE POLA W ŻĄDANIU (Rest04UnknownFieldsStrictTest)
    // =================================================================================================
    //
    // Klient wysyła {"id":1,"name":"Kawa","price":24.90,"color":"brązowy"}, a rekord Product nie ma pola "color".
    // Co się dzieje? Domyślnie — NIC złego: pole jest po cichu pomijane, kontroler dostaje poprawny Product,
    // odpowiedź 200. (Test unknownFieldsIgnored i unknownFieldIgnoredOverHttp. Sprawdzone w Boot 4.1.1: nawet
    // „czysty” JsonMapper.builder().build() z Jacksona 3 ignoruje nieznane pola; w Jacksonie 2 domyślnie rzucał
    // błąd i Boot wyłączał tę kontrolę — wynik dla Ciebie jest ten sam.)
    //
    // Czasem chcesz odwrotnie: zacząć żądać dokładnego kształtu JSON (np. literówka klienta „prise” zamiast „price”
    // nie powinna dać ceny null po cichu). Włączasz to jedną właściwością:
    //
    //   spring.jackson.deserialization.fail-on-unknown-properties=true
    //
    // Test Rest04UnknownFieldsStrictTest (ten sam kontroler, ta właściwość w @WebMvcTest(properties = …)):
    // nieznane pole "color" → 400 Bad Request, a poprawny JSON → 200.
    //
    // PUŁAPKA: literówka w nazwie pola przy domyślnym ignorowaniu. Dlaczego? Klient wysyła "prise":24.90 — pole jest
    //   pomijane, cena w rekordzie to null, a Ty dostajesz zapytanie bez błędu i bez ceny. Dla pól wymaganych
    //   walidacja z działu s05_validation (np. @NotNull) łapie brak wartości; ścisły tryb łapie literówkę od razu.
    //
    // DOBRA PRAKTYKA: zostaw domyślne ignorowanie nieznanych pól w API używanym przez wielu klientów. Dlaczego?
    //   Dzięki temu możesz DODAĆ pole do odpowiedzi, a starsi klienci (którzy go nie znają) nadal działają — to tzw.
    //   zasada tolerancyjnego czytelnika. Ścisły tryb włączaj świadomie, głównie dla wewnętrznych API.

    // =================================================================================================
    // 5. PIENIĄDZE (BigDecimal), ENUMY I CYKLE REFERENCJI
    // =================================================================================================
    //
    // BigDecimal dla cen — NIGDY double. Dlaczego? double zapisuje liczby dwójkowo, więc 0.1 nie istnieje dokładnie:
    // 0.1 + 0.2 = 0.30000000000000004 (test bigDecimalVersusDouble). BigDecimal liczy dziesiętnie: 0.1 + 0.2 = 0.3.
    // W JSON cena to LICZBA (24.90, bez cudzysłowu) i BigDecimal zachowuje skalę: "24.90" zostaje 24.90, podczas gdy
    // double zamieniłby ją w 24.9. Test pokazuje też, że ten sam rekord przyjmie cenę jako liczbę albo jako tekst
    // ("24.90") — Jackson konwertuje tekst na liczbę; mimo to wysyłaj liczbę.
    //   (Szczegóły pieniędzy w Javie: t15_numbers/Numbers01BigDecimal.)
    //
    // Enum w JSON to TEKST z nazwą stałej: "DRINKS", "PAID". Odczyt zna tylko dokładne nazwy: "FRUIT" (nieistniejąca)
    // i "paid" (inna wielkość liter) dają 400 (test unknownEnumIs400 i invalidDateAndEnum). Chcesz inne napisy w JSON?
    // @JsonProperty("small") nad stałą enuma (ćwiczenie 5).
    //
    // CYKLE REFERENCJI. Klasa Employee (szef zna podwładnych, podwładny zna szefa) to cykl: szef → team → Jan → boss →
    // szef → … Jackson idzie za referencjami bez końca, aż przekroczy limit głębokości (domyślnie 500 poziomów)
    // i rzuca wyjątek (StreamConstraintsException — komunikat „Document nesting depth … exceeds the maximum allowed”).
    // Test referenceCycle. W prawdziwej aplikacji z bazą danych encje JPA mają dokładnie takie powiązania dwukierunkowe
    // (Zamówienie ↔ Pozycje), więc zwracanie encji z kontrolera kończy się błędem 500 albo ogromną odpowiedzią.
    //
    // PUŁAPKA: zwracanie encji z relacjami dwukierunkowymi prosto z kontrolera. Dlaczego? Cykl referencji wysypuje
    //   serializację, a przy leniwym ładowaniu (lazy) Jackson dociąga z bazy całe grafy obiektów i wysyła dane,
    //   których klient nie powinien widzieć. Rozwiązanie na poziomie projektu: osobne DTO wyjściowe (rekordy bez
    //   powiązań zwrotnych) — s09_jpa_relations/Rel06EntityToDto. Adnotacje @JsonIgnore/@JsonManagedReference
    //   „łatają” cykl, ale zostawiają problem: kształt JSON jest przywiązany do kształtu bazy.
    //
    // PYTANIE REKRUTACYJNE: czym różni się serializacja od deserializacji i czemu w pieniądzach nie używa się double?
    //   (obiekt → tekst vs tekst → obiekt; double jest binarny, nie reprezentuje dokładnie ułamków dziesiętnych,
    //   więc sumy się „rozjeżdżają”; BigDecimal jest dziesiętny i niezmienny.)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Rekord zwracany z kontrolera → JSON (serializacja); @RequestBody → obiekt (deserializacja). Robi to Jackson 3.
     *   • Boot 4: bean tools.jackson.databind.json.JsonMapper; klasy w pakiecie tools.jackson.*, ADNOTACJE nadal
     *     w com.fasterxml.jackson.annotation; wyjątki Jacksona są niekontrolowane; mapper jest niezmienny.
     *   • @JsonProperty("x") — inna nazwa; @JsonIgnore — pole poza JSON (w obie strony); @JsonInclude(NON_NULL) — bez null;
     *     @JsonFormat(pattern = "…") — własny format daty.
     *   • Daty domyślnie tekstem ISO-8601: LocalDate "2026-03-15", Instant "2026-03-15T10:15:30Z".
     *     Liczby: spring.jackson.datatype.datetime.write-dates-as-timestamps=true (stara nazwa z serialization wywala start).
     *   • Nieznane pola w żądaniu: domyślnie ignorowane; ścisły tryb: spring.jackson.deserialization.fail-on-unknown-properties=true.
     *   • Złe dane (zły JSON, zła data, nieznana stała enuma, brak pola typu prostego) → 400.
     *   • Pieniądze: BigDecimal (liczba w JSON), nie double. Enum → tekst z nazwą stałej, wielkość liter ma znaczenie.
     *   • Cykl referencji → wyjątek przy serializacji; zwracaj DTO, nie encje.
     *   • Testy: @JsonTest (sam mapper, JacksonTester) → @WebMvcTest (przez HTTP).
     *
     * PYTANIA KONTROLNE:
     *   1. Co zwróci {@code mapper.writeValueAsString(new BigDecimal("24.90"))}, a co {@code mapper.writeValueAsString(24.90d)}?
     *      Dlaczego dla cen wybieramy BigDecimal?
     *   2. Co się stanie? Klient wysyła do POST /rest04/customers/inspect JSON z polem {@code "passwordHash":"abc"},
     *      a w rekordzie Customer pole passwordHash ma {@code @JsonIgnore}.
     *   3. Co się stanie? Klient wysyła {@code "orderDate":"15-03-2026"} do kontrolera, który przyjmuje rekord Order.
     *   4. ZNAJDŹ BŁĄD: w application.properties po migracji na Boot 4 zostało
     *      {@code spring.jackson.serialization.write-dates-as-timestamps=false}. Co się stanie przy starcie i jak poprawić?
     *   5. Co się stanie? Do kontrolera, który przyjmuje rekord Product, klient wysyła dodatkowe pole {@code "color":"red"}.
     *      Jak sprawić, żeby dostał 400?
     *   6. ZNAJDŹ BŁĄD: {@code import com.fasterxml.jackson.databind.ObjectMapper;} w nowym projekcie Boot 4. Co jest nie tak
     *      i jaki typ wstrzykniesz zamiast niego?
     *   7. Co się stanie? Zwracasz z kontrolera obiekt Employee, w którym szef wskazuje podwładnych, a podwładny szefa.
     *   8. Co się stanie? Klient wysyła {@code "status":"paid"}, a stała enuma nazywa się PAID.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — szkielet: ExerciseController, testy: Rest04JsonExercisesTest (@Tag("cwiczenie"))
    // =================================================================================================
    //
    // Mapowania są gotowe, a rekordy (GiftCard, Account, Coupon, Invoice, Parcel) leżą w ExerciseController.
    // W każdym ćwiczeniu dopisz adnotację Jacksona do rekordu ORAZ ciało metody. Rozwiązania wzorcowe:
    // Rest04SolutionsController (s04_web_rest.solutions.rest04_json).
    //
    // ĆWICZENIE 1 (łatwe): giftCard() ma zwrócić kartę ("GC-1", "ala@example.com"), a w JSON pole ownerEmail ma się
    //   nazywać owner_email. Podpowiedź: @JsonProperty("owner_email") na komponencie rekordu (sekcja 2).
    //
    // ĆWICZENIE 2 (łatwe): account() ma zwrócić konto ("ala", "hash-123"), ale w JSON nie może być passwordHash.
    //   Podpowiedź: @JsonIgnore (sekcja 2).
    //
    // ĆWICZENIE 3 (średnie): coupon() ma zwrócić kupon ("PROMO10", null), a w JSON ma być tylko "code" — bez
    //   "description": null. Podpowiedź: @JsonInclude(JsonInclude.Include.NON_NULL) (sekcja 2).
    //
    // ĆWICZENIE 4 (średnie): invoice() ma zwrócić fakturę ("FV/1/2026", 2026-01-31, 199.90) z datą w JSON jako
    //   "31.01.2026", a kwotą jako liczbą 199.90. Podpowiedź: @JsonFormat(pattern = "dd.MM.yyyy") (sekcje 2 i 3),
    //   kwota: new BigDecimal("199.90") (sekcja 5).
    //
    // ĆWICZENIE 5 (trudniejsze): enum ParcelSize ma w JSON małe litery: GET /rest04/cw/parcel zwraca paczkę LARGE jako
    //   {"size":"large"}, a POST /rest04/cw/parcel z {"size":"small"} odpowiada tekstem "rozmiar: SMALL".
    //   Podpowiedź: @JsonProperty("small") nad stałą SMALL (i analogicznie LARGE) — działa w obie strony (sekcja 5).

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code new BigDecimal("24.90")} daje {@code 24.90} (skala zachowana), a {@code 24.90d} daje {@code 24.9}.
     *      BigDecimal liczy dziesiętnie i dokładnie, a double jest binarny (0.1 + 0.2 = 0.30000000000000004),
     *      więc sumy w pieniądzach by się rozjeżdżały. Sprawdzone testem bigDecimalVersusDouble (sekcja 5).
     *   2. Pole jest zignorowane: rekord dostaje null, a endpoint odpowiada {@code passwordHash=null}. @JsonIgnore działa
     *      w obie strony, więc nikt nie wstrzyknie hasła przez żądanie. Sprawdzone testem jsonIgnoreOnInput (sekcja 2).
     *   3. Odpowiedź 400 Bad Request: format daty nie jest ISO ({@code yyyy-MM-dd}), Jackson rzuca InvalidFormatException,
     *      a Spring zamienia to na 400. Sprawdzone testem datesOverHttp (sekcja 3).
     *   4. Aplikacja nie wystartuje: Jackson 3 ma tę opcję w DateTimeFeature, więc pod spring.jackson.serialization
     *      Boot nie znajduje stałej i zgłasza błąd wiązania właściwości. Poprawnie:
     *      {@code spring.jackson.datatype.datetime.write-dates-as-timestamps=false} (a to i tak wartość domyślna —
     *      można usunąć wiersz). Sprawdzone testem oldPropertyNameFailsStartup (sekcja 3).
     *   5. Odpowiedź 200: nieznane pole jest po cichu pomijane i nie wraca w odpowiedzi. Żeby było 400:
     *      {@code spring.jackson.deserialization.fail-on-unknown-properties=true}. Sprawdzone testami
     *      unknownFieldIgnoredOverHttp i Rest04UnknownFieldsStrictTest (sekcja 4).
     *   6. Pakiet com.fasterxml.jackson.databind to Jackson 2. W Boot 4 klasy Jacksona 3 są w pakiecie
     *      {@code tools.jackson.databind}, a Boot udostępnia bean {@code tools.jackson.databind.json.JsonMapper}
     *      — wstrzykujesz JsonMapper. (Adnotacje, np. @JsonProperty, nadal są w com.fasterxml.jackson.annotation.)
     *      Sprawdzone testem mapperIsJackson3Bean (sekcja 1).
     *   7. Serializacja się nie kończy: Jackson wędruje szef → podwładny → szef… aż do limitu głębokości (500)
     *      i rzuca wyjątek (StreamConstraintsException). Przez HTTP odpowiedź nie powstałaby (błąd serwera 5xx). Rozwiązanie: DTO bez
     *      powiązania zwrotnego. Sprawdzone testem referenceCycle (sekcja 5).
     *   8. 400 Bad Request: nazwy stałych enuma są czytane z uwzględnieniem wielkości liter, więc "paid" nie pasuje
     *      do PAID (InvalidFormatException). Sprawdzone testem invalidDateAndEnum (sekcja 3).
     */
    // </editor-fold>
}
