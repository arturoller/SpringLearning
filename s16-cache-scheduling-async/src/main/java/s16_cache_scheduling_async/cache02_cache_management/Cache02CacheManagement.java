package s16_cache_scheduling_async.cache02_cache_management;

import org.springframework.cache.CacheManager;

/**
 * <pre>
 * TEMAT: Zarządzanie pamięcią podręczną — @CachePut, @CacheEvict, @Caching, spring.cache.*
 *        (put = włóż/nadpisz; evict = wyrzucić; entry = wpis; time to live (TTL) = czas życia wpisu;
 *         stale data = nieświeże dane)
 *
 * W SKRÓCIE:
 *   Pamięć podręczna przyspiesza, ale kłamie, gdy dane w bazie się zmienią, a ona o tym nie wie. Są trzy ruchy:
 *   (1) @CachePut — wykonaj metodę ZAWSZE i zapisz jej wynik w pamięci (nadpisz stary wpis);
 *   (2) @CacheEvict — wyrzuć wpis (albo wszystkie: allEntries = true), a następny odczyt policzy go od nowa;
 *   (3) wygaszanie samo (rozmiar i czas życia) — tego ConcurrentMapCacheManager NIE potrafi; do tego potrzebna jest
 *   biblioteka (Caffeine, Redis) — w tym module jej nie ma, więc opisujemy ją tylko słowami.
 *
 * ANALOGIA: kartka z cenami przy kasie (ciąg dalszy z Cache01Cacheable).
 *   @CachePut = kasjer słyszy „kawa podrożała” i od razu poprawia cenę na kartce.
 *   @CacheEvict = kasjer wykreśla wiersz „kawa” i przy następnym pytaniu dzwoni do hurtowni.
 *   allEntries = wyrwanie całej kartki (np. po zmianie cennika).
 *   TTL = kartka sama blaknie po godzinie (bez takiej funkcji kartka wisi do końca świata).
 *
 * JAK TO DZIAŁA:
 *   Wszystkie adnotacje działają przez ten sam proxy co @Cacheable (lekcja Cache01Cacheable) — więc te same pułapki.
 *     @CachePut(cacheNames = "prices", key = "#sku")   metoda → wynik (return) → cache.put(klucz, wynik)
 *     @CacheEvict(cacheNames = "prices", key = "#sku") metoda → (bez wyjątku) → cache.evict(klucz)
 *     @CacheEvict(cacheNames = "prices", allEntries = true)          → cache.clear()
 *     @Caching(evict = { ... }, put = { ... }, cacheable = { ... })  → kilka operacji na jednej metodzie
 *   Ręcznie to samo robi bean CacheManager: cacheManager.getCache("prices").put(k, v) / evict(k) / clear().
 *
 * SŁÓWKA:
 *   put = włożyć; evict = wyrzucić; allEntries = wszystkie wpisy; beforeInvocation = przed wywołaniem metody;
 *   native cache = natywna pamięć (obiekt biblioteki pod spodem, tu ConcurrentHashMap); time to live = czas życia;
 *   eviction policy = zasada wyrzucania (np. najdawniej użyte); stale = nieświeży.
 *
 * ZOBACZ TEŻ: s16_cache_scheduling_async/Cache01Cacheable (@Cacheable, proxy),
 *             s16_cache_scheduling_async/Sched03Scheduled (cykliczne czyszczenie pamięci),
 *             s10_transactions/Tx01Transactional (zmiana danych w transakcji — kiedy unieważniać cache).
 * </pre>
 */
public final class Cache02CacheManagement {

    private Cache02CacheManagement() {
    }

    // =================================================================================================
    // 1. PROBLEM: NIEŚWIEŻE DANE (test 1)
    // =================================================================================================
    //
    // ConcurrentMapCacheManager nie ma ani limitu rozmiaru, ani czasu życia: wpis żyje, dopóki ktoś go nie wyrzuci
    // albo aplikacja się nie zatrzyma. Gdy cena zmieni się w bazie „bokiem” (test 1: changePriceBehindCachesBack),
    // klienci nadal dostają starą.
    //
    // PUŁAPKA: zmiana danych w innym miejscu niż metoda z @CacheEvict/@CachePut (inna aplikacja, skrypt SQL, inna
    //   metoda bez adnotacji). Dlaczego? Cache nie obserwuje bazy — wie tylko to, co przejdzie przez jego adnotacje.
    //   Każda ścieżka zapisu do danych musi też odświeżyć albo unieważnić cache.

    // =================================================================================================
    // 2. @CachePut — odśwież wpis (test 2, 7)
    // =================================================================================================
    //
    //   @CachePut(cacheNames = "prices", key = "#sku")
    //   public long changePrice(String sku, long newPrice) { database.put(sku, newPrice); return newPrice; }
    //
    // W przeciwieństwie do @Cacheable metoda wykonuje się ZAWSZE; cache jest tylko odświeżany. Po zmianie ceny
    // następny priceOf(sku) zwraca nową wartość BEZ odczytu z bazy (test 2: licznik odczytów = 1).
    //
    // PUŁAPKA: @CachePut ze złym typem wyniku. Dlaczego? Do cache trafia DOKŁADNIE to, co metoda zwraca. Jeśli
    //   @Cacheable("prices") zwraca long, a @CachePut("prices") zwraca String "OK", to w tej samej
    //   pamięci leżą wartości dwóch typów i następny odczyt kończy się ClassCastException (test 7).
    //   Metoda zapisująca musi zwracać DOKŁADNIE to, co zwraca metoda odczytująca.
    //
    // PUŁAPKA: @Cacheable i @CachePut na TEJ SAMEJ metodzie. Dlaczego? Pierwsze mówi „nie wykonuj, jeśli jest”, drugie
    //   „zawsze wykonuj” — sprzeczne. Spring tego nie zabrania, ale wynik jest mylący; rozdziel metody.

    // =================================================================================================
    // 3. @CacheEvict — wyrzuć wpis (test 3, 4, 6)
    // =================================================================================================
    //
    //   @CacheEvict(cacheNames = "prices", key = "#sku")           // jeden wpis
    //   @CacheEvict(cacheNames = "prices", allEntries = true)      // cała pamięć
    //
    // Kiedy @CacheEvict, a kiedy @CachePut? Evict jest bezpieczniejszy: następny odczyt sam zbuduje poprawną wartość
    // z bazy (np. z wszystkimi polami wyliczanymi przez bazę). Put oszczędza jeden odczyt, ale wymaga, by metoda zapisu
    // znała pełną, finalną postać obiektu.
    //
    // Domyślnie wyrzucenie następuje PO wykonaniu metody — gdy metoda rzuci wyjątek, wpis ZOSTAJE (test 6): zapis
    // się nie udał, więc dane i cache są nadal zgodne. beforeInvocation = true wyrzuca PRZED wywołaniem (nawet gdy
    // metoda się wywali). Po co? Gdy pewność „nie ma starego wpisu” jest ważniejsza niż zgodność przy błędzie.
    //
    // DOBRA PRAKTYKA: unieważniaj cache w tej samej warstwie (serwis), która zmienia dane, i tuż obok transakcji.
    //   Dlaczego? Jeśli unieważnisz ZA WCZEŚNIE (przed zatwierdzeniem transakcji), inny wątek może w międzyczasie
    //   wczytać jeszcze starą wartość z bazy i zapisać ją z powrotem do cache.

    // =================================================================================================
    // 4. @Caching — wiele operacji na jednej metodzie (test 5)
    // =================================================================================================
    //
    // Adnotacje @CacheEvict/@CachePut/@Cacheable NIE są powtarzalne (sprawdzone w jarze Spring Framework 7.0.9) —
    // dwóch @CacheEvict na jednej metodzie napisać się nie da. Dla wielu operacji służy kontener
    // @Caching(evict = {...}, put = {...}). Przykład: zmiana ceny wyrzuca wpis "prices"
    // dla sku ORAZ całą listę cen "priceList" (bo lista zawierała starą cenę).
    //
    // PUŁAPKA: cache'owana LISTA albo zestawienie zbudowane z wielu rekordów. Dlaczego? Zmiana jednego rekordu
    //   unieważnia zestawienie, ale łatwo o tym zapomnieć. Zawsze pytaj: „które jeszcze pamięci zawierają tę daną?”.

    // =================================================================================================
    // 5. CacheManager W TESTACH I USTAWIENIA spring.cache.* (test 8, 9)
    // =================================================================================================
    //
    // Test może zajrzeć do pamięci: cacheManager.getCache("prices").get(klucz), getNativeCache() (tu ConcurrentMap),
    // put, evictIfPresent, clear. Dzięki temu widać, CO leży w cache, a nie tylko „ile razy wywołano metodę”.
    //
    // Ustawienia Boota (sprawdzone w metadanych spring-boot-cache 4.1.1):
    //   spring.cache.cache-names=prices,priceList   stała lista pamięci. Dla ConcurrentMapCacheManager to ZAMYKA listę:
    //        użycie nieznanej nazwy daje IllegalArgumentException „Cannot find cache named 'priceList' ...” (test 9).
    //        Bez tego ustawienia pamięci powstają same, przy pierwszym użyciu nazwy (literówka = nowa, pusta pamięć!).
    //   spring.cache.type                           wymuś dostawcę (np. none wyłącza cache); domyślnie wykrywany.
    //   spring.cache.caffeine.spec=maximumSize=500,expireAfterWrite=10m   — rozmiar i czas życia, ALE tylko gdy na
    //        ścieżce klas jest biblioteka Caffeine (w tym module jej nie ma — to tylko opis).
    //   spring.cache.redis.time-to-live=10m         — czas życia w Redisie, tylko z zależnością Redis (też tylko opis).
    //
    // Co zrobić z ConcurrentMapCacheManager, gdy potrzebujesz wygasania, a nie masz biblioteki? Cyklicznie czyść
    // pamięć: metoda @Scheduled z @CacheEvict(allEntries = true) (lekcja Sched03Scheduled) — to proste i działa,
    // choć tylko „hurtowo”, bez czasu życia pojedynczych wpisów.
    //
    // PUŁAPKA: pamięć bez limitu rozmiaru i klucz o dużej liczbie wartości (np. każde zapytanie wyszukiwania).
    //   Dlaczego? Mapa rośnie bez końca aż do OutOfMemoryError. Cache bez limitu to wyciek pamięci z nazwą „optymalizacja”.
    //   W produkcji wybierz bibliotekę z limitem rozmiaru i czasem życia.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @CachePut: metoda zawsze się wykonuje, wynik (return) nadpisuje wpis. Typ wyniku = typ wyniku @Cacheable.
     *   • @CacheEvict(key=...) wyrzuca jeden wpis, allEntries = true — wszystkie; domyślnie PO wywołaniu (wyjątek = brak
     *     wyrzucenia), beforeInvocation = true — PRZED.
     *   • @Caching(evict={...}, put={...}) — kilka operacji na jednej metodzie.
     *   • Każda ścieżka zmiany danych musi odświeżyć lub unieważnić cache (cache nie obserwuje bazy).
     *   • ConcurrentMapCacheManager: bez limitu i bez czasu życia; Caffeine/Redis dają je dopiero po dodaniu biblioteki.
     *   • spring.cache.cache-names zamyka listę pamięci; CacheManager w teście: getCache(...).get/put/evict/clear.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się @CachePut od @Cacheable pod względem wykonania metody?
     *   2. Co się stanie? Metoda z @CacheEvict(key = "#sku") rzuca wyjątek — czy wpis zostanie w cache? A z
     *      beforeInvocation = true?
     *   3. Co się stanie? @Cacheable("prices") zwraca long, a metoda z @CachePut("prices") zwraca String "OK" — co zrobi
     *      następne wywołanie metody z @Cacheable?
     *   4. ZNAJDŹ BŁĄD: administrator zmienia cenę skryptem SQL, a sklep dalej pokazuje starą. Dlaczego i jak to naprawić?
     *   5. Kiedy wybierzesz @CacheEvict zamiast @CachePut?
     *   6. Co się stanie? Ustawisz spring.cache.cache-names=prices, a metoda używa pamięci "priceList".
     *   7. ZNAJDŹ BŁĄD: zespół włączył cache wyników wyszukiwania (klucz = tekst zapytania) bez limitu rozmiaru. Co się
     *      stanie po miesiącu pracy i co zmienić?
     *   8. Jak wyczyścić jedną pamięć z poziomu kodu testu i jak zajrzeć do jej zawartości?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Cache02CacheManagementExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zwróć true, jeśli pamięć o nazwie {@code cacheName} istnieje w {@code cacheManager} i ma
     * wpis pod kluczem {@code key} (wpis, nie wartość — wpis z wartością null też się liczy). Brak pamięci = false.
     * Podpowiedź: {@code cacheManager.getCache(nazwa)} zwraca null, gdy pamięci nie ma; {@code Cache.get(klucz)}
     * zwraca {@code ValueWrapper} albo null.
     */
    static boolean exercise1(CacheManager cacheManager, String cacheName, Object key) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): wyczyść pamięć {@code cacheName} i zwróć, ILE wpisów w niej było przed czyszczeniem.
     * Brak takiej pamięci = 0. Zakładamy ConcurrentMapCacheManager.
     * Podpowiedź: {@code ConcurrentMapCache.getNativeCache()} to ConcurrentMap; policz wpisy PRZED {@code clear()}.
     */
    static int exercise2(CacheManager cacheManager, String cacheName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): w klasie ExercisePriceStore dopisz adnotację do metody changePrice, tak aby po
     * zmianie ceny kolejne priceOf zwracało nową cenę BEZ ponownego odczytu z „bazy”. Test sprawdza wynik i licznik
     * odczytów. Ta metoda jest tylko opisem — edytujesz ExercisePriceStore.
     * Podpowiedź: sekcja 2; metoda ma już poprawny typ wyniku.
     */
    static void exercise3() {
        // TODO: otwórz ExercisePriceStore i dodaj adnotację (ta metoda jest tylko opisem — nic w niej nie pisz)
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. @Cacheable przy trafieniu NIE wykonuje metody; @CachePut wykonuje ją ZAWSZE i zapisuje wynik w cache.
     *      Sprawdzone testem 2.
     *   2. Domyślnie wpis ZOSTAJE (wyrzucenie następuje po udanym wykonaniu); z beforeInvocation = true wpis jest
     *      wyrzucony przed wywołaniem, więc znika mimo wyjątku. Sprawdzone testem 6.
     *   3. Do cache trafia String "OK", więc następne priceOf rzuci ClassCastException (próba zwrócenia String jako
     *      long). Sprawdzone testem 7. Zawsze zwracaj z metody zapisującej ten sam typ co z odczytującej.
     *   4. Cache nie obserwuje bazy, a skrypt SQL ominął adnotacje. Naprawa: unieważnić cache po takiej zmianie
     *      (wywołać metodę z @CacheEvict albo wyczyścić pamięć) i dodać czas życia wpisów (biblioteka z TTL) jako
     *      zabezpieczenie. Sprawdzone testem 1 (stara cena po zmianie „za plecami” cache).
     *   5. Gdy chcesz, by następny odczyt zbudował wartość od nowa z bazy (bezpieczniejsze) albo gdy metoda zapisu nie zna
     *      pełnej, finalnej postaci obiektu. @CachePut wybierz, gdy metoda zwraca dokładnie to, co trzyma odczyt.
     *   6. Wywołanie rzuci IllegalArgumentException z komunikatem zawierającym „Cannot find cache named 'priceList'” —
     *      lista nazw jest zamknięta. Sprawdzone testem 9.
     *   7. ConcurrentMapCacheManager nie ma limitu, więc mapa rośnie, aż zabraknie pamięci (OutOfMemoryError). Zmiana:
     *      biblioteka z limitem rozmiaru i czasem życia (np. Caffeine ze spring.cache.caffeine.spec), albo
     *      przynajmniej cykliczne czyszczenie. (Opis — Caffeine nie jest zależnością tego modułu.)
     *   8. cacheManager.getCache("nazwa").clear(); zawartość: getNativeCache() (ConcurrentMap dla tego menedżera) albo
     *      get(klucz). Sprawdzone testem 8.
     */
    // </editor-fold>
}
