package s01_core_ioc.ioc02_constructor_injection;

/**
 * <pre>
 * TEMAT: Wstrzykiwanie zależności — konstruktor, pole, setter
 *        (constructor injection = wstrzykiwanie przez konstruktor; field injection = przez pole;
 *         setter injection = przez settera, czyli metodę ustawiającą; circular dependency = zależność cykliczna)
 *
 * W SKRÓCIE:
 *   Spring potrafi podać zależność na trzy sposoby: przez konstruktor, przez pole z @Autowired albo przez settera.
 *   Zalecany jest KONSTRUKTOR: zależności widać w podpisie klasy, pola mogą być final, a w teście wystarczy zwykłe
 *   new — bez Springa. Pole z @Autowired jest wygodne w pisaniu, ale ukrywa zależność i w teście bez Springa
 *   zostaje null. Setter jest dla zależności OPCJONALNYCH. Cykl A potrzebuje B, a B potrzebuje A, przy konstruktorach
 *   jest niemożliwy do utworzenia — i dobrze, bo to sygnał błędu w projekcie.
 *
 * ANALOGIA: pracownik i jego narzędzia.
 *   Konstruktor = narzędzia dostajesz PRZED pierwszym dniem pracy, w umowie jest lista: bez nich nie zaczynasz.
 *   Pole = narzędzia ktoś dokłada Ci do szuflady po cichu; w umowie o nich nic nie ma, a jak pójdziesz do innej
 *   firmy (test z new), szuflada jest pusta.
 *   Setter = dodatkowe wyposażenie, które możesz dostać później albo wcale (np. drugi monitor).
 *   Cykl = dwóch szefów, z których każdy zaczyna pracę dopiero, gdy drugi skończy.
 *
 * JAK TO DZIAŁA:
 *   1. Konstruktor: Spring widzi JEDEN konstruktor → sprawdza jego parametry → znajduje beany tych typów →
 *      woła new CustomerService(repository). Adnotacja @Autowired niepotrzebna.
 *   2. Pole: Spring woła pusty konstruktor, a potem refleksją (reflection) wpisuje wartość do prywatnego pola.
 *      Po zwykłym new pole jest null → NullPointerException.
 *   3. Setter: Spring woła konstruktor, a potem setter. @Autowired(required = false) → brak beana nie jest błędem.
 *   4. Dwa konstruktory: Spring wybiera ten z @Autowired; bez niego szuka konstruktora bez parametrów, a gdy go nie ma —
 *      zgłasza błąd.
 *   5. Cykl przez konstruktory: tworząc A, Spring potrzebuje B, a do B potrzebuje A, które jeszcze nie istnieje →
 *      BeanCurrentlyInCreationException (bean jest właśnie w trakcie tworzenia).
 *
 * SŁÓWKA:
 *   inject = wstrzyknąć; autowired = podłączony automatycznie; required = wymagany; optional = opcjonalny;
 *   constructor = konstruktor; setter = metoda ustawiająca; circular = cykliczny, kołowy; currently in creation =
 *   w trakcie tworzenia; test double = dubler testowy (zamiennik prawdziwej zależności); stub = prosty dubler
 *   zwracający gotowe odpowiedzi; unsatisfied dependency = niespełniona zależność.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc01Container (czym jest kontener), s01_core_ioc/Ioc03Stereotypes (skąd Spring wie, co jest beanem),
 *             t22_design_patterns/Patterns08DependencyInjection (DI w czystej Javie),
 *             t06_oop_basics/Oop02Constructors (konstruktory), t06_oop_basics/Oop06Immutability (pola final),
 *             t13_lambdas/Lambda02FunctionalInterfaces (interfejs funkcyjny — dubler jako lambda).
 * </pre>
 */
public final class Ioc02ConstructorInjection {

    private Ioc02ConstructorInjection() {
    }

    // =================================================================================================
    // 1. WSTRZYKIWANIE PRZEZ KONSTRUKTOR (CustomerService) — zalecane
    // =================================================================================================
    //
    // public CustomerService(CustomerRepository repository) { this.repository = repository; }
    //
    // • Jeden konstruktor → @Autowired niepotrzebne (od Springa 4.3).
    // • Pole jest final: po utworzeniu obiektu nikt go nie podmieni (niezmienność, Oop06Immutability).
    // • W teście: new CustomerService(id -> Optional.of(...)) — zwykła Java, bez Springa, błyskawicznie.
    //
    // DOBRA PRAKTYKA: zależności obowiązkowe wstrzykuj konstruktorem. Dlaczego? Nie da się utworzyć obiektu „w połowie”
    //   (bez zależności), zależności widać w podpisie, a test nie potrzebuje kontenera ani refleksji.

    // =================================================================================================
    // 2. WSTRZYKIWANIE PRZEZ POLE (FieldInjectedCustomerService) — antywzorzec
    // =================================================================================================
    //
    // @Autowired private CustomerRepository repository;
    //
    // W Springu działa. Ale w teście zwykłe new FieldInjectedCustomerService() daje obiekt z polem null, a pierwsze
    // wywołanie metody kończy się NullPointerException. Żeby go „naprawić”, trzeba kontekstu Springa albo refleksji.
    //
    // PUŁAPKA: field injection wygląda krócej (brak konstruktora), więc kusi. Dlaczego to zły wybór? Zależność jest
    //   ukryta (konstruktor jest pusty), pole nie może być final, klasy nie da się użyć bez Springa, a łatwo
    //   dopisać kolejne zależności, aż klasa robi za dużo — przy konstruktorze z 8 parametrami od razu to widać.

    // =================================================================================================
    // 3. WSTRZYKIWANIE PRZEZ SETTERA (SetterInjectedCustomerService) — dla zależności opcjonalnych
    // =================================================================================================
    //
    // @Autowired(required = false) public void setVipPolicy(VipPolicy vipPolicy) { ... }
    //
    // W naszym kontekście nie ma żadnego beana VipPolicy. Mimo to aplikacja startuje, a pole zostaje null
    // (klient nie dostaje gwiazdki). Setter pozwala też podmienić zależność po utworzeniu obiektu.
    //
    // DOBRA PRAKTYKA: setter tylko dla zależności NAPRAWDĘ opcjonalnych, z sensowną wartością domyślną w kodzie.
    //   Dlaczego? Zależność wymagana ustawiona setterem może zostać zapomniana — obiekt jest wtedy niepełny.

    // =================================================================================================
    // 4. DWA KONSTRUKTORY — który wybierze Spring?
    // =================================================================================================
    //
    // • Jeden konstruktor: ten.
    // • Kilka, jeden z @Autowired (TwoConstructorsService): ten z adnotacją.
    // • Kilka, żaden z @Autowired i brak konstruktora bez parametrów (AmbiguousConstructorsService): Spring nie zgaduje,
    //   rzuca wyjątek (szukał domyślnego konstruktora i go nie znalazł).
    // • Kilka, żaden z @Autowired, ale JEST konstruktor bez parametrów (NoArgConstructorTrapService): Spring po cichu
    //   używa właśnie jego — zależność z drugiego konstruktora nie zostaje wstrzyknięta.
    //
    // PUŁAPKA: dodajesz drugi konstruktor „na potrzeby testu” i aplikacja przestaje startować. Dlaczego? Wcześniej
    //   adnotacja była zbędna, bo konstruktor był jedyny; po dodaniu drugiego musisz wskazać, który ma użyć Spring.
    //   Gorzej, gdy dodany konstruktor jest BEZ parametrów: aplikacja startuje, ale Spring wybiera pusty konstruktor,
    //   więc pole zależności zostaje null, a NullPointerException wychodzi dopiero przy pierwszym użyciu.

    // =================================================================================================
    // 5. ZALEŻNOŚĆ CYKLICZNA (CycleA ↔ CycleB)
    // =================================================================================================
    //
    // CycleA(CycleB b) i CycleB(CycleA a): żeby zrobić A, trzeba B, a żeby zrobić B, trzeba A. Kontekst się nie
    // uruchomi: w przyczynie błędu jest BeanCurrentlyInCreationException.
    //
    // Cykl przez POLA (FieldCycleA ↔ FieldCycleB) da się „obejść” — najpierw powstają puste obiekty, potem uzupełniane
    // są pola — ale tylko po zezwoleniu na to. Spring Boot domyślnie ZABRANIA cykli:
    // spring.main.allow-circular-references=false (allow circular references = zezwalaj na referencje cykliczne).
    // Test sekcji 5 pokazuje obie wersje (w testach: withAllowCircularReferences(true/false)).
    // Uwaga: to ustawienie BOOTA. Goły Spring (new AnnotationConfigApplicationContext(...)) domyślnie na cykle przez
    // pola POZWALA — ten sam kod przejdzie w gołym kontekście, a w aplikacji Boota nie wystartuje.
    //
    // PUŁAPKA: włączenie allow-circular-references=true „żeby przestało się sypać”. Dlaczego to zła droga?
    //   Cykl zostaje, tylko go ukrywasz: obiekty są wtedy tworzone w dziwnej kolejności, w połowie gotowe.
    //
    // DOBRA PRAKTYKA: rozwiąż cykl projektem — wyciągnij wspólną logikę do trzeciej klasy C, od której zależą A i B.
    //   Dlaczego? Cykl zwykle znaczy, że dwie klasy mają zbyt splecione obowiązki.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Konstruktor (zalecany): pola final, zależności widoczne, test przez new. Jeden konstruktor → bez @Autowired.
     *   • Pole z @Autowired: ukryta zależność, nie da się final, new w teście → pole null → NullPointerException.
     *   • Setter: tylko zależności opcjonalne, @Autowired(required = false).
     *   • Kilka konstruktorów: oznacz jeden @Autowired. Bez tego: jest bezparametrowy → Spring po cichu bierze jego;
     *     nie ma → błąd przy tworzeniu kontekstu.
     *   • Cykl przez konstruktory → BeanCurrentlyInCreationException; Boot domyślnie zabrania też cykli przez pola
     *     (goły Spring na nie pozwala).
     *   • Dubler testowy: interfejs z jedną metodą zastąp lambdą — Mockito niepotrzebne.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego przy jednym konstruktorze nie trzeba pisać @Autowired?
     *   2. Co się stanie? {@code new FieldInjectedCustomerService().displayName(1)} w zwykłym teście.
     *   3. Co się stanie? Klasa ma dwa konstruktory z parametrami i żaden nie ma @Autowired — rejestrujesz ją w kontekście.
     *   4. ZNAJDŹ BŁĄD: {@code class A { A(B b) {} }} i {@code class B { B(A a) {} }} — dlaczego kontekst nie wystartuje?
     *   5. Co się stanie? Cykl przez POLA {@code @Autowired} w kontekście z zezwoleniem na cykle i w kontekście
     *      z zabronionymi cyklami (domyślnie jak w Bootcie).
     *   6. Dlaczego wstrzyknięcie przez settera nie nadaje się do zależności obowiązkowych?
     *   7. ZNAJDŹ BŁĄD: serwis z {@code @Autowired(required = false)} na settera VipPolicy wywołuje bez sprawdzenia
     *      {@code vipPolicy.isVip(id)}. Kiedy to się wysypie?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc02ConstructorInjectionExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): BEZ Springa złóż ręcznie CustomerService z InMemoryCustomerRepository i zwróć
     * displayName(id). Dla id = 2 wynik to "Jan Nowak".
     * Podpowiedź: new CustomerService(new InMemoryCustomerRepository()).
     */
    static String exercise1(long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zbuduj dubler (stub) CustomerRepository jako LAMBDĘ, która dla dowolnego id zwraca
     * klienta {@code new Customer(id, "Test", "Testowy")}, wstaw go do CustomerService i zwróć displayName(42).
     * Wynik: "Test Testowy". Podpowiedź: CustomerRepository ma jedną metodę, więc lambda
     * {@code id -> Optional.of(...)} wystarczy.
     */
    static String exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): BEZ Spring Boota utwórz AnnotationConfigApplicationContext z klasami
     * InMemoryCustomerRepository i CustomerService, pobierz CustomerService i zwróć displayName(id).
     * Zamknij kontekst (try-with-resources). Dla id = 3 wynik to "Maria Wiśniewska".
     */
    static String exercise3(long id) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo Spring nie ma wyboru: jedyny konstruktor jest tym, którego użyje (od Springa 4.3). Adnotacja jest
     *      potrzebna dopiero przy kilku konstruktorach.
     *   2. NullPointerException — new nie uruchamia wstrzykiwania, więc pole repository jest null.
     *      Sprawdzone testem w Ioc02ConstructorInjectionTest.
     *   3. Kontekst się nie uruchomi — Spring szuka konstruktora bez parametrów i go nie znajduje, więc zgłasza błąd
     *      tworzenia beana (przyczyną jest NoSuchMethodException, brak domyślnego konstruktora).
     *      Sprawdzone testem w Ioc02ConstructorInjectionTest.
     *   4. Żeby utworzyć A, kontener potrzebuje gotowego B, a do B gotowego A. Kontekst kończy się błędem, w którego
     *      przyczynie jest BeanCurrentlyInCreationException. Sprawdzone testem.
     *   5. Z zezwoleniem na cykle (withAllowCircularReferences(true)) kontekst startuje: Spring tworzy puste obiekty
     *      i uzupełnia pola. Przy zabronionych cyklach (domyślnie, tak jak w Bootcie) kontekst się nie uruchamia
     *      (przyczyna: BeanCurrentlyInCreationException). Goły AnnotationConfigApplicationContext domyślnie cykle
     *      przez pola dopuszcza. Sprawdzone testem.
     *   6. Bo kontener może pominąć setter albo ktoś może zapomnieć go wywołać w teście — obiekt zostaje „w połowie
     *      gotowy”, a błąd wychodzi dopiero w czasie działania.
     *   7. Gdy beana VipPolicy nie ma, pole zostaje null, więc takie wywołanie rzuci NullPointerException. Dlatego
     *      SetterInjectedCustomerService sprawdza {@code vipPolicy != null} (brak beana w kontekście lekcji i poprawne
     *      działanie label bez niego: sprawdzone testem).
     */
    // </editor-fold>
}
