package s05_validation.val04_custom_validators;

/**
 * <pre>
 * TEMAT: Własne ograniczenia i walidatory — gdy standardowe adnotacje nie wystarczają
 *        (custom = własny, napisany samodzielnie; constraint = ograniczenie, adnotacja z regułą;
 *         validator = klasa, która regułę sprawdza; composed = złożony z innych; class-level = na całej klasie)
 *
 * W SKRÓCIE:
 *   Reguła "kod produktu ma postać ABC-1234" albo "koniec promocji nie przed początkiem" nie ma gotowej adnotacji.
 *   Piszesz więc dwie rzeczy: ADNOTACJĘ (nazwa reguły, np. {@code @Sku}) i WALIDATOR (klasa z metodą
 *   {@code isValid}, która mówi tak/nie). Dalej użycie jest identyczne jak {@code @NotBlank}: kładziesz adnotację na polu,
 *   a Validator (Val01BeanValidation) i {@code @Valid} w kontrolerze (Val02ValidInControllers) robią resztę. Można też
 *   ZŁOŻYĆ nową adnotację z istniejących albo nałożyć regułę na całą klasę i przypiąć błąd do wybranego pola.
 *
 * ANALOGIA: pieczątka i urzędnik.
 *   Adnotacja to pieczątka "ZGODNE Z NORMĄ SKU" — mała, wisi na dokumencie i mówi, JAKĄ regułę sprawdzić. Walidator to
 *   urzędnik, który wie, JAK ją sprawdzić. Wniosek o nową pieczątkę wymaga obu: bez urzędnika pieczątka nic nie znaczy,
 *   a bez pieczątki urzędnik nie wie, na których dokumentach ma pracować. Ograniczenie złożone to pieczątka, która sama
 *   mówi "przepuść, jeśli dokument ma stempel A i stempel B".
 *
 * JAK TO DZIAŁA:
 *   1. Adnotacja z {@code @Constraint(validatedBy = MójWalidator.class)} i trzema obowiązkowymi atrybutami
 *      {@code message}, {@code groups}, {@code payload}.
 *   2. Walidator implementuje {@code ConstraintValidator<Adnotacja, TypWalidowany>} i ma metodę
 *      {@code isValid(wartość, kontekst)}: true = poprawne, false = naruszenie.
 *   3. Validator czyta adnotacje klasy, dla każdej szuka walidatora pasującego do typu pola, tworzy go (RAZ na ograniczenie
 *      i wywołuje {@code initialize}), a potem woła {@code isValid} dla każdej walidowanej wartości.
 *   4. Fabryka walidatorów w czystej Javie wymaga konstruktora BEZ argumentów. W aplikacji Springa walidatory
 *      tworzy {@code SpringConstraintValidatorFactory} — wtedy konstruktor może przyjmować beany (zależności).
 *   5. Komunikat to szablon: {@code {klucz}} odsyła do pliku {@code ValidationMessages.properties}, {@code {min}} wstawia
 *      atrybut adnotacji, {@code ${validatedValue}} — odrzuconą wartość.
 *
 * SŁÓWKA:
 *   payload = ładunek (dodatkowe dane dla klienta); retention = czas życia adnotacji; target = miejsce użycia;
 *   composed = złożony; single = pojedynczy; violation = naruszenie; template = szablon; interpolation = wstawianie
 *   wartości w szablon; property node = węzeł ścieżki (nazwa pola); unwrap = zdjąć otoczkę, dostać się do klasy
 *   bazowej; escape = zamaskować znak specjalny; Expression Language (EL) = język wyrażeń w komunikatach.
 *
 * ZOBACZ TEŻ: s05_validation/Val01BeanValidation (Validator, naruszenia, komunikaty),
 *             s05_validation/Val02ValidInControllers (@Valid w kontrolerze — własne ograniczenia działają tak samo),
 *             s05_validation/Val03NestedAndGroups (atrybut groups z sekcji 1),
 *             s05_validation/Val05ServiceValidation (te same adnotacje na parametrach metod serwisu),
 *             s01_core_ioc/Ioc02ConstructorInjection (wstrzykiwanie przez konstruktor — sekcja 4).
 * </pre>
 */
public final class Val04CustomValidators {

    private Val04CustomValidators() {
    }

    // =================================================================================================
    // 1. WŁASNA ADNOTACJA @Sku I JEJ WALIDATOR (Sku, SkuValidator; Val04CustomValidatorsTest, sekcja 1)
    // =================================================================================================
    //
    // PRZED (reguła rozsiana w kodzie, powtarzana w każdej metodzie, łatwo o różnice):
    //
    //     if (sku == null || !sku.matches("[A-Z]{3}-[0-9]{4}")) throw new IllegalArgumentException("zły SKU");
    //
    // PO (reguła ma nazwę, jedno miejsce i działa wszędzie tam, gdzie wisi adnotacja):
    //
    //     public record SkuForm(@Sku String sku) { }
    //
    // Adnotacja ({@code Sku}) musi mieć:
    //   • @Constraint(validatedBy = SkuValidator.class) — mówi, KTÓRA klasa sprawdza regułę;
    //   • atrybuty message, groups, payload — wszystkie trzy (spec. Jakarta Bean Validation);
    //   • @Retention(RUNTIME) — inaczej adnotacji nie ma w czasie działania programu i walidator jej nie widzi;
    //   • @Target — gdzie wolno ją położyć (pole, metoda, parametr…; klasa tylko dla ograniczeń klasowych, sekcja 3).
    //
    // Walidator ({@code SkuValidator}) implementuje {@code ConstraintValidator<Sku, String>}: pierwszy typ to adnotacja,
    // drugi — typ wartości, którą wolno nią oznaczyć. Dlatego @Sku na Integer kończy się {@code UnexpectedTypeException}
    // (SPRAWDZONE testem — jak @Size na liczbie w Val01BeanValidation).
    //
    // DOBRA PRAKTYKA: w isValid zwracaj true dla null. Dlaczego? Tak robią WSZYSTKIE standardowe ograniczenia
    //   (Val01BeanValidation, sekcja 2). Dzięki temu "pole opcjonalne, ale jeśli podane, to poprawne" to samo {@code @Sku},
    //   a "pole wymagane" to {@code @NotNull @Sku}. Gdyby walidator odrzucał null, nie dałoby się wyrazić pola opcjonalnego.
    //
    // PUŁAPKA: brak jednego z atrybutów message / groups / payload. Dlaczego łatwo to przeoczyć? Kod się kompiluje,
    //   błąd pojawia się dopiero przy PIERWSZYM użyciu: {@code ConstraintDefinitionException} z komunikatem, że adnotacji
    //   brakuje atrybutu (np. payload). SPRAWDZONE testem (adnotacja testowa bez payload).
    //
    // PUŁAPKA: {@code @Retention} pominięte. Dlaczego? Domyślne zachowanie to CLASS — adnotacja jest w pliku .class, ale
    //   nie w pamięci działającego programu, więc Validator jej nie znajduje i reguła po cichu nie działa.
    //
    // PUŁAPKA: walidator, który nie ma publicznego konstruktora bez argumentów (poza sekcją 4, gdzie robi to Spring).
    //   Dlaczego? Czysta fabryka tworzy go przez {@code new}; bez takiego konstruktora dostaniesz
    //   {@code ValidationException: HV000064 Unable to instantiate ConstraintValidator}. Dotyczy też klas wewnętrznych,
    //   które nie są {@code public static}.

    // =================================================================================================
    // 2. OGRANICZENIE ZŁOŻONE Z ISTNIEJĄCYCH (PhoneNumber, PhoneNumberLoud; Val04CustomValidatorsTest, sekcja 2)
    // =================================================================================================
    //
    // Czasem nie trzeba pisać walidatora — wystarczy nazwać KOMBINACJĘ standardowych adnotacji. Adnotacje-składniki kładziesz
    // na samej adnotacji (to tzw. meta-adnotacje):
    //
    //     @NotBlank
    //     @Pattern(regexp = "\\+?[0-9]{9,12}")
    //     @ReportAsSingleViolation
    //     @Constraint(validatedBy = {})          // brak własnego walidatora — sprawdzają składniki
    //     public @interface PhoneNumber { … }
    //
    // Efekt: {@code @PhoneNumber String phone} znaczy to samo co {@code @NotBlank @Pattern(…) String phone}, ale reguła ma
    // jedną nazwę i jedno miejsce zmiany (np. gdy zmieni się format numerów).
    //
    // @ReportAsSingleViolation: bez niej każdy składnik zgłasza OSOBNE naruszenie z własnym komunikatem (dla "   " —
    // NotBlank i Pattern, SPRAWDZONE testem na PhoneNumberLoud). Z nią klient dostaje JEDNO naruszenie z komunikatem
    // adnotacji złożonej ("Numer telefonu: 9-12 cyfr…"). Wybierz drugą wersję, gdy użytkownik ma widzieć jeden czytelny błąd.
    //
    // PUŁAPKA: złożone ograniczenie z {@code @NotBlank} NIE przepuszcza null (inaczej niż {@code @Sku} z sekcji 1).
    //   Dlaczego? Składnik {@code @NotBlank} sam wymaga wartości. Jeśli chcesz pole opcjonalne, zrób złożenie bez @NotBlank
    //   (tylko {@code @Pattern}, które null przepuszcza).

    // =================================================================================================
    // 3. OGRANICZENIE NA CAŁEJ KLASIE: @ValidDateRange (Promotion, ValidDateRangeValidator; test, sekcja 3)
    // =================================================================================================
    //
    // Reguła dotycząca DWÓCH pól ("początek nie po końcu", "hasło = powtórzenie hasła", "kwota minimalna ≤ maksymalna")
    // nie mieści się na pojedynczym polu — kładziesz ją na klasie lub rekordzie, a walidator dostaje CAŁY obiekt:
    //
    //     @ValidDateRange
    //     public record Promotion(@NotBlank String name, LocalDate start, LocalDate end) { }
    //
    // Domyślnie naruszenie dotyczy całego obiektu: ścieżka właściwości jest PUSTA, więc w formularzu nie wiadomo, które pole
    // podświetlić. Dlatego przypinamy błąd do pola (kolejność kroków ma znaczenie):
    //
    //     context.disableDefaultConstraintViolation();                       // 1. wyłącz domyślne naruszenie klasowe
    //     context.buildConstraintViolationWithTemplate(szablonKomunikatu)    // 2. zbuduj własne…
    //            .addPropertyNode("end")                                      //    …przypięte do pola "end"
    //            .addConstraintViolation();                                   // 3. DODAJ je — bez tego nic się nie stanie
    //
    // Test: ścieżka naruszenia to {@code "end"}, a nie pusty tekst. W odpowiedzi 400 z kontrolera (Val02ValidInControllers)
    // klient zobaczy błąd przy polu {@code end}.
    //
    // PUŁAPKA: wyłączasz domyślne naruszenie i zapominasz o {@code addConstraintViolation()}. Dlaczego to groźne?
    //   Spodziewasz się "po prostu false", a Hibernate Validator zgłasza {@code ValidationException}: "HV000033: At least
    //   one custom message must be created if the default error message gets disabled". SPRAWDZONE testem.
    //
    // PUŁAPKA: walidator klasowy dostaje obiekt z polami, które mogą być null (inne adnotacje mogły je odrzucić, ale
    //   WSZYSTKIE ograniczenia są sprawdzane niezależnie, nie "po kolei"). Dlaczego sprawdzamy null w isValid? Inaczej
    //   brak daty zakończyłby się {@code NullPointerException} zamiast zwykłego naruszenia {@code @NotNull}.
    //
    // DOBRA PRAKTYKA: reguły dla jednego pola trzymaj na polu, a klasowe tylko dla zależności między polami. Dlaczego?
    //   Błąd na polu jest dokładniejszy (ścieżka, odrzucona wartość), a walidator klasowy trudniej użyć ponownie.

    // =================================================================================================
    // 4. WALIDATOR JAKO CZĘŚĆ SPRINGA: WSTRZYKIWANIE ZALEŻNOŚCI (CategoryExistsValidator; Val04SpringTest)
    // =================================================================================================
    //
    // Reguła "kategoria istnieje w katalogu" potrzebuje danych — {@code ProductCatalog}. Walidator dostaje je w konstruktorze:
    //
    //     public CategoryExistsValidator(ProductCatalog catalog) { this.catalog = catalog; }
    //
    // Kto wywoła taki konstruktor?
    //   • Czysta fabryka {@code Validation.buildDefaultValidatorFactory()} — NIKT: szuka konstruktora bez argumentów
    //     i zgłasza {@code ValidationException: HV000064 Unable to instantiate ConstraintValidator}
    //     (SPRAWDZONE: Val04CustomValidatorsTest, sekcja 4).
    //   • Bean {@code defaultValidator} z Boota ({@code LocalValidatorFactoryBean}, Val01BeanValidation sekcja 4) — TAK:
    //     używa {@code SpringConstraintValidatorFactory}, który tworzy walidator przez kontener Springa i wstrzykuje
    //     zależności (wstrzykiwanie przez konstruktor, Ioc02ConstructorInjection). SPRAWDZONE: Val04SpringTest;
    //     Val04MockCatalogTest podmienia katalog atrapą ({@code @MockitoBean}) i walidator pyta atrapę — to ten sam bean.
    //
    // Dlatego własnego walidatora NIE oznaczasz {@code @Component}: Spring zbuduje go sam, gdy zajdzie potrzeba. Ten sam
    // mechanizm działa w @Valid w kontrolerach i w walidacji metod serwisu (Val05ServiceValidation), bo wszystkie używają
    // beana {@code defaultValidator}.
    //
    // Hibernate Validator pozwala jeszcze dodać do komunikatu WŁASNY parametr: w {@code isValid} robisz
    // {@code context.unwrap(HibernateConstraintValidatorContext.class).addMessageParameter("categories", …)}, a w szablonie
    // piszesz {@code {categories}} (tak powstaje komunikat "dostępne: akcesoria, herbata, kawa" w ValidationMessages).
    //
    // PUŁAPKA: zapytanie do bazy w walidatorze (np. "czy e-mail jest wolny?"). Dlaczego to zły pomysł?
    //   1) Wydajność: walidator uruchamia się przy KAŻDEJ walidacji, a wiele ograniczeń może pytać o to samo.
    //   2) Wyścig (race condition): między sprawdzeniem "wolny" a zapisem inny użytkownik może zająć ten sam e-mail.
    //   Unikalność gwarantuje dopiero ograniczenie UNIQUE w bazie (s07/s08), a walidator służy co najwyżej do
    //   czytelniejszego komunikatu. Walidatory z szybkim, tylko-do-odczytu źródłem (katalog w pamięci) są w porządku.
    //
    // PUŁAPKA: stan w polach walidatora. Dlaczego? Jedna instancja walidatora obsługuje wiele wywołań (także z wielu
    //   wątków jednocześnie). Zapisywanie w polach danych bieżącej wartości daje wzajemne psucie się wyników. Pola
    //   ustawiaj tylko w konstruktorze albo w {@code initialize} — i tylko do odczytu.

    // =================================================================================================
    // 5. KOMUNIKATY: PARAMETRY, EL, ValidationMessages.properties (MessageForms; test, sekcja 5)
    // =================================================================================================
    //
    // Szablon komunikatu może zawierać:
    //   • {@code {min}}, {@code {max}}, {@code {value}} — atrybuty adnotacji (nazwa w klamrach);
    //   • {@code {val04.sku.invalid}} — klucz z pliku {@code ValidationMessages.properties};
    //   • {@code ${validatedValue}} — odrzucona wartość (to już Expression Language, EL, stąd Tomcat Embed EL w starterze);
    //   • {@code ${formatter.format('%1$.2f', validatedValue)}} — formatowanie liczb (działa);
    //   • {@code \${…}} — dosłowny tekst "${…}" (ukośnik maskuje). W pliku .properties ukośnik trzeba podwoić.
    //
    // ValidationMessages.properties leży w KORZENIU classpath (u nas: src/main/resources/ValidationMessages.properties)
    // i jest czytany jako UTF-8 — polskie litery piszesz wprost, bez ą. SPRAWDZONE testem: komunikat
    // "…(zażółć gęślą jaźń)" wraca nietknięty. Plik obowiązuje WSZYSTKIE lekcje modułu na tej samej ścieżce klas, dlatego
    // klucze mają prefiks {@code val04.}. Warianty językowe to osobne pliki: {@code ValidationMessages_en.properties}, wybierane
    // według Locale (Val01BeanValidation, sekcja 6).
    //
    // WAŻNA ZMIANA (Hibernate Validator 6.2+, więc także 9): wyrażenia EL w komunikatach mają ograniczone możliwości
    // (poziom bezpieczeństwa "bean properties"). Wolno odczytać właściwości i użyć {@code formatter}, ale WYWOŁANIE METODY
    // jest wyłączone — {@code ${validatedValue.length()}} zostaje w komunikacie jako surowy tekst, a w logu pojawia się
    // błąd HV000264 "uses a disabled feature". SPRAWDZONE testem. W starych tutorialach (HV 5) takie wywołania działały —
    // powód zmiany to bezpieczeństwo: komunikaty z danymi od użytkownika mogły wykonać dowolny kod (wstrzyknięcie EL).
    //
    // DOBRA PRAKTYKA: komunikaty w pliku zasobów z kluczem w adnotacji ({@code message() default "{val04.sku.invalid}"}),
    //   a nie wpisane na sztywno. Dlaczego? Zmieniasz tekst w jednym miejscu, łatwo dodać tłumaczenie, a adnotacja
    //   pozostaje czytelna. W API zwracaj klientowi raczej kod ograniczenia i nazwę pola (Val01BeanValidation, sekcja 6).
    //
    // PUŁAPKA: wstawianie odrzuconej wartości do komunikatu bez myślenia. Dlaczego? Komunikat trafia do odpowiedzi i logów:
    //   ${validatedValue} dla hasła albo numeru karty ujawni dane. Dla pól wrażliwych nie używaj tego parametru.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Własne ograniczenie = adnotacja (@Constraint(validatedBy), message/groups/payload, @Retention(RUNTIME), @Target)
     *     + walidator {@code ConstraintValidator<Adnotacja, Typ>} z isValid; null → true.
     *   • Brak message/groups/payload → ConstraintDefinitionException; zły typ → UnexpectedTypeException.
     *   • Złożone: meta-adnotacje @NotBlank @Pattern… + @Constraint(validatedBy = {}); @ReportAsSingleViolation = jedno naruszenie.
     *   • Klasowe: @Target(TYPE); błąd przypnij do pola: disableDefaultConstraintViolation → buildConstraintViolationWithTemplate
     *     → addPropertyNode("pole") → addConstraintViolation (bez ostatniego: HV000033).
     *   • Wstrzykiwanie do walidatora działa tylko ze Springiem (defaultValidator); czysta fabryka: HV000064. Bez @Component.
     *   • Nie rób zapytań do bazy w walidatorze; unikalność gwarantuje UNIQUE w bazie.
     *   • Komunikat: {klucz}, {min}, ${validatedValue}; plik ValidationMessages.properties w korzeniu, UTF-8.
     *     EL: właściwości i formatter tak, wywołania metod nie (HV000264).
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie trzy atrybuty musi mieć każda własna adnotacja ograniczenia i co się stanie, gdy jednego zabraknie?
     *   2. Co się stanie? {@code SkuForm(null)} — ile naruszeń i dlaczego walidator zwraca tu true?
     *   3. Co się stanie? {@code PhoneForms.Quiet(null)} oraz {@code PhoneForms.Loud("   ")} — ile naruszeń w każdym
     *      przypadku i jakich adnotacji?
     *   4. ZNAJDŹ BŁĄD: walidator klasowy robi {@code context.disableDefaultConstraintViolation();
     *      context.buildConstraintViolationWithTemplate("błąd").addPropertyNode("end"); return false;}. Co jest nie tak
     *      i jaki wyjątek poleci?
     *   5. Co się stanie? {@code NewProduct("ABC-1234", "kawa")} walidowany czystą fabryką
     *      {@code Validation.buildDefaultValidatorFactory()}, a potem beanem {@code defaultValidator} ze Springa.
     *   6. Dlaczego walidatora z wstrzykiwanymi zależnościami nie oznaczamy {@code @Component}? A dlaczego nie powinien
     *      zapisywać bieżącej wartości w polu?
     *   7. Co się stanie? Komunikat {@code "Podano ${validatedValue.length()} znaków"} dla tekstu "ab". Dlaczego tak?
     *   8. Dlaczego sprawdzanie unikalności e-maila walidatorem z zapytaniem do bazy nie wystarcza?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA
    // =================================================================================================
    //
    // Szkielet: CustomValidatorExercises (adnotacje i rekordy gotowe, walidatory z TODO), testy:
    // Val04CustomValidatorsExercisesTest (@Tag("cwiczenie")). Rozwiązania wzorcowe:
    // s05_validation.solutions.val04_custom_validators.CustomValidatorSolutions.
    //
    // ĆWICZENIE 1 (łatwe): PostalCodeValidator — polski kod pocztowy "00-950" (dwie cyfry, myślnik, trzy cyfry); null jest poprawny.
    //   Podpowiedź: java.util.regex.Pattern + matcher(value).matches() (cały tekst musi pasować).
    //
    // ĆWICZENIE 2 (średnie): PasswordsMatchValidator — walidator klasowy; różne hasło i powtórzenie = naruszenie
    //   przypięte do pola "repeat"; gdy któreś jest null, zwróć true.
    //   Podpowiedź: sekcja 3, trzy kroki z context. Pamiętaj o addConstraintViolation().
    //   (Test idzie przez prawdziwy Validator, więc dopóki jest TODO, zobaczysz ValidationException z przyczyną TODO.)
    //
    // ĆWICZENIE 3 (trudniejsze): NoDuplicatesValidator — kolekcja tekstów bez powtórzeń, bez rozróżniania wielkości liter
    //   ("Kawa" i "kawa" to duplikat). null jest poprawny; elementy null pomijaj.
    //   Podpowiedź: Set + add() zwraca false, gdy element już był; zamień na małe litery przez
    //   toLowerCase(Locale.ROOT) (dlaczego ROOT? wynik nie może zależeć od ustawień językowych komputera, np. tureckie "I").

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. message (szablon komunikatu), groups (grupy walidacji) i payload (dodatkowe dane). Kod się skompiluje, ale
     *      przy pierwszym użyciu Validator zgłosi {@code ConstraintDefinitionException} (komunikat wskazuje brakujący
     *      atrybut). Sprawdzone w Val04CustomValidatorsTest (sekcja 1).
     *   2. Zero naruszeń. Walidator zwraca true dla null, bo tak robią wszystkie standardowe ograniczenia: wymagalność
     *      wyraża się osobno ({@code @NotNull @Sku}). Dzięki temu jedno {@code @Sku} obsługuje pola opcjonalne.
     *   3. {@code Quiet(null)}: jedno naruszenie nazwane PhoneNumber — składnik @NotBlank odrzuca null, a
     *      @ReportAsSingleViolation zamienia je w jedno naruszenie adnotacji złożonej. {@code Loud("   ")}: dwa naruszenia,
     *      NotBlank i Pattern (każdy składnik zgłasza własne). Sprawdzone w teście sekcji 2.
     *   4. Brakuje {@code .addConstraintViolation()} na końcu łańcucha. Domyślne naruszenie wyłączone, a własnego nie
     *      dodano, więc leci {@code ValidationException} z komunikatem "HV000033: At least one custom message must be
     *      created if the default error message gets disabled". Sprawdzone w teście sekcji 3.
     *   5. Czysta fabryka: {@code ValidationException} "HV000064: Unable to instantiate ConstraintValidator" — brak
     *      konstruktora bez argumentów, nikt nie poda katalogu. Bean defaultValidator: 0 naruszeń, bo
     *      SpringConstraintValidatorFactory wstrzykuje ProductCatalog, a kategoria "kawa" istnieje. Sprawdzone
     *      w testach sekcji 4.
     *   6. Spring tworzy walidator sam (SpringConstraintValidatorFactory), więc @Component jest zbędne i tylko
     *      zaciemnia (powstałby dodatkowy, nieużywany bean). Pole ze stanem bieżącej wartości psuje wyniki, bo jedna
     *      instancja walidatora obsługuje wiele wywołań, także równoległych (wyścig między wątkami).
     *   7. Komunikat zostaje surowy: "Podano ${validatedValue.length()} znaków…", a w logu pojawia się błąd HV000264
     *      (disabled feature). Od Hibernate Validator 6.2 EL w komunikatach nie pozwala na wywołania metod
     *      (bezpieczeństwo); działają właściwości i {@code formatter}. Sprawdzone w teście sekcji 5.
     *   8. Między sprawdzeniem a zapisem inny użytkownik może zająć ten sam e-mail (wyścig), a pytanie do bazy przy każdej
     *      walidacji kosztuje. Gwarancję daje ograniczenie UNIQUE w bazie (s07/s08); walidator najwyżej poprawia komunikat.
     */
    // </editor-fold>
}
