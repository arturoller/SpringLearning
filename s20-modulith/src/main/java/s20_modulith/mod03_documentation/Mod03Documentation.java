package s20_modulith.mod03_documentation;

import org.springframework.modulith.core.ApplicationModules;

import java.util.List;
import java.util.Map;

/**
 * <pre>
 * TEMAT: Dokumentacja architektury z kodu (Documenter), nazwane interfejsy (@NamedInterface) i wybór: monolit
 *        modułowy czy mikroserwisy
 *        (documenter = generator dokumentacji; diagram = rysunek; canvas = kanwa, tu: tabela opisująca moduł;
 *         named interface = nazwany interfejs; observability = obserwowalność; microservice = mikroserwis)
 *
 * W SKRÓCIE:
 *   Rysunki architektury w wiki starzeją się w dniu, w którym ktoś zmieni kod. Spring Modulith rysuje je Z KODU:
 *   {@code new Documenter(ApplicationModules.of(App.class)).writeDocumentation()} w teście tworzy w
 *   {@code target/spring-modulith-docs} diagramy PlantUML (styl C4 albo UML) — całości i każdego modułu — oraz
 *   kanwy modułów (tabele AsciiDoc: pakiet, beany, zależności, zdarzenia). Każde budowanie = aktualna dokumentacja.
 *   {@code @NamedInterface} pozwala wystawić na zewnątrz wybrany PODPAKIET modułu (np. {@code orders.events})
 *   i pozwolić innemu modułowi tylko na niego: {@code allowedDependencies = "orders::events"}. Na koniec: kiedy
 *   monolit modułowy wystarcza, a kiedy moduł warto wydzielić do mikroserwisu.
 *
 * ANALOGIA: plan ewakuacyjny budynku generowany z projektu.
 *   Gdy plan rysuje ręcznie woźny, po pierwszym remoncie pokazuje drzwi, których już nie ma. Gdy plan drukuje
 *   się automatycznie z aktualnego projektu (kodu) przy każdej zmianie, zawsze jest prawdziwy. Nazwany interfejs to
 *   osobne okienko podawcze w ścianie działu — inni mogą korzystać tylko z niego, a nie z całego działu.
 *
 * JAK TO DZIAŁA:
 *   1. {@code ApplicationModules.of(Mod03Application.class)} — ta sama analiza klas co w Mod01Modules.
 *   2. Documenter zamienia moduły na model C4 (biblioteka Structurizr): moduł = komponent, zależność = strzałka
 *      z opisem: „uses” (bean innego modułu), „listens to” (słuchanie zdarzenia), „depends on” (inna zależność).
 *   3. Z modelu powstają pliki .puml (PlantUML — tekstowy opis diagramu) i .adoc (AsciiDoc — tekstowy format
 *      dokumentów); obrazki rysuje dopiero PlantUML (np. wtyczka w IntelliJ albo Asciidoctor w budowaniu).
 *   4. Folder wynikowy: {@code target/spring-modulith-docs}, gdy w katalogu roboczym jest pom.xml (Maven), albo
 *      {@code build/spring-modulith-docs} (Gradle). Domyślnie jest CZYSZCZONY przed zapisem.
 *   5. {@code @NamedInterface("events")} w package-info.java podpakietu dodaje modułowi drugi interfejs (obok
 *      nienazwanego — pakietu bazowego). {@code allowedDependencies} odwołuje się do niego składnią
 *      {@code moduł::interfejs}.
 *
 * SŁÓWKA:
 *   documenter = generator dokumentacji; diagram = rysunek; component = komponent; relationship = relacja (strzałka);
 *   canvas = kanwa (tabela opisu modułu); bean reference = użycie beana; events listened to = słuchane zdarzenia;
 *   published events = publikowane zdarzenia; named interface = nazwany interfejs; unnamed = nienazwany;
 *   reveal internals = pokaż wnętrze; output folder = folder wynikowy; wildcard = symbol wieloznaczny (*);
 *   observability = obserwowalność; microservice = mikroserwis; deployment = wdrożenie.
 *
 * ZOBACZ TEŻ: s20_modulith/Mod01Modules (moduły, API, verify), s20_modulith/Mod02ModuleEvents (zdarzenia
 *             i słuchacze), s15_actuator_logging/Obs01Actuator (Actuator), s15_actuator_logging/Obs05Observations
 *             (obserwacje i ślady), s19_deploy/Dep01ExecutableJar (jeden JAR = jedno wdrożenie monolitu),
 *             t30_build_modules/Build06QualityToolsJavadoc (Javadoc i narzędzia jakości).
 * </pre>
 */
public final class Mod03Documentation {

    private Mod03Documentation() {
    }

    // =================================================================================================
    // 1. Documenter — DIAGRAMY ARCHITEKTURY GENEROWANE W TEŚCIE
    // =================================================================================================
    //
    //   @Test
    //   void writeDocumentation() {
    //       new Documenter(ApplicationModules.of(Mod03Application.class)).writeDocumentation();
    //   }
    //
    // Pliki w s20-modulith/target/spring-modulith-docs (prawdziwy wynik):
    //   components.puml                       diagram WSZYSTKICH modułów
    //   module-orders.puml, module-…puml      diagram jednego modułu i modułów, od których zależy
    //   module-orders.adoc, module-…adoc      kanwa modułu (sekcja 2)
    //   all-docs.adoc                         dokument zbiorczy dołączający wszystko powyżej
    // oraz target/classes/META-INF/spring-modulith/application-modules.json — metadane modułów „do wykorzystania
    // w czasie działania aplikacji” (tak opisuje je Javadoc metody writeModuleMetadata; korzystają z nich dodatki
    // z sekcji 4, których w tym dziale nie ma).
    //
    // Fragment prawdziwego components.puml (styl C4, czyli „Context, Containers, Components, Code”):
    //
    //   !include <C4/C4_Component>
    //   Component(Mod03Application.Mod03Application.Produkty, "Produkty", $techn="Module", …)
    //   Component(Mod03Application.Mod03Application.Zamówienia, "Zamówienia", $techn="Module", …)
    //   Component(Mod03Application.Mod03Application.Magazyn, "Magazyn", $techn="Module", …)
    //   Rel(…Zamówienia, …Produkty, "uses", …)              ← OrderManagement wstrzykuje ProductCatalog
    //   Rel(…Magazyn, …Zamówienia, "listens to", …)          ← słuchacz OrderPlaced w module inventory
    //
    // Nazwy na diagramie to nazwy wyświetlane z @ApplicationModule(displayName = …). Diagram jednego modułu
    // (module-orders.puml) pokazuje Zamówienia i Produkty, ale nie Magazyn — tylko moduł i to, od czego zależy.
    // Inny styl: DiagramOptions.defaults().withStyle(DiagramOptions.DiagramStyle.UML) — klasyczne komponenty UML
    // zamiast C4 (sprawdza to test sekcji 1). Obrazek zobaczysz po otwarciu .puml z wtyczką PlantUML w IntelliJ.
    //
    // PUŁAPKA: Documenter z domyślnymi opcjami CZYŚCI folder wynikowy przed zapisem. Dlaczego to groźne? Gdy
    //   wskażesz mu folder z własnymi plikami (Options.defaults().withOutputFolder("docs")), znikną. Do folderu
    //   współdzielonego używaj Options.defaults().withoutClean() albo osobnego katalogu w target.
    //
    // PUŁAPKA: ścieżka wynikowa jest liczona od KATALOGU ROBOCZEGO testu: „target”, jeśli jest w nim pom.xml.
    //   Maven (./mvnw -pl s20-modulith test) uruchamia testy w katalogu modułu, więc pliki trafiają do
    //   s20-modulith/target/spring-modulith-docs. Dlaczego warto to wiedzieć? Gdy uruchomisz test z innym katalogiem
    //   roboczym (np. inaczej ustawiona konfiguracja w IntelliJ), pliki wylądują gdzie indziej.
    //
    // DOBRA PRAKTYKA: generuj dokumentację w zwykłym teście przy każdym budowaniu, a w CI publikuj all-docs.adoc
    //   jako HTML. Dlaczego? Dokumentacja jest wtedy zawsze zgodna z kodem — powstaje z niego, nie obok niego.

    // =================================================================================================
    // 2. KANWA MODUŁU (MODULE CANVAS)
    // =================================================================================================
    //
    // Prawdziwa kanwa module-inventory.adoc:
    //
    //   [%autowidth.stretch, cols="h,a"]
    //   |===
    //   |Base package
    //   |`s20_modulith.mod03_documentation.inventory`
    //   |Spring components
    //   |_Services_
    //
    //   * `s.m.i.InventoryManagement`
    //   |Events listened to
    //   |* `s.m.o.events.OrderPlaced` (async)
    //   |===
    //
    // Kanwa modułu orders ma wiersz „Bean references” (* `s.m.p.ProductCatalog` (in Produkty)). Czego NIE ma:
    //   • wnętrza modułu — InventoryEventHandler i OrderStore leżą w internal, kanwa pokazuje tylko API;
    //     CanvasOptions.defaults().revealInternals() dokłada je (OrderStore w grupie „_Others_”, InventoryEventHandler
    //     w grupie „_Event listeners_”) — test sekcji 2;
    //   • wiersza „Published events” — Spring Modulith 2.1 rozpoznaje publikowane zdarzenia tylko po typach
    //     jMolecules (adnotacja @DomainEvent albo interfejs DomainEvent); nasze zwykłe rekordy się nie liczą
    //     (sprawdzone: ApplicationModule.getPublishedEvents() zwraca pustą listę);
    //   • wiersza „Description” (opis modułu z Javadoc) — wymaga pliku z Javadoc, który przy kompilacji tworzy
    //     procesor adnotacji spring-modulith-apt; w budowaniu tego kursu ten plik nie powstaje.
    //
    // PUŁAPKA: szukasz na kanwie zdarzeń, które moduł publikuje, a wiersza nie ma. Dlaczego? Publikację
    //   zdarzenia (publishEvent(new OrderPlaced(...))) Modulith widzi tylko jako zwykłą zależność od typu. „Events
    //   listened to” działa, bo słuchacz ma adnotację (@ApplicationModuleListener / @EventListener) z typem w
    //   parametrze.

    // =================================================================================================
    // 3. @NamedInterface — WYSTAWIENIE PODPAKIETU
    // =================================================================================================
    //
    //   orders                    ← interfejs NIENAZWANY (pakiet bazowy): OrderManagement
    //   orders.events             ← interfejs NAZWANY „events”: OrderPlaced       (@NamedInterface("events"))
    //   orders.internal           ← środek: OrderStore
    //
    //   // orders/events/package-info.java
    //   @NamedInterface("events")
    //   package s20_modulith.mod03_documentation.orders.events;
    //
    //   // inventory/package-info.java — magazyn może używać TYLKO zdarzeń zamówień
    //   @ApplicationModule(displayName = "Magazyn", allowedDependencies = "orders::events")
    //
    // Opis modułu (prawdziwy wynik):
    //   > Named interfaces:
    //     + NamedInterface: name=<<UNNAMED>>, types=[ s.m.o.OrderManagement ]
    //     + NamedInterface: name=events, types=[ s.m.o.e.OrderPlaced ]
    //
    // Zła aplikacja (src/test/java, s20_modulith.mod03_broken.named_interface): magazyn z „orders::events” wstrzykuje
    // też OrderManagement. Prawdziwy komunikat:
    //   Module 'inventory' depends on module 'orders' via …inventory.InventoryEventHandler ->
    //   …orders.OrderManagement. Allowed targets: orders :: events.
    //
    // Po co? Moduł bywa duży; nazwane interfejsy dzielą jego API na części o różnym przeznaczeniu (zdarzenia,
    // SPI — punkty rozszerzeń, API dla administracji) i pozwalają innym modułom zależeć tylko od potrzebnej części.
    // @NamedInterface można też postawić na pojedynczej KLASIE (wtedy ta klasa należy do nazwanego interfejsu).
    //
    // PUŁAPKA: allowedDependencies = "orders" NIE obejmuje nazwanych interfejsów — tylko nienazwany (pakiet
    //   bazowy). Magazyn słuchający orders.events.OrderPlaced z samym "orders" dostaje (sprawdzone uruchomieniem):
    //   „Module 'inventory' depends on named interface(s) 'orders :: events' via … Allowed targets: orders.”
    //   Dlaczego tak? Nazwa modułu bez „::” oznacza jego domyślny interfejs. Wymień potrzebne: "orders::events",
    //   albo wszystkie naraz: "orders::*".

    // =================================================================================================
    // 4. OBSERWOWALNOŚĆ MODUŁÓW (TYLKO WZMIANKA)
    // =================================================================================================
    //
    // Starter spring-modulith-starter-insight (NIE ma go w tym dziale) dokłada Actuator i obserwowalność:
    //   • endpoint /actuator/modulith — struktura modułów działającej aplikacji (moduły, zależności, beany);
    //   • ślady (traces) z granicami modułów — wywołania API modułu dostają własne fragmenty śladu (spany), więc
    //     w narzędziu do śledzenia (np. Zipkin) widać, który moduł brał udział w żądaniu i ile to trwało.
    // Podstawy Actuatora i obserwacji: lekcje Obs01Actuator i Obs05Observations.

    // =================================================================================================
    // 5. MONOLIT MODUŁOWY CZY MIKROSERWISY?
    // =================================================================================================
    //
    //                          monolit modułowy                     mikroserwisy
    //   wdrożenie              jeden JAR, jedno wdrożenie            wiele usług, osobne wdrożenia
    //   komunikacja            wywołanie metody / zdarzenie w JVM    sieć (HTTP, kolejki) — opóźnienia, awarie
    //   transakcje             zwykła transakcja bazy                rozproszone: sagi, kompensacje
    //   spójność danych        łatwa (jedna baza, osobne tabele)     ostateczna (eventual consistency)
    //   granice modułów        test verify()                         sieć i osobne repozytoria
    //   koszt operacyjny       mały                                  duży (monitoring, CI, infrastruktura)
    //   skalowanie             cała aplikacja naraz                  każda usługa osobno
    //   zespoły                jedno wydanie dla wszystkich          niezależne wydania
    //
    // PYTANIE REKRUTACYJNE: od czego zacząć nowy system? Zwykle od monolitu modułowego: granice biznesowe dopiero
    //   się wyjaśniają, a przesunięcie granicy w monolicie to refaktoryzacja, nie migracja danych między usługami.
    //   Moduł wydzielamy do mikroserwisu, gdy jest konkretny powód: inne wymagania skalowania, osobny zespół
    //   z własnym rytmem wydań, inna technologia, izolacja awarii. Dobrze wydzielony moduł (API, zdarzenia, własne
    //   tabele, zero cykli) jest do tego gotowy — zdarzenia z rejestru można nawet wysyłać na zewnątrz
    //   (adnotacja @Externalized i moduły spring-modulith-events-kafka / -amqp / -jms).
    //
    // PUŁAPKA: „rozproszony monolit” — mikroserwisy, które wołają się nawzajem synchronicznie przy każdym żądaniu
    //   i muszą być wdrażane razem. Dlaczego to najgorsze z obu światów? Koszty sieci i operacji mikroserwisów,
    //   a sprzężenie monolitu. Granice najpierw ćwicz w monolicie modułowym, gdzie ich złamanie wykryje test.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • new Documenter(ApplicationModules.of(App.class)).writeDocumentation() — w teście; wynik:
     *     target/spring-modulith-docs (components.puml, module-X.puml, module-X.adoc, all-docs.adoc)
     *     + target/classes/META-INF/spring-modulith/application-modules.json.
     *   • Strzałki: „uses” (bean), „listens to” (zdarzenie), „depends on” (inne typy). Styl: C4 (domyślnie) albo UML.
     *   • Folder wynikowy jest czyszczony (Options.defaults()); withoutClean() / withOutputFolder(...) to zmieniają.
     *   • Kanwa: pakiet, beany API, Bean references, Events listened to; wnętrze — revealInternals();
     *     Published events — tylko typy jMolecules.
     *   • @NamedInterface("nazwa") na package-info podpakietu (albo na klasie) = dodatkowy interfejs modułu;
     *     allowedDependencies = "moduł::nazwa"; samo "moduł" = tylko interfejs nienazwany; "moduł::*" = wszystkie.
     *   • Obserwowalność: spring-modulith-starter-insight → /actuator/modulith i ślady z granicami modułów.
     *   • Zacznij od monolitu modułowego; wydzielaj moduł do mikroserwisu z konkretnego powodu.
     *
     * PYTANIA KONTROLNE:
     *   1. Jakie pliki tworzy writeDocumentation() i gdzie (przy uruchomieniu ./mvnw -pl s20-modulith test)?
     *   2. Co oznaczają opisy strzałek „uses” i „listens to” na diagramie components.puml naszej aplikacji?
     *   3. Co się stanie? Wywołujesz new Documenter(modules, Documenter.Options.defaults().withOutputFolder("src/main/
     *      resources")).writeDocumentation(), żeby trzymać diagramy przy kodzie.
     *   4. Dlaczego na kanwie modułu orders nie ma wiersza „Published events”, choć moduł publikuje OrderPlaced?
     *   5. ZNAJDŹ BŁĄD: inventory ma @ApplicationModule(allowedDependencies = "orders"), a jego słuchacz przyjmuje
     *      OrderPlaced z pakietu orders.events oznaczonego @NamedInterface("events"). Co zgłosi verify() i jak to
     *      naprawić?
     *   6. Co się stanie? Magazyn z allowedDependencies = "orders::events" wstrzykuje OrderManagement z pakietu
     *      bazowego modułu orders.
     *   7. Podaj dwa konkretne powody, dla których warto wydzielić moduł do mikroserwisu, i dwa koszty takiej decyzji.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Mod03DocumentationExercisesTest (pliki w target/mod03-exercises)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zapisz SAM diagram wszystkich modułów (components.puml) do podanego folderu.
     * Podpowiedź: {@code new Documenter(modules, Documenter.Options.defaults().withOutputFolder(folder))}
     * i metoda {@code writeModulesAsPlantUml()}.
     */
    static void exercise1(ApplicationModules modules, String outputFolder) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć posortowane nazwy NAZWANYCH interfejsów modułu (bez interfejsu nienazwanego).
     * Dla "orders": [events], dla "products": []. Brak modułu → {@code IllegalArgumentException} „Nie ma modułu:
     * nazwa”. Podpowiedź: {@code getNamedInterfaces().stream()}, {@code isNamed()}, {@code getName()}.
     */
    static List<String> exercise2(ApplicationModules modules, String moduleName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): zapisz do podanego folderu kanwy wszystkich modułów RAZEM z wnętrzem (klasy
     * z pakietów internal). Podpowiedź: {@code writeModuleCanvases(Documenter.CanvasOptions.defaults()
     * .revealInternals())}.
     */
    static void exercise3(ApplicationModules modules, String outputFolder) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): zbuduj tekstową „mapę architektury”: dla KAŻDEGO modułu (klucz — nazwa, mapa
     * posortowana po kluczach) posortowaną listę nazw modułów, od których bezpośrednio zależy. Dla Mod03Application:
     * {inventory=[orders], orders=[products], products=[]}. Podpowiedź: {@code TreeMap},
     * {@code getDirectDependencies(modules).uniqueModules()}.
     */
    static Map<String, List<String>> exercise4(ApplicationModules modules) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. W s20-modulith/target/spring-modulith-docs: components.puml, module-<moduł>.puml i module-<moduł>.adoc dla
     *      każdego modułu oraz all-docs.adoc; dodatkowo target/classes/META-INF/spring-modulith/application-modules.json.
     *      Katalog „target” wynika z pom.xml w katalogu roboczym testu (Maven uruchamia testy w katalogu modułu).
     *      Sprawdzone testem sekcji 1.
     *   2. „uses”: moduł Zamówienia używa beana z modułu Produkty (OrderManagement wstrzykuje ProductCatalog).
     *      „listens to”: moduł Magazyn słucha zdarzenia z modułu Zamówienia (OrderPlaced). Sprawdzone testem sekcji 1.
     *   3. Documenter z domyślnymi opcjami przed pierwszym zapisem CZYŚCI wskazany folder — skasuje
     *      application.properties, migracje i wszystko inne w src/main/resources. Trzymaj diagramy w osobnym
     *      folderze albo użyj withoutClean(). Czyszczenie (i jego brak z withoutClean()) sprawdza test sekcji 1 na
     *      folderze w target.
     *   4. Bo Modulith 2.1 rozpoznaje publikowane zdarzenia tylko po typach jMolecules (@DomainEvent / DomainEvent);
     *      publishEvent(new OrderPlaced(...)) to dla analizy zwykła zależność od typu. Sprawdzone testem sekcji 2.
     *   5. „Module 'inventory' depends on named interface(s) 'orders :: events' via … Allowed targets: orders.” —
     *      samo "orders" oznacza tylko interfejs nienazwany. Naprawa: allowedDependencies = "orders::events" (albo
     *      "orders::*"). Sprawdzone uruchomieniem.
     *   6. verify() rzuci Violations: „Module 'inventory' depends on module 'orders' via …InventoryEventHandler ->
     *      …OrderManagement. Allowed targets: orders :: events.” Sprawdzone testem sekcji 3.
     *   7. Powody: inne wymagania skalowania (np. katalog czytany 100× częściej niż zamówienia), osobny zespół z własnym
     *      rytmem wydań, izolacja awarii, inna technologia. Koszty: komunikacja przez sieć (opóźnienia, awarie,
     *      ponawianie), brak wspólnej transakcji (sagi, spójność ostateczna), więcej infrastruktury i monitoringu.
     */
    // </editor-fold>
}
