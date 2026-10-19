package s01_core_ioc.ioc03_stereotypes;

import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * <pre>
 * TEMAT: Stereotypy — @Component, @Service, @Repository, @Controller i nazwy beanów
 *        (stereotype = stereotyp, adnotacja mówiąca, jaką rolę pełni klasa; component = komponent;
 *         bean name = nazwa beana; meta-annotation = metaadnotacja, czyli adnotacja położona na adnotacji)
 *
 * W SKRÓCIE:
 *   Spring tworzy beany z klas oznaczonych @Component. Adnotacje @Service, @Repository i @Controller to @Component
 *   pod inną nazwą — skanowanie traktuje je tak samo, a nazwa mówi czytelnikowi, w której warstwie leży klasa
 *   (logika, dane, wejście). @Service nic więcej nie robi. @Controller i @Repository też same nic nie robią, ale są
 *   ZNACZNIKAMI dla innych modułów Springa: Spring MVC obsługuje żądania HTTP tylko w klasach z @Controller,
 *   a tłumaczenie wyjątków bazy danych działa tylko w klasach z @Repository (gdy w kontekście jest odpowiedni
 *   postprocesor). W tym module nie ma ani weba, ani bazy, więc tu wszystkie cztery działają identycznie.
 *   Każdy bean ma nazwę: domyślnie to nazwa klasy z małej litery
 *   (wyjątek: gdy pierwsze DWIE litery są wielkie, nazwa zostaje bez zmian). Możesz też podać nazwę jawnie albo
 *   stworzyć własny stereotyp, np. @ShopComponent.
 *
 * ANALOGIA: identyfikatory w firmie.
 *   Każdy pracownik ma identyfikator (@Component). Kolor smyczy mówi, w jakim dziale pracuje: księgowość (@Service),
 *   magazyn (@Repository), recepcja (@Controller). Wejście do budynku (skanowanie) działa tak samo dla wszystkich —
 *   smycz służy ludziom, a nie bramce. Ale niektóre drzwi w środku sprawdzają kolor smyczy: do okienka dla
 *   klientów wpuszczą tylko recepcję (@Controller w aplikacji webowej). Nazwisko na identyfikatorze (nazwa beana)
 *   jest domyślnie wzięte z dokumentów,
 *   ale możesz poprosić o inne.
 *
 * JAK TO DZIAŁA:
 *   @SpringBootApplication → włącza skanowanie komponentów (component scan) pakietu klasy startowej i jego podpakietów
 *     1. Spring przegląda klasy i szuka adnotacji @Component — także pośrednio (metaadnotacje):
 *        @Service jest oznaczona @Component, więc klasa z @Service też się łapie.
 *     2. Dla każdej znalezionej klasy tworzy definicję beana. Nazwa: wartość adnotacji, a gdy jej nie ma —
 *        nazwa klasy z małej litery (ta sama reguła co java.beans.Introspector.decapitalize; Spring 7 liczy ją
 *        metodą StringUtils.uncapitalizeAsProperty).
 *     3. Klasa bez adnotacji (NotAComponent) jest pomijana.
 *   Co robią znaczniki @Controller i @Repository — dopiero w obecności innych modułów:
 *     • @Controller: Spring MVC (aplikacja webowa) szuka metod obsługi żądań TYLKO w klasach z @Controller
 *       (także @RestController, który ją zawiera). Klasa z @Component i metodami @RequestMapping (mapowanie żądań)
 *       nie stanie się punktem końcowym (endpoint) — więcej w s04_web_rest/Rest01Controllers.
 *     • @Repository: tłumaczenie wyjątków bazy danych na wyjątki Springa (persistence exception translation).
 *       Działa, gdy w kontekście jest PersistenceExceptionTranslationPostProcessor (postprocesor z biblioteki
 *       spring-tx): bean z @Repository dostaje wtedy pośrednika (proxy), który zamienia wyjątek bazy na
 *       DataAccessException. Potrzebny jest jeszcze tłumacz wyjątków danej technologii (PersistenceExceptionTranslator)
 *       — bez niego pośrednik przepuszcza wyjątek bez zmian. Spring Boot rejestruje postprocesor sam
 *       (autokonfiguracja), gdy dołączysz starter bazy danych, np. spring-boot-starter-data-jpa. W tym module go
 *       nie ma, więc CatalogRepository jest zwykłym obiektem, bez pośrednika (test sekcji 1).
 *
 * SŁÓWKA:
 *   component scan = skanowanie komponentów; layer = warstwa; service = serwis (logika biznesowa);
 *   repository = repozytorium (dostęp do danych); controller = kontroler (wejście, obsługa żądań);
 *   retention = zachowanie adnotacji (RUNTIME = widoczna w czasie działania); target = cel adnotacji;
 *   explicit = jawny; default = domyślny; capitalize = zamienić pierwszą literę na wielką; uncapitalize = zamienić
 *   pierwszą literę na małą; post processor = postprocesor (obiekt, który obrabia beany po ich utworzeniu);
 *   persistence = utrwalanie danych (zapis w bazie); translator = tłumacz.
 *
 * ZOBACZ TEŻ: s01_core_ioc/Ioc01Container (czym jest bean), s01_core_ioc/Ioc02ConstructorInjection (jak beany dostają zależności),
 *             s04_web_rest/Rest01Controllers (@Controller i @RestController w aplikacji webowej),
 *             t19_annotations_reflection/Annotations01BuiltIn (adnotacje wbudowane),
 *             t19_annotations_reflection/Annotations02Custom (własne adnotacje, @Retention, @Target),
 *             t34_toward_spring/Spring02Layers (warstwy aplikacji), t06_oop_basics/Oop08PackagesAccess (pakiety).
 * </pre>
 */
public final class Ioc03Stereotypes {

    private Ioc03Stereotypes() {
    }

    // =================================================================================================
    // 1. @Service, @Repository, @Controller TO ODMIANY @Component
    // =================================================================================================
    //
    // Wystarczy zajrzeć do kodu adnotacji: @Service jest oznaczona @Component (metaadnotacja). Spring szuka
    // @Component również "przez" inne adnotacje — AnnotatedElementUtils.hasAnnotation(CatalogService.class, Component.class)
    // (annotated element utils = narzędzia do adnotacji na elementach kodu; has annotation = ma adnotację)
    // zwraca true, choć na klasie stoi tylko @Service.
    //
    // PUŁAPKA: myślenie "@Service coś robi, a @Component nie". Dlaczego to błąd? @Service niczego nie dodaje —
    //   to etykieta dla czytelnika i narzędzi. Odwrotny błąd też się zdarza: "skoro to wszystko @Component, to
    //   wszędzie mogę pisać @Component". W aplikacji webowej zamiana @Controller na @Component sprawia, że Spring MVC
    //   przestaje widzieć metody obsługi żądań (punkty końcowe znikają), a zamiana @Repository na @Component wyłącza
    //   tłumaczenie wyjątków bazy. Te dwa znaczniki czytają inne moduły Springa (JAK TO DZIAŁA).

    // =================================================================================================
    // 2. CO KTÓRA OZNACZA — warstwy aplikacji
    // =================================================================================================
    //
    //   @Controller  — wejście (przyjmuje żądania)        → CatalogController
    //   @Service     — logika biznesowa                    → CatalogService, ShippingService
    //   @Repository  — dostęp do danych                    → CatalogRepository
    //   @Component   — reszta (pomocnicze, bez warstwy)    → PriceFormatter, URLShortener
    //
    // Ruch idzie w jedną stronę: kontroler → serwis → repozytorium. Test sekcji 2 sprawdza cały łańcuch.
    //
    // DOBRA PRAKTYKA: wybieraj najbardziej konkretny stereotyp. Dlaczego? Nazwa adnotacji dokumentuje architekturę,
    //   a narzędzia (i Ty za pół roku) od razu wiedzą, gdzie jest logika, a gdzie dane.

    // =================================================================================================
    // 3. DOMYŚLNA NAZWA BEANA
    // =================================================================================================
    //
    //   CatalogService  → "catalogService"      (pierwsza litera mała)
    //   PriceFormatter  → "priceFormatter"
    //   URLShortener    → "URLShortener"        (pierwsze DWIE litery wielkie → bez zmian!)
    //
    // Reguła (ta sama co java.beans.Introspector.decapitalize z JDK; Spring 7 liczy ją własną metodą
    // StringUtils.uncapitalizeAsProperty): gdy nazwa ma co najmniej 2 znaki i pierwsze dwa są wielkimi
    // literami, zostaje bez zmian; w przeciwnym razie pierwsza litera jest zamieniana na małą.
    //
    // PUŁAPKA: szukanie beana klasy URLShortener pod nazwą "uRLShortener" albo "urlShortener". Dlaczego to nie działa?
    //   Nazwy są rozróżniane co do wielkości liter i wyjątek z dwiema wielkimi literami zachowuje oryginał.
    //   Po typie (getBean(URLShortener.class)) problemu nie ma — to kolejny powód, by wołać po typie.

    // =================================================================================================
    // 4. JAWNA NAZWA — @Component("taxCalculator")
    // =================================================================================================
    //
    // Wartość adnotacji zastępuje nazwę domyślną: VatCalculator → "taxCalculator". Stara nazwa "vatCalculator"
    // przestaje istnieć.
    //
    // DOBRA PRAKTYKA: jawną nazwę podawaj tylko, gdy naprawdę jej potrzebujesz (np. dwie klasy o tej samej nazwie
    //   w różnych pakietach). Dlaczego? Domyślne nazwy są przewidywalne, a własne łatwo rozjechać z kodem,
    //   który pobiera bean po nazwie.

    // =================================================================================================
    // 5. WŁASNY STEREOTYP — @ShopComponent
    // =================================================================================================
    //
    // @Component + @Target(TYPE) + @Retention(RUNTIME) na własnej adnotacji = nowy stereotyp. Klasa LoyaltyDiscount
    // z @ShopComponent jest skanowana i ma nazwę "loyaltyDiscount". @Retention(RUNTIME) jest ważne: bez niego
    // adnotacja nie byłaby widoczna dla Springa w czasie działania.
    //
    // DOBRA PRAKTYKA: własny stereotyp dodawaj, gdy cała grupa klas ma wspólną rolę i chcesz ją nazwać.
    //   Dlaczego? Czytelność i możliwość filtrowania (ćwiczenie 3 wyszukuje beany po adnotacji). Nie mnóż ich
    //   bez potrzeby — standardowe cztery zwykle wystarczą.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @Service, @Repository, @Controller = @Component z innym znaczeniem dla czytelnika (metaadnotacja);
     *     skanowanie traktuje je tak samo.
     *   • @Controller: w Spring MVC tylko takie klasy (i @RestController) obsługują żądania HTTP.
     *   • @Repository: tłumaczenie wyjątków bazy, ale tylko gdy działa PersistenceExceptionTranslationPostProcessor
     *     (Boot dodaje go przy starterach bazy danych); bez niego @Repository = @Component.
     *   • Skanowanie robi beany z klas z @Component (też pośrednio) w skanowanym pakiecie; zwykłą klasę pomija.
     *   • Nazwa domyślna: nazwa klasy z małej litery; przy dwóch wielkich na początku — bez zmian (URLShortener).
     *   • Jawna nazwa: @Component("nazwa") — wtedy stara nazwa domyślna nie istnieje.
     *   • Własny stereotyp: adnotacja z @Component i @Retention(RUNTIME).
     *   • Pobieraj bean po typie; po nazwie tylko gdy musisz (wielkość liter ma znaczenie).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego klasa oznaczona tylko @Service jest skanowana tak samo jak klasa z @Component?
     *   2. Co się stanie? Pobierasz {@code context.getBean("uRLShortener")}. Jaka jest właściwa nazwa tego beana?
     *   3. ZNAJDŹ BŁĄD: {@code context.getBean("vatCalculator")} dla klasy {@code @Component("taxCalculator") VatCalculator}.
     *   4. Co się stanie? {@code context.getBean(NotAComponent.class)} — klasa leży w skanowanym pakiecie, ale nie ma adnotacji.
     *   5. Czym @Repository różni się technicznie od @Service?
     *   6. Co się stanie? Pobierasz {@code context.getBean("catalogservice")} (wszystko małymi literami).
     *   7. Kiedy ma sens własny stereotyp, a kiedy wystarczy @Component?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ioc03StereotypesExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć domyślną nazwę beana dla prostej nazwy klasy. "CatalogService" → "catalogService",
     * "URLShortener" → "URLShortener", "X" → "x", "Ab" → "ab", pusty tekst → pusty tekst.
     * Podpowiedź: sprawdź, czy pierwsze dwa znaki są wielkimi literami (Character.isUpperCase); jeśli tak — zwróć bez zmian.
     * Nie używaj Introspector.decapitalize — napisz regułę sam.
     */
    static String exercise1(String simpleClassName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć true, jeśli klasa jest komponentem Springa — ma @Component bezpośrednio albo
     * pośrednio (przez @Service, @Repository, @ShopComponent...). Podpowiedź:
     * AnnotatedElementUtils.hasAnnotation(typ, Component.class).
     */
    static boolean exercise2(Class<?> type) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): z kontekstu zwróć POSORTOWANE nazwy beanów oznaczonych @Service.
     * Podpowiedź: context.getBeanNamesForAnnotation(Service.class) zwraca tablicę bez gwarancji kolejności.
     */
    static List<String> exercise3(ApplicationContext context) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Bo @Service jest sama oznaczona @Component (metaadnotacja), a Spring szuka @Component także pośrednio.
     *      Sprawdzone testem w Ioc03StereotypesTest (AnnotatedElementUtils).
     *   2. NoSuchBeanDefinitionException — nazwa tego beana to "URLShortener" (dwie wielkie litery na początku, więc
     *      nazwa zostaje bez zmian). Sprawdzone testem.
     *   3. Jawna nazwa zastąpiła domyślną, więc "vatCalculator" nie istnieje (NoSuchBeanDefinitionException);
     *      bean nazywa się "taxCalculator". Sprawdzone testem.
     *   4. NoSuchBeanDefinitionException — bez adnotacji klasa nie jest beanem. Sprawdzone testem.
     *   5. Sama adnotacja — niczym, obie są @Component. Różnica pojawia się, gdy w kontekście działa
     *      PersistenceExceptionTranslationPostProcessor (w Boocie: po dołączeniu startera bazy danych): bean
     *      z @Repository dostaje wtedy pośrednika (proxy), który z pomocą tłumacza wyjątków zamienia wyjątki bazy na
     *      DataAccessException; bean z @Service — nie. W tym module postprocesora nie ma, więc różnicy nie ma żadnej
     *      (test sekcji 1: CatalogRepository nie jest proxy). Sprawdzone uruchomieniem (z postprocesorem: proxy
     *      i przetłumaczony wyjątek tylko dla @Repository).
     *   6. NoSuchBeanDefinitionException — nazwy rozróżniają wielkość liter ("catalogService" a nie "catalogservice").
     *      Sprawdzone testem.
     *   7. Własny stereotyp, gdy grupa klas ma wspólną rolę, którą chcesz nazwać i filtrować (np. po adnotacji);
     *      w pozostałych przypadkach @Component lub standardowe stereotypy wystarczą.
     */
    // </editor-fold>
}
