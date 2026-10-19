package s16_cache_scheduling_async.cache01_cacheable;

import java.util.Map;
import java.util.function.Function;

/**
 * <pre>
 * TEMAT: Pamięć podręczna — @Cacheable i CacheManager
 *        (cache = pamięć podręczna; cacheable = „nadający się do zapamiętania”; key = klucz; condition = warunek;
 *         unless = „chyba że”; proxy = pośrednik)
 *
 * W SKRÓCIE:
 *   Jeśli metoda jest wolna (zapytanie do bazy, wywołanie cudzego API), a dla tych samych parametrów zwraca ten sam wynik,
 *   nie liczymy go za każdym razem. Dopisujesz @Cacheable("nazwa") do metody, a Spring przy pierwszym wywołaniu
 *   wykonuje metodę i zapamiętuje wynik pod kluczem zbudowanym z parametrów. Przy kolejnym wywołaniu z tym samym kluczem
 *   metoda NIE jest wykonywana — wynik wraca z pamięci. W teście widać to po liczniku prawdziwych wywołań.
 *
 * ANALOGIA: kartka z cenami przy kasie.
 *   Kasjer nie dzwoni do hurtowni za każdym razem, gdy ktoś pyta o cenę kawy. Za pierwszym razem dzwoni (wolno),
 *   zapisuje cenę na kartce, a potem odczytuje z kartki (szybko). Kartka ma „nagłówki” (nazwa pamięci) i wiersze
 *   (klucze). Ryzyko: hurtownia zmieni cenę, a kartka pokazuje starą — dlatego w lekcji Cache02CacheManagement
 *   uczysz się kartkę poprawiać i wyrzucać z niej wpisy.
 *
 * JAK TO DZIAŁA:
 *   1. @EnableCaching (włącz pamięć podręczną) na klasie konfiguracji/startowej rejestruje „aspekt” cache.
 *   2. Spring Boot (starter spring-boot-starter-cache) tworzy bean CacheManager (menedżer pamięci podręcznych).
 *      Bez żadnej dodatkowej biblioteki jest to ConcurrentMapCacheManager — zwykłe ConcurrentHashMap w pamięci JVM.
 *   3. Bean z metodą @Cacheable jest owijany w PROXY (pośrednika — obiekt-przebranie, który przechwytuje wywołania).
 *   4. Wywołanie metody trafia najpierw do proxy:
 *        proxy buduje klucz → szuka w pamięci "products" → [jest] zwraca zapamiętany wynik, metoda się NIE wykonuje
 *                                                         → [nie ma] wykonuje metodę, zapisuje wynik, zwraca go.
 *   5. Klucz domyślny: brak parametrów = SimpleKey.EMPTY; jeden parametr = on sam; wiele = SimpleKey(a, b, ...).
 *
 * SŁÓWKA:
 *   cache = pamięć podręczna; cache hit = trafienie (wynik był w pamięci); cache miss = chybienie (trzeba policzyć);
 *   key = klucz; value = wartość; evict = wyrzucić z pamięci (Cache02); SpEL = Spring Expression Language, język wyrażeń
 *   Springa (#parametr, #result); self-invocation = wywołanie z tej samej klasy; mutable = zmienny; stale = nieświeży,
 *   przestarzały (o danych).
 *
 * ZOBACZ TEŻ: s16_cache_scheduling_async/Cache02CacheManagement (zmiana i unieważnianie wpisów),
 *             s10_transactions/Tx03ProxyPitfalls (ta sama pułapka proxy przy @Transactional),
 *             s01_core_ioc/Ioc04BeanMethods (beany), t21_concurrency/Concurrency06ConcurrentCollections (ConcurrentHashMap).
 * </pre>
 */
public final class Cache01Cacheable {

    private Cache01Cacheable() {
    }

    // =================================================================================================
    // 1. @Cacheable i DOMYŚLNY CacheManager (ProductLookup.findBySku, test 1–2)
    // =================================================================================================
    //
    //   @Cacheable("products")                       // "products" = nazwa pamięci (szuflady), klucz = parametr sku
    //   public Product findBySku(String sku) { ... } // wykona się tylko przy chybieniu
    //
    // Co wybiera Boot, gdy nie dodasz żadnej biblioteki? CacheAutoConfiguration szuka obsługiwanych dostawców
    // (Caffeine, Redis, JCache...) i — jeśli żadnego nie ma na ścieżce klas — bierze ConcurrentMapCacheManager.
    // Sprawdzone: bean "cacheManager" ma tę klasę, a pamięci powstają „na żądanie” (przy pierwszym użyciu nazwy),
    // więc lista nazw na starcie jest pusta.
    //
    // WAŻNE (zmiana w Boot 4): autokonfiguracja cache mieszka w osobnym module spring-boot-cache
    // (pakiet org.springframework.boot.cache.autoconfigure) i przychodzi ze starterem spring-boot-starter-cache.
    // W starych tutorialach zobaczysz inne pakiety i sam starter-web — nie szukaj ich tutaj.
    //
    // DOBRA PRAKTYKA: cache'uj metody CZYSTE (ten sam klucz = ten sam wynik, bez efektów ubocznych).
    //   Dlaczego? Przy trafieniu metoda w ogóle się nie wykona, więc jej efekt uboczny (zapis, e-mail, log) by zniknął.

    // =================================================================================================
    // 2. KLUCZ: domyślny, z wielu parametrów i własny SpEL (test 3–4)
    // =================================================================================================
    //
    //   findByCategory("ELEKTRONIKA", 0)   klucz = SimpleKey [ELEKTRONIKA, 0]   (oba parametry razem)
    //   search(category, page, verbose)    key = "#category + ':' + #page"      klucz "KSIAZKI:0", verbose pominięty
    //
    // Wyrażenie SpEL widzi parametry po nazwach (#sku, #category) — projekt Boota kompiluje z flagą -parameters
    // (ustawia ją spring-boot-starter-parent). Bez tej flagi nazwy parametrów znikają z bajtkodu i trzeba pisać
    // #p0, #a0 (numer parametru) — stąd tajemnicze błędy w projektach budowanych bez rodzica Boota.
    //
    // PUŁAPKA: parametr, który zmienia wynik, a NIE wchodzi do klucza. Dlaczego? Wtedy dwa różne zapytania dzielą
    //   jeden wpis i klient dostaje cudzy wynik (test 4: „szczegółowa” odpowiedź to w rzeczywistości „krótka”).
    //   Klucz musi zawierać WSZYSTKO, od czego zależy wynik.
    //
    // PUŁAPKA: klucz z obiektu bez poprawnych equals/hashCode (zwykła klasa zamiast rekordu). Dlaczego? Pamięć to mapa:
    //   dwa „takie same” obiekty o różnych tożsamościach to dwa różne klucze i cache nigdy nie trafia. Rekordy mają
    //   equals/hashCode za darmo (t23_modern_java/Modern05RecordsSealedPatterns).

    // =================================================================================================
    // 3. condition i unless (test 5–6)
    // =================================================================================================
    //
    //   condition = "#sku.startsWith('SPO')"   PRZED wywołaniem: false → cache w ogóle nie jest użyty (ani odczyt,
    //                                           ani zapis) — działa zwykła metoda.
    //   unless    = "#result == null"          PO wywołaniu: true → wynik NIE jest zapisywany (ale i tak zwracany).
    //
    // Pamiętaj: condition = „czy w ogóle używać cache”, unless = „czy zapisać TEN wynik”. #result jest dostępne tylko
    // w unless (przed wywołaniem wyniku jeszcze nie ma).
    //
    // Domyślnie null też jest zapamiętywany (ConcurrentMapCacheManager pozwala na null — sprawdzone). Czasem to dobre
    // („tego produktu naprawdę nie ma”), czasem zła wiadomość: produkt pojawi się w bazie, a cache nadal mówi „brak”.
    // DOBRA PRAKTYKA: świadomie wybierz — albo zostaw null w pamięci (z krótkim czasem życia, Cache02), albo dodaj
    //   unless = "#result == null". Dlaczego? Zapamiętany „brak” to najczęstsze źródło błędu „dodałem, a nie widać”.

    // =================================================================================================
    // 4. PUŁAPKA: PROXY (test 7, 9)
    // =================================================================================================
    //
    // Cache to tylko „przebranie” (proxy). Działa, gdy wywołanie PRZECHODZI przez proxy — czyli z innego beana.
    // Gdy metoda woła inną metodę TEJ SAMEJ klasy (this.findBySku(...)), idzie bezpośrednio do obiektu i omija
    // przebranie: licznik rośnie, a w pamięci nic się nie pojawia (test 7).
    //   PRZED:  findTwiceFromInside() { findBySku(sku); findBySku(sku); }   // 2 prawdziwe wywołania
    //   PO:     przenieś metodę z @Cacheable do OSOBNEGO beana i wołaj go przez wstrzykniętą zależność.
    //
    // PUŁAPKA: brak @EnableCaching. Dlaczego? Bez niego nikt nie tworzy proxy, więc adnotacja jest zwykłym komentarzem —
    //   aplikacja działa, tylko wolno i bez żadnego błędu (test 9). Szukaj tego pierwszego, gdy „cache nie działa”.
    //
    // Ta sama zasada obowiązuje przy @Transactional i @Async (s10_transactions/Tx03ProxyPitfalls).

    // =================================================================================================
    // 5. PUŁAPKA: OBIEKTY ZMIENNE W PAMIĘCI (test 8)
    // =================================================================================================
    //
    // ConcurrentMapCacheManager przechowuje REFERENCJE (store-by-reference): odczyt z cache zwraca dokładnie ten sam
    // obiekt, który tam leży (test 2: isSameAs). Jeśli ma settery i ktoś go zmieni, zmienia się pamięć podręczna
    // dla WSZYSTKICH następnych klientów.
    //
    // DOBRA PRAKTYKA: do cache wkładaj obiekty NIEZMIENNE (rekordy, List.copyOf, Map.copyOf).
    //   Dlaczego? Wspólny obiekt zmienny to ukryty stan wspólny między wywołaniami — błąd, który pojawia się tylko
    //   przy konkretnej kolejności żądań. Rekord nie ma setterów, więc zmiany nie da się wykonać „po cichu”.
    //   (Redis i inne magazyny serializujące wartości zwracają kopię — ta pułapka dotyczy pamięci w JVM.)

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @EnableCaching + spring-boot-starter-cache; bez dodatkowych bibliotek CacheManager = ConcurrentMapCacheManager.
     *   • @Cacheable("nazwa"): trafienie → metoda NIE wykonuje się; chybienie → wykonuje się i wynik jest zapisany.
     *   • Klucz: 0 parametrów = SimpleKey.EMPTY, 1 = parametr, wiele = SimpleKey; własny: key = "#a + ':' + #b".
     *   • condition (przed, parametry) = czy używać cache; unless (po, #result) = czy NIE zapisywać wyniku.
     *   • Domyślnie null też jest zapamiętany.
     *   • Proxy: wywołanie z tej samej klasy omija cache. Brak @EnableCaching = adnotacje ignorowane.
     *   • W cache trzymaj obiekty niezmienne (ConcurrentMap trzyma referencje).
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego @Cacheable działa tylko dla wywołań z innego beana?
     *   2. Co się stanie? Metoda findTwiceFromInside woła this.findBySku("ELE-001") dwa razy — ile razy wykona się
     *      prawdziwa metoda i co będzie w pamięci "products"?
     *   3. Co się stanie? findBySku("NIE-MA") zwraca null i wołasz ją dwa razy — ile prawdziwych wywołań?
     *   4. ZNAJDŹ BŁĄD: zespół dodał @Cacheable("products") do metody, aplikacja działa, ale każde wywołanie idzie do
     *      bazy i żaden błąd nie jest zgłaszany. Co sprawdzisz najpierw?
     *   5. Jaki klucz zbuduje Spring dla findByCategory("ELEKTRONIKA", 0)?
     *   6. ZNAJDŹ BŁĄD: serwis zwraca z cache obiekt z setterem, a kontroler robi setPriceGrosze(1) „tylko na potrzeby
     *      widoku”. Co zobaczy następny klient?
     *   7. Czym różni się condition od unless i które z nich może użyć #result?
     *   8. Jaki CacheManager Boot utworzy bez żadnej dodatkowej biblioteki?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cache01CacheableExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć wyrażenie SpEL (jako String) na klucz złożony z kategorii i strony, rozdzielonych
     * dwukropkiem, np. "ELEKTRONIKA:2" dla parametrów {@code category} i {@code page}. Test oblicza Twoje wyrażenie
     * prawdziwym parserem SpEL.
     * Podpowiedź: sekcja 2 lekcji, przykład {@code search}.
     */
    static String exercise1() {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zrób ręcznie to, co robi @Cacheable („pobierz z mapy albo policz i zapamiętaj”).
     * Jeśli {@code cache} zawiera klucz — zwróć wartość; jeśli nie — wywołaj {@code loader.apply(key)}, zapisz wynik
     * w mapie i zwróć go. Wartość null z loadera NIE może być zapisana (jak {@code unless = "#result == null"}).
     * Podpowiedź: {@code Map.containsKey}; loader ma być wołany co najwyżej raz na klucz, o ile wynik nie jest null.
     */
    static <K, V> V exercise2(Map<K, V> cache, K key, Function<K, V> loader) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): w klasie ExerciseStockService dopisz adnotację do metody stockOf, tak aby wynik był
     * pamiętany w pamięci "exerciseStock" pod kluczem sku, ale wynik 0 nie trafiał do pamięci. Test sprawdza liczbę
     * prawdziwych wywołań. Ta metoda jest tylko opisem — edytujesz ExerciseStockService.
     * Podpowiedź: sekcje 1 i 3; w {@code unless} wynik to {@code #result}.
     */
    static void exercise3() {
        // TODO: otwórz ExerciseStockService i dodaj adnotację (ta metoda jest tylko opisem — nic w niej nie pisz)
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Cache to proxy owinięte wokół beana: tylko wywołanie przez proxy przechodzi przez logikę cache. Wywołanie
     *      this.metoda() idzie bezpośrednio do obiektu i omija proxy.
     *   2. Dwa razy (licznik = 2), a pamięć "products" nie ma wpisu dla ELE-001. Sprawdzone testem 7.
     *   3. Jeden raz: ConcurrentMapCacheManager domyślnie zapamiętuje także null (wpis istnieje, wartość to null).
     *      Sprawdzone testem 6. Żeby tego uniknąć: unless = "#result == null".
     *   4. Czy jest @EnableCaching (bez niego adnotacje są ignorowane), a potem czy wywołanie nie idzie z tej samej klasy.
     *      Sprawdzone testem 9 (zwykły kontekst bez @EnableCaching: 2 wywołania) i testem 7.
     *   5. new SimpleKey("ELEKTRONIKA", 0), w zapisie SimpleKey [ELEKTRONIKA, 0]. Sprawdzone testem 3.
     *   6. Następny klient dostanie cenę 1 grosz — ConcurrentMapCache trzyma referencję do tego samego obiektu, więc
     *      zmiana zmienia pamięć podręczną. Sprawdzone testem 8. Naprawa: rekord/obiekt niezmienny.
     *   7. condition działa PRZED wywołaniem i widzi tylko parametry (czy w ogóle używać cache); unless działa PO
     *      wywołaniu i jako jedyne widzi #result (czy pominąć zapis tego wyniku). Sprawdzone testem 5.
     *   8. ConcurrentMapCacheManager (pamięci tworzone na żądanie). Sprawdzone testem 1.
     */
    // </editor-fold>
}
