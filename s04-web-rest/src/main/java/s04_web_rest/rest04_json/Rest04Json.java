package s04_web_rest.rest04_json;

/**
 * <pre>
 * TEMAT: JSON w Spring Boot 4 — Jackson 3: rekordy, adnotacje {@code @JsonProperty}, {@code @JsonIgnore},
 *        {@code @JsonFormat}, {@code @JsonInclude}, daty, BigDecimal, pola null, nieznane pola, customizer
 *        i {@code @JsonTest}; co się zmieniło względem Jacksona 2
 *        (serialization = serializacja, zamiana obiektu na JSON; deserialization = deserializacja, zamiana JSON-a
 *        na obiekt; mapper = „tłumacz” obiekt ↔ JSON)
 *
 * W SKRÓCIE:
 *   Spring nie zamienia obiektów na JSON sam — robi to biblioteka Jackson. Boot 4 używa JACKSONA 3: klasy są
 *   w pakiecie tools.jackson (np. tools.jackson.databind.json.JsonMapper), a adnotacje NADAL w
 *   com.fasterxml.jackson.annotation. Boot tworzy jeden bean JsonMapper i używa go wszędzie: w kontrolerach,
 *   w RestClient, w testach. Rekord → JSON z polami w kolejności składowych, daty jako tekst ISO ("2026-10-19"),
 *   BigDecimal jako liczba (5499.99). Adnotacje zmieniają nazwy, ukrywają pola, zmieniają format.
 *   Ustawienia całej aplikacji: właściwości spring.jackson.* albo bean JsonMapperBuilderCustomizer — nigdy własny
 *   JsonMapper od zera.
 *
 * ANALOGIA: tłumacz przysięgły.
 *   Obiekt Javy to dokument po polsku, JSON — po angielsku. JsonMapper to tłumacz, którego zatrudnia Boot.
 *   Adnotacje to karteczki przyklejone do dokumentu: „to pole przetłumacz jako productName” (@JsonProperty),
 *   „tego nie tłumacz” (@JsonIgnore), „datę zapisz po polsku” (@JsonFormat). Właściwości spring.jackson.* i customizer
 *   to instrukcje dla tłumacza „we wszystkich dokumentach”. Własny JsonMapper = zwolnienie tłumacza Boota razem
 *   ze wszystkimi jego instrukcjami.
 *
 * JAK TO DZIAŁA:
 *   1. JacksonAutoConfiguration (moduł spring-boot-jackson) zbiera wszystkie beany JsonMapperBuilderCustomizer —
 *      w tym własny customizer Boota, który czyta spring.jackson.* — i buduje z nich bean jacksonJsonMapper.
 *   2. Moduł spring-boot-http-converter tworzy z tego mappera JacksonJsonHttpMessageConverter (Spring Framework 7).
 *   3. @RestController zwraca rekord → konwerter woła jsonMapper.writeValueAsString(...) i pisze wynik do ciała.
 *      @RequestBody → konwerter woła jsonMapper.readValue(...); błąd Jacksona → HttpMessageNotReadableException → 400.
 *
 * SŁÓWKA:
 *   property = właściwość (pole w JSON-ie); inclusion = dołączanie (czy pole trafia do JSON-a); non null = nie-null;
 *   unknown property = nieznana właściwość; primitive = typ prosty (int, long, boolean); shape = kształt (liczba czy
 *   tekst); pattern = wzorzec (format daty); customizer = „dostrajacz” (kod, który zmienia ustawienia gotowego
 *   budowniczego); builder = budowniczy; tester = sprawdzacz; type reference = odwołanie do typu (np. List of Product).
 *
 * ZOBACZ TEŻ: t18_io_files/Io05JsonManual (JSON ręcznie, bez bibliotek), t09_records/Records01Basics (rekordy),
 *             t17_datetime/DateTime04ZonesInstant (Instant i strefy), t15_numbers/Numbers01BigDecimal,
 *             s02_configuration/Cfg01Properties (właściwości Boota), s03_boot_basics/Boot01AutoConfiguration
 *             (autokonfiguracja i {@code @ConditionalOnMissingBean}), s04_web_rest/Rest02RequestData ({@code @RequestBody}),
 *             s13_rest_client/Rc01RestClient (ten sam JsonMapper w kliencie HTTP).
 * </pre>
 */
public final class Rest04Json {

    private Rest04Json() {
    }

    // =================================================================================================
    // 1. JACKSON 3 W BOOT 4 — CO SIĘ ZMIENIŁO (test: Rest04JsonMapperTest, Rest04JsonOwnMapperTest.jackson2Defaults)
    // =================================================================================================
    //
    //                         Jackson 2 (Boot 2/3, stare tutoriale)         Jackson 3 (Boot 4, ten kurs)
    //   pakiet klas           com.fasterxml.jackson.databind                tools.jackson.databind
    //   główna klasa          ObjectMapper                                  JsonMapper (dziedziczy po ObjectMapper)
    //   adnotacje             com.fasterxml.jackson.annotation              BEZ ZMIAN: com.fasterxml.jackson.annotation
    //   wyjątki               JsonProcessingException (kontrolowany)        JacksonException (RuntimeException)
    //   daty java.time        osobny moduł jackson-datatype-jsr310           wbudowane
    //   customizer w Boot     Jackson2ObjectMapperBuilderCustomizer         JsonMapperBuilderCustomizer
    //   konwerter w Springu   MappingJackson2HttpMessageConverter           JacksonJsonHttpMessageConverter
    //
    // Boot 4 tworzy bean JsonMapper o nazwie jacksonJsonMapper. Wstrzykniesz go jako JsonMapper albo jako ObjectMapper
    // (to TEN SAM obiekt). Wyjątki Jacksona 3 są niekontrolowane — readValue/writeValueAsString nie wymagają już
    // throws ani try/catch.
    //
    // PUŁAPKA: kod skopiowany z internetu: import com.fasterxml.jackson.databind.ObjectMapper; — w tym module NIE
    //   skompiluje się: „error: package com.fasterxml.jackson.databind does not exist”. Dlaczego? Boot 4 dołącza tylko
    //   Jacksona 3 (tools.jackson) i bibliotekę adnotacji; jackson-databind 2.x nie ma na ścieżce klas. Zmień import na
    //   tools.jackson.databind.ObjectMapper albo (lepiej) wstrzyknij JsonMapper Boota.
    //
    // Migracja starej aplikacji: właściwość spring.jackson.use-jackson2-defaults=true przywraca zachowanie z Boot 3
    // (enumy po nazwie, brakujący int = 0, kolejność pól jak w klasie). Traktuj ją jako koło ratunkowe na czas migracji.

    // =================================================================================================
    // 2. DOMYŚLNY JSON (JsonDemoController.card, labeled; test defaultRecordJson)
    // =================================================================================================
    //
    //   new ProductCard("ELE-001", "Laptop Pro 14", ELEKTRONIKA, new BigDecimal("5499.99"), 7,
    //                   LocalDate.of(2026, 10, 19), Instant.parse("2026-10-19T08:30:00Z"))
    //   → {"sku":"ELE-001","name":"Laptop Pro 14","category":"ELEKTRONIKA","price":5499.99,"stock":7,
    //      "addedOn":"2026-10-19","updatedAt":"2026-10-19T08:30:00Z"}
    //
    //   • rekord: nazwy pól = nazwy składowych, kolejność = kolejność składowych,
    //   • LocalDate → "2026-10-19", Instant → "2026-10-19T08:30:00Z", LocalDateTime → "2026-10-19T08:30:00"
    //     (format ISO 8601; w Jacksonie 3 WRITE_DATES_AS_TIMESTAMPS jest domyślnie wyłączone — a nie liczba milisekund),
    //   • BigDecimal → liczba z zachowaną skalą: 79.00 zostaje 79.00, nie 79.0 ani 79.
    //
    // PUŁAPKA: (Jackson 3) enum z własnym toString(). Jackson 3 domyślnie PISZE i CZYTA enumy przez toString()
    //   (EnumFeature.WRITE_ENUMS_USING_TO_STRING i READ_ENUMS_USING_TO_STRING są włączone). PrettyCategory.KSIAZKI
    //   z toString() = "książki" trafia do JSON-a jako "książki", a "KSIAZKI" w żądaniu zostanie ODRZUCONE.
    //   Dlaczego groźne? Zmiana tekstu w toString() (np. poprawka literówki w interfejsie) po cichu zmienia API.
    //   Enumy w API — bez nadpisanego toString() albo z jawnym @JsonProperty na stałych.
    //
    // PUŁAPKA: (Jackson 3) zwykła klasa z getterami (nie rekord) ma pola w kolejności ALFABETYCZNEJ
    //   (MapperFeature.SORT_PROPERTIES_ALPHABETICALLY włączone). Kod, który porównuje JSON jako tekst, nagle się sypie.
    //   Dlaczego tylko klasa? Rekordy zachowują kolejność składowych (SORT_CREATOR_PROPERTIES_FIRST). W testach porównuj
    //   JSON przez jsonPath albo content().json(...), a nie jako zwykły napis.

    // =================================================================================================
    // 3. ADNOTACJE (JsonModels.PublicProduct; test annotations)
    // =================================================================================================
    //
    //   @JsonProperty("productName") String name                → "productName":"Laptop Pro 14"
    //   @JsonIgnore BigDecimal purchasePrice                     → pola nie ma w JSON-ie
    //   @JsonFormat(pattern = "dd.MM.yyyy") LocalDate addedOn    → "addedOn":"19.10.2026"
    //   @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal  → "price":"5499.99" (tekst zamiast liczby)
    //
    // Adnotacje działają W OBIE STRONY: przy czytaniu Jackson też oczekuje "productName" i daty "19.10.2026".
    //
    // PUŁAPKA: @JsonIgnore na składowej rekordu ignoruje pole także przy CZYTANIU — purchasePrice z żądania zawsze
    //   będzie null. Dlaczego to ma znaczenie? Jeśli ten sam rekord służy do wysyłania i przyjmowania danych, pole
    //   ukryte w odpowiedzi „znika” też z formularza. Rozwiązanie: osobne rekordy na żądanie i odpowiedź (DTO —
    //   s04_web_rest/Rest05CrudApi).
    //
    // DOBRA PRAKTYKA: ceny w API jako liczba BigDecimal (domyślnie) albo jako tekst (shape = STRING), NIGDY double.
    //   Dlaczego? 0.1 + 0.2 w double to 0.30000000000000004 — pieniądze muszą być dokładne (t15_numbers).

    // =================================================================================================
    // 4. POLA NULL (customer, customer-without-nulls; testy nullFields, Rest04JsonPropertiesTest)
    // =================================================================================================
    //
    //   domyślnie:                                    {"id":2,"name":"Maria Nowak","email":null}
    //   @JsonInclude(JsonInclude.Include.NON_NULL)    {"id":2,"name":"Maria Nowak"}         — tylko ta klasa
    //   spring.jackson.default-property-inclusion=non_null                                   — CAŁA aplikacja
    //
    // DOBRA PRAKTYKA: zdecyduj raz dla całego API i opisz to w dokumentacji. Dlaczego? Klient musi wiedzieć, czy
    //   „brak pola” i „pole = null” znaczą to samo; mieszanie stylów w jednym API prowadzi do błędów po stronie klienta.

    // =================================================================================================
    // 5. CZYTANIE JSON-a Z ŻĄDANIA (POST /api/json/products; testy unknownFieldsIgnored, missingPrimitiveGives400)
    // =================================================================================================
    //
    // Nieznane pole ("color":"brązowy") jest IGNOROWANE — w Jacksonie 3 FAIL_ON_UNKNOWN_PROPERTIES jest domyślnie
    // wyłączone (w Jacksonie 2 było włączone, ale Boot 2/3 i tak je wyłączał — dla Ciebie zachowanie się nie zmienia).
    // Chcesz ścisłości? spring.jackson.deserialization.fail-on-unknown-properties=true → 400 i komunikat
    // „Unrecognized property "color" ...”.
    //
    // PUŁAPKA: (Jackson 3) brak pola typu int w żądaniu → 400 Bad Request: „JSON parse error: Cannot map `null` into
    //   type `int` (set `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES` to 'false' to allow)”. W Jacksonie 2 pole
    //   dostawało po cichu 0. Dlaczego zmiana jest dobra? „Klient zapomniał podać stan magazynu” to nie to samo, co
    //   „stan = 0”. Pole naprawdę opcjonalne → typ obiektowy (Integer) i jawna obsługa null; wymagane → walidacja
    //   (@NotNull — s05_validation/Val01BeanValidation).

    // =================================================================================================
    // 6. DOSTRAJANIE MAPPERA: właściwości, customizer, NIE własny bean (JsonConfig; Rest04JsonOwnMapperTest)
    // =================================================================================================
    //
    //   a) właściwości: spring.jackson.default-property-inclusion, spring.jackson.serialization.*,
    //      spring.jackson.deserialization.*, spring.jackson.mapper.*, spring.jackson.time-zone... — najprostsze,
    //   b) @Bean JsonMapperBuilderCustomizer — kod dla rzeczy, których nie da się wyrazić właściwością (moduły,
    //      złożone reguły); dostraja TEN SAM budowniczy, więc właściwości z a) nadal działają,
    //   c) własny @Bean JsonMapper — Boot się wycofuje (@ConditionalOnMissingBean) i NIE stosuje spring.jackson.*.
    //
    // PUŁAPKA: „potrzebuję jednej zmiany, więc tworzę własny JsonMapper”. Dlaczego źle? Tracisz wszystko, co Boot
    //   ustawia (właściwości, moduły, mixiny), a test Rest04JsonOwnMapperTest.ownMapperIgnoresProperties pokazuje, że
    //   default-property-inclusion=non_null przestaje działać bez żadnego ostrzeżenia. Używaj customizera.

    // =================================================================================================
    // 7. TESTY JSON-a: @JsonTest i JacksonTester (Rest04JsonMapperTest)
    // =================================================================================================
    //
    // @JsonTest — wycinek kontekstu z samym Jacksonem (JsonMapper Boota + beany @JacksonComponent i moduły Jacksona),
    // bez serwera i kontrolerów. JacksonTester<T> zapisuje obiekt do JSON-a i pozwala sprawdzać ścieżki
    // (extractingJsonPathStringValue("$.addedOn")). Zwykłe klasy @Configuration (np. JsonConfig) NIE są ładowane —
    // dlatego w @JsonTest enum znowu rozróżnia wielkość liter (test withoutCustomizerEnumIsCaseSensitive).
    // Listy z JSON-a: jsonMapper.readValue(json, new TypeReference<List<NewProduct>>() {}) — TypeReference z pakietu
    // tools.jackson.core.type (w Jacksonie 2: com.fasterxml.jackson.core.type).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Jackson 3: tools.jackson.* (JsonMapper), adnotacje nadal com.fasterxml.jackson.annotation.*.
     *   • Wstrzykuj JsonMapper Boota (bean jacksonJsonMapper); wyjątki JacksonException są niekontrolowane.
     *   • Domyślnie: rekord w kolejności składowych, klasa — alfabetycznie; daty ISO; BigDecimal jako liczba ze skalą;
     *     enum przez toString(); null jako null; nieznane pola ignorowane; brak int → 400.
     *   • @JsonProperty("nazwa"), @JsonIgnore (w obie strony!), @JsonFormat(pattern = "dd.MM.yyyy"),
     *     @JsonFormat(shape = STRING), @JsonInclude(NON_NULL).
     *   • Cała aplikacja: spring.jackson.* albo @Bean JsonMapperBuilderCustomizer. Nigdy własny JsonMapper od zera.
     *   • Testy: @JsonTest + JacksonTester<T>; listy: new TypeReference<List<T>>() {}.
     *   • Migracja z Boot 3: spring.jackson.use-jackson2-defaults=true (tymczasowo).
     *
     * PYTANIA KONTROLNE:
     *   1. W jakim pakiecie są klasy Jacksona 3, a w jakim adnotacje? Dlaczego adnotacje się nie przeniosły?
     *   2. Co się stanie? Wyślesz POST /api/json/products bez pola "stock" — jaki status i dlaczego?
     *   3. Co się stanie? GET /api/json/labeled — jaką wartość ma pole category w JSON-ie?
     *   4. ZNAJDŹ BŁĄD: w klasie konfiguracji jest @Bean JsonMapper jsonMapper() { return JsonMapper.builder().build(); },
     *      a w application.properties spring.jackson.default-property-inclusion=non_null. Pola null nadal są w JSON-ie.
     *   5. Jak sprawić, żeby pola null znikały tylko w jednej klasie, a jak — w całej aplikacji?
     *   6. ZNAJDŹ BŁĄD: rekord ProductForm(String sku, @JsonIgnore BigDecimal purchasePrice) służy do przyjmowania
     *      danych z formularza. Cena zakupu zawsze jest null, choć klient ją wysyła. Dlaczego?
     *   7. Jak zapisać datę jako "19.10.2026" i cenę jako tekst "5499.99"?
     *   8. Czym różni się @JsonTest od @WebMvcTest? Którego użyjesz do sprawdzenia samego formatu JSON-a?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w klasie Rest04Exercises (rekordy i metody do uzupełnienia), testy: Rest04JsonExercisesTest.
    // Rozwiązania wzorcowe: s04_web_rest.solutions.rest04_json.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klasy — tools.jackson (np. tools.jackson.databind.json.JsonMapper), adnotacje — com.fasterxml.jackson.annotation.
     *      Biblioteka jackson-annotations jest wspólna dla Jacksona 2 i 3 (tu w wersji 2.21), dzięki czemu modele
     *      z adnotacjami nie wymagają zmian przy migracji.
     *   2. 400 Bad Request — HttpMessageNotReadableException: „JSON parse error: Cannot map `null` into type `int` ...”.
     *      Jackson 3 ma domyślnie włączone FAIL_ON_NULL_FOR_PRIMITIVES, więc brakujący int to błąd, a nie 0.
     *      Sprawdzone testem missingPrimitiveGives400.
     *   3. "category":"książki" — Jackson 3 pisze enumy przez toString(), a PrettyCategory.KSIAZKI ma toString() =
     *      "książki". Sprawdzone testem enumUsesToStringInJackson3.
     *   4. Własny bean JsonMapper wyłącza mapper Boota (@ConditionalOnMissingBean w JacksonAutoConfiguration), więc
     *      właściwości spring.jackson.* nie są stosowane. Poprawka: usuń własny bean, a zmiany wprowadź customizerem.
     *      Sprawdzone testem Rest04JsonOwnMapperTest.ownMapperIgnoresProperties.
     *   5. Jedna klasa — @JsonInclude(JsonInclude.Include.NON_NULL) na rekordzie. Cała aplikacja —
     *      spring.jackson.default-property-inclusion=non_null (albo customizer).
     *   6. @JsonIgnore działa w obie strony: Jackson nie pisze pola i NIE CZYTA go z JSON-a, więc konstruktor rekordu
     *      dostaje null. Poprawka: osobny rekord na wejście (bez @JsonIgnore) i na wyjście (z @JsonIgnore albo bez pola).
     *   7. @JsonFormat(pattern = "dd.MM.yyyy") na polu LocalDate i @JsonFormat(shape = JsonFormat.Shape.STRING) na polu
     *      BigDecimal. Sprawdzone testem annotations.
     *   8. @JsonTest ładuje tylko Jacksona (JsonMapper, JacksonTester), @WebMvcTest — warstwę webową z MockMvc.
     *      Do samego formatu JSON-a wystarczy szybszy @JsonTest.
     */
    // </editor-fold>
}
