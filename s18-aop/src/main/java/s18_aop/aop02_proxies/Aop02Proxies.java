package s18_aop.aop02_proxies;

import java.util.List;

/**
 * <pre>
 * TEMAT: Proxy od środka — jak Spring „dokłada” aspekty i kiedy to po cichu nie działa
 *        (proxy = pośrednik; JDK dynamic proxy = dynamiczne proxy z JDK; CGLIB = biblioteka generująca podklasy;
 *         self-invocation = wywołanie samego siebie; target = obiekt docelowy)
 *
 * W SKRÓCIE:
 *   Spring nie zmienia kodu Twojej klasy. Gdy bean pasuje do jakiegoś punktu przecięcia, Spring wstrzykuje innym
 *   beanom PROXY: obiekt-pośrednika, który wykonuje porady i przekazuje wywołanie do prawdziwego obiektu.
 *   Proxy jest jednego z dwóch rodzajów: JDK (implementuje te same INTERFEJSY) albo CGLIB (jest PODKLASĄ Twojej
 *   klasy). Spring Boot domyślnie wybiera CGLIB (spring.aop.proxy-target-class=true). Z tej budowy wynikają
 *   wszystkie pułapki: proxy nie widzi wywołań this.metoda() z wnętrza obiektu, nie nadpisze metody private ani
 *   final, nie opakuje klasy final, a proxy JDK nie jest instancją Twojej klasy (rzutowanie się nie uda).
 *
 * ANALOGIA: sekretariat i asystent.
 *   Klienci dzwonią do firmy przez sekretariat (proxy): sekretarka notuje rozmowę (porada) i łączy z szefem
 *   (obiekt docelowy). Gdy szef sam do siebie zapisze notatkę (this.metoda()), sekretariat o tym nie wie — nic nie
 *   jest notowane. Proxy JDK to sekretarka z agencji: zna tylko „cennik usług” (interfejs), więc o sprawy spoza
 *   cennika (metody spoza interfejsu) zapytać jej nie można, a udawać szefa (rzutowanie na klasę) nie umie.
 *   Proxy CGLIB to zastępca wyszkolony „na wzór szefa” (podklasa): umie to, co szef, oprócz spraw prywatnych
 *   (private) i decyzji „ostatecznych” (final), których nikt poza szefem podjąć nie może.
 *
 * JAK TO DZIAŁA:
 *   tworzenie beana StockService:
 *     konstruktor → wstrzykiwanie → @PostConstruct → BeanPostProcessor „po inicjalizacji”:
 *       AnnotationAwareAspectJAutoProxyCreator: „czy jakiś advisor pasuje do metod StockService?”
 *         tak → tworzy proxy: CGLIB generuje klasę StockService$$SpringCGLIB$$0 (podklasę StockService),
 *               a jej obiekt dostaje w środku odnośnik do prawdziwego StockService
 *         nie → zwraca obiekt bez zmian (ReportPrinter)
 *     do kontenera i do wszystkich wstrzyknięć trafia PROXY
 *   wywołanie: inny bean → proxy.reserve(...) → porady → prawdziwyObiekt.reserve(...)
 *   wewnątrz prawdziwego obiektu: this = prawdziwy obiekt (NIE proxy) → this.reserve(...) omija porady
 *
 * SŁÓWKA:
 *   proxy = pośrednik; target = obiekt docelowy; dynamic = dynamiczny (tworzony w czasie działania programu);
 *   subclass = podklasa; interface-based = oparty na interfejsie; class-based = oparty na klasie;
 *   proxy target class = „rób proxy z klasy docelowej” (CGLIB); expose = udostępnić; current = bieżący;
 *   advised = „doradzany” (obiekt z poradami); final = ostateczny (nie do nadpisania/dziedziczenia);
 *   lazy = leniwy; self-invocation = wywołanie samego siebie; workaround = obejście.
 *
 * ZOBACZ TEŻ: s18_aop/Aop01Aspects (porady i punkty przecięcia),
 *             s18_aop/Aop03CustomAnnotations (własne adnotacje, kolejność aspektów),
 *             s10_transactions/Tx03ProxyPitfalls (te same pułapki dla {@code @Transactional}),
 *             s16_cache_scheduling_async/Cache01Cacheable i s16_cache_scheduling_async/Async04Async
 *             (też działają przez proxy), s13_rest_client/Rc04Resilience ({@code @Retryable} — też proxy),
 *             s01_core_ioc/Ioc07Lifecycle (BeanPostProcessor), s01_core_ioc/Ioc06Scopes (ObjectProvider),
 *             t19_annotations_reflection/Annotations06DynamicProxy (java.lang.reflect.Proxy w czystej Javie).
 * </pre>
 */
public final class Aop02Proxies {

    private Aop02Proxies() {
    }

    // =================================================================================================
    // 1. SKĄD SIĘ BIERZE PROXY i co jest w środku
    // =================================================================================================
    //
    // Proxy tworzy BeanPostProcessor (przetwarzacz beanów) AnnotationAwareAspectJAutoProxyCreator — ten sam, którego
    // włącza Boot (s18_aop/Aop01Aspects, sekcja 2). Działa w kroku „po inicjalizacji” beana (s01_core_ioc/Ioc07Lifecycle):
    // gotowy obiekt podmienia na proxy — ale TYLKO gdy jakiś punkt przecięcia pasuje do którejś metody beana.
    //
    // Sprawdzone testem sekcji 1 (CountingAspect łapie beany *Service i *Calculator):
    //   stockService.getClass()               → s18_aop.aop02_proxies.StockService$$SpringCGLIB$$0
    //   AopUtils.isAopProxy(stockService)     → true;  AopUtils.getTargetClass(stockService) → StockService
    //   ((Advised) stockService).getAdvisors()→ zawiera poradę CountingAspect.count
    //   ...getTargetSource().getTarget()      → zwykły StockService (nie proxy) — to on wykonuje Twój kod
    //   reportPrinter.getClass()              → ReportPrinter (nazwa nie pasuje do bean(*Service) — brak proxy)
    //
    // Ciekawostka „spod maski”: obiekt proxy CGLIB Spring tworzy BEZ wywołania konstruktora Twojej klasy (biblioteka
    // Objenesis). Konstruktor biegnie tylko raz — dla prawdziwego obiektu (sprawdzone uruchomieniem: licznik
    // wywołań konstruktora = 1). Skutek: POLA obiektu proxy są puste (null) — to się zemści w sekcji 5.
    //
    // Narzędzia do zaglądania (org.springframework.aop.support.AopUtils):
    //   AopUtils.isAopProxy(obj)        czy to proxy Springa
    //   AopUtils.isJdkDynamicProxy(obj) czy proxy JDK
    //   AopUtils.isCglibProxy(obj)      czy proxy CGLIB
    //   AopUtils.getTargetClass(obj)    klasa obiektu docelowego (dla zwykłego obiektu — jego klasa)
    //
    // Test: Aop02ProxiesTest#proxyOnlyForMatchedBeans

    // =================================================================================================
    // 2. DWA RODZAJE PROXY: JDK i CGLIB — i co wybiera Spring Boot
    // =================================================================================================
    //
    //                      | proxy JDK (dynamic proxy)               | proxy CGLIB
    //   -------------------+-----------------------------------------+---------------------------------------------
    //   jak powstaje       | java.lang.reflect.Proxy — nowa klasa,   | wygenerowana PODKLASA Twojej klasy
    //                      | która implementuje TWOJE INTERFEJSY     | (CGLIB jest wbudowany w spring-core)
    //   nazwa klasy        | jdk.proxy2.$Proxy60 (numer zmienny)     | StockService$$SpringCGLIB$$0
    //   instanceof klasa?  | NIE — tylko instanceof interfejs        | TAK (to podklasa)
    //   co przechwytuje    | tylko metody z interfejsów              | metody, które podklasa może nadpisać
    //   wymaga             | interfejsu                              | klasy, która nie jest final
    //
    // Kto wybiera?
    //   • Spring Boot: spring.aop.proxy-target-class=true (domyślnie) → ZAWSZE CGLIB, także dla klasy z interfejsem.
    //     (AopAutoConfiguration: @EnableAspectJAutoProxy(proxyTargetClass = true). To samo ustawienie Boot stosuje
    //     do proxy transakcji — TransactionAutoConfiguration ma ten sam warunek.)
    //   • spring.aop.proxy-target-class=false → klasa z interfejsem: JDK; klasa bez interfejsu: nadal CGLIB.
    //   • czysty Spring (bez Boota): @EnableAspectJAutoProxy ma proxyTargetClass = false, więc JDK dla klas
    //     z interfejsem (PlainSpringConfig). Stąd różnice między starymi poradnikami a Bootem.
    //   • NOWOŚĆ Spring Framework 7: @Proxyable na klasie (albo metodzie @Bean) zmienia typ proxy dla JEDNEGO beana:
    //       @Proxyable(ProxyType.INTERFACES)    → proxy JDK (np. dla klasy final z interfejsem — VipDiscountCalculator)
    //       @Proxyable(ProxyType.TARGET_CLASS)  → proxy CGLIB, nawet gdy globalnie ustawiono JDK
    //
    // Sprawdzone testami: Boot → priceCalculator (z interfejsem) to CGLIB i instanceof StandardPriceCalculator;
    // czysty Spring → ten sam bean to jdk.proxy...; VipDiscountCalculator (final + @Proxyable(INTERFACES)) → JDK.
    //
    // DOBRA PRAKTYKA: zostaw domyślne CGLIB Boota, a wstrzykuj przez interfejs, gdy go masz.
    //   Dlaczego? Z CGLIB wstrzyknięcie przez klasę też działa (sekcja 3 Cię nie dotyczy), a kod pisany „pod interfejs”
    //   działa przy obu rodzajach proxy — zmiana ustawienia niczego nie popsuje.
    //
    // Test: Aop02ProxiesTest#bootUsesCglibByDefault, #plainSpringUsesJdkProxyForInterfaces,
    //       #proxyableChoosesJdkProxyForOneBean

    // =================================================================================================
    // 3. PUŁAPKA: proxy JDK i klasa zamiast interfejsu (Aop02ProxiesJdkTest, proxy-target-class=false)
    // =================================================================================================
    //
    // Proxy JDK implementuje PriceCalculator, ale NIE jest StandardPriceCalculator. Komunikaty z testów:
    //   context.getBean(StandardPriceCalculator.class)
    //     → NoSuchBeanDefinitionException: No qualifying bean of type 's18_aop.aop02_proxies.StandardPriceCalculator'
    //       available                                     (bean jest, ale nie tego typu!)
    //   context.getBean("standardPriceCalculator", StandardPriceCalculator.class)
    //     → BeanNotOfRequiredTypeException: Bean named 'standardPriceCalculator' is expected to be of type
    //       's18_aop.aop02_proxies.StandardPriceCalculator' but was actually of type 'jdk.proxy2.$Proxy60'
    //   (StandardPriceCalculator) bean
    //     → ClassCastException: class jdk.proxy2.$Proxy60 cannot be cast to class
    //       s18_aop.aop02_proxies.StandardPriceCalculator (...)
    //   bean z konstruktorem CheckoutByClass(StandardPriceCalculator calculator) → aplikacja NIE STARTUJE:
    //     UnsatisfiedDependencyException (przyczyna: BeanNotOfRequiredTypeException), a Boot wypisuje raport:
    //       APPLICATION FAILED TO START
    //       Description:
    //       The bean 'standardPriceCalculator' could not be injected because it is a JDK dynamic proxy
    //       The bean is of type 'jdk.proxy2.$Proxy60' and implements:
    //           s18_aop.aop02_proxies.PriceCalculator
    //           org.springframework.aop.SpringProxy
    //           org.springframework.aop.framework.Advised
    //           org.springframework.core.DecoratingProxy
    //       Expected a bean of type 's18_aop.aop02_proxies.StandardPriceCalculator' which implements:
    //           s18_aop.aop02_proxies.PriceCalculator
    //       Action:
    //       Consider injecting the bean as one of its interfaces or forcing the use of CGLib-based proxies by
    //       setting proxyTargetClass=true on @EnableAsync and/or @EnableCaching.
    //   (Zwróć uwagę na listę interfejsów: każde proxy Springa implementuje też SpringProxy i Advised — to po nich
    //   AopUtils rozpoznaje proxy.)
    //
    // PUŁAPKA: kod działa, dopóki ktoś nie przełączy proxy na JDK (albo nie przeniesie klasy do czystego Springa).
    //   Dlaczego groźne? Błąd nie zależy od Twojej zmiany, tylko od konfiguracji — i wychodzi przy starcie albo
    //   dopiero przy rzutowaniu w rzadko używanej ścieżce. Naprawa: wstrzykuj interfejs (PriceCalculator).
    //
    // Test: Aop02ProxiesJdkTest#jdkProxyForInterfacesCglibForClasses, #jdkProxyIsNotTheClass,
    //       #injectingClassFailsWithJdkProxy

    // =================================================================================================
    // 4. PUŁAPKA: wywołanie z tej samej klasy omija proxy (PYTANIE REKRUTACYJNE)
    // =================================================================================================
    //
    //   public List<String> reserveAll(List<String> skus) {
    //       for (String sku : skus) { results.add(this.reserve(sku, 1)); }    ← this = obiekt, NIE proxy
    //   }
    //
    //   test → [proxy StockService$$SpringCGLIB$$0].reserveAll(...)  → porada: reserveAll +1
    //            → [obiekt StockService].reserveAll(...)
    //                 this.reserve(...) x 3                         → porada: reserve +0 (proxy nie wie o tych wywołaniach)
    // Sprawdzone testem sekcji 4: licznik reserve = 1 po wywołaniu z zewnątrz i NADAL 1 po reserveAll z trzema SKU;
    // wewnątrz metody AopUtils.isAopProxy(this) = false.
    //
    // PUŁAPKA: adnotacja „działa” przy wywołaniu z innego beana, a przy wywołaniu z tej samej klasy po cichu nie.
    //   Dlaczego groźne? Dotyczy KAŻDEJ adnotacji opartej na proxy: @Transactional (brak transakcji —
    //   s10_transactions/Tx03ProxyPitfalls), @Cacheable (brak pamięci podręcznej), @Async (wykonanie w tym samym
    //   wątku), @Retryable (brak ponawiania), własne aspekty (brak logu/audytu). Nie ma błędu ani ostrzeżenia.
    //   Odpowiedź na rozmowie: „bo Spring AOP działa przez proxy, a this wskazuje obiekt docelowy, nie proxy”.

    // =================================================================================================
    // 5. CO PROXY CGLIB UMIE PRZECHWYCIĆ: public, protected, pakietowe, private, final, klasa final
    // =================================================================================================
    //
    //   metoda / klasa (WarehouseService, CGLIB)  | porada się wykona?  | sprawdzone testem
    //   ------------------------------------------+---------------------+------------------------------------------
    //   public                                    | TAK                 | stockOf: licznik 1
    //   protected                                 | TAK                 | protectedInfo: licznik 1
    //   pakietowa (bez modyfikatora)              | TAK                 | packageInfo: licznik 1 (test w tym pakiecie)
    //   private                                   | NIE (po cichu)      | privateInfo: licznik 0
    //   public final                              | NIE + błąd w środku | finalStockOf: NullPointerException
    //   cała klasa final (FinalPriceService)      | aplikacja NIE STARTUJE przy CGLIB
    //   (proxy JDK przechwytuje tylko metody INTERFEJSU — np. currency() spoza interfejsu nie jest dostępna wcale)
    //
    // Dlaczego private nie? Podklasa nie widzi metod prywatnych, więc nie może ich nadpisać. Zresztą private woła się
    // tylko z wnętrza obiektu — czyli zawsze przez this (sekcja 4). Metody static też nie (nie da się ich nadpisać).
    //
    // Metoda final — najgorszy przypadek. Proxy nie może jej nadpisać, więc wywołanie trafia do kodu z Twojej klasy,
    // ale wykonuje się na OBIEKCIE PROXY, który ma puste pola (sekcja 1, Objenesis). Komunikat z testu:
    //   NullPointerException: Cannot invoke "java.util.Map.getOrDefault(Object, Object)" because "this.stock" is null
    // Spring ostrzega przy starcie (log WARN, raz na klasę — zobaczysz go w wyniku testów):
    //   o.s.aop.framework.CglibAopProxy : Public final method [public final int
    //   s18_aop.aop02_proxies.WarehouseService.finalStockOf(java.lang.String)] cannot get proxied via CGLIB, consider
    //   removing the final marker or using interface-based JDK proxies.
    //
    // Klasa final (FinalPriceService zarejestrowana w teście) — przy CGLIB start kończy się błędem:
    //   BeanCreationException: ... Could not generate CGLIB subclass of class s18_aop.aop02_proxies.FinalPriceService:
    //   Common causes of this problem include using a final class or a non-visible class
    //   (na dnie: IllegalArgumentException: Cannot subclass final class s18_aop.aop02_proxies.FinalPriceService)
    // Ta sama klasa przy proxy JDK (ma interfejs) — kontekst startuje. W Spring 7 da się to wybrać dla jednego beana:
    // @Proxyable(ProxyType.INTERFACES) (VipDiscountCalculator — klasa final, a aplikacja lekcji startuje).
    //
    // DOBRA PRAKTYKA: metody objęte aspektem (i @Transactional, @Cacheable…) rób public i nie-final, a klasy — nie-final.
    //   Dlaczego? Wtedy zachowanie nie zależy od rodzaju proxy ani od szczegółów CGLIB. Uwaga na rekordy (record):
    //   rekord jest zawsze final, więc jako bean objęty aspektem przy CGLIB nie wystartuje — tak jak FinalPriceService.
    //
    // Test: Aop02ProxiesTest#whichMethodsCglibCanIntercept, #finalMethodRunsOnProxyWithEmptyFields,
    //       #finalClassCannotBeSubclassed

    // =================================================================================================
    // 6. OBEJŚCIA dla this.metoda() — od najlepszego
    // =================================================================================================
    //
    //   1. OSOBNY BEAN (StockBatchService woła stockService.reserve) ← NAJLEPSZE: jasny podział, zero sztuczek.
    //   2. WSTRZYKNIĘCIE SAMEGO SIEBIE: SelfInjectingStockService(@Lazy SelfInjectingStockService self)
    //      i self.reserve(...). @Lazy wstrzykuje leniwego pośrednika, który przy pierwszym użyciu pobiera z kontenera
    //      gotowe proxy. Zamiast @Lazy: ObjectProvider<SelfInjectingStockService> i getObject() (ćwiczenie 2).
    //   3. AopContext.currentProxy() (AopContextStockService) — „podaj proxy, przez które mnie wywołano”.
    //      Wymaga @EnableAspectJAutoProxy(exposeProxy = true) (tu: na Aop02Application). Bez tego — także WEWNĄTRZ
    //      wywołania przez proxy — rzuca (komunikat z testu):
    //        IllegalStateException: Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it
    //        available, and ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation
    //        context.
    //      Ten sam wyjątek dostaniesz, wołając AopContext.currentProxy() spoza jakiegokolwiek wywołania przez proxy.
    //   4. Tryb AspectJ z wplataniem (weaving) podczas kompilacji lub ładowania klas — kod porad trafia DO klasy, więc
    //      działa też this.metoda(). Wymaga dodatkowej konfiguracji; w kursie go nie używamy.
    // Sprawdzone testem sekcji 6: obejścia 1–3 dają licznik reserve = 3 dla trzech SKU.
    //
    // PUŁAPKA: AopContext.currentProxy() w kodzie biznesowym. Dlaczego groźne? Klasa przestaje działać bez Spring AOP
    //   i bez exposeProxy (np. w teście jednostkowym rzuca IllegalStateException), a w innym wątku (np. w @Async albo
    //   w parallelStream) proxy „nie istnieje”. Traktuj to jako ostateczność.
    //
    // DOBRA PRAKTYKA: gdy kusi Cię wywołanie „samego siebie przez proxy”, wydziel metodę do osobnego beana.
    //   Dlaczego? Zwykle to znak, że klasa ma dwie odpowiedzialności; osobny bean usuwa problem u źródła i nie wymaga
    //   znajomości proxy od kolejnych czytelników kodu.
    //
    // Test: Aop02ProxiesTest#workaroundsGoThroughProxy, #aopContextOutsideProxyCall, #aopContextWithoutExposeProxy

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Spring AOP = proxy. Proxy dostaje tylko bean pasujący do punktu przecięcia; w środku jest prawdziwy obiekt.
     *   • JDK proxy = implementuje interfejsy (nie jest instancją klasy); CGLIB = podklasa (jest instancją klasy).
     *   • Boot: spring.aop.proxy-target-class=true (domyślnie) → CGLIB zawsze; false → JDK dla klas z interfejsem.
     *   • Czysty Spring: {@code @EnableAspectJAutoProxy} → JDK dla klas z interfejsem. Spring 7: {@code @Proxyable}.
     *   • Proxy JDK + wstrzyknięcie/rzutowanie na klasę → BeanNotOfRequiredTypeException / ClassCastException.
     *   • this.metoda() omija proxy → nie działają aspekty, @Transactional, @Cacheable, @Async, @Retryable.
     *   • CGLIB: public/protected/pakietowe TAK; private NIE; final NIE (+ NPE na pustych polach); klasa final → brak startu.
     *   • Obejścia: osobny bean (najlepiej) → @Lazy self / ObjectProvider → AopContext (exposeProxy) → wplatanie AspectJ.
     *   • Podgląd: AopUtils.isAopProxy / isJdkDynamicProxy / isCglibProxy / getTargetClass, (Advised) proxy.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się proxy JDK od proxy CGLIB? Które wybiera Spring Boot domyślnie, a które czysty Spring?
     *   2. Co się stanie? W aplikacji ze spring.aop.proxy-target-class=false bean CheckoutByClass ma konstruktor
     *      z parametrem StandardPriceCalculator (klasa implementująca PriceCalculator, objęta aspektem).
     *   3. PYTANIE REKRUTACYJNE: dlaczego {@code @Transactional} na metodzie wywołanej przez this.metoda() z tej samej
     *      klasy nie otwiera transakcji? Podaj dwa sposoby naprawy.
     *   4. Co się stanie? Metoda {@code public final int finalStockOf(String sku)} używa pola {@code stock}; bean jest
     *      opakowany w proxy CGLIB. Wywołujesz ją z testu przez wstrzyknięty bean.
     *   5. ZNAJDŹ BŁĄD: {@code @Service public final class PriceListService} z metodą objętą aspektem; aplikacja na Boot
     *      z ustawieniami domyślnymi nie startuje. Dlaczego? Podaj dwie poprawki.
     *   6. Co się stanie? AopContext.currentProxy() wywołane w metodzie beana, gdy nikt nie ustawił exposeProxy = true.
     *   7. Dlaczego metoda private nigdy nie zostanie przechwycona przez proxy — ani JDK, ani CGLIB?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Aop02ProxiesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć rodzaj proxy obiektu: „JDK”, „CGLIB” albo „BRAK PROXY”.
     * Podpowiedź: AopUtils.isJdkDynamicProxy(bean) i AopUtils.isCglibProxy(bean).
     */
    static String exercise1(Object bean) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /*
     * ĆWICZENIE 2 (średnie): otwórz Aop02Exercise2Service i uzupełnij reserveAll tak, żeby KAŻDE wywołanie reserve
     * przeszło przez proxy (test sprawdza licznik CountingAspect: 3 wywołania reserve dla 3 SKU).
     * Podpowiedź: klasa dostaje w konstruktorze ObjectProvider<Aop02Exercise2Service> self —
     * self.getObject() zwraca bean z kontenera, czyli proxy.
     */

    /**
     * ĆWICZENIE 3 (trudniejsze): zbuduj RĘCZNIE proxy JDK dla podanego kalkulatora. Każde wywołanie metody ma dopisać
     * do logu „przed priceFor” przed wywołaniem i „po priceFor” po nim, a wynik ma być prawdziwy.
     * Test sprawdza też, że wynik to proxy JDK i że rzutowanie na StandardPriceCalculator się nie uda.
     * Podpowiedź: {@code ProxyFactory factory = new ProxyFactory(target);} (klasa z interfejsem → JDK),
     * {@code factory.addAdvice((MethodInterceptor) invocation -> { ... invocation.proceed() ... });}
     * (MethodInterceptor z pakietu org.aopalliance.intercept; nazwa metody: invocation.getMethod().getName()),
     * {@code return (PriceCalculator) factory.getProxy();}
     */
    static PriceCalculator exercise3(PriceCalculator target, List<String> log) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Proxy JDK (java.lang.reflect.Proxy) to nowa klasa implementująca interfejsy beana — przechwytuje tylko metody
     *      interfejsów i nie jest instancją klasy beana. CGLIB generuje podklasę klasy beana — jest jej instancją,
     *      przechwytuje metody, które da się nadpisać. Boot domyślnie: CGLIB (spring.aop.proxy-target-class=true).
     *      Czysty Spring z @EnableAspectJAutoProxy: JDK dla klas z interfejsem. Testy sekcji 2.
     *   2. Aplikacja nie wystartuje: UnsatisfiedDependencyException z przyczyną BeanNotOfRequiredTypeException
     *      („Bean named 'standardPriceCalculator' is expected to be of type '...StandardPriceCalculator' but was actually
     *      of type 'jdk.proxy2.$Proxy...'”) i raport Boota „could not be injected because it is a JDK dynamic proxy”.
     *      Test: Aop02ProxiesJdkTest#injectingClassFailsWithJdkProxy.
     *   3. Transakcję otwiera proxy, a this wskazuje obiekt docelowy — wywołanie w ogóle nie przechodzi przez proxy.
     *      Naprawy: przenieść metodę do osobnego beana (najlepiej); wstrzyknąć samego siebie (@Lazy albo
     *      ObjectProvider); TransactionTemplate (s10_transactions/Tx05ProgrammaticTransactions); AopContext.
     *   4. NullPointerException: Cannot invoke "java.util.Map.getOrDefault(Object, Object)" because "this.stock" is null.
     *      Metody final proxy nie nadpisze, więc wykonuje się ona na obiekcie proxy, którego pola są puste
     *      (konstruktor nie był wołany). Porada się nie wykonuje. Test: Aop02ProxiesTest#finalMethodRunsOnProxyWithEmptyFields.
     *   5. Boot robi proxy CGLIB (podklasę), a po klasie final nie da się dziedziczyć: BeanCreationException „Could not
     *      generate CGLIB subclass of class ...” (na dnie „Cannot subclass final class ...”). Poprawki: usunąć final;
     *      albo — jeśli klasa ma interfejs — @Proxyable(ProxyType.INTERFACES) na klasie (Spring 7) lub globalnie
     *      spring.aop.proxy-target-class=false. Testy: #finalClassCannotBeSubclassed, #proxyableChoosesJdkProxyForOneBean.
     *   6. IllegalStateException: „Cannot find current proxy: Set 'exposeProxy' property on Advised to 'true' to make it
     *      available, and ensure that AopContext.currentProxy() is invoked in the same thread as the AOP invocation
     *      context.” Test: Aop02ProxiesTest#aopContextWithoutExposeProxy.
     *   7. Proxy JDK zna tylko metody interfejsów (a te są publiczne). Proxy CGLIB jest podklasą, a podklasa nie widzi
     *      metod private, więc nie może ich nadpisać. Do tego private woła się tylko przez this, czyli z pominięciem
     *      proxy. Test: Aop02ProxiesTest#whichMethodsCglibCanIntercept (privateInfo: licznik 0).
     */
    // </editor-fold>
}
