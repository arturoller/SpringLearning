package s04_web_rest.rest04_json;

import tools.jackson.databind.json.JsonMapper;

/**
 * <pre>
 * TEMAT: JSON w Spring Boot 4 — Jackson 3 (pakiety tools.jackson.*, JsonMapper), rekordy, @JsonProperty, @JsonIgnore,
 *        @JsonInclude, daty java.time, nieznane pola i ustawienia spring.jackson.* — co się zmieniło względem
 *        Jacksona 2 i Boot 3
 *        (JSON = JavaScript Object Notation, tekstowy format danych; serialization = serializacja, zapis obiektu jako
 *        JSON; deserialization = deserializacja, odczyt JSON-a do obiektu; mapper = „tłumacz” obiekt ↔ JSON)
 *
 * W SKRÓCIE:
 *   Każdy obiekt zwracany z @RestController i każde @RequestBody przechodzi przez Jacksona. Boot 4 używa Jacksona 3:
 *   klasy są w pakietach tools.jackson.* (JsonMapper, ObjectMapper, DeserializationFeature), a ADNOTACJE zostały
 *   w com.fasterxml.jackson.annotation (@JsonProperty, @JsonIgnore, @JsonInclude, @JsonFormat). Boot tworzy jeden
 *   bean JsonMapper i używa go w MVC. Domyślnie (sprawdzone testem): daty jako tekst ISO-8601 ("2026-10-01"),
 *   nieznane pola w zapytaniu są ignorowane, brak wartości dla pola typu prostego (int) to błąd 400, a właściwości
 *   spoza konstruktora są sortowane alfabetycznie. Zmieniasz to adnotacjami (jedno pole) albo spring.jackson.* (cała aplikacja).
 *
 * ANALOGIA: tłumacz przysięgły.
 *   Jackson tłumaczy dokumenty z „języka Javy” na „język JSON” i z powrotem. Adnotacje to dopiski na marginesie
 *   jednego dokumentu („to pole przetłumacz jako product_name”, „tego nie tłumacz — poufne”). Ustawienia
 *   spring.jackson.* to regulamin całego biura tłumaczeń („daty zawsze w formacie ISO”, „odrzucaj dokumenty
 *   z nieznanymi rubrykami”). Jackson 3 to nowe wydanie regulaminu — kilka zasad domyślnych się zmieniło.
 *
 * JAK TO DZIAŁA:
 *   JacksonAutoConfiguration (moduł spring-boot-jackson) tworzy bean JsonMapper (nazwa: jacksonJsonMapper)
 *     ← ustawienia spring.jackson.* ← beany JsonMapperBuilderCustomizer (własne poprawki w kodzie)
 *   Spring MVC bierze ten bean do konwertera JacksonJsonHttpMessageConverter:
 *     return product;           → konwerter → jsonMapper.writeValueAsString(product) → ciało odpowiedzi
 *     @RequestBody NewOrder o   → konwerter → jsonMapper.readValue(ciało, NewOrder.class)
 *   Błąd odczytu (zły JSON, zły typ, null do int) → HttpMessageNotReadableException → 400.
 *
 * SŁÓWKA:
 *   property = właściwość (pole w JSON-ie); ignore = ignoruj; include = dołącz; non null = nie-null; format = format;
 *   timestamp = znacznik czasu (liczba); unknown property = nieznana właściwość; primitive = typ prosty (int, long...);
 *   feature = cecha, przełącznik zachowania; builder = budowniczy; customizer = „dostrajacz” (bean poprawiający
 *   konfigurację); ISO-8601 = międzynarodowy standard zapisu dat ("2026-10-01", "2026-10-01T08:30:00Z").
 *
 * ZOBACZ TEŻ: t18_io_files/Io05JsonManual (JSON „ręcznie”, bez biblioteki), t28_networking_http/Http04JsonApi (JSON
 *             przez HTTP), t17_datetime/DateTime03Formatting (formaty dat), t09_records/Records01Basics (rekordy),
 *             s04_web_rest/Rest02RequestData (@RequestBody), s02_configuration/Cfg01Properties (ustawienia aplikacji).
 * </pre>
 */
public final class Rest04Json {

    private Rest04Json() {
    }

    // =================================================================================================
    // 1. JACKSON 3 W BOOT 4 — pakiety i bean JsonMapper (Rest04JsonTest, sekcja 1)
    // =================================================================================================
    //
    //   co                               Jackson 2 (Boot 2/3, stare tutoriale)       Jackson 3 (Boot 4)
    //   główna klasa                     com.fasterxml.jackson.databind.ObjectMapper  tools.jackson.databind.json.JsonMapper
    //                                                                                 (podklasa tools.jackson.databind.ObjectMapper)
    //   adnotacje                        com.fasterxml.jackson.annotation.*           BEZ ZMIAN: com.fasterxml.jackson.annotation.*
    //   wyjątki                          JsonProcessingException (sprawdzany)         JacksonException (niesprawdzany — RuntimeException)
    //   daty java.time                   osobny moduł jackson-datatype-jsr310          wbudowane w jackson-databind
    //
    // Boot tworzy bean JsonMapper — wstrzykujesz go jak każdy inny: konstruktor z parametrem JsonMapper.
    // Ten sam obiekt jest też dostępny jako ObjectMapper (to jego nadklasa) — sprawdzone testem.
    // Nie trzeba już try/catch przy writeValueAsString — wyjątek jest niesprawdzany.
    //
    // PUŁAPKA: kod z tutoriala do Boot 3 z importem com.fasterxml.jackson.databind.ObjectMapper w Boot 4 się nie
    //   skompiluje (tej biblioteki nie ma na classpath — ścieżce klas). Dlaczego adnotacje działają po staremu?
    //   Jackson 3 celowo zostawił je w starym pakiecie (artefakt jackson-annotations jest wspólny dla obu wersji), żeby
    //   modele z adnotacjami nie wymagały zmian. Zmieniasz importy klas, nie adnotacji.

    // =================================================================================================
    // 2. REKORDY I ADNOTACJE — @JsonProperty, @JsonIgnore, @JsonInclude (sekcja 2)
    // =================================================================================================
    //
    // Rekord → JSON: każdy składnik rekordu to jedna właściwość, w kolejności deklaracji. Odczyt: przez konstruktor.
    //   @JsonProperty("product_name") String name                     ← inna nazwa w JSON-ie (w obie strony)
    //   @JsonInclude(JsonInclude.Include.NON_NULL) String description ← pomiń pole, gdy null
    //   @JsonIgnore Integer purchasePriceGrosze                       ← nigdy nie wypuszczaj do JSON-a
    // GET /api/catalog/1 → {"id":1,"product_name":"Kawa","priceGrosze":2999,"description":"Ziarnista, 1 kg",
    //                       "addedOn":"2026-10-01","updatedAt":"2026-10-01T08:30:00Z"}
    // GET /api/catalog/2 → to samo bez "description" (null) — sprawdzone testem.
    //
    // PUŁAPKA: @JsonIgnore na składniku rekordu typu PROSTEGO (int). Zapis działa, ale KAŻDY odczyt takiego rekordu
    //   kończy się 400: Jackson nie czyta zignorowanego pola, więc do konstruktora podaje null, a null do int
    //   w Jacksonie 3 to błąd („Cannot map `null` into type `int`”). Dlaczego? Jackson 3 domyślnie włącza
    //   DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES (w Jacksonie 2 było wyłączone i po cichu wstawiał 0).
    //   POST /api/catalog/secret-echo → 400 nawet z polem w JSON-ie. Sprawdzone testem. Naprawa: typ obiektowy
    //   (Integer — jak w CatalogProduct) albo osobny rekord na wejście i na wyjście.
    //
    // DOBRA PRAKTYKA: osobne rekordy na wejście (NewProduct) i wyjście (Product), zamiast jednego z @JsonIgnore.
    //   Dlaczego? Każdy pokazuje dokładnie to, co trzeba; nie ma ryzyka, że nowe pole encji „wycieknie” do API
    //   (np. hasło), bo DTO (data transfer object — obiekt do przesyłania danych) zmieniasz świadomie.

    // =================================================================================================
    // 3. DATY java.time — tekst ISO-8601 (sekcja 3)
    // =================================================================================================
    //
    //   LocalDate     2026-10-01            →  "2026-10-01"
    //   LocalDateTime 2026-10-01 08:30      →  "2026-10-01T08:30:00"
    //   Instant       ...08:30 UTC          →  "2026-10-01T08:30:00Z"
    //   Duration      90 minut              →  "PT1H30M"
    // W Jacksonie 3 DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS (zapis dat jako liczb) jest domyślnie WYŁĄCZONE.
    // W Jacksonie 2 było włączone (LocalDate → [2026,10,1]), dlatego Boot 2/3 sam je wyłączał. Efekt dla Ciebie: tekst
    // ISO jak dawniej, ale teraz to domyślne zachowanie samego Jacksona — sprawdzone testem dla JsonMapper.builder().
    // Jeden format dla jednego pola: @JsonFormat(pattern = "dd.MM.yyyy") — Promotion: "validUntil":"31.10.2026".
    //
    // PUŁAPKA: data w innym formacie w zapytaniu, np. "01.10.2026" dla LocalDate bez @JsonFormat → 400 (JSON parse
    //   error). Dlaczego? Jackson czyta tylko ISO; nie zgaduje, czy 01.10 to 1 października czy 10 stycznia.
    //
    // DOBRA PRAKTYKA: w API używaj ISO-8601 (i Instant/UTC dla chwil w czasie), a format „po polsku” zostaw
    //   interfejsowi użytkownika. Dlaczego? ISO jest jednoznaczne, sortuje się poprawnie jako tekst i każdy język
    //   programowania umie je czytać.

    // =================================================================================================
    // 4. NIEZNANE POLA, BRAKUJĄCE POLA, KOLEJNOŚĆ — domyślne zachowanie Jacksona 3 (sekcja 4)
    // =================================================================================================
    //
    //   przełącznik (feature)                         Jackson 2  Jackson 3  co to znaczy w Boot 4 (sprawdzone testem)
    //   FAIL_ON_UNKNOWN_PROPERTIES                    true       false      nieznane pole "color" → ignorowane, 200
    //   FAIL_ON_NULL_FOR_PRIMITIVES                   false      true       brak/null dla int → 400 (nie po cichu 0)
    //   WRITE_DATES_AS_TIMESTAMPS (DateTimeFeature)   true       false      daty jako tekst ISO
    //   SORT_PROPERTIES_ALPHABETICALLY                false      true       pola alfabetycznie, ALE parametry
    //                                                                       konstruktora (składniki rekordu) pierwsze,
    //                                                                       w kolejności konstruktora
    // (Wartości Jacksona 2 sprawdzone na jackson-databind 2.21.0, Jacksona 3 — na 3.1.5 z Boot 4.1.1.)
    // Boot 2/3 i tak wyłączał FAIL_ON_UNKNOWN_PROPERTIES i WRITE_DATES_AS_TIMESTAMPS, więc te dwa zachowania „dla
    // użytkownika Boota” się nie zmieniły. Zmieniły się: null dla typów prostych i kolejność pól.
    // ShelfLabel (bez konstruktora z parametrami) ma gettery price, name, barcode → JSON {"barcode":...,"name":...,
    // "price":...}. Rekord Promotion(code, validUntil) → kolejność składników, bo to parametry konstruktora. Klasa
    // z konstruktorem (int zeta, String beta) i dodatkowym getterem getAlpha() → {"zeta":..,"beta":..,"alpha":..}
    // — najpierw parametry konstruktora, potem reszta alfabetycznie (sprawdzone uruchomieniem).
    //
    // PUŁAPKA: test porównujący JSON jako TEKST ("{\"price\":...,\"name\":...}") po przejściu na Boot 4 może
    //   przestać przechodzić, bo zmieniła się kolejność pól. Dlaczego to nie błąd Jacksona? Kolejność
    //   pól w obiekcie JSON nie ma znaczenia — porównuj JSON jako strukturę (content().json(...), jsonPath).

    // =================================================================================================
    // 5. USTAWIENIA spring.jackson.* — dla całej aplikacji (Rest04JsonPropertiesTest)
    // =================================================================================================
    //
    //   spring.jackson.datatype.datetime.write-dates-as-timestamps=true  → LocalDate jako [2026,10,1]
    //   spring.jackson.deserialization.fail-on-unknown-properties=true   → nieznane pole → 400
    //   spring.jackson.default-property-inclusion=non_null               → wszystkie null pomijane (bez adnotacji)
    //   spring.jackson.use-jackson2-defaults=true                        → domyślne jak w Boot 3 (nowość Boot 4)
    // W kursie każda lekcja ustawia je tylko w SWOIM teście: @WebMvcTest(properties = "...") — inne lekcje ich nie widzą.
    // Ustawienia w kodzie: bean JsonMapperBuilderCustomizer (org.springframework.boot.jackson.autoconfigure) —
    // builder -> builder.enable(...). Wszystkie nazwy sprawdzone w metadanych spring-boot-jackson 4.1.1.
    //
    // PUŁAPKA: ustawienie z Boot 3 spring.jackson.serialization.write-dates-as-timestamps=true w Boot 4 nie jest
    //   ignorowane — aplikacja NIE WYSTARTUJE („No enum constant tools.jackson.databind.SerializationFeature
    //   .write-dates-as-timestamps”). Dlaczego? W Jacksonie 3 ten przełącznik przeniesiono z SerializationFeature
    //   do DateTimeFeature, więc i właściwość Boota zmieniła adres: spring.jackson.datatype.datetime.*.
    //   Sprawdzone testem (Rest04JsonPropertiesTest).
    //
    // DOBRA PRAKTYKA: NIE twórz własnego beana JsonMapper „od zera” (JsonMapper.builder().build() jako @Bean), jeśli
    //   chcesz tylko coś zmienić. Dlaczego? Własny bean wyłącza konfigurację Boota — tracisz spring.jackson.*
    //   i moduły dodawane przez Boota. Do poprawek służą właściwości i JsonMapperBuilderCustomizer.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Boot 4 = Jackson 3: klasy tools.jackson.* (JsonMapper), adnotacje com.fasterxml.jackson.annotation.* (bez zmian).
     *   • Wyjątki Jacksona 3 (JacksonException) są niesprawdzane. Daty java.time — wbudowane, bez dodatkowego modułu.
     *   • @JsonProperty("nazwa"), @JsonIgnore, @JsonInclude(NON_NULL), @JsonFormat(pattern = "dd.MM.yyyy").
     *   • @JsonIgnore na int w rekordzie → odczyt zawsze 400 (null do int). Użyj Integer albo osobnych DTO.
     *   • Domyślnie: daty ISO-8601, nieznane pola ignorowane, null/brak dla int → 400, pola alfabetycznie
     *     (parametry konstruktora i składniki rekordu — pierwsze, w swojej kolejności).
     *   • spring.jackson.datatype.datetime.write-dates-as-timestamps (NIE ...serialization... jak w Boot 3!).
     *   • spring.jackson.deserialization.fail-on-unknown-properties, default-property-inclusion, use-jackson2-defaults.
     *   • Zmiany w kodzie: JsonMapperBuilderCustomizer, nie własny JsonMapper od zera.
     *
     * PYTANIA KONTROLNE:
     *   1. W jakich pakietach są JsonMapper i @JsonProperty w Boot 4? Dlaczego nie w tym samym?
     *   2. Co się stanie? Klient wyśle do @RequestBody pole, którego rekord nie ma ("color":"złoty").
     *   3. Co się stanie? Rekord ma składnik int quantity, a klient go nie przyśle.
     *   4. ZNAJDŹ BŁĄD: record SecretPriceProduct(String name, @JsonIgnore int purchasePriceGrosze) — każdy POST z tym
     *      rekordem w @RequestBody daje 400. Dlaczego i jak naprawić?
     *   5. Co się stanie? Przeniesiesz do Boot 4 plik application.properties z linią
     *      spring.jackson.serialization.write-dates-as-timestamps=false.
     *   6. Jak LocalDate i Instant wyglądają w JSON-ie domyślnie? Jak zmienić format jednego pola?
     *   7. Dlaczego lepiej użyć JsonMapperBuilderCustomizer niż zdefiniować własny @Bean JsonMapper?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rest04JsonExercisesTest (zwykłe testy jednostkowe, bez Springa)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): dopisz adnotacje w rekordzie CustomerJson (fullName → "full_name"; email pomijany, gdy null;
     * password nigdy w JSON-ie) i zwróć JSON klienta. Przykład: CustomerJson("Anna Nowak", null, "tajne") →
     * {"full_name":"Anna Nowak"}. Podpowiedź: mapper.writeValueAsString(customer) — bez try/catch (Jackson 3).
     */
    static String exercise1(JsonMapper mapper, CustomerJson customer) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): JSON to tablica produktów, np. {@code [{"name":"Kawa","priceGrosze":2999},{"name":"Herbata",
     * "priceGrosze":1599}]}. Zwróć sumę cen (tu 4598). Pusta tablica → 0. Podpowiedź: mały rekord
     * {@code record Item(String name, int priceGrosze)} i {@code mapper.readValue(json, new TypeReference<List<Item>>() {})}
     * (TypeReference z pakietu tools.jackson.core.type) albo mapper.readTree(json) i pętla po węzłach.
     */
    static int exercise2(JsonMapper mapper, String jsonArray) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zbuduj „surowy” JsonMapper dla integracji z zewnętrznym systemem: nieznane pola → wyjątek
     * (UnrecognizedPropertyException), wartości null pomijane we WSZYSTKICH polach (bez adnotacji), a daty dalej jako
     * tekst ISO. Podpowiedź: JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
     * .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)).build().
     */
    static JsonMapper exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. JsonMapper: tools.jackson.databind.json; @JsonProperty: com.fasterxml.jackson.annotation. Jackson 3 zmienił
     *      pakiety klas, ale adnotacje zostawił (wspólny artefakt jackson-annotations), żeby modele nie wymagały zmian.
     *      Sprawdzone w plikach jar (jackson-databind 3.1.5, jackson-annotations 2.21).
     *   2. Pole zostanie zignorowane, odpowiedź 200 — w Jacksonie 3 FAIL_ON_UNKNOWN_PROPERTIES jest domyślnie wyłączone.
     *      Sprawdzone testem (sekcja 4).
     *   3. 400 Bad Request: „Cannot map `null` into type `int`” — Jackson 3 ma włączone FAIL_ON_NULL_FOR_PRIMITIVES.
     *      W Jacksonie 2 (i Boot 3) byłoby po cichu 0. Sprawdzone testem (sekcja 4 i Rest02RequestDataTest).
     *   4. Zignorowanego pola Jackson nie czyta, więc do konstruktora podaje null, a null do int to błąd w Jacksonie 3.
     *      Naprawa: Integer zamiast int albo osobny rekord wejściowy bez tego pola. Sprawdzone testem (sekcja 2).
     *   5. Aplikacja nie wystartuje: w Jacksonie 3 nie ma SerializationFeature.WRITE_DATES_AS_TIMESTAMPS („No enum
     *      constant ...”). Poprawny klucz: spring.jackson.datatype.datetime.write-dates-as-timestamps — a wartość false
     *      i tak jest domyślna, więc tę linię można po prostu usunąć. Sprawdzone testem (Rest04JsonPropertiesTest).
     *   6. Jako tekst ISO-8601: "2026-10-01" i "2026-10-01T08:30:00Z". Jedno pole: @JsonFormat(pattern = "dd.MM.yyyy").
     *      Sprawdzone testem (sekcja 3).
     *   7. Własny @Bean JsonMapper zastępuje bean Boota — przestają działać ustawienia spring.jackson.* i dostrajanie
     *      Boota. JsonMapperBuilderCustomizer dokłada zmianę DO konfiguracji Boota, niczego nie tracąc.
     *      Sprawdzone uruchomieniem: z własnym beanem JsonMapper ustawienie write-dates-as-timestamps=true nie działało.
     */
    // </editor-fold>
}
