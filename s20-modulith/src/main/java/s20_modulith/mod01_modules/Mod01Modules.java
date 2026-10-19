package s20_modulith.mod01_modules;

import org.springframework.modulith.core.ApplicationModules;

import java.util.List;

/**
 * <pre>
 * TEMAT: Monolit modułowy ze Spring Modulith — moduły, API modułu i test granic verify()
 *        (modular monolith = monolit modułowy; module = moduł; boundary = granica; verify = sprawdzić;
 *         internal = wewnętrzny; exposed = wystawiony na zewnątrz; cycle = cykl; violation = naruszenie;
 *         allowed dependencies = dozwolone zależności)
 *
 * W SKRÓCIE:
 *   Monolit modułowy to JEDNA aplikacja (jeden JAR, jedna baza, jedno wdrożenie) podzielona na moduły biznesowe
 *   z wyraźnymi granicami: produkty, klienci, zamówienia. Każdy moduł ma API (to, czego wolno używać innym) i środek
 *   (szczegóły, których nikt poza nim nie dotyka). Spring Modulith wyznacza moduły z PAKIETÓW: moduł = bezpośredni
 *   podpakiet pakietu klasy {@code @SpringBootApplication}. Granic pilnuje test
 *   {@code ApplicationModules.of(Mod01Application.class).verify()}: zabrania sięgania do środka innego modułu,
 *   cykli między modułami i zależności spoza zadeklarowanej listy. Granica, której nikt nie sprawdza, po roku
 *   już nie istnieje — dlatego sprawdza ją automat przy każdym {@code mvnw test}.
 *
 * ANALOGIA: centrum handlowe.
 *   Każdy sklep (moduł) ma ladę (API), przy której obsługuje klientów i inne sklepy, oraz zaplecze (internal), na
 *   które wstęp mają tylko jego pracownicy. Ochrona (verify) wyprasza każdego, kto wchodzi na cudze zaplecze, i
 *   pilnuje, żeby dwa sklepy nie były od siebie wzajemnie zależne (cykl). Wszystko jest pod jednym dachem (jedna
 *   aplikacja) — nikt nie jeździ między budynkami (sieć, jak w mikroserwisach).
 *
 * JAK TO DZIAŁA:
 *   1. {@code ApplicationModules.of(Mod01Application.class)} czyta adnotację klasy ({@code @SpringBootApplication},
 *      {@code @Modulith} albo {@code @Modulithic}) i bierze jej pakiet jako pakiet główny (root package).
 *   2. Biblioteka ArchUnit (analiza skompilowanych klas) wczytuje klasy z pakietu głównego — BEZ klas testowych —
 *      i buduje graf zależności między typami: pola, parametry konstruktorów i metod, wywołania, typy zwracane.
 *   3. Strategia wykrywania (detection strategy) {@code direct-sub-packages} (domyślna): każdy bezpośredni podpakiet
 *      to moduł, a jego nazwa (identyfikator) to nazwa podpakietu: customers, orders, products. Głębsze podpakiety
 *      (np. products.internal) należą do swojego modułu — nie są osobnymi modułami.
 *   4. API modułu = typy w PAKIECIE BAZOWYM modułu (products, nie products.internal). Wszystko w podpakietach jest
 *      wewnętrzne, nawet klasy {@code public}.
 *   5. {@code verify()} sprawdza: brak cykli między modułami, użycie innych modułów tylko przez API, zgodność
 *      z {@code allowedDependencies} i brak wstrzykiwania przez pole. Naruszenia → wyjątek {@code Violations}
 *      (naruszenia) z listą wszystkich problemów naraz.
 *   6. W czasie działania nic się nie zmienia: Spring tworzy beany jak zawsze. Modulith to test ARCHITEKTURY —
 *      jak test jednostkowy, tylko że sprawdza strukturę kodu, a nie wynik metody.
 *
 * SŁÓWKA:
 *   modular monolith = monolit modułowy; module = moduł; root package = pakiet główny; base package = pakiet bazowy
 *   modułu; exposed = wystawiony (dostępny dla innych); internal = wewnętrzny; violation = naruszenie;
 *   cycle = cykl; slice = wycinek (tak ArchUnit nazywa grupę pakietów); allowed = dozwolony; display name = nazwa
 *   wyświetlana; detection strategy = strategia wykrywania; open = otwarty; closed = zamknięty;
 *   field injection = wstrzykiwanie przez pole.
 *
 * ZOBACZ TEŻ: t06_oop_basics/Oop08PackagesAccess (pakiety i modyfikatory dostępu),
 *             t30_build_modules/Build03Modules (moduły JPMS — granice na poziomie JAR-ów),
 *             s01_core_ioc/Ioc02ConstructorInjection (wstrzykiwanie przez konstruktor),
 *             s01_core_ioc/Ioc03Stereotypes (@Service, @Component),
 *             s20_modulith/Mod02ModuleEvents (moduły rozmawiają zdarzeniami — sposób na cykle),
 *             s20_modulith/Mod03Documentation (diagramy modułów, @NamedInterface, monolit czy mikroserwisy).
 * </pre>
 */
public final class Mod01Modules {

    private Mod01Modules() {
    }

    // =================================================================================================
    // 1. MONOLIT MODUŁOWY: MODUŁ = BEZPOŚREDNI PODPAKIET KLASY STARTOWEJ
    // =================================================================================================
    //
    //   s20_modulith.mod01_modules            ← pakiet główny (tu leży Mod01Application)
    //   ├── customers                         ← moduł „customers”  (CustomerDirectory, CustomerView)
    //   ├── orders                            ← moduł „orders”     (OrderService, OrderConfirmation)
    //   │   └── internal                      ← część modułu orders (OrderNumbers), NIE osobny moduł
    //   └── products                          ← moduł „products”   (ProductCatalog, ProductView)
    //       └── internal                      ← część modułu products (ProductStore)
    //
    //   ApplicationModules modules = ApplicationModules.of(Mod01Application.class);
    //   System.out.println(modules);          // opis wszystkich modułów — prawdziwy wynik:
    //
    //   # Zamówienia
    //   > Logical name: orders
    //   > Base package: s20_modulith.mod01_modules.orders
    //   > Excluded packages: none
    //   > Direct module dependencies:
    //     + customers
    //     + products
    //   > Spring beans:
    //     + ….OrderService
    //     o ….internal.OrderNumbers
    //
    // „Logical name” to identyfikator modułu (nazwa podpakietu), „Zamówienia” — nazwa wyświetlana z
    // @ApplicationModule(displayName = …) w package-info.java (bez niej byłoby „Orders”: nazwa podpakietu wielką
    // literą). Klasy leżące bezpośrednio w pakiecie głównym (Mod01Application, ta klasa lekcji) nie należą do
    // żadnego zwykłego modułu — Modulith przypisuje je specjalnemu modułowi „root:s20_modulith.mod01_modules”.
    // Test sekcji 1.
    //
    // PYTANIE REKRUTACYJNE: czym jest monolit modułowy? Jedna aplikacja wdrażana w całości, ale podzielona na moduły
    //   biznesowe z jawnym API i sprawdzanymi granicami. Daje porządek podobny do mikroserwisów bez kosztów sieci,
    //   osobnych wdrożeń i rozproszonych transakcji. Dobrze wydzielony moduł można później wyjąć do mikroserwisu.
    //
    // Izolacja lekcji w tym kursie: każda lekcja ma własną klasę startową, a moduły biznesowe są JEJ podpakietami.
    // Rozwiązania ćwiczeń leżą w s20_modulith.solutions.mod01_modules — poza pakietem głównym tej aplikacji, więc
    // Modulith ich nie widzi i nie robi z nich modułu „solutions” (sprawdza to test sekcji 1).
    //
    // DOBRA PRAKTYKA: dziel na moduły według BIZNESU (produkty, zamówienia, płatności), a nie według warstw
    //   (controllers, services, repositories). Dlaczego? Zmiana biznesowa (np. nowy rodzaj rabatu) dotyka wtedy
    //   jednego modułu. Przy podziale na warstwy każda zmiana rozlewa się po wszystkich pakietach, a „moduł
    //   services” zależy od wszystkiego — granice nic nie chronią.

    // =================================================================================================
    // 2. API MODUŁU A PAKIET internal
    // =================================================================================================
    //
    //   products                        ← API: ProductCatalog, ProductView        (isExposed = true)
    //   products.internal               ← środek: ProductStore                    (isExposed = false)
    //
    // W opisie modułu „+” oznacza typ wystawiony (część API), a „o” — wewnętrzny. ProductStore jest public, bo
    // ProductCatalog (inny pakiet) musi go użyć — Java nie ma modyfikatora „widoczny tylko w moim pakiecie i jego
    // podpakietach”. Granicę „środek modułu” wyznacza więc konwencja pakietów, a pilnuje jej verify().
    // Test sekcji 2 (ApplicationModule.isExposed).
    //
    // DOBRA PRAKTYKA: API modułu niech będzie małe i opisane w języku biznesu (ProductCatalog.findBySku,
    //   isAvailable), a do innych modułów wychodzą niezmienne widoki (rekordy ProductView, CustomerView), nie obiekty
    //   ze środka. Dlaczego? Wszystko, co wystawisz, ktoś zacznie używać — i każda zmiana stanie się zmianą API.
    //
    // DOBRA PRAKTYKA: gdy moduł jest mały, trzymaj go w JEDNYM pakiecie, a szczegóły zrób pakietowo-prywatne
    //   (bez public). Dlaczego? Wtedy granicy pilnuje już kompilator. Podpakiet internal przydaje się, gdy moduł
    //   urośnie — za cenę klas public, których chroni tylko verify().

    // =================================================================================================
    // 3. verify(): SIĘGANIE DO ŚRODKA INNEGO MODUŁU
    // =================================================================================================
    //
    //   @Test
    //   void modulesAreValid() {
    //       ApplicationModules.of(Mod01Application.class).verify();   // nic nie rzuca = granice w porządku
    //   }
    //
    // Celowo zła aplikacja (src/test/java, pakiet s20_modulith.mod01_broken.internal_access): OrderService
    // z modułu orders wstrzykuje ProductStore z products.internal. Kompilator to przepuszcza, Spring też —
    // aplikacja startuje i działa (sprawdza to test sekcji 3). Dopiero verify() rzuca Violations. Prawdziwy komunikat
    // (fragment; takich pozycji jest 4 — po jednej na każde miejsce użycia: deklaracja konstruktora, jego parametr,
    // typ pola i wywołanie metody ProductStore):
    //
    //   - Module 'orders' depends on non-exposed type
    //     s20_modulith.mod01_broken.internal_access.products.internal.ProductStore within module 'products'!
    //   OrderService declares constructor OrderService(ProductStore) in (OrderService.java:0)
    //
    // („Moduł orders zależy od niewystawionego typu ProductStore z modułu products”). Naprawa: użyć API
    // (ProductCatalog), a gdy API czegoś nie oferuje — dodać metodę do API, a nie otwierać środek.
    //
    // PUŁAPKA: celowo złe aplikacje (jak tutaj) w src/test/java i zwykłe ApplicationModules.of(Zla.class).
    //   Wynik: IllegalArgumentException „No classes found in packages [s20_modulith.mod01_broken.internal_access]!”.
    //   Dlaczego? Modulith domyślnie pomija klasy testowe (opcja ArchUnit ImportOption.DoNotIncludeTests) — testy
    //   mogą swobodnie sięgać do środka modułów i nie psuć weryfikacji. Do analizy aplikacji z testów trzeba
    //   podać ApplicationModules.of(Zla.class, new ImportOption.OnlyIncludeTests()) — tak robią testy tej lekcji.
    //
    // PUŁAPKA: verify() sprawdza dany obiekt ApplicationModules tylko RAZ — drugie wywołanie nic nie robi, nawet
    //   jeśli pierwsze rzuciło wyjątek (sprawdzone testem). Dlaczego? Obiekt zapamiętuje, że weryfikacja się odbyła,
    //   a ApplicationModules.of(Klasa.class) zwraca obiekt z pamięci podręcznej. Gdy chcesz sprawdzać wielokrotnie
    //   albo obejrzeć naruszenia bez wyjątku — użyj detectViolations() (zwraca Violations, liczy za każdym razem).

    // =================================================================================================
    // 4. CYKLE MIĘDZY MODUŁAMI
    // =================================================================================================
    //
    // Zła aplikacja s20_modulith.mod01_broken.cycle: orders → products (OrderService używa ProductCatalog —
    // w porządku), ale products → orders (ProductCatalog pyta OrderHistory o bestsellery). Oba użycia idą przez API,
    // a mimo to verify() zgłasza (prawdziwy komunikat, skrócony):
    //
    //   - Cycle detected: Slice orders ->
    //                   Slice products ->
    //                   Slice orders
    //     1. Dependencies of Slice orders
    //       - Constructor <…OrderService.<init>(…ProductCatalog)> has parameter of type <…ProductCatalog> …
    //     2. Dependencies of Slice products
    //       - Constructor <…ProductCatalog.<init>(…OrderHistory)> has parameter of type <…OrderHistory> …
    //
    // Dlaczego cykl jest zły? Dwa moduły zależne od siebie nawzajem to w praktyce JEDEN moduł: nie da się ich
    // zrozumieć, przetestować ani wydzielić osobno, a zmiana w jednym psuje drugi. Jak rozciąć cykl:
    //   • odwrócić jedną zależność zdarzeniem — orders publikuje „zamówienie złożone”, a products samo liczy
    //     bestsellery, słuchając zdarzeń (lekcja Mod02ModuleEvents);
    //   • przenieść wspólną rzecz do modułu, od którego oba mogą zależeć;
    //   • zastanowić się, czy granica jest we właściwym miejscu (może to naprawdę jeden moduł).
    //
    // PUŁAPKA: cykl bez cyklu beanów. Spring wykrywa tylko cykl wstrzykiwania (A potrzebuje B, B potrzebuje A).
    //   Tutaj beany NIE tworzą cyklu (ProductCatalog → OrderHistory, OrderService → ProductCatalog), więc aplikacja
    //   wystartuje — a moduły i tak są splecione. Cykl między modułami widzi dopiero verify().

    // =================================================================================================
    // 5. @ApplicationModule(allowedDependencies = …) — LISTA DOZWOLONYCH MODUŁÓW
    // =================================================================================================
    //
    //   // orders/package-info.java
    //   @ApplicationModule(displayName = "Zamówienia", allowedDependencies = {"products", "customers"})
    //   package s20_modulith.mod01_modules.orders;
    //
    //   // products/package-info.java i customers/package-info.java
    //   @ApplicationModule(displayName = "Produkty", allowedDependencies = {})   // moduł bazowy: zero zależności
    //
    // Trzy stany atrybutu:
    //   • brak atrybutu (wartość domyślna) — wolno zależeć od dowolnego modułu, byle przez API;
    //   • {} — nie wolno zależeć od żadnego modułu;
    //   • {"products", "customers"} — tylko od wymienionych (nazwy modułów = nazwy podpakietów).
    // Zła aplikacja s20_modulith.mod01_broken.not_allowed: orders ma allowedDependencies = "products", a używa
    // CustomerDirectory. Prawdziwy komunikat:
    //
    //   - Module 'orders' depends on module 'customers' via
    //     s20_modulith.mod01_broken.not_allowed.orders.OrderService ->
    //     s20_modulith.mod01_broken.not_allowed.customers.CustomerDirectory. Allowed targets: products.
    //
    // Przy allowedDependencies = {} komunikat kończy się „Allowed targets: none.” (sprawdzone uruchomieniem).
    // Test sekcji 5 sprawdza też, że dobra aplikacja ma dokładnie zadeklarowane zależności (orders → customers,
    // products; products i customers — żadnych).
    //
    // DOBRA PRAKTYKA: deklaruj allowedDependencies przynajmniej dla modułów bazowych ({}). Dlaczego? Nowa zależność
    //   staje się świadomą decyzją widoczną w przeglądzie kodu (zmiana package-info.java), a nie przypadkiem,
    //   który ktoś zauważy po roku.
    //
    // Dla porządku: @ApplicationModule(type = ApplicationModule.Type.OPEN) oznacza moduł OTWARTY — bez ukrytego
    // środka i bez sprawdzania cykli. To furtka na czas porządkowania starego kodu, a nie stan docelowy.

    // =================================================================================================
    // 6. verify() ZGŁASZA TEŻ WSTRZYKIWANIE PRZEZ POLE
    // =================================================================================================
    //
    // Aplikacja s20_modulith.mod01_broken.field_injection: OrderService ma @Autowired private ProductCatalog catalog.
    // Zależność jest dozwolona (API innego modułu), ale verify() i tak zgłasza (prawdziwy komunikat):
    //
    //   - Module Orders uses field injection in s20_modulith.mod01_broken.field_injection.orders.OrderService.catalog.
    //     Prefer constructor injection instead!
    //
    // („Moduł używa wstrzykiwania przez pole — wybierz wstrzykiwanie przez konstruktor”). W tym komunikacie stoi
    // nazwa wyświetlana „Orders”, a nie identyfikator 'orders'. Test sekcji 6.
    //
    // PUŁAPKA: przenosisz do projektu ze Spring Modulith stary kod z @Autowired na polach — i test architektury
    //   pada, choć „przecież działa”. Dlaczego Modulith to zgłasza? Zależność przez konstruktor jest widoczna
    //   w sygnaturze, obiekt da się złożyć w teście bez Springa, a pole final gwarantuje, że zależność jest zawsze
    //   ustawiona. Szczegóły: lekcja Ioc02ConstructorInjection.

    // =================================================================================================
    // 7. GRANICE PILNUJE TEST, NIE KOMPILATOR I NIE SPRING
    // =================================================================================================
    //
    // W czasie działania aplikacji moduły to zwykłe beany: Spring skanuje cały pakiet główny i tworzy także beany
    // z pakietów internal (ProductStore, OrderNumbers) — każdy może je wstrzyknąć. Test sekcji 7 uruchamia
    // Mod01Application (@SpringBootTest, profil mod01): zamówienie przechodzi przez trzy moduły, a bean ProductStore
    // istnieje w kontekście. Dlatego test verify() MUSI być w projekcie — bez niego granice są tylko umową.
    //
    // DOBRA PRAKTYKA: jeden test architektury na aplikację, uruchamiany przy każdym budowaniu:
    //   class ModularityTest { @Test void verify() { ApplicationModules.of(App.class).verify(); } }
    //   Dlaczego? Kosztuje kilka sekund, a łapie złamanie granic w chwili powstania, a nie przy próbie wydzielenia
    //   modułu po latach. Zależność: spring-modulith-starter-test (scope test) albo spring-modulith-core.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Moduł = bezpośredni podpakiet pakietu klasy @SpringBootApplication; nazwa modułu = nazwa podpakietu.
     *   • API modułu = typy w jego pakiecie bazowym; podpakiety (np. internal) = środek, nawet gdy klasy są public.
     *   • ApplicationModules.of(App.class).verify() w teście: brak cykli, tylko API innych modułów,
     *     allowedDependencies, brak wstrzykiwania przez pole. Błąd → Violations z listą naruszeń.
     *   • detectViolations() — naruszenia bez wyjątku, liczone za każdym razem; verify() działa raz na obiekt.
     *   • package-info.java: @ApplicationModule(displayName = "…", allowedDependencies = {…}); brak atrybutu =
     *     wszystko wolno (przez API), {} = nic nie wolno.
     *   • Klasy testowe są pomijane w analizie (ImportOption.DoNotIncludeTests); aplikacja z testów →
     *     ApplicationModules.of(Klasa.class, new ImportOption.OnlyIncludeTests()).
     *   • Modulith nie zmienia działania aplikacji — Spring tworzy beany jak zawsze; granic pilnuje test.
     *   • Cykl rozcinaj zdarzeniem (Mod02ModuleEvents) albo przeniesieniem wspólnej części.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie moduły wyznaczy Modulith dla Mod01Application i dlaczego orders.internal nie jest osobnym modułem?
     *   2. Co się stanie? W module orders zmieniasz konstruktor na OrderService(ProductStore store), gdzie ProductStore
     *      pochodzi z products.internal. Czy kod się skompiluje, czy aplikacja wystartuje i co zrobi test verify()?
     *   3. ZNAJDŹ BŁĄD: ProductCatalog (moduł products) wstrzykuje OrderHistory z modułu orders, żeby pokazać
     *      bestsellery, a OrderService (moduł orders) używa ProductCatalog. Obie klasy są w pakietach bazowych
     *      modułów. Co zgłosi verify() i jak to naprawić?
     *   4. Co się stanie? Moduł orders ma @ApplicationModule(allowedDependencies = "products"), a OrderService
     *      wstrzykuje CustomerDirectory z modułu customers.
     *   5. Dlaczego ProductStore musi być public, skoro jest „wewnętrzny”? Co w takim razie chroni go przed użyciem
     *      z innego modułu?
     *   6. ZNAJDŹ BŁĄD: w module orders: @Service class OrderService { @Autowired private ProductCatalog catalog; … }
     *      — ProductCatalog to API modułu products, zależność jest dozwolona. Dlaczego test architektury pada?
     *   7. Co się stanie? Celowo zła aplikacja leży w src/test/java, a test woła ApplicationModules.of(Zla.class)
     *      .verify() bez żadnej ImportOption.
     *   8. Czym różni się allowedDependencies = {} od pominięcia tego atrybutu?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Mod01ModulesExercisesTest (dobra aplikacja Mod01Application i złe aplikacje z testów)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć nazwy (identyfikatory) wszystkich modułów, posortowane alfabetycznie. Dla
     * Mod01Application: [customers, orders, products]. Podpowiedź: {@code modules.stream()},
     * {@code getIdentifier().toString()}, {@code sorted()}.
     */
    static List<String> exercise1(ApplicationModules modules) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (łatwe): zwróć {@code true}, gdy aplikacja nie ma żadnych naruszeń architektury, i {@code false}
     * w przeciwnym razie — BEZ rzucania wyjątku. Podpowiedź: {@code detectViolations()} i {@code hasViolations()}.
     */
    static boolean exercise2(ApplicationModules modules) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): sprawdź, czy typ należy do API swojego modułu (jest wystawiony). Typ spoza wszystkich
     * modułów → {@code false}. Podpowiedź: {@code modules.getModuleByType(type)} zwraca Optional, a moduł ma
     * metodę {@code isExposed(Class)}.
     */
    static boolean exercise3(ApplicationModules modules, Class<?> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zwróć posortowane nazwy modułów, od których BEZPOŚREDNIO zależy moduł o podanej
     * nazwie (dla "orders" w Mod01Application: [customers, products]). Gdy modułu nie ma — rzuć
     * {@code IllegalArgumentException} z komunikatem „Nie ma modułu: nazwa”. Podpowiedź:
     * {@code getModuleByName(name)}, {@code getDirectDependencies(modules)}, {@code uniqueModules()}.
     */
    static List<String> exercise4(ApplicationModules modules, String moduleName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Trzy: customers, orders, products — po jednym na każdy BEZPOŚREDNI podpakiet pakietu klasy startowej
     *      (strategia direct-sub-packages). orders.internal to podpakiet modułu orders, więc należy do niego; jego
     *      klasy są wewnętrzne. Sprawdzone testem sekcji 1.
     *   2. Kod się skompiluje (ProductStore jest public) i aplikacja wystartuje (to zwykły bean) — sprawdzone testem
     *      sekcji 3 na złej aplikacji. Test verify() rzuci Violations: „Module 'orders' depends on non-exposed type
     *      …products.internal.ProductStore within module 'products'!”.
     *   3. Cykl między modułami: „Cycle detected: Slice orders -> Slice products -> Slice orders” (sprawdzone testem
     *      sekcji 4). Spring wystartuje, bo beany nie tworzą cyklu. Naprawa: odwrócić jedną zależność (orders publikuje
     *      zdarzenie, products liczy bestsellery z zdarzeń — Mod02ModuleEvents) albo przenieść wspólną część.
     *   4. verify() rzuci Violations: „Module 'orders' depends on module 'customers' via …OrderService ->
     *      …CustomerDirectory. Allowed targets: products.” Sprawdzone testem sekcji 5.
     *   5. Bo ProductCatalog leży w innym pakiecie (products), a Java nie ma widoczności „pakiet i jego podpakiety”.
     *      Przed użyciem z innych modułów chroni go tylko konwencja (podpakiet = środek modułu) i test verify().
     *   6. Spring Modulith zgłasza wstrzykiwanie przez pole: „Module Orders uses field injection in …OrderService.catalog.
     *      Prefer constructor injection instead!” (sprawdzone testem sekcji 6). Naprawa: pole final + konstruktor.
     *   7. IllegalArgumentException: „No classes found in packages […]!” — Modulith domyślnie pomija klasy testowe.
     *      Trzeba podać new ImportOption.OnlyIncludeTests(). Sprawdzone testem sekcji 3.
     *   8. {} = moduł nie może zależeć od żadnego innego modułu (naruszenie kończy się „Allowed targets: none.” —
     *      sprawdzone uruchomieniem). Brak atrybutu = może zależeć od dowolnego modułu, byle przez API i bez cykli.
     */
    // </editor-fold>
}
