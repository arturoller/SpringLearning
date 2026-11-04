package s10_transactions.tx03_proxy_pitfalls;

/**
 * <pre>
 * TEMAT: Pułapki proxy — dlaczego mój {@code @Transactional} nie działa: wywołanie z tej samej klasy
 *        (self-invocation), metody private/final/static, klasa final, obiekt z {@code new}, {@code @PostConstruct},
 *        inny wątek; proxy CGLIB a proxy JDK
 *        (proxy = pośrednik; self-invocation = wywołanie samego siebie; CGLIB = biblioteka generująca podklasy
 *         w czasie działania; JDK dynamic proxy = dynamiczne proxy z JDK, oparte na interfejsie)
 *
 * W SKRÓCIE:
 *   {@code @Transactional} nie jest „magią w metodzie”. To proxy — obiekt-pośrednik, który Spring wstawia do kontenera
 *   ZAMIAST Twojego obiektu — otwiera i zamyka transakcję wokół wywołania. Wszystko, co omija proxy, omija transakcję:
 *   wywołanie {@code this.metoda()}, metoda, której podklasa nie może przesłonić (private, final, static), obiekt
 *   utworzony przez {@code new}, kod w {@code @PostConstruct} (proxy jeszcze nie ma) i praca w innym wątku (transakcja
 *   jest przypięta do wątku). Adnotacja wtedy NIC nie robi — po cichu, bez błędu i bez ostrzeżenia.
 *
 * ANALOGIA: recepcja w biurowcu.
 *   Goście wchodzą przez recepcję (proxy): recepcjonistka wpisuje ich do książki, daje przepustkę, a przy wyjściu ją
 *   odbiera (begin, commit). Pracownik, który z jednego pokoju przechodzi do drugiego WEWNĄTRZ budynku
 *   (this.metoda()), nie mija recepcji — nikt nic nie zapisuje. Ktoś, kto wejdzie oknem (obiekt z new), też nie.
 *   A do pokoju bez drzwi od strony korytarza (private/final) recepcja nikogo nie zaprowadzi.
 *
 * JAK TO DZIAŁA:
 *   1. Po utworzeniu beana (konstruktor, wstrzyknięcia, {@code @PostConstruct}) post-procesor auto-proxy widzi
 *      {@code @Transactional} i zamiast beana rejestruje proxy. Boot domyślnie robi proxy CGLIB — PODKLASĘ
 *      {@code Klasa$$SpringCGLIB$$0} ({@code spring.aop.proxy-target-class=true}; sprawdzone w autokonfiguracji
 *      i testem — także dla beana z interfejsem).
 *   2. Proxy przesłania (override) metody klasy: przesłonięta metoda woła TransactionInterceptor, a ten — PRAWDZIWY
 *      obiekt (target = cel), który proxy trzyma w środku.
 *   3. Wewnątrz prawdziwego obiektu {@code this} wskazuje na prawdziwy obiekt, nie na proxy. Dlatego wywołanie
 *      {@code this.innaMetoda()} nie przechodzi przez interceptor.
 *
 *     wywołujący ──► [proxy PriceService$$SpringCGLIB$$0] ──begin──► [PriceService (target)].metoda()
 *                         ▲                                                 │ this.innaMetoda()  ← bez proxy!
 *                         └────────────commit / rollback◄────────────────────┘
 *
 * SŁÓWKA:
 *   proxy = pośrednik; target = cel (prawdziwy obiekt za proxy); override = przesłonić (metodę w podklasie);
 *   self-invocation = wywołanie samego siebie; subclass = podklasa; interface = interfejs; lazy = leniwy;
 *   object provider = dostawca obiektu; expose proxy = udostępnij proxy; post construct = po utworzeniu;
 *   application ready event = zdarzenie „aplikacja gotowa”; thread = wątek; ThreadLocal = zmienna osobna dla wątku;
 *   price = cena; limit exceeded = przekroczony limit.
 *
 * ZOBACZ TEŻ: s10_transactions/Tx01Transactional (jak działa proxy transakcyjne),
 *             s10_transactions/Tx02Propagation (sekcja 5 — this.metoda() ignoruje REQUIRES_NEW),
 *             s05_validation/Val05ServiceValidation (ta sama pułapka self-invocation i new przy walidacji),
 *             t19_annotations_reflection/Annotations06DynamicProxy (proxy JDK zbudowane ręcznie),
 *             t21_concurrency/Concurrency05CompletableFuture (CompletableFuture),
 *             s01_core_ioc/Ioc07Lifecycle (cykl życia beana, {@code @PostConstruct}),
 *             s16_cache_scheduling_async (to samo proxy przy {@code @Async} i {@code @Cacheable}),
 *             s18_aop (proxy i aspekty od środka).
 * </pre>
 */
public final class Tx03ProxyPitfalls {

    private Tx03ProxyPitfalls() {
    }

    // =================================================================================================
    // 1. Proxy CGLIB (podklasa) a proxy JDK (interfejs) — co robi Boot (SeasonalDiscountPolicy, DiscountPolicy)
    // =================================================================================================
    //
    //   Rodzaj proxy     jak powstaje                              widać jako                   wymaga
    //   CGLIB            podklasa klasy beana (bytecode w locie)   klasa beana I jej interfejsy  klasy nie-final, metody nie-final
    //   JDK dynamic      {@code java.lang.reflect.Proxy} z interfejsami   TYLKO interfejsy                bean musi mieć interfejs
    //
    // Sprawdzone testami:
    //   • domyślnie (Boot, {@code spring.aop.proxy-target-class=true}): SeasonalDiscountPolicy implementuje DiscountPolicy,
    //     a i tak dostaje CGLIB — {@code s10_transactions.tx03_proxy_pitfalls.SeasonalDiscountPolicy$$SpringCGLIB$$0};
    //     {@code AopUtils.getTargetClass(bean)} → SeasonalDiscountPolicy.class (klasa ukryta za proxy);
    //   • z {@code spring.aop.proxy-target-class=false}: bean z interfejsem dostaje proxy JDK (klasa {@code jdk.proxy….$Proxy…},
    //     {@code AopUtils.isJdkDynamicProxy} = true), które NIE jest SeasonalDiscountPolicy. {@code getBean(SeasonalDiscountPolicy.class)}
    //     → {@code NoSuchBeanDefinitionException: No qualifying bean of type '…SeasonalDiscountPolicy' available} —
    //     wstrzyknięcie po klasie wywraca start aplikacji. Klasa bez interfejsu (PriceService) i tak dostaje CGLIB.
    //   Czysty Spring Framework (bez Boota) domyślnie wybiera JDK proxy, gdy bean ma interfejs — stąd w starszych
    //   tutorialach rada „zawsze pisz interfejs do serwisu”. W Boocie nie jest to potrzebne.
    //
    // DOBRA PRAKTYKA: zostaw domyślne {@code proxy-target-class=true} i wstrzykuj zależności po interfejsie tam,
    //   gdzie interfejs ma sens (kilka implementacji), a po klasie tam, gdzie go nie ma. Dlaczego? CGLIB działa w obu
    //   przypadkach; JDK proxy psuje wstrzykiwanie po klasie.

    // =================================================================================================
    // 2. Self-invocation: this.metodaTransakcyjna() — brak transakcji; rozwiązania (PriceService i spółka)
    // =================================================================================================
    //
    //   PRZED (PriceService.raiseAllSelfInvoked): publiczna metoda BEZ adnotacji woła
    //     {@code raiseAllInTransaction(20)} — metodę z {@code @Transactional} w tej samej klasie. Proxy przepuściło
    //     wywołanie raiseAllSelfInvoked (nic tam do roboty), a dalej {@code this} = prawdziwy obiekt. Podwyżka 20%:
    //     Klawiatura 100 → 120, Mysz 50 → 60, Monitor 800 → 960 > limit 900 → PriceLimitExceededException.
    //     Wynik w bazie (test): {@code Klawiatura=120.00, Mysz=60.00, Monitor=800.00} — połowa podwyżki zapisana.
    //     {@code stateSelfInvoked()} → {@code isActualTransactionActive()} = false.
    //   PYTANIE REKRUTACYJNE: „Dlaczego @Transactional nie działa, gdy wywołuję metodę z tej samej klasy?”
    //
    //   PO — sposoby, od najlepszego (wszystkie sprawdzone testem: wyjątek → ceny bez zmian):
    //   a) OSOBNY bean (PriceService.raiseAllViaSeparateBean → PriceUpdater.raiseAll) — wywołanie przez proxy
    //      innego beana. DOBRA PRAKTYKA: zwykle to też lepszy podział odpowiedzialności. Dlaczego? Nie trzeba
    //      pamiętać, które wywołanie idzie przez proxy — każde wywołanie innego beana idzie.
    //   b) Adnotacja na metodzie wywoływanej z ZEWNĄTRZ ({@code raiseAllInTransaction} wywołane prosto z testu):
    //      często wystarczy przenieść {@code @Transactional} „piętro wyżej”.
    //   c) Wstrzyknięcie samego siebie (SelfInjectingPriceService): {@code @Lazy SelfInjectingPriceService self}
    //      w konstruktorze (leniwy pośrednik — bez {@code @Lazy} start kończy się błędem cyklu zależności) albo
    //      {@code ObjectProvider<SelfInjectingPriceService>} i {@code getObject()} w chwili wywołania. Działa, ale to obejście.
    //   d) {@code AopContext.currentProxy()} — zwraca proxy bieżącego wywołania, ALE tylko po włączeniu
    //      {@code @EnableAspectJAutoProxy(exposeProxy = true)} (expose proxy = udostępnij proxy). Bez tego (domyślnie):
    //      {@code IllegalStateException: Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true'…}
    //      (test). Z włączonym (aplikacja testowa tx03_pitfalls/expose_proxy): działa (test). Wiąże kod z AOP Springa
    //      — ostateczność.
    //   e) AspectJ w trybie wplatania kodu ({@code @EnableTransactionManagement(mode = AdviceMode.ASPECTJ)}) — transakcja
    //      wpleciona w bytecode, bez proxy; wymaga dodatkowej konfiguracji budowania (tylko wzmianka).

    // =================================================================================================
    // 3. private / final / static / package-private; klasa final (VisibilityShowcase, tx03_pitfalls/final_class)
    // =================================================================================================
    //
    //   Metoda z {@code @Transactional}  transakcja?   dlaczego (sprawdzone testem)
    //   public                     TAK
    //   protected                  TAK           od Spring Framework 6.0 proxy klasowe (CGLIB) obsługuje też
    //   package-private            TAK           metody protected i pakietowe; w Spring 5 — tylko public!
    //   private                    NIE           podklasa nie widzi metod prywatnych — nie może ich przesłonić
    //   final                      NIE           podklasie nie wolno przesłonić — wykonuje się kod z klasy, NA PROXY
    //   static                     NIE           metoda klasy, nie obiektu — żadne proxy jej nie dotyczy
    //
    // PUŁAPKA: metoda {@code final} jest gorsza niż „brak transakcji”. Wykonuje się na obiekcie PROXY, który Spring tworzy
    //   bez wywołania konstruktora — jego pola są puste. Test: {@code finalMethodSeesDependency()} → {@code jdbc == null}
    //   (false), a ta sama metoda bez final → true. Dlaczego groźne? Użycie zależności w metodzie final kończy się
    //   NullPointerException, który wygląda na błąd wstrzykiwania. Uwaga w Kotlinie: klasy i metody są domyślnie
    //   final (stąd plugin {@code kotlin-spring}).
    //
    // Klasa {@code final} z {@code @Transactional} (aplikacja testowa tx03_pitfalls/final_class — osobny pakiet, bo
    //   zepsułaby start aplikacji lekcji) — aplikacja NIE STARTUJE (test):
    //   {@code BeanCreationException: … Could not generate CGLIB subclass of class …FinalPriceService: Common causes of}
    //   {@code this problem include using a final class or a non-visible class}, przyczyna: AopConfigException,
    //   a na dnie {@code IllegalArgumentException: Cannot subclass final class …FinalPriceService}.
    //   To akurat dobra wiadomość: błąd jest głośny, nie cichy. Rekordy (record) są final — nie robimy z nich serwisów.
    //
    // DOBRA PRAKTYKA: {@code @Transactional} tylko na metodach public, wywoływanych z zewnątrz; klasa i metody serwisu
    //   bez final. Dlaczego? To jedyny układ, który działa tak samo dla CGLIB i JDK proxy, w każdej wersji Springa.

    // =================================================================================================
    // 4. Obiekt z new; @PostConstruct; kod startowy (StartupShowcase)
    // =================================================================================================
    //
    // {@code new PriceService(jdbc, new PriceUpdater(jdbc))} — zwykły obiekt, nie bean: {@code AopUtils.isAopProxy} = false,
    //   {@code stateInTransaction()} bez transakcji, a {@code raiseAllInTransaction(20)} zostawia połowę podwyżki (test).
    //   Kontener tworzy proxy tylko dla obiektów, które SAM tworzy. To samo dotyczy obiektów z fabryk, mapperów,
    //   klas tworzonych przez bibliotekę (np. Jackson).
    //
    // {@code @PostConstruct} + {@code @Transactional} (StartupShowcase.warmUpCache) — bez transakcji (test). Dlaczego?
    //   Kolejność tworzenia beana: konstruktor → wstrzyknięcia → {@code @PostConstruct} (na SUROWYM obiekcie) → dopiero
    //   potem post-procesor owija obiekt w proxy (s01_core_ioc/Ioc07Lifecycle). Kod startowy, który potrzebuje
    //   transakcji: słuchacz {@code @EventListener(ApplicationReadyEvent.class)} z {@code @Transactional} — Spring woła go
    //   na beanie z kontenera, czyli na proxy (test: transakcja
    //   {@code s10_transactions.tx03_proxy_pitfalls.StartupShowcase.onApplicationReady}), albo
    //   {@code ApplicationRunner}/{@code CommandLineRunner} wołający metodę transakcyjną innego beana.
    //   Konstruktor — tym bardziej bez transakcji (proxy jeszcze nie istnieje).

    // =================================================================================================
    // 5. Wątki: transakcja jest przypięta do wątku (ThreadShowcase) + checklista
    // =================================================================================================
    //
    // Menedżer transakcji przypina połączenie i stan transakcji do bieżącego wątku (ThreadLocal w
    // TransactionSynchronizationManager). Inny wątek ma własne, PUSTE ThreadLocal — nie widzi transakcji (test):
    //   w metodzie z {@code @Transactional}: wątek wywołujący — active = true, name = …ThreadShowcase.statesInBothThreads;
    //   {@code CompletableFuture.supplyAsync(TxState::now).join()} — active = false, name = null.
    //   Skutek (test insertInBothThreadsThenFail): INSERT „Słuchawki” w wątku z transakcją i INSERT „Kabel” w innym
    //   wątku, potem wyjątek → rollback cofa Słuchawki, a Kabel ZOSTAJE (zatwierdził się sam, na osobnym połączeniu).
    //   ({@code join()} = poczekaj na wynik — kolejność jest jawna, bez usypiania wątków.)
    //
    // PUŁAPKA: równoległe przetwarzanie „dla przyspieszenia” w metodzie transakcyjnej ({@code parallelStream()},
    //   {@code CompletableFuture}, własny executor) wychodzi poza transakcję: zapisy z innych wątków nie są wycofywane,
    //   a odczyty nie widzą niezatwierdzonych zmian wątku głównego. Dlaczego? Każdy wątek bierze własne połączenie.
    //   To samo dotyczy {@code @Async} (s16_cache_scheduling_async): metoda asynchroniczna nigdy nie należy do
    //   transakcji wywołującego — jeśli ma być transakcyjna, otwiera WŁASNĄ (w swoim wątku).
    //
    // CHECKLISTA „dlaczego mój @Transactional nie działa” — w ŚCIĄDZE poniżej.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   „Dlaczego mój @Transactional nie działa?” — sprawdź po kolei:
     *   1. Czy wołasz metodę z TEJ SAMEJ klasy ({@code this.metoda()})? → osobny bean / adnotacja wyżej / @Lazy self.
     *   2. Czy obiekt jest beanem? ({@code new}, fabryka, biblioteka → brak proxy) → wstrzyknij bean.
     *   3. Czy metoda jest private, final albo static? → public, bez final. (protected/pakietowe: od Spring 6 OK z CGLIB.)
     *   4. Czy klasa jest final (Kotlin!)? → aplikacja nie wstanie: „Could not generate CGLIB subclass”.
     *   5. Czy to {@code @PostConstruct} albo konstruktor? → {@code @EventListener(ApplicationReadyEvent.class)} / ApplicationRunner.
     *   6. Czy praca idzie w innym wątku (CompletableFuture, parallelStream, @Async)? → transakcja otwierana w tamtym wątku.
     *   7. Czy wyjątek jest checked albo złapany w metodzie? → s10_transactions/Tx01Transactional (sekcje 3, 4).
     *   8. Czy to właściwa adnotacja (org.springframework…, nie jakarta…) i właściwy menedżer? → Tx01 sekcja 5, Tx02 sekcja 4.
     *   Dowód zamiast zgadywania: {@code TransactionSynchronizationManager.isActualTransactionActive()}, {@code AopUtils.isAopProxy(bean)},
     *   log {@code org.springframework.orm.jpa.JpaTransactionManager=debug}.
     *   Proxy: Boot domyślnie CGLIB ({@code Klasa$$SpringCGLIB$$0}); {@code proxy-target-class=false} → JDK (tylko interfejs).
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się proxy CGLIB od proxy JDK? Które wybiera Boot dla beana implementującego interfejs?
     *   2. Co się stanie? Publiczna metoda {@code raiseAll()} bez adnotacji woła {@code this.raiseAllInTransaction()}
     *      ({@code @Transactional}), która zmienia dwa wiersze i przy trzecim rzuca RuntimeException.
     *   3. Podaj trzy sposoby obejścia self-invocation. Który jest zalecany i dlaczego?
     *   4. ZNAJDŹ BŁĄD: {@code @Transactional public final void transfer(...) { repository.save(...); }} —
     *      „dostaję NullPointerException na repository, a przecież jest wstrzyknięte przez konstruktor”.
     *   5. Co się stanie? Klasa serwisu jest {@code final} i ma metodę z {@code @Transactional}.
     *   6. Dlaczego {@code @Transactional} na metodzie z {@code @PostConstruct} nie działa? Jak uruchomić kod startowy
     *      w transakcji?
     *   7. ZNAJDŹ BŁĄD: {@code @Transactional void importAll(List<Row> rows) { rows.parallelStream().forEach(repo::save);
     *      throw new IllegalStateException(); }} — „rollback nie cofa wszystkich wierszy”.
     *   8. Czy w Spring Framework 7 {@code @Transactional} działa na metodzie protected? A na private?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w plikach ProxyExercises.java (1, 4) i ExerciseProductService.java (2, 3),
    // testy: Tx03ProxyPitfallsExercisesTest (pełna aplikacja, bez transakcji testu).
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. CGLIB to wygenerowana PODKLASA klasy beana — widać ją jako klasę i jako jej interfejsy; nie działa z final.
     *      JDK proxy implementuje tylko INTERFEJSY beana — nie da się go wstrzyknąć po klasie. Boot domyślnie
     *      ({@code spring.aop.proxy-target-class=true}) robi CGLIB także dla beana z interfejsem (test sekcji 1).
     *   2. this omija proxy, więc transakcji nie ma: dwa pierwsze UPDATE-y zatwierdzają się od razu i zostają w bazie
     *      po wyjątku (test sekcji 2: Klawiatura=120.00, Mysz=60.00, Monitor=800.00).
     *   3. Osobny bean z metodą transakcyjną (zalecany — każde wywołanie innego beana idzie przez proxy, a podział
     *      odpowiedzialności zwykle jest lepszy); adnotacja na metodzie wywoływanej z zewnątrz; wstrzyknięcie samego
     *      siebie ({@code @Lazy} albo {@code ObjectProvider}); {@code AopContext.currentProxy()} z exposeProxy = true.
     *   4. Metoda final nie jest przesłonięta przez proxy CGLIB: wykonuje się NA PROXY, którego pola są null (proxy
     *      powstaje bez konstruktora) — stąd NPE; transakcji i tak by nie było (test sekcji 3). Usuń final.
     *   5. Aplikacja nie wystartuje: BeanCreationException „Could not generate CGLIB subclass of class …: Common causes
     *      of this problem include using a final class or a non-visible class”, na dnie IllegalArgumentException
     *      „Cannot subclass final class …” (test).
     *   6. {@code @PostConstruct} wykonuje się na surowym obiekcie, zanim post-procesor utworzy proxy — nie ma kto
     *      otworzyć transakcji (test sekcji 4). Kod startowy w transakcji: {@code @EventListener(ApplicationReadyEvent.class)}
     *      z {@code @Transactional} (wołany na proxy) albo ApplicationRunner wołający metodę transakcyjną innego beana.
     *   7. parallelStream wykonuje część save w wątkach puli ForkJoin (część — w wątku wywołującym; podział zależy od
     *      liczby procesorów). Tamte wątki nie mają transakcji (jest przypięta do wątku wywołującego), więc ich zapisy
     *      zatwierdzają się same i rollback ich nie cofa — ten sam mechanizm co z CompletableFuture w teście sekcji 5.
     *      Przetwarzaj w wątku transakcji albo otwieraj osobną transakcję w każdym wątku.
     *   8. protected — tak (od Spring Framework 6.0 proxy klasowe obsługuje metody protected i pakietowe; sprawdzone
     *      testem). private — nie, podklasa nie może jej przesłonić.
     */
    // </editor-fold>
}
