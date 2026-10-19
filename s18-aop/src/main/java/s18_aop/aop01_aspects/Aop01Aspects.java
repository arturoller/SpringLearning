package s18_aop.aop01_aspects;

/**
 * <pre>
 * TEMAT: Aspekty — jedno zachowanie dołożone do wielu metod bez zmiany ich kodu
 *        (AOP = aspect-oriented programming = programowanie aspektowe; aspect = aspekt; advice = porada;
 *         pointcut = punkt przecięcia; join point = punkt złączenia; proxy = pośrednik)
 *
 * W SKRÓCIE:
 *   Niektóre zadania dotyczą WIELU metod naraz: logowanie wywołań, pomiar czasu, sprawdzanie uprawnień, audyt.
 *   Gdy dopisujesz je ręcznie w każdej metodzie, logika biznesowa tonie w powtórzeniach. Aspekt zbiera taki kod
 *   w JEDNYM miejscu: klasa z {@code @Aspect} + {@code @Component} zawiera PORADY (co zrobić: przed, po, wokół metody),
 *   a każda porada ma PUNKT PRZECIĘCIA (których metod dotyczy, np. {@code execution(* ProductCatalog.find*(..))}).
 *   Spring nie zmienia Twojej klasy: zamiast niej wstrzykuje PROXY, które wykonuje porady i woła prawdziwą metodę.
 *
 * ANALOGIA: recepcja w biurowcu.
 *   Firmy w biurowcu (serwisy) zajmują się swoją pracą. Każdy gość (wywołanie metody) przechodzi przez recepcję
 *   (proxy). Recepcjonistka ma instrukcje (porady): „wpisz gościa do księgi przed wejściem” (@Before), „odnotuj
 *   wyjście” (@After), „jeśli wyszedł z awanturą — zgłoś ochronie” (@AfterThrowing). Każda instrukcja mówi, kogo
 *   dotyczy (punkt przecięcia): „goście do księgowości”, „wszyscy kurierzy”. Firmy nie muszą nic robić —
 *   a gdy zmienia się regulamin, poprawia się go w recepcji, nie w każdej firmie.
 *
 * JAK TO DZIAŁA:
 *   start kontekstu
 *     → autokonfiguracja Boota AopAutoConfiguration włącza {@code @EnableAspectJAutoProxy(proxyTargetClass = true)}
 *     → ta rejestruje BeanPostProcessor (przetwarzacz beanów) AnnotationAwareAspectJAutoProxyCreator
 *     → on znajduje beany z {@code @Aspect} i z każdej metody-porady robi „advisora” (doradcę = porada + punkt przecięcia)
 *     → po utworzeniu KAŻDEGO beana pyta: czy któryś punkt przecięcia pasuje do którejś jego metody?
 *          tak → do kontekstu trafia PROXY (np. klasa ProductCatalog$$SpringCGLIB$$0), a obiekt jest „w środku”
 *          nie → do kontekstu trafia zwykły obiekt (np. CallJournal — test sekcji 1)
 *   wywołanie catalog.findPrice("SPO-001")
 *     → proxy → porady „przed” → prawdziwa metoda ProductCatalog.findPrice → porady „po” → wynik dla wołającego
 *
 * SŁÓWKA:
 *   aspect = aspekt; advice = porada; pointcut = punkt przecięcia; join point = punkt złączenia (w Spring AOP:
 *   wykonanie metody beana); cross-cutting concern = sprawa przekrojowa; target = obiekt docelowy;
 *   proceed = kontynuuj; before/after = przed/po; around = wokół; returning = zwracający; throwing = rzucający;
 *   signature = sygnatura (nazwa + parametry metody); execution = wykonanie; within = wewnątrz;
 *   args = argumenty; weaving = wplatanie (dokładanie kodu aspektu do programu).
 *
 * ZOBACZ TEŻ: s18_aop/Aop02Proxies (skąd się bierze proxy i kiedy aspekt NIE działa),
 *             s18_aop/Aop03CustomAnnotations (własne adnotacje jako punkty przecięcia, kolejność aspektów),
 *             s10_transactions/Tx03ProxyPitfalls (te same reguły proxy dla {@code @Transactional}),
 *             s01_core_ioc/Ioc07Lifecycle (BeanPostProcessor i cykl życia beana),
 *             t19_annotations_reflection/Annotations06DynamicProxy (proxy w czystej Javie),
 *             t22_design_patterns/Patterns07Decorator (dekorator — „owijanie” obiektu dodatkowym zachowaniem).
 * </pre>
 */
public final class Aop01Aspects {

    private Aop01Aspects() {
    }

    // =================================================================================================
    // 1. PO CO ASPEKTY — sprawy przekrojowe (cross-cutting concerns)
    // =================================================================================================
    //
    // PRZED — logowanie dopisane ręcznie w każdej metodzie:
    //   public BigDecimal findPrice(String sku) {
    //       log.info("start findPrice {}", sku);                 ← to samo w findStock, findName, placeOrder...
    //       try { ... logika ... } finally { log.info("koniec findPrice"); }
    //   }
    // Skutek: logika ginie w powtórzeniach, a zmiana formatu logu to poprawki w kilkudziesięciu miejscach
    // (i zawsze o którejś metodzie się zapomni).
    //
    // PO — ProductCatalog zawiera TYLKO logikę, a logowanie siedzi w jednym aspekcie (LoggingAspect):
    //   @Aspect @Component
    //   class LoggingAspect {
    //       @Before("execution(* s18_aop.aop01_aspects.ProductCatalog.findPrice(..))")
    //       public void before(JoinPoint joinPoint) { ... }
    //   }
    //
    // Słownik AOP (zapamiętaj te pięć słów — pojawiają się w dokumentacji i na rozmowach):
    //   aspekt (aspect)              klasa z poradami, np. LoggingAspect
    //   porada (advice)              CO i KIEDY zrobić: metoda z @Before/@After/@Around...
    //   punkt przecięcia (pointcut)  GDZIE: wyrażenie wybierające metody, np. execution(...)
    //   punkt złączenia (join point) konkretne miejsce w działającym programie; w Spring AOP to zawsze WYKONANIE
    //                                METODY beana (pełny AspectJ zna też np. dostęp do pól i konstruktory)
    //   proxy                        obiekt-pośrednik, który Spring wstrzykuje zamiast Twojego beana
    //
    // Typowe sprawy przekrojowe: logowanie, pomiar czasu, audyt („kto i kiedy zmienił cenę”), uprawnienia,
    // ponawianie przy błędzie, transakcje (@Transactional to też aspekt — s10_transactions/Tx01Transactional).
    //
    // Test: Aop01AspectsTest#aspectWorksThroughProxy — wstrzyknięty katalog to proxy CGLIB
    //   (AopUtils.isAopProxy = true, klasa ProductCatalog$$SpringCGLIB$$0), a CallJournal, którego nie dotyczy
    //   żaden punkt przecięcia, jest zwykłym obiektem.

    // =================================================================================================
    // 2. PIERWSZY ASPEKT: @Aspect + @Component (i co robi Spring Boot)
    // =================================================================================================
    //
    //   @Aspect        // aspect = aspekt: „ta klasa zawiera porady” (adnotacja z biblioteki AspectJ, org.aspectj...)
    //   @Component     // component = komponent: „zrób z tej klasy bean” (adnotacja Springa)
    //   public class LoggingAspect { ... }
    //
    // Kto włącza obsługę aspektów? Spring Boot, sam. Autokonfiguracja AopAutoConfiguration (moduł
    // spring-boot-autoconfigure) dokłada @EnableAspectJAutoProxy(proxyTargetClass = true), jeśli:
    //   • na ścieżce klas jest biblioteka aspectjweaver (warunek: klasa org.aspectj.weaver.Advice) — dostarcza ją
    //     starter spring-boot-starter-aspectj,
    //   • ustawienie spring.aop.auto nie jest wyłączone (domyślnie true).
    // W starych poradnikach zobaczysz @EnableAspectJAutoProxy na klasie konfiguracji i starter spring-boot-starter-aop.
    // W Boot 4 starter nazywa się spring-boot-starter-aspectj (-aop nie ma już w BOM Boota 4.1), a @EnableAspectJAutoProxy
    // dopisujesz tylko w czystym Springu (bez Boota) albo gdy chcesz zmienić ustawienia (Aop02Proxies, sekcja 6).
    //
    // PUŁAPKA: @Aspect BEZ @Component (ForgottenAspect). Dlaczego groźne? @Aspect nie jest stereotypem Springa, więc
    //   skanowanie komponentów pomija tę klasę: nie powstaje bean, a Spring szuka porad TYLKO w beanach. Kod się
    //   kompiluje, aplikacja startuje, a porada po cichu nigdy się nie wykonuje.
    //   (Zamiast @Component może być metoda @Bean zwracająca aspekt — ważne, żeby aspekt był beanem.)
    //
    // Test: Aop01AspectsTest#aspectWithoutComponentIsIgnored — getBeanNamesForType(ForgottenAspect.class) jest puste,
    //   a po findStock w dzienniku nie ma wpisu „ZAPOMNIANY”.

    // =================================================================================================
    // 3. PIĘĆ RODZAJÓW PORAD i ich kolejność (LoggingAspect na findPrice)
    // =================================================================================================
    //
    //   porada            | kiedy biegnie                         | co może
    //   ------------------+---------------------------------------+---------------------------------------------
    //   @Before           | przed metodą                          | czytać argumenty; rzucić wyjątek = metoda
    //                     |                                       | się NIE wykona (np. brak uprawnień)
    //   @AfterReturning   | po NORMALNYM zakończeniu              | czytać wynik (returning = "result")
    //   @AfterThrowing    | po wyjątku z metody                   | czytać wyjątek (throwing = "error"); nie może
    //                     |                                       | go „połknąć”, ale może rzucić INNY (ćwiczenie 4)
    //   @After            | ZAWSZE po metodzie (jak finally)      | sprzątać, zapisać „koniec”
    //   @Around           | wokół: przed I po, w jednej metodzie  | wszystko — patrz sekcja 4
    //
    // Kolejność dla catalog.findPrice("SPO-001") — skopiowana z testu (sukces):
    //   LOG @Around: przed
    //   LOG @Before: findPrice [SPO-001]
    //   METODA findPrice(SPO-001)
    //   LOG @AfterReturning: wynik 64.99
    //   LOG @After: findPrice zakończona
    //   LOG @Around: po, wynik 64.99
    // A dla findPrice("XYZ-999") (nieznany SKU — wyjątek):
    //   LOG @Around: przed / LOG @Before: ... / METODA ... / LOG @AfterThrowing: Nieznany SKU: XYZ-999 /
    //   LOG @After: findPrice zakończona / LOG @Around: wyjątek IllegalArgumentException
    //   — @AfterReturning się NIE wykonało, @After TAK, a wyjątek poleciał dalej do wołającego.
    //
    // Skąd ta kolejność? Od Spring Framework 5.2.7 porady JEDNEGO aspektu mają stały priorytet według rodzaju:
    // @Around, @Before, @After, @AfterReturning, @AfterThrowing. „Na wejściu” wyższy priorytet biegnie pierwszy,
    // „na wyjściu” — ostatni (dlatego @Around otwiera i zamyka całość). Dwie porady TEGO SAMEGO rodzaju w jednym
    // aspekcie mają kolejność NIEOKREŚLONĄ (Java nie przechowuje kolejności metod w pliku .class) — gdy kolejność
    // ma znaczenie, rozdziel je na osobne aspekty z @Order (s18_aop/Aop03CustomAnnotations).
    //
    // Co porada wie o wywołaniu? Parametr JoinPoint (punkt złączenia):
    //   joinPoint.getSignature().getName()  → "findPrice"           (nazwa metody)
    //   joinPoint.getArgs()                 → [SPO-001]             (argumenty)
    //   joinPoint.getTarget()               → prawdziwy obiekt ProductCatalog
    //   joinPoint.getThis()                 → proxy (ProductCatalog$$SpringCGLIB$$0)
    // @Around dostaje ProceedingJoinPoint — JoinPoint z dodatkową metodą proceed().
    //
    // DOBRA PRAKTYKA: wybieraj NAJSŁABSZĄ poradę, która wystarcza (@Before/@AfterReturning zamiast @Around).
    //   Dlaczego? Zalecenie z dokumentacji Springa: @Before nie może zapomnieć wywołać metody ani zgubić jej wyniku,
    //   a jej kod od razu mówi, KIEDY biegnie. @Around zostaw na sytuacje, gdy naprawdę potrzebujesz „przed i po”
    //   w jednym miejscu (pomiar czasu, ponawianie, zamiana wyniku).
    //
    // Test: Aop01AspectsTest#adviceOrderOnSuccess, Aop01AspectsTest#adviceOrderOnException

    // =================================================================================================
    // 4. @Around i proceed() — porada, która może wszystko (i wszystko zepsuć)
    // =================================================================================================
    //
    //   @Around("findPrice()")
    //   public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
    //       ... przed ...
    //       Object result = joinPoint.proceed();        ← TU wykonuje się prawdziwa metoda (i porady „głębiej”)
    //       ... po ...
    //       return result;                              ← wynik MUSI wrócić do wołającego
    //   }
    //
    // @Around może: wykonać kod przed i po; zmierzyć czas; złapać wyjątek; zwrócić INNY wynik; wywołać metodę
    // kilka razy (ponawianie) albo wcale; podmienić argumenty: joinPoint.proceed(new Object[] {...}).
    // Pokaz podmiany argumentów: SkuNormalizingAspect — orderService.placeOrder(" spo-001 ", 2) zwraca
    // „Zamówienie przyjęte: 2 x SPO-001”, bo aspekt oczyścił SKU, zanim metoda je zobaczyła.
    //
    // DOBRA PRAKTYKA: nie ukrywaj w aspektach logiki biznesowej (zmiany argumentów, rabatów, statusów).
    //   Dlaczego? Czytając OrderService, nikt nie zgadnie, że SKU jest po drodze zmieniane — kod „robi coś innego,
    //   niż jest napisane”. Aspekty są dla spraw TECHNICZNYCH; reguły biznesowe pisz jawnie w serwisie.
    //
    // PUŁAPKA: @Around bez proceed() (BrokenAroundAspect na StockReport). Dlaczego groźne? Metoda po prostu się
    //   NIE wykonuje, a wołający dostaje to, co zwróciła porada:
    //     stockReport.title()        → null (bez wyjątku! błąd wyjdzie dużo dalej, np. jako NullPointerException)
    //     stockReport.productCount() → wyjątek (typ zwracany int nie przyjmie null):
    //       AopInvocationException: Null return value from advice does not match primitive return type for:
    //       public int s18_aop.aop01_aspects.StockReport.productCount()
    //     metoda void                → nic się nie dzieje i nikt tego nie zauważy
    //   To samo daje „joinPoint.proceed();” bez „return” — metoda się wykona, ale jej wynik przepadnie.
    //
    // Test: Aop01AspectsTest#aroundCanReplaceArguments, Aop01AspectsTest#aroundWithoutProceed

    // =================================================================================================
    // 5. WYRAŻENIA PUNKTÓW PRZECIĘCIA (PointcutDemoAspect, ShopPointcuts)
    // =================================================================================================
    //
    // execution — wzorzec sygnatury metody (najczęściej używane):
    //   execution( [modyfikator] typZwracany [pakiet.Klasa.]nazwa(parametry) [throws ...] )
    //   execution(* s18_aop.aop01_aspects.ProductCatalog.find*(..))
    //             │  │                                  │     └─ (..) dowolne parametry; (String) dokładnie jeden String;
    //             │  │                                  │        (String, ..) String i potem cokolwiek; () bez parametrów
    //             │  │                                  └─ nazwa metody, * = dowolny ciąg znaków
    //             │  └─ pełna nazwa klasy; s18_aop..* = każda klasa w pakiecie s18_aop i jego podpakietach
    //             └─ * = dowolny typ zwracany
    //   Modyfikator można pominąć — wtedy pasują też metody protected i pakietowe (Aop02Proxies, sekcja 5).
    //
    //   wyrażenie                                  | wybiera
    //   -------------------------------------------+-----------------------------------------------------------
    //   within(s18_aop.aop01_aspects.OrderService) | wszystkie metody zadeklarowane w klasie
    //   within(s18_aop.aop01_aspects..*)           | wszystkie metody klas z pakietu i podpakietów
    //   @annotation(pakiet.Adnotacja)              | metody oznaczone adnotacją (s18_aop/Aop03CustomAnnotations)
    //   bean(orderService), bean(*Service)         | beany o pasującej NAZWIE (tylko Spring AOP, nie AspectJ)
    //   args(sku)                                  | metody o pasujących argumentach + wiązanie wartości z parametrem
    //   A && B,  A || B,  !A                       | łączenie: i, lub, nie
    //
    // Wyniki z testu sekcji 5 (dziennik z przedrostkiem „PC”):
    //   catalog.findPrice("SPO-001")              → execution(find*) i args(sku)
    //   catalog.findCheaperThan(50)               → tylko execution(find*) — args(sku) chce JEDNEGO Stringa
    //   catalog.stockValue()                      → tylko @annotation(RolesAllowed)
    //   orderService.placeOrder("SPO-001", 1)     → within(OrderService) i bean(orderService), a ponieważ
    //                                               placeOrder woła katalog PRZEZ PROXY — także findStock łapią
    //                                               execution(find*) i args(sku)
    //
    // @Pointcut — wyrażenie wielokrotnego użytku z nazwą:
    //   w tej samej klasie:   @Pointcut("execution(...)") void findPrice() {}   →   @Before("findPrice()")
    //   w innej klasie:       ShopPointcuts (zwykła klasa, nie bean)          →   pełna nazwa:
    //                         @Before("s18_aop.aop01_aspects.ShopPointcuts.catalogFinders()")
    //
    // PUŁAPKA: import nie działa w wyrażeniach. Dlaczego groźne? Wyrażenie to tekst w cudzysłowie — kompilator Javy
    //   go nie sprawdza, a AspectJ nie widzi importów z pliku. PointcutDemoAspect#importTrap ma
    //   „import jakarta.annotation.security.RolesAllowed;” i wyrażenie @annotation(RolesAllowed) — porada NIGDY
    //   się nie uruchamia, bez błędu i bez ostrzeżenia. Krótka nazwa działa tylko dla typów z pakietu aspektu
    //   i z java.lang. Zawsze pisz PEŁNĄ nazwę z pakietem.
    //
    // PUŁAPKA: za szeroki punkt przecięcia łapie beany SAMEGO Springa (BroadDeprecatedAspect). Dlaczego groźne?
    //   Spring sprawdza każdy punkt przecięcia na KAŻDYM beanie kontekstu — także na beanach Boota. Wyrażenie
    //   @annotation(java.lang.Deprecated) bez zawężenia złapało bean Boota z przestarzałą metodą, który jest klasą
    //   final — a takiej klasy CGLIB nie umie opakować. Aplikacja NIE STARTUJE (komunikat z testu):
    //     BeanCreationException: Error creating bean with name 'org.springframework.boot.autoconfigure.AutoConfigurationPackages':
    //     Could not generate CGLIB subclass of class org.springframework.boot.autoconfigure.AutoConfigurationPackages$BasePackages:
    //     Common causes of this problem include using a final class or a non-visible class
    //     (na dnie: IllegalArgumentException: Cannot subclass final class ...AutoConfigurationPackages$BasePackages)
    //   A gdyby klasa nie była final, aspekt po cichu działałby także na cudzych beanach.
    //
    // DOBRA PRAKTYKA: każdy punkt przecięcia zawężaj do SWOICH pakietów: within(s18_aop.aop01_aspects..*) && ...
    //   (tak robi ShopPointcuts.managerOnly). Dlaczego? Aspekt robi dokładnie to, co zamierzasz, nie zależy od tego,
    //   jakie beany przyniosą biblioteki, a Spring nie traci czasu na sprawdzanie obcych klas.
    //
    // Test: Aop01AspectsTest#pointcutExpressions, Aop01AspectsTest#annotationPointcutNeedsFullName,
    //       Aop01AspectsTest#tooBroadPointcutBreaksStartup

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Aspekt = {@code @Aspect} + {@code @Component} (albo {@code @Bean}). Bez beana porady po cichu nie działają.
     *   • Boot włącza AOP sam (AopAutoConfiguration, starter spring-boot-starter-aspectj; dawny -aop zniknął w Boot 4).
     *   • Porady: @Before, @AfterReturning (wynik), @AfterThrowing (wyjątek), @After (zawsze), @Around (wszystko).
     *   • Kolejność w jednym aspekcie: Around → Before → metoda → AfterReturning/AfterThrowing → After → Around.
     *   • @Around: {@code return joinPoint.proceed();} — bez proceed metoda się nie wykona, bez return wynik przepada.
     *   • Wyrażenia: execution (sygnatura), within (klasa/pakiet), @annotation, bean (nazwa), args (argumenty),
     *     łączone {@code &&}, {@code ||}, {@code !}; @Pointcut nadaje wyrażeniu nazwę.
     *   • W wyrażeniach PEŁNE nazwy klas — importy nie działają.
     *   • Zawsze zawężaj: {@code within(twój.pakiet..*) && ...} — inaczej łapiesz beany Springa.
     *   • Wybieraj najsłabszą poradę, która wystarcza; logika biznesowa nie należy do aspektów.
     *
     * PYTANIA KONTROLNE:
     *   1. Co to jest sprawa przekrojowa? Podaj trzy przykłady. Dlaczego aspekt jest lepszy niż kopiowanie kodu?
     *   2. Co się stanie? Klasa z samym {@code @Aspect} (bez {@code @Component}) w skanowanym pakiecie, z poradą
     *      {@code @Before} na findStock. Wywołujesz findStock — czy porada się wykona? Czy będzie błąd przy starcie?
     *   3. Co się stanie? W jakiej kolejności pojawią się wpisy LoggingAspect dla findPrice("XYZ-999")?
     *      Czy wykona się @AfterReturning? A @After?
     *   4. ZNAJDŹ BŁĄD: porada mierząca czas
     *        @Around("...")
     *        public Object time(ProceedingJoinPoint jp) throws Throwable {
     *            long start = System.nanoTime();
     *            jp.proceed();
     *            log(System.nanoTime() - start);
     *            return null;
     *        }
     *      Co dostanie wołający metodę zwracającą String? A metodę zwracającą int?
     *   5. Czym różnią się execution(...), within(...), bean(...) i @annotation(...)? Które wyrażenie wybierze
     *      wszystkie metody klas z pakietu com.shop.service i jego podpakietów?
     *   6. ZNAJDŹ BŁĄD: {@code import com.shop.audit.Audited;} i porada {@code @Before("@annotation(Audited)")} w klasie
     *      z pakietu com.shop.aspects. Metody z {@code @Audited} nie są logowane, w logach nic nie ma. Dlaczego?
     *   7. Co się stanie? Do aplikacji tej lekcji dokładasz aspekt {@code @Before("@annotation(java.lang.Deprecated)")}
     *      bez żadnego zawężenia. Jak to naprawić?
     *   8. Dlaczego dokumentacja Springa radzi wybierać najsłabszą poradę, która wystarcza?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Aop01AspectsExercisesTest (bez Springa: AspectJProxyFactory zakłada aspekt ręcznie)
    // =================================================================================================

    /*
     * ĆWICZENIE 1 (łatwe): otwórz Aop01Exercise1Aspect i uzupełnij poradę {@code @Before}: dla każdej metody find*
     * dopisz do listy wpis „wywołanie: findPrice[SPO-001]”. Podpowiedź: joinPoint.getSignature().getName()
     * i Arrays.toString(joinPoint.getArgs()).
     *
     * ĆWICZENIE 3 (średnie): otwórz Aop01Exercise3Aspect i uzupełnij poradę {@code @Around}: policz wywołania każdej
     * metody (counts.merge(nazwa, 1, Integer::sum)) i ZWRÓĆ wynik metody. Podpowiedź: {@code return joinPoint.proceed();}
     * — test sprawdza też, czy findPrice zwraca prawdziwą cenę.
     *
     * ĆWICZENIE 4 (trudniejsze): otwórz Aop01Exercise4Aspect. Gdy metoda katalogu rzuci IllegalArgumentException,
     * porada {@code @AfterThrowing} ma rzucić ProductNotFoundException z komunikatem
     * „Brak produktu w katalogu (Nieznany SKU: XYZ-999)” i przyczyną (cause) = oryginalny wyjątek.
     * Podpowiedź: porada nie może połknąć wyjątku, ale jeśli sama rzuci inny — to on poleci do wołającego.
     */

    /**
     * ĆWICZENIE 2 (średnie): zwróć wyrażenie punktu przecięcia, które wybiera TYLKO metody klasy ProductCatalog,
     * których nazwa zaczyna się od „find” i które mają DOKŁADNIE jeden parametr typu String
     * (findPrice, findStock, findName — ale nie findCheaperThan(BigDecimal) ani stockValue()).
     * Podpowiedź: zacznij od {@code execution(* s18_aop.aop01_aspects.ProductCatalog.find*(..))} i zmień nawias.
     */
    public static String exercise2Pointcut() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zadanie, które dotyczy wielu metod/klas naraz, a nie jednej funkcji biznesowej: logowanie, pomiar czasu,
     *      audyt, uprawnienia, transakcje, ponawianie. Aspekt trzyma ten kod w jednym miejscu: zmiana w jednym pliku,
     *      logika biznesowa czysta, nie da się „zapomnieć” o metodzie, którą obejmuje punkt przecięcia.
     *   2. Porada się NIE wykona i NIE będzie żadnego błędu. {@code @Aspect} nie jest stereotypem, więc skanowanie
     *      nie tworzy beana, a Spring szuka porad tylko w beanach. Sprawdzone testem
     *      Aop01AspectsTest#aspectWithoutComponentIsIgnored (ForgottenAspect).
     *   3. LOG @Around: przed → LOG @Before: findPrice [XYZ-999] → METODA findPrice(XYZ-999) →
     *      LOG @AfterThrowing: Nieznany SKU: XYZ-999 → LOG @After: findPrice zakończona →
     *      LOG @Around: wyjątek IllegalArgumentException. @AfterReturning NIE (metoda nie zakończyła się normalnie),
     *      @After TAK (biegnie zawsze). Wyjątek dociera do wołającego. Test: Aop01AspectsTest#adviceOrderOnException.
     *   4. Brakuje „return” przed jp.proceed() — porada zwraca null. Metoda String: wołający dostaje null zamiast
     *      wyniku. Metoda int: AopInvocationException „Null return value from advice does not match primitive return
     *      type for: ...”. Poprawka: {@code Object result = jp.proceed(); ...; return result;}. To samo zachowanie
     *      co w teście Aop01AspectsTest#aroundWithoutProceed.
     *   5. execution — wzorzec sygnatury (typ zwracany, klasa, nazwa, parametry); within — wszystkie metody
     *      zadeklarowane w klasie/pakiecie; bean — beany o danej nazwie (tylko Spring AOP); @annotation — metody
     *      z adnotacją. Cały pakiet z podpakietami: {@code within(com.shop.service..*)}.
     *   6. Wyrażenie to tekst — AspectJ nie widzi importów, a krótka nazwa działa tylko dla typów z pakietu aspektu
     *      (com.shop.aspects) i z java.lang. „Audited” nie wskazuje com.shop.audit.Audited, więc porada nie pasuje do
     *      niczego (bez błędu). Poprawka: {@code @Before("@annotation(com.shop.audit.Audited)")}. Sprawdzone testem
     *      Aop01AspectsTest#annotationPointcutNeedsFullName (PointcutDemoAspect#importTrap).
     *   7. Aplikacja nie wystartuje: BeanCreationException „Could not generate CGLIB subclass of class
     *      org.springframework.boot.autoconfigure.AutoConfigurationPackages$BasePackages: Common causes of this problem
     *      include using a final class or a non-visible class” — wyrażenie złapało bean Boota (klasa final
     *      z metodą @Deprecated). Naprawa: zawęzić do swoich pakietów, np.
     *      {@code within(s18_aop.aop01_aspects..*) && @annotation(java.lang.Deprecated)}. Test:
     *      Aop01AspectsTest#tooBroadPointcutBreaksStartup.
     *   8. Mniej możliwości = mniej sposobów na błąd: @Before nie może zapomnieć proceed() ani zgubić wyniku, a sama
     *      nazwa porady mówi, kiedy biegnie. @Around daje pełną kontrolę, więc też pełną odpowiedzialność.
     */
    // </editor-fold>
}
