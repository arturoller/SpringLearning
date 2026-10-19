package s01_core_ioc.ioc01_container;

import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * <pre>
 * TEMAT: Kontener IoC — kto tworzy obiekty i łączy je ze sobą
 *        (IoC, Inversion of Control = odwrócenie sterowania; container = kontener; bean = ziarno, obiekt zarządzany
 *         przez Springa; application context = kontekst aplikacji, czyli kontener z beanami)
 *
 * W SKRÓCIE:
 *   W zwykłej Javie klasa sama tworzy to, czego potrzebuje (new PriceList()). W Springu jest odwrotnie: klasa tylko
 *   MÓWI, czego potrzebuje (parametr konstruktora), a obiekty tworzy i łączy KONTENER. To jest odwrócenie sterowania:
 *   sterowanie tworzeniem obiektów przechodzi z Twojego kodu do frameworka. Kontener w Springu to ApplicationContext —
 *   trzyma wszystkie beany i podaje je tam, gdzie są potrzebne (wstrzykiwanie zależności, dependency injection).
 *
 * ANALOGIA: restauracja z kuchnią centralną.
 *   PRZED: każdy kelner sam kupuje składniki i gotuje (new w każdej klasie) — chaos, nie da się podmienić dostawcy.
 *   PO: kuchnia (kontener) przygotowuje wszystko według listy, a kelner tylko mówi „potrzebuję zupy” (parametr
 *   konstruktora) i dostaje gotowe danie. Zmiana dostawcy to zmiana w kuchni, a nie u każdego kelnera.
 *
 * JAK TO DZIAŁA:
 *   new AnnotationConfigApplicationContext(PriceList.class, OrderCalculator.class)
 *     1. rejestracja: kontener zapisuje „przepisy” na beany (bean definitions — definicje beanów)
 *     2. tworzenie: PriceList nie ma zależności → new PriceList()
 *     3. OrderCalculator potrzebuje PriceList → kontener podaje gotowy bean: new OrderCalculator(priceList)
 *     4. context.getBean(OrderCalculator.class) → ten sam, gotowy obiekt przy każdym wywołaniu
 *   Spring Boot robi to samo, tylko listę klas znajduje sam (skanowanie pakietu) i dokłada beany z autokonfiguracji.
 *
 * SŁÓWKA:
 *   dependency = zależność (obiekt, którego klasa potrzebuje); dependency injection = wstrzykiwanie zależności;
 *   bean definition = definicja beana („przepis”); get bean = pobierz bean; coupling = powiązanie (tight — ścisłe,
 *   loose — luźne); annotation config = konfiguracja przez adnotacje; no such bean = nie ma takiego beana.
 *
 * ZOBACZ TEŻ: t34_toward_spring/Spring01IocContainer (własny mini-kontener — to samo ręcznie),
 *             t22_design_patterns/Patterns08DependencyInjection (wzorzec DI w czystej Javie),
 *             s00_start/Start03FirstApplication (pierwsza aplikacja), s01_core_ioc/Ioc02ConstructorInjection.
 * </pre>
 */
public final class Ioc01Container {

    private Ioc01Container() {
    }

    // =================================================================================================
    // 1. PRZED: klasa sama tworzy zależność (ManualOrderCalculator)
    // =================================================================================================
    //
    // ManualOrderCalculator ma pole: private final PriceList priceList = new PriceList();
    // Działa, ale klasa jest PRZYKLEJONA do konkretnego cennika. Chcesz w teście cennik z innymi cenami? Musisz
    // zmienić kod kalkulatora. Chcesz cennik promocyjny w weekend? Znowu zmiana kodu.
    //
    // PUŁAPKA: „new w środku klasy” wygląda niewinnie, ale ukrywa zależność. Dlaczego to problem? Patrząc na
    //   konstruktor, nie widać, czego klasa potrzebuje; a w teście nie da się podać zamiennika.

    // =================================================================================================
    // 2. PO: zależność przez konstruktor (OrderCalculator) — bez Springa
    // =================================================================================================
    //
    // OrderCalculator(PriceList priceList) — klasa MÓWI, czego potrzebuje. Kto ją tworzy, ten podaje cennik.
    // W teście: new OrderCalculator(new PriceList()). To już jest wstrzykiwanie zależności — ręczne. Spring tylko
    // zautomatyzuje to podawanie.
    //
    // DOBRA PRAKTYKA: zależności przez konstruktor. Dlaczego? Widać je od razu, pola mogą być final, a obiektu nie
    //   da się utworzyć „w połowie” (bez wymaganej zależności).

    // =================================================================================================
    // 3. KONTENER SPRINGA bez Boota — AnnotationConfigApplicationContext
    // =================================================================================================
    //
    // try (var context = new AnnotationConfigApplicationContext(PriceList.class, OrderCalculator.class)) {
    //     OrderCalculator calc = context.getBean(OrderCalculator.class);   ← kontener utworzył i połączył oba obiekty
    // }
    // To „goły” Spring: tylko klasy, które podasz. Kontekst trzeba zamknąć (close) — try-with-resources robi to sam
    // (t10_exceptions/Exceptions04TryWithResources).
    //
    // Klasa podana tu WPROST staje się beanem nawet BEZ adnotacji @Component — to jawna rejestracja (register =
    // zarejestruj), a nie skanowanie. Np. new AnnotationConfigApplicationContext(ManualOrderCalculator.class) zadziała.
    // Tak właśnie testy kursu dodają do małych kontekstów klasy-przykłady bez adnotacji.

    // =================================================================================================
    // 4. TO SAMO ze Spring Bootem — Ioc01Application
    // =================================================================================================
    //
    // @SpringBootApplication sam znajduje klasy z @Component/@Service w pakiecie (skanowanie) i dodaje dziesiątki
    // beanów z autokonfiguracji (logowanie, konwersje, ustawienia). Dlatego kontekst Boota ma DUŻO więcej beanów niż
    // ręczny — test sekcji 4 to porównuje.
    //
    // PUŁAPKA: klasa bez adnotacji (ManualOrderCalculator) NIE jest beanem, nawet jeśli leży w skanowanym pakiecie.
    //   Dlaczego? Skanowanie szuka adnotacji @Component (i jej odmian), a nie wszystkich klas. getBean dla takiej klasy
    //   kończy się wyjątkiem NoSuchBeanDefinitionException (nie ma definicji takiego beana). Beanem stałaby się
    //   dopiero po jawnej rejestracji (sekcja 3).

    // =================================================================================================
    // 5. POBIERANIE BEANÓW — getBean po typie i po nazwie
    // =================================================================================================
    //
    //   context.getBean(OrderCalculator.class)              po typie — najczęściej
    //   context.getBean("orderCalculator", OrderCalculator.class)   po nazwie (domyślnie: nazwa klasy z małej litery)
    //   context.getBeanNamesForType(PriceList.class)        nazwy beanów danego typu (pusta tablica, gdy brak)
    //
    // DOBRA PRAKTYKA: w kodzie aplikacji NIE wołaj getBean — poproś o zależność w konstruktorze. Dlaczego? getBean
    //   to „sięganie do kontenera” (wzorzec service locator — lokalizator usług: obiekt sam wyszukuje swoje zależności
    //   w rejestrze), który znowu ukrywa zależności. getBean jest dobre
    //   w testach i w nauce — żeby zajrzeć do kontenera.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • IoC: obiekty tworzy i łączy kontener (ApplicationContext), nie Twój kod.
     *   • DI: zależności przychodzą z zewnątrz — najlepiej przez konstruktor (pola final).
     *   • Goły Spring: new AnnotationConfigApplicationContext(Klasa1.class, Klasa2.class) — tylko podane klasy; zamknij go.
     *   • Boot: @SpringBootApplication = skanowanie pakietu + autokonfiguracja (dużo więcej beanów).
     *   • Skanowanie robi beany z klas z @Component/@Service/...; zwykłą klasę pomija. Beanem jest też wynik metody
     *     @Bean i każda klasa zarejestrowana jawnie (new AnnotationConfigApplicationContext(Klasa.class)).
     *   • getBean(Typ.class), getBean("nazwa", Typ.class); brak beana → NoSuchBeanDefinitionException.
     *   • W aplikacji nie używaj getBean — proś o zależności w konstruktorze.
     *
     * PYTANIA KONTROLNE:
     *   1. Co dokładnie „odwraca się” w odwróceniu sterowania?
     *   2. Co się stanie? context.getBean(ManualOrderCalculator.class) w kontekście Ioc01Application.
     *   3. Co się stanie? Usuniesz @Component z PriceList i uruchomisz Ioc01Application.
     *   4. ZNAJDŹ BŁĄD: {@code new AnnotationConfigApplicationContext(OrderCalculator.class)} — dlaczego getBean(OrderCalculator.class)
     *      nie zadziała?
     *   5. Jaką nazwę ma bean klasy OrderCalculator, jeśli nie podasz jej sam?
     *   6. Dlaczego w kodzie aplikacji nie powinno się wołać context.getBean(...)?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc01ContainerExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): pobierz z kontekstu bean OrderCalculator i policz sumę zamówienia: 2 × Kawa + 1 × Herbata.
     * Wynik w groszach (2 × 2999 + 1599 = 7597). Podpowiedź: context.getBean(...) i Map.of(...).
     */
    static int exercise1(ApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): BEZ Spring Boota utwórz kontekst AnnotationConfigApplicationContext z klasami PriceList
     * i OrderCalculator, policz sumę zamówienia 3 × Czekolada i zamknij kontekst (try-with-resources). Wynik: 2697.
     */
    static int exercise2() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): zwróć posortowane nazwy beanów, których klasy leżą w pakiecie
     * s01_core_ioc.ioc01_container (czyli NASZYCH beanów, bez beanów Springa).
     * Podpowiedź: context.getBeanDefinitionNames(), context.getType(nazwa).getPackageName(), stream + filter + sorted.
     */
    static List<String> exercise3(ApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kto tworzy obiekty i łączy je ze sobą. Bez IoC robi to Twój kod (new w klasach), z IoC — kontener, a klasa
     *      tylko deklaruje, czego potrzebuje.
     *   2. NoSuchBeanDefinitionException — klasa nie ma adnotacji, więc nie jest beanem (test w Ioc01ContainerTest).
     *   3. Aplikacja się nie uruchomi: OrderCalculator potrzebuje beana PriceList, a takiego nie ma — „Parameter 0 of
     *      constructor in ...OrderCalculator required a bean of type ...PriceList that could not be found.”
     *      Sprawdzone uruchomieniem.
     *   4. Zarejestrowano tylko OrderCalculator, bez PriceList. Kontener nie ma czego wstrzyknąć do konstruktora, więc
     *      już przy tworzeniu kontekstu poleci wyjątek (UnsatisfiedDependencyException — niespełniona zależność).
     *      Sprawdzone testem w Ioc01ContainerTest.
     *   5. „orderCalculator” — nazwa klasy zaczynająca się małą literą (szczegóły w Ioc03Stereotypes).
     *   6. Bo to ukrywa zależności (nie widać ich w konstruktorze), utrudnia testy i wiąże kod z kontenerem Springa.
     */
    // </editor-fold>
}
