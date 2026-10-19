package s22_extras.ext06_native_image;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

import java.util.List;

/**
 * <pre>
 * TEMAT: GraalVM native image — obraz natywny: co daje, co kosztuje; przetwarzanie AOT Springa (spring-boot:process-aot),
 *        RuntimeHints / RuntimeHintsRegistrar i ich testy bez GraalVM; jak zbudować obraz (opis)
 *        (native image = obraz natywny — program skompilowany do kodu maszynowego, bez JVM; AOT = ahead-of-time,
 *        „przed czasem”, czyli przy budowaniu, a nie w czasie działania; hint = podpowiedź)
 *
 * W SKRÓCIE:
 *   • GraalVM kompiluje aplikację z bibliotekami do JEDNEGO pliku wykonywalnego. Start w dziesiątki milisekund, mniej
 *     pamięci. Koszt: kompilacja trwa minuty i zjada gigabajty RAM, a program żyje w „świecie zamkniętym”.
 *   • Świat zamknięty: do programu trafia tylko kod, który kompilator WIDZI jako osiągalny. Refleksja (Class.forName),
 *     zasoby z classpath, dynamiczne proxy — trzeba zgłosić podpowiedziami (RuntimeHints).
 *   • Spring robi większość za Ciebie: przetwarzanie AOT przy budowaniu generuje kod Javy z definicjami beanów
 *     i plik reachability-metadata.json z podpowiedziami. Ty dopisujesz podpowiedzi tylko dla SWOJEJ refleksji.
 *   • Podpowiedzi testujesz zwykłym JUnitem: RuntimeHintsPredicates — bez GraalVM.
 *
 * ANALOGIA: pakowanie walizki na rejs dookoła świata.
 *   JVM to podróż z domem pod ręką: czegoś brakuje — idziesz i bierzesz (ładujesz klasę w czasie działania). Obraz
 *   natywny to walizka spakowana przed rejsem: lekka i od razu gotowa, ale czego nie spakowałeś, tego na statku nie ma.
 *   Kompilator pakuje to, co widzi na liście (kod osiągalny). Lista podpowiedzi to karteczka „zabierz też ładowarkę
 *   z szuflady” — rzecz, której nie widać, dopóki nie będzie potrzebna.
 *
 * JAK TO DZIAŁA:
 *   ./mvnw -Pnative native:compile   (profil native z spring-boot-starter-parent)
 *     1. compile                 — zwykłe .class
 *     2. spring-boot:process-aot — Spring „startuje na niby” kontekst (refreshForAotProcessing, BEZ tworzenia beanów),
 *                                  sprawdza warunki @Conditional i generuje do target/spring-aot/main:
 *                                  sources/ ...__BeanDefinitions.java, ...__ApplicationContextInitializer.java,
 *                                  classes/ klasy proxy CGLIB dla @Configuration,
 *                                  resources/META-INF/native-image/... reachability-metadata.json, native-image.properties
 *     3. native:compile          — GraalVM: analiza osiągalności od main + podpowiedzi → plik wykonywalny
 *   Na starcie obrazu Spring nie skanuje pakietów i nie czyta adnotacji — wykonuje wygenerowany kod.
 *
 * SŁÓWKA:
 *   closed world = świat zamknięty; reachability = osiągalność; reachability metadata = metadane osiągalności;
 *   reflection = refleksja (dostęp do klas/metod po nazwie w czasie działania); resource = zasób (plik z classpath);
 *   JDK proxy = proxy JDK (obiekt tworzony dynamicznie dla interfejsu); registrar = rejestrator;
 *   predicate = predykat (warunek tak/nie); buildpacks = paczki budujące (obraz kontenera bez Dockerfile);
 *   JIT = just-in-time (kompilacja w czasie działania — w JVM, nie w obrazie natywnym).
 *
 * ZOBACZ TEŻ: t19_annotations_reflection/Annotations03ReflectionBasics (refleksja),
 *             t19_annotations_reflection/Annotations06DynamicProxy (proxy JDK), s19_deploy/Dep01ExecutableJar (jar),
 *             s19_deploy/Dep02Docker (obrazy kontenerów), s19_deploy/Dep04StartupAndShutdown (czas startu),
 *             s22_extras/Ext02Messaging (obraz apache/kafka-native — Kafka jako program natywny i jego pułapka).
 * </pre>
 */
public final class Ext06NativeImage {

    private Ext06NativeImage() {
    }

    // =================================================================================================
    // 1. CO DAJE, CO KOSZTUJE — KIEDY PO TO SIĘGNĄĆ
    // =================================================================================================
    //
    // ZYSK:  start w dziesiątkach milisekund zamiast sekund, mniej pamięci (brak JIT i metadanych klas „na zapas”),
    //        jeden plik bez JVM → mały obraz kontenera. Przykład z życia: apache/kafka-native to Kafka skompilowana
    //        GraalVM-em (oficjalny obraz, oznaczony jako eksperymentalny).
    // KOSZT: kompilacja trwa minuty i potrzebuje kilku GB RAM; brak JIT, więc przy długiej pracy szczytowa wydajność
    //        bywa niższa niż na rozgrzanej JVM; świat zamknięty (refleksja, zasoby, proxy, serializacja → podpowiedzi);
    //        biblioteki bez metadanych mogą nie działać; inne narzędzia do profilowania i debugowania; mniejsza
    //        dojrzałość — kontener apache/kafka-native:4.2.0 raz padł nam przy starcie z „SegfaultHandler caught
    //        a segfault” (dlatego testy Ext02Messaging używają zwykłego apache/kafka na JVM).
    //
    // Kiedy warto: funkcje serverless i „skalowanie do zera” (start na żądanie), narzędzia CLI, dużo małych instancji.
    // Kiedy nie: zwykły serwis działający tygodniami (JVM i tak się rozgrzeje), dużo „magicznych” bibliotek,
    // zespół bez czasu na utrzymanie podpowiedzi.
    //
    // DOBRA PRAKTYKA: zanim sięgniesz po obraz natywny, zmierz, czy problemem naprawdę jest start. Dlaczego? Na JVM są
    //   tańsze sposoby: leniwa inicjalizacja, mniej starterów, CDS i pamięć podręczna AOT samego JDK (Project Leyden)
    //   — bez świata zamkniętego.

    // =================================================================================================
    // 2. PRZETWARZANIE AOT SPRINGA — CO POWSTAJE PRZY BUDOWANIU (Ext06NativeImageAotTest)
    // =================================================================================================
    //
    // Uruchomienie dla tej lekcji (sprawdzone; moduł ma 6 aplikacji, więc trzeba wskazać klasę):
    //   ./mvnw -pl s22-extras compile spring-boot:process-aot -Dspring-boot.run.main-class=s22_extras.ext06_native_image.Ext06Application
    // Wynik w s22-extras/target/spring-aot/main (276 plików):
    //   sources/s22_extras/ext06_native_image/Ext06Application__ApplicationContextInitializer.java
    //   sources/s22_extras/ext06_native_image/PromotionLoader__BeanDefinitions.java:
    //       beanDefinition.setInstanceSupplier(PromotionLoader::new);        ← zwykłe wywołanie, BEZ refleksji
    //   classes/s22_extras/ext06_native_image/Ext06Application$$SpringCGLIB$$0.class   ← proxy @Configuration
    //   resources/META-INF/native-image/springlearning/s22-extras/reachability-metadata.json (ok. 220 KB) z m.in.:
    //       {"type": "s22_extras.ext06_native_image.FixedDiscountRule", "allDeclaredConstructors": true}
    //       {"glob": "ext06/promocje.txt"}
    //   resources/META-INF/native-image/springlearning/s22-extras/native-image.properties:
    //       Args = -H:Class=s22_extras.ext06_native_image.Ext06Application --no-fallback
    //
    // Ten sam mechanizm w teście (Ext06NativeImageAotTest): new ApplicationContextAotGenerator().processAheadOfTime(
    // kontekst, generationContext) z InMemoryGeneratedFiles — pliki i podpowiedzi lądują w pamięci.
    //
    // PUŁAPKA: w module z kilkoma klasami @SpringBootApplication -Dspring-boot.aot.main-class=... NIE działa: „Unable to
    //   find a single main class from the following candidates [...]” (sprawdzone). Dlaczego? spring-boot-starter-parent
    //   ustawia w konfiguracji wtyczki <mainClass>${spring-boot.run.main-class}</mainClass>, a jawna konfiguracja
    //   w pom ma pierwszeństwo przed właściwością z linii poleceń. Działa -Dspring-boot.run.main-class=... (albo
    //   -Dstart-class=...).
    //
    // PUŁAPKA: przy AOT warunki są liczone PRZY BUDOWANIU (dokumentacja Boota, rozdział o ograniczeniach AOT): zestaw
    //   beanów z @Profile / @ConditionalOnProperty zostaje „zamrożony”. Dlaczego ważne? Zmiana profilu albo właściwości
    //   w czasie uruchomienia NIE doda ani nie usunie beana — zmienisz tylko wartości właściwości.

    // =================================================================================================
    // 3. RuntimeHints — PODPOWIEDZI DLA TWOJEJ REFLEKSJI (Ext06RuntimeHints, Ext06NativeImageTest)
    // =================================================================================================
    //
    // PromotionLoader czyta plik ext06/promocje.txt (ZASÓB) i tworzy klasy po nazwie (REFLEKSJA):
    //   Class.forName("s22_extras.ext06_native_image.PercentDiscountRule").getDeclaredConstructor().newInstance()
    // Kompilator nie wie, jakie nazwy będą w pliku — sam ich nie spakuje. Rejestrator:
    //   public class Ext06RuntimeHints implements RuntimeHintsRegistrar {
    //       public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    //           hints.resources().registerPattern("ext06/promocje.txt");
    //           hints.reflection().registerType(PercentDiscountRule.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
    //       }
    //   }
    // podpinasz adnotacją @ImportRuntimeHints(Ext06RuntimeHints.class) na klasie konfiguracji (albo w pliku
    // META-INF/spring/aot.factories). Inne rodzaje: hints.proxies().registerJdkProxy(...) — proxy JDK,
    // hints.serialization() — serializacja Javy.
    //
    // Test BEZ GraalVM (Ext06NativeImageTest):
    //   RuntimeHints hints = new RuntimeHints();
    //   new Ext06RuntimeHints().registerHints(hints, getClass().getClassLoader());
    //   assertThat(RuntimeHintsPredicates.resource().forResource("ext06/promocje.txt")).accepts(hints);
    //   assertThat(RuntimeHintsPredicates.reflection().onType(PercentDiscountRule.class)
    //           .withMemberCategory(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
    //
    // PUŁAPKA: „na JVM działa, więc działa”. Na JVM refleksja i zasoby działają ZAWSZE (test reflectionWorksOnJvm), więc
    //   brak podpowiedzi wychodzi dopiero w obrazie natywnym — jako ClassNotFoundException albo brak pliku, często po
    //   długiej kompilacji. Dlaczego test predykatów jest ważny? Łapie brak podpowiedzi w sekundę, bez GraalVM.
    //
    // PUŁAPKA: Spring Framework 7 oznaczył kategorie INTROSPECT_* oraz PUBLIC_FIELDS / DECLARED_FIELDS jako przestarzałe
    //   do usunięcia (sprawdzone w spring-core 7.0.9). Stare przykłady z nimi kompilują się z ostrzeżeniem. Używaj
    //   INVOKE_* (wywołanie konstruktorów/metod) i ACCESS_* (dostęp do pól).

    // =================================================================================================
    // 4. JSON I REFLEKSJA — @RegisterReflectionForBinding (PromotionDto, Ext06Application)
    // =================================================================================================
    //
    // Jackson tworzy i czyta obiekty refleksją. Typy parametrów i wyników kontrolerów Spring wykrywa sam. Gdy obiekt
    // zamieniasz na JSON ręcznie (tu: jsonMapper.writeValueAsString(dto) w ApplicationRunner) albo wysyłasz RestClientem,
    // zgłoś typ: @RegisterReflectionForBinding(PromotionDto.class). W reachability-metadata.json pojawia się wtedy
    // (sprawdzone): PromotionDto z "allDeclaredFields", "allDeclaredConstructors" i metodami description(), ...
    // Ogólniejsza adnotacja (Spring 6.2+): @RegisterReflection(classes = ..., memberCategories = ...).

    // =================================================================================================
    // 5. BUDOWA OBRAZU NATYWNEGO — OPIS (nie wymagamy instalacji GraalVM)
    // =================================================================================================
    //
    // a) Lokalnie z GraalVM:
    //    • JDK GraalVM 25 (IntelliJ: File → Project Structure → SDK → Download JDK → GraalVM) + na Windows narzędzia
    //      kompilatora C++ (Visual Studio Build Tools).
    //    • W pom.xml modułu: <plugin><groupId>org.graalvm.buildtools</groupId><artifactId>native-maven-plugin</artifactId>
    //      </plugin> — wersję (1.1.8) i konfigurację daje spring-boot-starter-parent, ale TYLKO w pluginManagement,
    //      więc sam profil native bez tej deklaracji nie zbuduje obrazu.
    //    • ./mvnw -Pnative native:compile -Dspring-boot.run.main-class=... → plik wykonywalny w target/.
    //    • Testy w obrazie natywnym: ./mvnw -PnativeTest test (profil nativeTest z rodzica).
    // b) Bez GraalVM, przez Docker (Buildpacks — s19_deploy/Dep02Docker):
    //    ./mvnw -Pnative spring-boot:build-image — kompilacja natywna odbywa się w kontenerze budującym, wynik to obraz
    //    Dockera z programem natywnym.
    // c) Biblioteki: wiele z nich ma gotowe metadane w GraalVM Reachability Metadata Repository — native-maven-plugin
    //    dołącza je sam (cel add-reachability-metadata w profilu native rodzica).
    //
    // CO CZYTAĆ DALEJ: dokumentacja Spring Boot — „GraalVM Native Image Support” (w tym ograniczenia AOT), Spring
    // Framework — „Ahead of Time Optimizations” (RuntimeHints, @Reflective), GraalVM — Native Image Reference Manual,
    // tracing agent (agent GraalVM zbierający podpowiedzi podczas działania na JVM).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Obraz natywny: szybki start, mało pamięci; długa kompilacja, świat zamknięty, brak JIT.
     *   • spring-boot:process-aot → target/spring-aot/main: __BeanDefinitions.java, __ApplicationContextInitializer.java,
     *     reachability-metadata.json, native-image.properties.
     *   • W module z wieloma aplikacjami: -Dspring-boot.run.main-class=... (nie spring-boot.aot.main-class).
     *   • Własna refleksja/zasoby/proxy → RuntimeHintsRegistrar + @ImportRuntimeHints.
     *   • DTO dla Jacksona → @RegisterReflectionForBinding(Typ.class).
     *   • Test bez GraalVM: RuntimeHintsPredicates.reflection()/resource()/proxies() ... .accepts(hints).
     *   • Spring 7: INVOKE_* i ACCESS_* zamiast przestarzałych INTROSPECT_* / PUBLIC_FIELDS / DECLARED_FIELDS.
     *   • Budowa: native-maven-plugin w pom + ./mvnw -Pnative native:compile, albo -Pnative spring-boot:build-image.
     *   • Warunki (@Profile, @ConditionalOnProperty) liczone przy budowaniu.
     *
     * PYTANIA KONTROLNE:
     *   1. Co zyskujesz, a co tracisz, kompilując aplikację do obrazu natywnego?
     *   2. Co to jest „świat zamknięty” i dlaczego Class.forName(nazwaZPliku) jest problemem?
     *   3. Co generuje spring-boot:process-aot? Dlaczego na starcie obrazu Spring nie musi skanować pakietów?
     *   4. Co się stanie? W tym module uruchomisz ./mvnw -pl s22-extras compile spring-boot:process-aot
     *      -Dspring-boot.aot.main-class=s22_extras.ext06_native_image.Ext06Application
     *   5. ZNAJDŹ BŁĄD: aplikacja działa na JVM i przechodzą wszystkie testy, a obraz natywny pada przy wczytywaniu
     *      promocji. Rejestrator zgłasza tylko hints.resources().registerPattern("ext06/promocje.txt").
     *   6. Jak sprawdzić podpowiedzi bez instalowania GraalVM?
     *   7. Co się stanie? Testujesz predykatem onMethodInvocation(PercentDiscountRule.class, "apply") podpowiedzi
     *      z Ext06RuntimeHints.
     *   8. Kiedy NIE wybrałbyś obrazu natywnego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Ext06NativeImageExercisesTest (RuntimeHintsPredicates, bez GraalVM)
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): dopisz do hints: (a) zasoby pasujące do wzorca "ext06/regulamin-*.txt",
     * (b) refleksję dla PercentDiscountRule z kategorią INVOKE_PUBLIC_METHODS (wywoływanie metod publicznych).
     * Podpowiedź: hints.resources().registerPattern(...), hints.reflection().registerType(klasa, kategoria).
     */
    static void exercise1(RuntimeHints hints) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć RuntimeHintsRegistrar, który dla każdej nazwy klasy z listy rejestruje refleksję
     * z INVOKE_DECLARED_CONSTRUCTORS — ale TYLKO, jeśli klasa istnieje na classpath (nazwy przychodzą z pliku
     * konfiguracyjnego, mogą być literówki). Nieistniejących nazw nie zgłaszaj i nie rzucaj wyjątku.
     * Podpowiedź: hints.reflection().registerTypeIfPresent(classLoader, nazwa, kategoria).
     */
    static RuntimeHintsRegistrar exercise2(List<String> classNames) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): kod sklepu owija promocje w proxy JDK, które loguje wywołania
     * (Proxy.newProxyInstance(..., new Class<?>[] {PromotionRule.class}, handler)), a handler woła metodę „apply”
     * refleksją (method.invoke(cel, args)). Dopisz do hints: (a) proxy JDK dla interfejsu PromotionRule,
     * (b) wywołanie metody PromotionRule.apply(BigDecimal) refleksją.
     * Podpowiedź: hints.proxies().registerJdkProxy(...), hints.reflection().registerMethod(metoda, ExecutableMode.INVOKE),
     * metodę znajdziesz przez PromotionRule.class.getMethod("apply", BigDecimal.class)
     * (t19_annotations_reflection/Annotations06DynamicProxy).
     */
    static void exercise3(RuntimeHints hints) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Zysk: start w dziesiątkach milisekund, mniej pamięci, jeden plik bez JVM (mały obraz kontenera). Strata:
     *      długa i pamięciożerna kompilacja, świat zamknięty (podpowiedzi dla refleksji, zasobów, proxy), brak JIT
     *      (szczytowa wydajność bywa niższa), trudniejsze profilowanie, warunki beanów zamrożone przy budowaniu.
     *   2. Do programu trafia tylko kod osiągalny z main, widoczny dla analizy przy kompilacji. Nazwa klasy czytana
     *      z pliku w czasie działania jest dla kompilatora niewidoczna — klasy nie spakuje (ClassNotFoundException),
     *      chyba że zgłosisz ją podpowiedzią. Plik z classpath — tak samo.
     *   3. Kod Javy z definicjami beanów (__BeanDefinitions — tworzenie przez new / wywołanie metody @Bean) i inicjalizator
     *      kontekstu (__ApplicationContextInitializer), klasy proxy CGLIB oraz metadane: reachability-metadata.json
     *      i native-image.properties (sprawdzone uruchomieniem). Skanowanie i czytanie adnotacji odbyło się przy
     *      budowaniu — na starcie wykonuje się gotowy kod.
     *   4. BUILD FAILURE: „Unable to find a single main class from the following candidates [...6 klas...]”. Rodzic
     *      ustawia <mainClass>${spring-boot.run.main-class}</mainClass> w konfiguracji wtyczki, co wygrywa z właściwością
     *      spring-boot.aot.main-class. Poprawnie: -Dspring-boot.run.main-class=... Sprawdzone uruchomieniem.
     *   5. Brakuje podpowiedzi REFLEKSJI dla klas promocji: plik się wczyta, ale Class.forName(...) nie znajdzie klas
     *      (nie zostały spakowane). Na JVM nic tego nie zdradzi. Poprawka: registerType(PercentDiscountRule.class,
     *      MemberCategory.INVOKE_DECLARED_CONSTRUCTORS) itd. i test z RuntimeHintsPredicates.
     *   6. Zarejestrować podpowiedzi w obiekcie RuntimeHints (registrar.registerHints(hints, classLoader) albo AOT
     *      w teście — ApplicationContextAotGenerator) i sprawdzić RuntimeHintsPredicates — zwykły JUnit.
     *   7. Predykat ODRZUCI podpowiedzi (rejects): zgłoszono tylko wywołanie konstruktorów, nie metod. Tu to nie szkodzi —
     *      apply() jest wołane zwykle, nie refleksją. Sprawdzone testem hintsAreExact.
     *   8. Długo działający serwis (JVM się rozgrzeje i JIT go przyspieszy), dużo bibliotek opartych na refleksji bez
     *      metadanych, częste zmiany i brak czasu na utrzymanie podpowiedzi, potrzeba zmiany beanów profilem
     *      w czasie uruchomienia.
     */
    // </editor-fold>
}
