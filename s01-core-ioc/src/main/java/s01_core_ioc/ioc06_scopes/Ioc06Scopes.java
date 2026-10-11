package s01_core_ioc.ioc06_scopes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * <pre>
 * TEMAT: Zasięgi beanów — ile obiektów tworzy kontener i jak długo żyją
 *        (scope = zasięg; singleton = jedynak, jedna instancja na kontener; prototype = prototyp, nowa instancja
 *         przy każdym pobraniu; ObjectProvider = dostawca obiektów)
 *
 * W SKRÓCIE:
 *   Domyślnie Spring tworzy z każdego beana JEDEN obiekt (singleton) i podaje go wszystkim. To dobre dla serwisów
 *   bez stanu. Gdy potrzebujesz osobnego obiektu dla każdego użytkownika lub operacji (np. koszyk), nadajesz beanowi
 *   zasięg prototype: {@code @Scope("prototype")}. Wtedy każde pobranie z kontenera daje NOWY obiekt.
 *   Dwie klasyczne pułapki: (1) prototype wstrzyknięty do singletona jest wstrzyknięty RAZ i już się nie odnawia,
 *   (2) zmienny stan w singletonie jest wspólny dla wszystkich (także dla wszystkich wątków).
 *
 * ANALOGIA: sklep z kluczem do szatni.
 *   singleton = jedna wspólna szafka dla wszystkich klientów: każdy otwiera tę samą i widzi rzeczy poprzednika.
 *   prototype = recepcja wydaje nowy, pusty koszyk każdemu, kto o niego poprosi.
 *   Pułapka: kasjer dostał koszyk RAZ rano (wstrzyknięcie) i do wieczora wkłada w niego zakupy wszystkich klientów.
 *   Poprawka: kasjer za każdym razem idzie do recepcji po nowy koszyk (ObjectProvider).
 *
 * JAK TO DZIAŁA:
 *   getBean(Typ.class) dla singletona  → kontener ma gotowy obiekt, zwraca TEN SAM (utworzony przy starcie kontekstu)
 *   getBean(Typ.class) dla prototype   → kontener tworzy nowy obiekt przy KAŻDYM wywołaniu (nie trzyma go u siebie)
 *   wstrzyknięcie do konstruktora      → to też jest jedno pobranie z kontenera, w chwili tworzenia właściciela
 *   ObjectProvider.getObject()         → pobranie z kontenera w chwili wywołania, czyli nowy prototype za każdym razem
 *   Zasięgi webowe (request — jedno żądanie HTTP, session — jedna sesja użytkownika) istnieją, ale należą do aplikacji
 *   webowych i pojawią się w modułach z webem. Ten moduł nie ma weba, więc tylko o nich wspominamy.
 *
 * SŁÓWKA:
 *   scope = zasięg; singleton = jedynak; prototype = prototyp; shared state = stan współdzielony;
 *   stateless = bezstanowy; thread-safe = bezpieczny dla wątków; atomic = niepodzielny;
 *   provider = dostawca; lookup = wyszukanie; request = żądanie; session = sesja.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc02ConstructorInjection (wstrzykiwanie przez konstruktor),
 *             s01_core_ioc/Ioc04BeanMethods (metody {@code @Bean}),
 *             s01_core_ioc/Ioc07Lifecycle (cykl życia — w tym prototype nie jest niszczony przez Springa),
 *             t21_concurrency/Concurrency02RaceConditions (wyścig wątków: dlaczego {@code count++} gubi zliczenia),
 *             t22_design_patterns/Patterns04Singleton (wzorzec jedynaka w czystej Javie).
 * </pre>
 */
public final class Ioc06Scopes {

    private Ioc06Scopes() {
    }

    // =================================================================================================
    // 1. SINGLETON — domyślny zasięg
    // =================================================================================================
    //
    // @Component class VisitCounter { ... }     ← bez @Scope = singleton
    // context.getBean(VisitCounter.class) == context.getBean(VisitCounter.class)   → ten sam obiekt
    //
    // "Singleton" w Springu znaczy: jedna instancja NA KONTENER (nie na cały program, jak we wzorcu Singleton
    // z Javy). Dwa konteksty = dwa osobne obiekty. Kontener tworzy singletony przy starcie, więc błąd w konstruktorze
    // wychodzi od razu, a nie dopiero przy pierwszym użyciu.
    //
    // DOBRA PRAKTYKA: zostaw domyślny singleton dla serwisów, repozytoriów i konfiguracji. Dlaczego? Są bezstanowe,
    //   więc jedna instancja wystarczy, kosztuje najmniej pamięci i nie trzeba nic tworzyć przy każdym użyciu.
    //
    // Test: Ioc06ScopesTest#singletonIsDefault

    // =================================================================================================
    // 2. PROTOTYPE — nowy obiekt przy każdym pobraniu
    // =================================================================================================
    //
    // @Component
    // @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)      ← to samo co @Scope("prototype")
    // class ShoppingCart { ... }
    //
    // context.getBean(ShoppingCart.class) != context.getBean(ShoppingCart.class)   → dwa różne koszyki
    //
    // Pod spodem: kontener tworzy prototype dopiero na żądanie i NIE trzyma go u siebie. Po wydaniu obiektu
    // kontener o nim "zapomina" — za sprzątanie odpowiada Twój kod (więcej: Ioc07Lifecycle).
    // Dla metody @Bean zasięg ustawiasz adnotacją na METODZIE (adnotacja na klasie zwracanego typu nie działa,
    // bo wtedy kontener nie czyta klasy, tylko metodę) — to jest ćwiczenie 3.
    //
    // Test: Ioc06ScopesTest#prototypeCreatesNewObjects, Ioc06ScopesTest#beanMethodDefaultsToSingleton
    //
    // PUŁAPKA: zasięg ustawiony tylko na klasie nie obejmuje beana zdefiniowanego metodą @Bean tej klasy.
    //   Dlaczego? Dla metody @Bean obowiązuje to, co napisano przy metodzie; bez @Scope na metodzie dostajesz singleton
    //   i wszyscy dzielą jeden koszyk — bez żadnego błędu i bez ostrzeżenia.

    // =================================================================================================
    // 3. PUŁAPKA: prototype wstrzyknięty do singletona
    // =================================================================================================
    //
    // PRZED (BrokenCartService):
    //   @Service class BrokenCartService { BrokenCartService(ShoppingCart cart) { ... } }
    //   Serwis jest singletonem, więc konstruktor wywołuje się RAZ → koszyk wstrzyknięty RAZ → ten sam koszyk
    //   obsługuje wszystkich klientów. Pierwszy klient dodaje Kawę, drugi widzi w swoim koszyku Kawę pierwszego.
    //
    // PO (CartService):
    //   @Service class CartService { CartService(ObjectProvider<ShoppingCart> cartProvider) { ... } }
    //   cartProvider.getObject() przy każdej operacji → za każdym razem nowy koszyk.
    //
    // PUŁAPKA: prototype w polu lub konstruktorze singletona nie odnawia się.
    //   Dlaczego? Wstrzykiwanie dzieje się raz, przy tworzeniu singletona. Adnotacja @Scope("prototype") mówi tylko,
    //   co kontener robi PRZY KAŻDYM pobraniu — a pobranie nastąpiło tylko jeden raz.
    //
    // DOBRA PRAKTYKA: gdy singleton potrzebuje nowego obiektu za każdym razem, wstrzyknij ObjectProvider<T>.
    //   Dlaczego? To jawne i proste: widać w konstruktorze, że obiekt jest pobierany wielokrotnie, a kontener
    //   dalej zarządza tworzeniem (zależności nowego obiektu są wstrzyknięte jak zawsze).
    //   (Istnieją też proxy zasięgu — scoped proxy — ale ObjectProvider jest prostszy do zrozumienia.)
    //
    // Test: Ioc06ScopesTest#prototypeInSingletonIsInjectedOnlyOnce

    // =================================================================================================
    // 4. PUŁAPKA: zmienny stan singletona jest wspólny
    // =================================================================================================
    //
    // VisitCounter (singleton) ma pole count. HomePageService zwiększa je, ReportService je czyta — obaj dostali
    // TEN SAM obiekt, więc raport widzi odwiedziny ze strony głównej. To bywa celowe, ale przy większym stanie
    // (np. "bieżący klient" w polu serwisu) kończy się pomieszaniem danych użytkowników.
    //
    // PUŁAPKA: pole zmieniane przez wiele żądań w singletonie to wspólny stan.
    //   Dlaczego? Jeden obiekt obsługuje wszystkich naraz. Dane jednego klienta wyciekają do drugiego,
    //   a przy wielu wątkach dochodzi jeszcze wyścig (race condition): {@code count++} to odczyt, dodanie i zapis,
    //   więc dwa wątki mogą odczytać tę samą wartość i jedno zwiększenie przepada
    //   (t21_concurrency/Concurrency02RaceConditions). Błąd pojawia się losowo, więc jest trudny do znalezienia.
    //
    // DOBRA PRAKTYKA: singletony bezstanowe (stan w parametrach metod, w bazie, w obiektach tworzonych na żądanie)
    //   albo stan bezpieczny dla wątków (SafeVisitCounter: AtomicInteger). Dlaczego? Bezstanowy obiekt można
    //   bezpiecznie współdzielić bez żadnej synchronizacji.
    //
    // Test: Ioc06ScopesTest#singletonStateIsShared, Ioc06ScopesTest#atomicCounterSurvivesManyThreads

    // =================================================================================================
    // 5. ZASIĘGI WEBOWE (tylko wzmianka)
    // =================================================================================================
    //
    // W aplikacji webowej dochodzą zasięgi: request (jeden obiekt na żądanie HTTP) i session (jeden na sesję
    // użytkownika). W tym module nie ma weba, więc ich nie uruchamiamy. Reguła z sekcji 3 obowiązuje tak samo:
    // bean o krótszym życiu (request) w singletonie wymaga ObjectProvider albo proxy zasięgu.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Domyślny zasięg to singleton: jeden obiekt na kontener, ten sam z każdego getBean i z każdego wstrzyknięcia.
     *   • {@code @Scope("prototype")}: nowy obiekt przy każdym pobraniu z kontenera (kontener go potem nie trzyma).
     *   • Wstrzyknięcie to jedno pobranie: prototype w singletonie powstaje RAZ i zostaje na zawsze.
     *   • Nowy obiekt za każdym razem: ObjectProvider.getObject() w singletonie.
     *   • Dla metody @Bean zasięg ustawia się na metodzie; bez niego bean jest singletonem.
     *   • Singleton ma być bezstanowy albo mieć stan bezpieczny dla wątków (AtomicInteger, kolekcje współbieżne).
     *   • Zasięgi request i session należą do webu — pojawią się w modułach webowych.
     *
     * PYTANIA KONTROLNE:
     *   1. Co znaczy "singleton" w Springu i czym różni się od wzorca Singleton z Javy?
     *   2. Co się stanie? BrokenCartService.addAndCount("Kawa"), a potem addAndCount("Herbata") — jakie liczby zwrócą
     *      wywołania, choć koszyk jest prototype?
     *   3. Co się stanie? To samo dla CartService (z ObjectProvider).
     *   4. ZNAJDŹ BŁĄD: {@code @Service class Basket { Basket(ShoppingCart cart) {...} }} — autor chce osobnego koszyka
     *      dla każdego klienta. Dlaczego to nie działa i jak to naprawić?
     *   5. Co się stanie? Metoda {@code @Bean ShoppingCart cart()} bez {@code @Scope} — dwa razy getBean(ShoppingCart.class).
     *   6. Dlaczego zmienne pole w singletonie jest groźne, nawet jeśli aplikacja ma jednego użytkownika w testach?
     *   7. Jak zrobić licznik w singletonie bezpieczny dla wątków?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc06ScopesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): pobierz z kontekstu trzy razy bean ShoppingCart i zwróć, ile jest wśród nich RÓŻNYCH
     * obiektów (zgodnie z tożsamością, czyli ==, a nie equals). Podpowiedź:
     * {@code Collections.newSetFromMap(new IdentityHashMap<>())} — zbiór porównujący obiekty przez ==.
     */
    static int exercise1(ApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): dla każdego zamówienia (lista nazw produktów) pobierz z dostawcy NOWY koszyk, dodaj do
     * niego produkty i zapisz liczbę produktów w koszyku. Zwróć listę rozmiarów w kolejności zamówień.
     * Podpowiedź: {@code cartProvider.getObject()} wewnątrz pętli (NIE przed nią — wtedy koszyk byłby wspólny).
     */
    static List<Integer> exercise2(ObjectProvider<ShoppingCart> cartProvider, List<List<String>> orders) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): otwórz klasę Ioc06Exercise3Config. Metoda {@code cart()} jest gotowa, ale bean jest
     * singletonem. Dodaj nad metodą jedną adnotację, tak żeby dwa wywołania getBean(ShoppingCart.class) dały różne
     * obiekty. Podpowiedź: {@code @Scope("prototype")} i import {@code org.springframework.context.annotation.Scope}.
     * Ta klasa nie ma metody do uzupełnienia — sprawdza ją test w Ioc06ScopesExercisesTest.
     */

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Jedna instancja beana na kontener (ApplicationContext): dwa konteksty mają dwa osobne obiekty. We wzorcu
     *      Singleton z Javy jest jedna instancja na cały program (na classloader), a klasa pilnuje tego sama.
     *   2. Zwrócą 1 i 2. Obie operacje dodają do tego samego koszyka wstrzykniętego raz do konstruktora.
     *      Sprawdzone testem Ioc06ScopesTest#prototypeInSingletonIsInjectedOnlyOnce.
     *   3. Zwrócą 1 i 1: za każdym razem powstaje nowy, pusty koszyk. Sprawdzone tym samym testem.
     *   4. Konstruktor wywołuje się raz (Basket jest singletonem), więc koszyk jest wstrzyknięty raz i wspólny dla
     *      wszystkich klientów. Naprawa: ObjectProvider<ShoppingCart> i getObject() przy każdej operacji.
     *   5. Oba wywołania zwrócą TEN SAM obiekt (singleton), mimo że klasa ShoppingCart ma @Scope("prototype") —
     *      zasięg z klasy nie obejmuje metody @Bean. Sprawdzone testem Ioc06ScopesTest#beanMethodDefaultsToSingleton.
     *   6. Bo singleton jest jeden i obsługuje wszystkich; testy z jednym użytkownikiem nie pokażą pomieszania
     *      danych ani wyścigu wątków. Błąd wychodzi dopiero na produkcji, losowo. Wspólny stan widać też w testach:
     *      licznik rośnie między testami korzystającymi z tego samego kontekstu (Ioc06ScopesTest#singletonStateIsShared).
     *   7. Użyć AtomicInteger (incrementAndGet) zamiast int z count++, albo zrobić serwis bezstanowym.
     *      Sprawdzone testem Ioc06ScopesTest#atomicCounterSurvivesManyThreads (4 wątki x 1000 zwiększeń = 4000).
     */
    // </editor-fold>
}
