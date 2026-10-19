package s01_core_ioc.ioc07_lifecycle;

import java.util.List;

/**
 * <pre>
 * TEMAT: Cykl życia beana — od konstruktora do zamknięcia kontekstu
 *        (lifecycle = cykl życia; {@code @PostConstruct} = po skonstruowaniu; {@code @PreDestroy} = przed zniszczeniem;
 *         initialization = inicjalizacja, czyli przygotowanie do pracy; destruction = zniszczenie, sprzątanie)
 *
 * W SKRÓCIE:
 *   Bean nie "pojawia się" gotowy. Kontener tworzy go krok po kroku: konstruktor, potem wstrzyknięcie zależności
 *   (setter albo pole), potem metoda {@code @PostConstruct}, w której WSZYSTKO jest już wstrzyknięte i można zacząć
 *   pracę (nawiązać połączenie, załadować dane). Gdy kontekst się zamyka, kontener woła {@code @PreDestroy}, żebyś
 *   zwolnił zasoby. Praca potrzebująca zależności należy do {@code @PostConstruct}, a NIE do konstruktora.
 *
 * ANALOGIA: otwieranie i zamykanie sklepu.
 *   Konstruktor = postawienie budynku (są ściany, nie ma jeszcze towaru ani prądu). Wstrzyknięcie zależności =
 *   podłączenie prądu, dostawa towaru. {@code @PostConstruct} = rano obchód: kasjer włącza kasę i układa towar —
 *   dopiero teraz, gdy wszystko jest podłączone. {@code @PreDestroy} = wieczorem: zamknij kasę, wyłącz światło.
 *   Gdy nikt nie zamknie sklepu (nie wywoła close), wieczorny obchód się nie odbędzie.
 *
 * JAK TO DZIAŁA:
 *   new AnnotationConfigApplicationContext(EventLog.class, ProductCatalog.class, WarehouseConnection.class)
 *     1. konstruktor WarehouseConnection(EventLog)       → dziennik: "konstruktor"   (katalogu jeszcze nie ma)
 *     2. wstrzyknięcie setCatalog(ProductCatalog)        → dziennik: "setter"
 *     3. metoda z {@code @PostConstruct}                 → dziennik: "@PostConstruct" (bean gotowy do pracy)
 *     4. ... bean pracuje, aplikacja działa ...
 *     5. context.close()                                 → metoda z {@code @PreDestroy}: "@PreDestroy"
 *   Kolejność wszystkich mechanizmów (sekcja 4):
 *     start:  {@code @PostConstruct} → afterPropertiesSet() → metoda z initMethod
 *     koniec: {@code @PreDestroy}    → destroy()            → metoda z destroyMethod
 *   Spring Boot sam dba o zamknięcie kontekstu przy zatrzymaniu aplikacji (haczyk zamykający, shutdown hook).
 *   W gołym kontekście z testu zamykasz go Ty (close() albo try-with-resources).
 *
 * SŁÓWKA:
 *   constructor = konstruktor; setter = metoda ustawiająca; post = po; pre = przed; destroy = zniszczyć;
 *   shutdown hook = haczyk zamykający (kod uruchamiany przy zatrzymaniu programu); cache = pamięć podręczna;
 *   warm up = rozgrzać (wypełnić z góry); resource = zasób (połączenie, plik); callback = metoda wywoływana przez framework.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc02ConstructorInjection (konstruktor, pole, setter),
 *             s01_core_ioc/Ioc06Scopes (zasięgi — prototype ma inny cykl życia),
 *             t10_exceptions/Exceptions04TryWithResources (zamykanie zasobów w czystej Javie).
 * </pre>
 */
public final class Ioc07Lifecycle {

    private Ioc07Lifecycle() {
    }

    // =================================================================================================
    // 1. KOLEJNOŚĆ ZDARZEŃ — konstruktor, setter, @PostConstruct, @PreDestroy
    // =================================================================================================
    //
    // WarehouseConnection (połączenie z magazynem) zapisuje każdy etap w EventLog (dzienniku zdarzeń).
    // Test tworzy kontekst, odczytuje dziennik, zamyka kontekst i odczytuje dziennik jeszcze raz:
    //   przed close():  konstruktor, setter, @PostConstruct
    //   po close():     konstruktor, setter, @PostConstruct, @PreDestroy
    //
    // PUŁAPKA: kontekst, którego nie zamkniesz, nigdy nie wywoła @PreDestroy.
    //   Dlaczego? Wywołanie sprzątania to część zamykania kontekstu. W teście z "gołym" kontekstem sam wywołujesz
    //   close(); w aplikacji Spring Boot zamknięcie przy zatrzymaniu programu załatwia haczyk zamykający.
    //
    // Test: Ioc07LifecycleTest#orderOfEvents

    // =================================================================================================
    // 2. DLACZEGO NIE W KONSTRUKTORZE — zależności z settera i pola są jeszcze null
    // =================================================================================================
    //
    // Spring najpierw woła konstruktor, a DOPIERO POTEM wstrzykuje zależności do pól i setterów. Zależność przez
    // konstruktor jest dostępna od razu; ta przez pole lub setter nie.
    //
    // PRZED (BrokenProductCache):
    //   @Autowired private ProductCatalog catalog;
    //   public BrokenProductCache() { products.addAll(catalog.names()); }   ← catalog == null → NullPointerException
    //   Kontekst się nie uruchomi (BeanCreationException z przyczyną NullPointerException).
    //
    // PO (ProductCache):
    //   @PostConstruct void warmUp() { products.addAll(catalog.names()); }   ← catalog jest już wstrzyknięty
    //
    // PUŁAPKA: praca w konstruktorze na zależnościach wstrzykiwanych polem lub setterem.
    //   Dlaczego? Konstruktor działa, zanim Spring ustawi pola i settery, więc widzisz null. Błąd potrafi się
    //   ukryć, gdy ktoś zmieni sposób wstrzykiwania z konstruktora na pole.
    //
    // DOBRA PRAKTYKA: przygotowanie wymagające zależności (ładowanie danych, otwieranie połączenia) rób w
    //   @PostConstruct, a w konstruktorze tylko przypisuj pola. Dlaczego? W @PostConstruct masz gwarancję, że
    //   wszystkie zależności są gotowe — niezależnie od tego, jak zostały wstrzyknięte. (Gdy wszystko idzie przez
    //   konstruktor, zależności są dostępne i w nim, ale ładowanie danych nadal lepiej trzymać w osobnej metodzie.)
    //
    // Test: Ioc07LifecycleTest#constructorSeesNoSetterDependencies, #brokenCacheFailsInConstructor,
    //       #cacheWarmedUpInPostConstruct

    // =================================================================================================
    // 3. @PostConstruct i błędy — wyjątek przerywa start
    // =================================================================================================
    //
    // Jeśli metoda @PostConstruct rzuci wyjątek, bean nie powstaje, a kontekst nie wystartuje
    // (BeanCreationException). To dobrze: aplikacja z niedziałającym magazynem ma się nie uruchomić po cichu,
    // tylko od razu pokazać problem (zasada "fail fast" — pada szybko, zamiast po cichu działać źle).
    //
    // Uwaga: w Spring Boot 4 / Spring Framework 7 adnotacje @PostConstruct i @PreDestroy to nadal
    // jakarta.annotation (pakiet jakarta, nie javax — starsze samouczki piszą javax.annotation).
    //
    // Test: Ioc07LifecycleTest#failingPostConstructStopsStartup

    // =================================================================================================
    // 4. INNE SPOSOBY: InitializingBean, DisposableBean, initMethod, destroyMethod
    // =================================================================================================
    //
    // a) interfejsy Springa: InitializingBean.afterPropertiesSet() (initializing bean = bean inicjalizowany;
    //    after properties set = po ustawieniu właściwości) i DisposableBean.destroy() (disposable bean = bean do
    //    zniszczenia; destroy = zniszcz). Wiążą klasę ze Springiem, więc zwykle gorsze niż adnotacje.
    //    Spotkasz je w starszym kodzie.
    // b) @Bean(initMethod = "customInit", destroyMethod = "customDestroy") — nazwy metod podane w konfiguracji.
    //    Przydatne dla klas, których kodu nie możesz zmienić (z cudzej biblioteki).
    // c) Dla beanów z metod @Bean Spring sam wnioskuje metodę zamykającą: jeśli klasa ma publiczną metodę close()
    //    bez parametrów, wywoła ją przy zamknięciu kontekstu; gdy close() nie ma — publiczną shutdown(). Nigdy obu
    //    naraz: przy obu metodach wołane jest tylko close(). Wyłączysz to przez @Bean(destroyMethod = "").
    //    Do tego KAŻDY bean (także z @Component albo zarejestrowany wprost), który implementuje AutoCloseable,
    //    dostaje close() przy zamknięciu kontekstu.
    //
    // Gdy użyjesz wszystkich naraz, kolejność jest stała (sprawdzone testem):
    //   start:  @PostConstruct → afterPropertiesSet → initMethod
    //   koniec: @PreDestroy    → destroy            → destroyMethod
    //
    // DOBRA PRAKTYKA: wybierz JEDEN mechanizm w klasie — najczęściej @PostConstruct i @PreDestroy.
    //   Dlaczego? Kilka mechanizmów naraz działa, ale czytelnik musi znać kolejność, żeby zrozumieć, co się dzieje.
    //
    // Test: Ioc07LifecycleTest#allCallbacksOrder, #inferredDestroyMethod, #closeWinsOverShutdownAndAutoCloseable

    // =================================================================================================
    // 5. PROTOTYPE — Spring nie niszczy tych beanów
    // =================================================================================================
    //
    // Dla beana prototype kontener woła konstruktor, wstrzykuje zależności i wywołuje @PostConstruct przy
    // KAŻDYM pobraniu, ale potem o obiekcie "zapomina". @PreDestroy NIE zostanie wywołany nigdy — ani przy
    // zamknięciu kontekstu, ani wcześniej.
    //
    // PUŁAPKA: zasób zwalniany w @PreDestroy beana prototype nigdy nie zostanie zwolniony przez Springa.
    //   Dlaczego? Kontener nie trzyma referencji do prototype, więc nie wie, których obiektów jeszcze używasz.
    //   Zwalnianie to obowiązek kodu, który pobrał obiekt (np. try-with-resources dla AutoCloseable).
    //
    // Test: Ioc07LifecycleTest#prototypeIsNotDestroyed

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Kolejność: konstruktor → wstrzyknięcie (setter/pole) → @PostConstruct → praca → @PreDestroy (przy close()).
     *   • W konstruktorze tylko przypisuj pola; ładowanie danych i połączenia rób w @PostConstruct.
     *   • Zależność z pola lub settera w konstruktorze to jeszcze null (NullPointerException).
     *   • Wyjątek w @PostConstruct = kontekst nie startuje (BeanCreationException).
     *   • Bez context.close() nie ma @PreDestroy (Spring Boot zamyka kontekst sam przy zatrzymaniu aplikacji).
     *   • Kolejność mechanizmów: @PostConstruct → afterPropertiesSet → initMethod; @PreDestroy → destroy → destroyMethod.
     *   • Beany @Bean: publiczne close() (a gdy go brak — shutdown()) jest wywoływane automatycznie;
     *     destroyMethod="" to wyłącza. Każdy bean implementujący AutoCloseable dostaje close().
     *   • Prototype: @PostConstruct przy każdym pobraniu, @PreDestroy nigdy.
     *   • Adnotacje to jakarta.annotation.PostConstruct i PreDestroy.
     *
     * PYTANIA KONTROLNE:
     *   1. W jakiej kolejności Spring tworzy bean: konstruktor, wstrzyknięcie przez setter, @PostConstruct?
     *   2. Co się stanie? BrokenProductCache woła catalog.names() w konstruktorze, a catalog jest polem z @Autowired.
     *   3. Co się stanie? Test tworzy kontekst z WarehouseConnection i kończy się bez context.close() — czy dziennik
     *      zawiera "@PreDestroy"?
     *   4. Co się stanie? Metoda @PostConstruct rzuca IllegalStateException.
     *   5. Co się stanie? Bean prototype ma metodę @PreDestroy; pobrałeś go dwa razy i zamknąłeś kontekst.
     *   6. ZNAJDŹ BŁĄD: {@code @Bean(destroyMethod = "") CloseResource closeResource(...)} — autor oczekuje, że
     *      przy zamknięciu kontekstu zostanie wywołane close(). Dlaczego nie zostanie?
     *   7. W jakiej kolejności działają @PostConstruct, afterPropertiesSet i initMethod?
     *   8. Dlaczego ładowanie danych lepiej wykonać w @PostConstruct niż w konstruktorze?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc07LifecycleExercisesTest
    // =================================================================================================
    //
    // ĆWICZENIE 1 (łatwe): klasa Ioc07ExerciseCache — uzupełnij metodę load() z @PostConstruct tak, żeby po starcie
    //   kontekstu mapa miała 3 produkty. Szkielet leży w osobnym pliku: Ioc07ExerciseCache.java.
    //
    // ĆWICZENIE 2 (średnie): klasa Ioc07ExerciseSessionCache — uzupełnij metodę clear() z @PreDestroy: wyczyść mapę
    //   i ustaw closed = true. Test zamyka kontekst i sprawdza efekt. Plik: Ioc07ExerciseSessionCache.java.

    /**
     * ĆWICZENIE 3 (trudniejsze): utwórz AnnotationConfigApplicationContext z klasami EventLog, ProductCatalog
     * i WarehouseConnection, zamknij go (try-with-resources) i zwróć KOPIĘ listy zdarzeń z dziennika. Oczekiwany wynik:
     * konstruktor, setter, @PostConstruct, @PreDestroy. Podpowiedź: zadeklaruj {@code EventLog log;} przed blokiem
     * try, przypisz go wewnątrz ({@code context.getBean(EventLog.class)}), a {@code log.events()} odczytaj PO bloku —
     * wtedy kontekst jest już zamknięty. Metoda events() sama zwraca kopię.
     */
    static List<String> exercise3() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Najpierw konstruktor, potem wstrzyknięcie przez setter, potem @PostConstruct (dopiero po zakończeniu
     *      wstrzykiwania). Sprawdzone testem Ioc07LifecycleTest#orderOfEvents.
     *   2. Kontekst się nie uruchomi: BeanCreationException, a jej przyczyną jest NullPointerException (pole catalog
     *      jest jeszcze null). Sprawdzone testem Ioc07LifecycleTest#brokenCacheFailsInConstructor.
     *   3. Nie. Dziennik ma tylko "konstruktor", "setter", "@PostConstruct" — sprzątanie jest częścią close().
     *      Sprawdzone testem Ioc07LifecycleTest#orderOfEvents (odczyt dziennika przed close()).
     *   4. Bean nie powstaje, a start kontekstu kończy się BeanCreationException z przyczyną IllegalStateException.
     *      Sprawdzone testem Ioc07LifecycleTest#failingPostConstructStopsStartup.
     *   5. @PostConstruct wykona się dwa razy (raz na pobranie), a @PreDestroy ani razu. Sprawdzone testem
     *      Ioc07LifecycleTest#prototypeIsNotDestroyed.
     *   6. destroyMethod = "" wyłącza wnioskowanie metody zamykającej, więc close() nie jest wywoływane. Bez tego
     *      parametru close() zostałoby wywołane. Sprawdzone testem Ioc07LifecycleTest#inferredDestroyMethod.
     *   7. @PostConstruct, potem afterPropertiesSet, potem initMethod. Przy zamknięciu: @PreDestroy, destroy,
     *      destroyMethod. Sprawdzone testem Ioc07LifecycleTest#allCallbacksOrder.
     *   8. W konstruktorze zależności z pól i setterów są jeszcze null, a w @PostConstruct wszystkie są już wstrzyknięte
     *      (sekcja 2, test constructorSeesNoSetterDependencies). Wyjątek w @PostConstruct dodatkowo przerywa start
     *      od razu i czytelnie.
     */
    // </editor-fold>
}
