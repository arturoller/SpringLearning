package s05_validation.val05_service_validation;

/**
 * <pre>
 * TEMAT: Walidacja w warstwie serwisów i w ustawieniach — {@code @Validated}, walidacja metod, {@code @ConfigurationProperties}
 *        (service = serwis, klasa z logiką biznesową; method validation = walidacja metod, czyli parametrów i wyniku;
 *         proxy = pośrednik, obiekt udający prawdziwy bean; self-invocation = wywołanie własnej metody;
 *         configuration properties = ustawienia z application.properties zbindowane do obiektu)
 *
 * W SKRÓCIE:
 *   Walidacja nie kończy się na kontrolerze. Ten sam serwis może wołać zadanie cykliczne, kolejka albo inny serwis —
 *   żaden z nich nie przechodzi przez {@code @Valid} z kontrolera. Dlatego na klasie serwisu kładziesz {@code @Validated},
 *   a na parametrach i wyniku metod zwykłe ograniczenia ({@code @NotBlank}, {@code @Positive}, {@code @Valid}).
 *   Spring owija wtedy serwis w proxy, które sprawdza je przy każdym wywołaniu ZE ZEWNĄTRZ i rzuca
 *   {@code ConstraintViolationException}. Tak samo można zwalidować ustawienia aplikacji: źle skonfigurowana aplikacja
 *   nie wystartuje, zamiast działać z niewłaściwymi wartościami.
 *
 * ANALOGIA: recepcjonista przed gabinetem lekarza.
 *   Lekarz (metoda serwisu) przyjmuje tylko pacjentów z kompletnym skierowaniem. Zamiast sprawdzać to sam, zatrudnia
 *   recepcjonistę (proxy), który stoi w drzwiach i odsyła niekompletnych pacjentów. Działa, dopóki pacjenci wchodzą drzwiami.
 *   Gdy lekarz poprosi asystenta z gabinetu obok ("to samo pomieszczenie", self-invocation), asystent nie mija recepcji —
 *   tam nikt nie pilnuje. A pacjent wchodzący tylnym wejściem (obiekt zrobiony przez {@code new}) recepcji w ogóle nie widzi.
 *
 * JAK TO DZIAŁA:
 *   1. Boot (autokonfiguracja {@code ValidationAutoConfiguration}) rejestruje bean {@code methodValidationPostProcessor}
 *      ({@code MethodValidationPostProcessor}). To "post-procesor beanów" (ang. post processor — obróbka po utworzeniu).
 *   2. Dla każdego beana z {@code @Validated} na klasie owija go w proxy (domyślnie CGLIB, czyli podklasa tworzona w locie).
 *   3. Wywołanie metody z zewnątrz trafia najpierw do proxy: ono waliduje parametry (używa beana
 *      {@code defaultValidator}, Val01BeanValidation sekcja 4), wywołuje prawdziwą metodę i waliduje wynik.
 *   4. Naruszenia → {@code ConstraintViolationException}; prawdziwa metoda w ogóle się nie wykonuje (dla parametrów).
 *   5. {@code @ConfigurationProperties} + {@code @Validated}: przy tworzeniu obiektu z ustawieniami Boot uruchamia Validator
 *      i przy naruszeniach przerywa start aplikacji.
 *
 * SŁÓWKA:
 *   validated = zwalidowany; post processor = obróbka po utworzeniu beana; cascade = kaskada, wejście "w głąb" obiektu;
 *   return value = wartość zwracana; bypass = obejście; bind = powiązać (ustawienia z polami obiektu);
 *   fail fast = "pad szybko", wykryć błąd od razu przy starcie; last line of defense = ostatnia linia obrony.
 *
 * ZOBACZ TEŻ: s05_validation/Val02ValidInControllers (walidacja w kontrolerze; tam metody kontrolerów NIE potrzebują
 *             @Validated na klasie),
 *             s05_validation/Val04CustomValidators (własne ograniczenia też można kłaść na parametrach serwisu),
 *             s02_configuration/Cfg02ConfigurationProperties (jak działa @ConfigurationProperties),
 *             s10_transactions/Tx03ProxyPitfalls (ta sama pułapka self-invocation dla @Transactional),
 *             s06_error_handling/Err02ExceptionHandler (zamiana ConstraintViolationException na odpowiedź 400).
 * </pre>
 */
public final class Val05ServiceValidation {

    private Val05ServiceValidation() {
    }

    // =================================================================================================
    // 1. @Validated NA SERWISIE (StockService; Val05ServiceValidationTest, sekcja 1)
    // =================================================================================================
    //
    //     @Service
    //     @Validated
    //     public class StockService {
    //         public int reserve(@NotBlank String sku, @Positive int quantity) { … }          // ograniczenia na parametrach
    //         public void register(@Valid @NotNull NewItem item) { … }                          // @Valid = wejdź do środka
    //         public @NotBlank String label(String sku, boolean buggy) { … }                   // ograniczenie na wyniku
    //     }
    //
    // Co widać w testach (wszystko SPRAWDZONE):
    //   • Bean w kontekście to proxy: klasa {@code StockService$$SpringCGLIB$$0}, podklasa naszego serwisu. Tworzy je
    //     {@code methodValidationPostProcessor} (autokonfiguracja Boota; sam nic nie dodajesz poza starterem validation).
    //   • {@code stockService.reserve("", -1)} → {@code ConstraintViolationException}, ciało metody się NIE wykonuje (log serwisu
    //     bez zmian). Ścieżki naruszeń to "metoda.parametr": {@code reserve.sku}, {@code reserve.quantity}. Nazwy parametrów są
    //     znane, bo Maven w projektach Boota kompiluje z opcją {@code -parameters}; bez niej zobaczysz {@code arg0}, {@code arg1}.
    //   • {@code @Valid} na parametrze wchodzi do obiektu: {@code register.item.sku}. Bez {@code @Valid} adnotacje wewnątrz
    //     {@code NewItem} są POMINIĘTE (kaskada nie jest domyślna — jak w Val03NestedAndGroups; SPRAWDZONE: registerWithoutCascade). {@code null} łamie {@code @NotNull}.
    //   • Ograniczenie na WYNIKU ({@code @NotBlank} przed typem zwracanym) łapie błąd w samym serwisie: ścieżka
    //     {@code label.<return value>}. To siatka bezpieczeństwa na pomyłki programisty, nie na dane z zewnątrz.
    //
    // Wyjątek jest nieobsłużony i z kontrolera wyjdzie jako błąd 500, jeśli niczego z nim nie zrobisz. Zamianę na czytelne 400
    // pokazuje s06_error_handling/Err02ExceptionHandler. UWAGA: to inny wyjątek niż w kontrolerach z Val02ValidInControllers
    // (tam: {@code MethodArgumentNotValidException} dla @RequestBody oraz {@code HandlerMethodValidationException} dla
    // ograniczeń na parametrach — bez @Validated na klasie, od Spring 6.1). W serwisach @Validated jest NADAL potrzebne.
    //
    // DOBRA PRAKTYKA: reguły "kształtu" danych (niepusty, dodatni, format) na granicy (DTO + @Valid w kontrolerze), a w serwisie
    //   ograniczenia na parametrach tych metod, które mają wielu wołających (zadania, kolejki, inne serwisy). Dlaczego?
    //   Serwis jest ostatnim miejscem, w którym złe dane można jeszcze zatrzymać przed logiką i bazą, a ograniczenia
    //   przy parametrach dokumentują kontrakt metody.
    //
    // PUŁAPKA: {@code @Validated} położone TYLKO na metodzie. Dlaczego nie działa? Spring szuka znacznika na KLASIE;
    //   bez niego bean nie dostaje proxy i adnotacje przy parametrach są martwe.
    //
    // PUŁAPKA: starsze tutoriale podpowiadają dodanie własnego beana {@code MethodValidationPostProcessor}. Dlaczego niepotrzebne?
    //   Boot rejestruje go sam (ten jeden bean o nazwie {@code methodValidationPostProcessor}). Własny dubel tylko zamiesza.

    // =================================================================================================
    // 2. PUŁAPKA: SELF-INVOCATION OMIJA PROXY (StockService, BatchService; test, sekcja 2)
    // =================================================================================================
    //
    // Proxy stoi PRZED serwisem. Gdy metoda serwisu woła inną metodę tego samego serwisu, robi to przez {@code this}, czyli
    // przez goły obiekt — recepcja zostaje za plecami:
    //
    //     public int reserveTwiceBroken(String sku, int quantity) {
    //         reserve(sku, quantity);                // to samo co this.reserve(…) — BEZ walidacji
    //         return reserve(sku, quantity);
    //     }
    //
    // SPRAWDZONE: {@code stockService.reserveTwiceBroken("", -5)} nie rzuca żadnego wyjątku, a log pokazuje, że {@code reserve}
    // wykonało się dwa razy z błędnymi danymi. To samo wołanie przez INNY bean ({@code BatchService}, który ma wstrzyknięty
    // StockService — czyli proxy) kończy się {@code ConstraintViolationException}, a log serwisu się nie zmienia.
    //
    // Wniosek: o tym, czy walidacja zadziała, decyduje KTO woła, a nie sama metoda. Dokładnie ta sama pułapka dotyczy
    // {@code @Transactional}, {@code @Async}, {@code @Cacheable} — wszystko, co działa przez proxy (s10_transactions/Tx03ProxyPitfalls).
    //
    // Co robić:
    //   • przenieś metodę do osobnego beana i wołaj ją przez wstrzyknięcie (rozwiązanie najczystsze, jak BatchService);
    //   • albo waliduj ręcznie na początku publicznej metody (Validator z Val01BeanValidation).
    // Nie sięgaj po wstrzykiwanie "samego siebie" ani {@code AopContext.currentProxy()} — to obejścia, które utrudniają czytanie.
    //
    // PYTANIE REKRUTACYJNE: dlaczego wewnętrzne wywołanie metody z @Transactional / @Validated nie działa? (Bo adnotacje
    //   obsługuje proxy, a wywołanie przez this idzie wprost do obiektu, z pominięciem proxy.)

    // =================================================================================================
    // 3. PUŁAPKA: new NIE WALIDUJE (NewItem; test, sekcja 3)
    // =================================================================================================
    //
    // Adnotacje są tylko opisem reguł. Sprawdza je Validator, wołany albo przez Springa (kontroler, proxy), albo przez Ciebie.
    //   • {@code new NewItem("", -1)} — żaden wyjątek, obiekt powstaje (rekord nie ma żadnej logiki w konstruktorze);
    //   • {@code new StockService().reserve("", -1)} — żaden wyjątek: goły obiekt nie ma proxy, więc ograniczenia z sekcji 1 są
    //     nieaktywne. Dotyczy to też testów jednostkowych tworzących serwis przez {@code new}: tam walidacji nie zobaczysz.
    //     Aby ją przetestować, potrzebujesz kontekstu Springa (@SpringBootTest, jak w tej lekcji).
    //   • Sprawdzenie ręczne: {@code validator.validate(obiekt)} — albo helper rzucający wyjątek (ćwiczenie 1).
    //
    // Dla niezmiennych obiektów domenowych (które nigdy nie powinny istnieć w złym stanie) walidacja bywa wpisywana
    // w konstruktor rekordu (np. {@code if (quantity <= 0) throw new IllegalArgumentException(…)}). To osobny mechanizm
    // niż adnotacje i nie zastępuje ich na granicy systemu.
    //
    // PUŁAPKA: założenie "mam adnotację, więc obiekt jest poprawny". Dlaczego? Poprawność gwarantuje dopiero miejsce, które
    //   faktycznie wywołuje walidację. Obiekt zbudowany w teście, z pliku CSV albo z innego serwisu nie przeszedł żadnej kontroli.

    // =================================================================================================
    // 4. WALIDACJA USTAWIEŃ: @ConfigurationProperties + @Validated (ShopProperties; Val05ConfigurationPropertiesTest)
    // =================================================================================================
    //
    //     @ConfigurationProperties("val05.shop")
    //     @Validated
    //     public record ShopProperties(@NotBlank String name, @Min(1) @Max(1000) int maxItemsPerOrder, @Valid Delivery delivery) { … }
    //
    // Idea fail fast ("pad szybko"): błędna konfiguracja (pusta nazwa, limit 0, brak miasta) ma przerwać start aplikacji z czytelnym
    // komunikatem, a nie wyjść dopiero w nocy w produkcji. Wiązanie ustawień opisuje s02_configuration/Cfg02ConfigurationProperties.
    //
    // Test używa {@code ApplicationContextRunner} — malutkiego kontekstu, który nie uruchamia całej aplikacji:
    //     runner.withPropertyValues("val05.shop.name=", …).run(context -> assertThat(context).hasFailed());
    // SPRAWDZONE:
    //   • złe wartości → start kończy się {@code ConfigurationPropertiesBindException}, w łańcuchu przyczyn jest
    //     {@code BindValidationException} z listą pól (tu: {@code maxItemsPerOrder}, {@code name});
    //   • BRAK właściwości też łamie ograniczenia: brakujący tekst to null, brakujące {@code int} to 0 (czyli niżej niż {@code @Min(1)});
    //   • zagnieżdżone ustawienia sprawdza tylko {@code @Valid} na polu ({@code delivery.city});
    //   • bez {@code @Validated} na klasie ({@code LaxShopProperties}) ograniczenia są IGNOROWANE — kontekst startuje, a nazwa jest
    //     pusta. To najczęstsza pomyłka: adnotacje wyglądają jak zabezpieczenie, ale nie robi nic.
    //
    // DOBRA PRAKTYKA: ustawienia obowiązkowe oznaczaj {@code @NotBlank}/{@code @NotNull}/{@code @Min}, a nie sprawdzaj ich w kodzie
    //   "kiedyś, w pierwszym użyciu". Dlaczego? Błąd wychodzi przy starcie, komunikat wskazuje klucz ({@code val05.shop.name}),
    //   a administrator dowiaduje się o literówce w ustawieniach od razu, nie po godzinie działania.

    // =================================================================================================
    // 5. GDZIE CO WALIDOWAĆ (tabela)
    // =================================================================================================
    //
    //   Rodzaj reguły                | Przykład                               | Gdzie                          | Narzędzie / błąd
    //   -----------------------------+----------------------------------------+--------------------------------+------------------------------------
    //   Kształt danych na wejściu    | nazwa niepusta, cena > 0, e-mail       | granica: DTO + @Valid          | Bean Validation → 400 (Val02)
    //   Parametry metody wołanej     | sku niepuste, ilość dodatnia           | serwis z @Validated            | ConstraintViolationException
    //   z wielu miejsc               |                                        |                                |
    //   Reguła biznesowa zależna     | "towaru jest za mało", "zamówienie     | kod serwisu (if + własny       | własne wyjątki → 404/409 (s06)
    //   od stanu                     | już wysłane"                           | wyjątek)                       |
    //   Ustawienia aplikacji         | limit zamówienia, adres serwisu        | @ConfigurationProperties       | aplikacja nie startuje
    //                                |                                        | + @Validated                   |
    //   Spójność w bazie             | unikalny e-mail, NOT NULL, klucz obcy  | baza: UNIQUE, NOT NULL, FK     | wyjątek bazy (s07/s08) —
    //                                |                                        |                                | ostatnia linia obrony
    //
    // Zasada: im bliżej danych, tym twardsza gwarancja, im bliżej użytkownika, tym lepszy komunikat. Walidacja adnotacjami nie
    // zastępuje bazy (wyścig dwóch żądań), a baza nie zastępuje walidacji (dostaniesz brzydki błąd SQL zamiast "podaj nazwę").
    //
    // DOBRA PRAKTYKA: nie powielaj tych samych reguł w trzech warstwach "na wszelki wypadek". Dlaczego? Każda kopia zestarzeje się
    //   inaczej. Wybierz warstwę zgodnie z tabelą, a baza niech pilnuje tylko tego, co faktycznie gwarantuje spójność.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @Validated} NA KLASIE serwisu + ograniczenia na parametrach/wyniku → proxy (CGLIB) z
     *     {@code methodValidationPostProcessor} (autokonfiguracja Boota, nic nie dodajesz).
     *   • Błąd: {@code ConstraintViolationException}; ścieżka "metoda.parametr", wynik: "metoda.<return value>".
     *   • {@code @Valid} na parametrze = kaskada do środka obiektu; bez niego adnotacje w środku są pomijane.
     *   • Self-invocation (this.metoda()) i {@code new} omijają proxy → brak walidacji. Przenieś wołaną metodę do innego beana.
     *   • Kontroler: bez @Validated na klasie (Val02); serwis: @Validated potrzebne.
     *   • {@code @ConfigurationProperties} + {@code @Validated} (+ {@code @Valid} na polach zagnieżdżonych) → zła konfiguracja
     *     przerywa start; bez {@code @Validated} ograniczenia są ignorowane. Test: ApplicationContextRunner + hasFailed().
     *   • Kształt danych: granica (DTO); reguły zależne od stanu: serwis + własne wyjątki (s06); spójność: baza (UNIQUE, NOT NULL).
     *
     * PYTANIA KONTROLNE:
     *   1. Co musisz dodać do serwisu, żeby ograniczenia na parametrach jego metod zaczęły działać? Gdzie się kładzie
     *      {@code @Validated} i jaki wyjątek dostaniesz?
     *   2. Co się stanie? {@code stockService.reserve("", -1)} — które ścieżki naruszeń dostaniesz i czy ciało metody się wykona?
     *   3. Co się stanie? {@code stockService.reserveTwiceBroken("", -5)}, a co przy tym samym wołaniu
     *      {@code batchService.reserveTwice("", -5)}? Skąd ta różnica?
     *   4. ZNAJDŹ BŁĄD: {@code StockService s = new StockService(); s.reserve("", -1);} w teście jednostkowym — autor spodziewa się
     *      {@code ConstraintViolationException}. Dlaczego jej nie będzie i jak to naprawić?
     *   5. Co się stanie? {@code register(new NewItem(" ", 0))} z {@code @Valid} i bez {@code @Valid} na parametrze.
     *   6. ZNAJDŹ BŁĄD: {@code @ConfigurationProperties("app") public record AppProperties(@NotBlank String url) { }} — w pliku
     *      ustawień brak klucza {@code app.url}, a aplikacja i tak startuje. Dlaczego?
     *   7. Co zwróci walidacja ustawień, gdy w pliku nie ma właściwości typu {@code int} oznaczonej {@code @Min(1)}?
     *   8. Gdzie zwalidujesz: (a) że ilość w zamówieniu jest dodatnia, (b) że towaru jest wystarczająco dużo, (c) że e-mail klienta
     *      jest unikalny? Uzasadnij.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================
    //
    // Szkielet: ServiceValidationExercises (metody z TODO), testy: Val05ServiceValidationExercisesTest (@Tag("cwiczenie")).
    // Rozwiązania wzorcowe: s05_validation.solutions.val05_service_validation.ServiceValidationSolutions.
    //
    // ĆWICZENIE 1 (łatwe): validateOrThrow — ręczna walidacja obiektu: poprawny wraca, błędny kończy się
    //   ConstraintViolationException.
    //   Podpowiedź: validator.validate(obiekt) zwraca zbiór; {@code new ConstraintViolationException(zbiór)}.
    //
    // ĆWICZENIE 2 (średnie): describe — z wyjątku zrób posortowane linie "ścieżka (NazwaAdnotacji)",
    //   np. "reserve.quantity (Positive)".
    //   Podpowiedź: exception.getConstraintViolations(), getPropertyPath(), getConstraintDescriptor().getAnnotation().annotationType().
    //
    // ĆWICZENIE 3 (trudniejsze): startsWith — czy aplikacja z ShopPropertiesConfig wystartuje z podanymi ustawieniami?
    //   Podpowiedź: SpringApplicationBuilder(ShopPropertiesConfig.class).web(WebApplicationType.NONE).properties("k=v").run();
    //   zamknij kontekst (try-with-resources), a wyjątek zamień na false. (W teście ustawienia ze złym limitem dadzą false.)

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. {@code @Validated} NA KLASIE serwisu (adnotacja z pakietu {@code org.springframework.validation.annotation}); dalej
     *      ograniczenia na parametrach i wyniku. Spring (MethodValidationPostProcessor, autokonfiguracja Boota) owija serwis
     *      w proxy. Wyjątek: {@code jakarta.validation.ConstraintViolationException}. Sprawdzone w teście sekcji 1.
     *   2. Dwie ścieżki: {@code reserve.sku} (NotBlank) i {@code reserve.quantity} (Positive). Ciało metody się NIE wykona
     *      (log serwisu bez zmian) — proxy zatrzymuje wywołanie przed metodą. Sprawdzone w teście sekcji 1.
     *   3. {@code reserveTwiceBroken}: bez wyjątku, {@code reserve} wykona się dwa razy z błędnymi danymi (wywołanie przez this
     *      omija proxy). {@code batchService.reserveTwice}: ConstraintViolationException, bo BatchService woła StockService
     *      przez wstrzyknięte proxy. Różnica: kto woła i przez co. Sprawdzone w teście sekcji 2.
     *   4. Goły obiekt z {@code new} nie ma proxy, więc nikt nie czyta @Validated ani ograniczeń na parametrach — metoda wykona się
     *      normalnie. Naprawa: test z kontekstem Springa (@SpringBootTest i wstrzyknięty serwis) albo ręczne Validator w kodzie
     *      metody. Sprawdzone w teście sekcji 3.
     *   5. Z {@code @Valid}: dwa naruszenia, {@code register.item.sku} i {@code register.item.quantity}. Bez {@code @Valid}:
     *      brak naruszeń (poza ewentualnym {@code @NotNull}), bo kaskada do wnętrza obiektu nie jest domyślna; adnotacje
     *      w {@code NewItem} są pomijane. Sprawdzone w teście sekcji 1 (register i registerWithoutCascade).
     *   6. Brak {@code @Validated} na klasie ustawień — Boot nie uruchamia wtedy Validatora. Sprawdzone na LaxShopProperties:
     *      kontekst startuje z pustą nazwą. Z @Validated brakujący klucz to null i start się nie powiedzie.
     *   7. Naruszenie. Brakująca właściwość typu int to 0 (domyślna wartość prymitywu), czyli mniej niż {@code @Min(1)}; start
     *      zakończy się błędem. Sprawdzone w teście sekcji 4 (maxItemsPerOrder).
     *   8. (a) Dodatnia ilość — kształt danych: adnotacja {@code @Positive} na DTO (granica), ewentualnie na parametrze serwisu.
     *      (b) Wystarczająca ilość towaru — reguła zależna od stanu: serwis (if + własny wyjątek, s06), bo wymaga odczytu stanu
     *      magazynu. (c) Unikalny e-mail — UNIQUE w bazie (jedyna gwarancja bez wyścigu; zapytanie w walidatorze nie wystarcza).
     */
    // </editor-fold>
}
