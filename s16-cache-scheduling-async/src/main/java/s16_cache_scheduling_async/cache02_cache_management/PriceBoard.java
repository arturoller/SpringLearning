package s16_cache_scheduling_async.cache02_cache_management;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tablica cen z pamięcią podręczną. „Baza” to mapa w pamięci (ceny w groszach, long). Licznik {@link #loads()} liczy
 * PRAWDZIWE odczyty z „bazy” — gdy stoi w miejscu, odpowiedź przyszła z cache.
 */
@Service
public class PriceBoard {

    private final Map<String, Long> database = new ConcurrentHashMap<>();
    private final AtomicInteger loads = new AtomicInteger();

    public PriceBoard() {
        fill();
    }

    /** Przywraca dane startowe (metoda publiczna, NIE final — proxy CGLIB nie może przesłonić metody final). */
    public void resetDatabase() {
        fill();
    }

    private void fill() {
        database.clear();
        database.put("ELE-001", 549999L);
        database.put("SPO-001", 6499L);
        database.put("KSI-001", 7900L);
        loads.set(0);
    }

    public int loads() {
        return loads.get();
    }

    /** Odczyt wprost z „bazy”, z pominięciem cache (tylko do porównań w teście; wywołanie z zewnątrz, ale bez adnotacji). */
    public long databasePrice(String sku) {
        return database.get(sku);
    }

    // ---- odczyt ---------------------------------------------------------------------------------------------------

    @Cacheable("prices")
    public long priceOf(String sku) {
        loads.incrementAndGet();
        return database.get(sku);
    }

    @Cacheable("priceList")
    public List<String> priceList() {
        loads.incrementAndGet();
        return database.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).sorted().toList();
    }

    // ---- zmiana danych: @CachePut ----------------------------------------------------------------------------------

    /** @CachePut ZAWSZE wykonuje metodę, a jej wynik zapisuje w cache pod podanym kluczem (nadpisuje stary wpis). */
    @CachePut(cacheNames = "prices", key = "#sku")
    public long changePrice(String sku, long newPrice) {
        database.put(sku, newPrice);
        return newPrice; // TO, co zwracamy, ląduje w cache — musi być tego samego typu co wynik @Cacheable
    }

    /** PUŁAPKA: @CachePut z innym typem wyniku niż @Cacheable("prices") — do cache trafia String zamiast long. */
    @CachePut(cacheNames = "prices", key = "#sku")
    public String changePriceWrongType(String sku, long newPrice) {
        database.put(sku, newPrice);
        return "OK";
    }

    /** Zmiana „za plecami” cache: baza się zmienia, a pamięć podręczna nie wie o tym. */
    public void changePriceBehindCachesBack(String sku, long newPrice) {
        database.put(sku, newPrice);
    }

    // ---- unieważnianie: @CacheEvict --------------------------------------------------------------------------------

    @CacheEvict(cacheNames = "prices", key = "#sku")
    public void removeProduct(String sku) {
        database.remove(sku);
    }

    /** allEntries = true wyrzuca WSZYSTKIE wpisy pamięci "prices" (klucz jest wtedy ignorowany). */
    @CacheEvict(cacheNames = "prices", allEntries = true)
    public void reloadAll() {
        resetDatabase();
    }

    /** Domyślnie wyrzucenie następuje PO udanym wykonaniu metody — wyjątek oznacza: nic nie wyrzucono. */
    @CacheEvict(cacheNames = "prices", key = "#sku")
    public void failingEvictAfter(String sku) {
        throw new IllegalStateException("awaria zapisu");
    }

    /** beforeInvocation = true: wyrzucenie następuje PRZED wykonaniem metody, więc nawet wyjątek go nie cofnie. */
    @CacheEvict(cacheNames = "prices", key = "#sku", beforeInvocation = true)
    public void failingEvictBefore(String sku) {
        throw new IllegalStateException("awaria zapisu");
    }

    /** @Caching łączy kilka operacji cache na jednej metodzie: zmiana ceny unieważnia wpis ceny i całą listę cen. */
    @Caching(evict = {
            @CacheEvict(cacheNames = "prices", key = "#sku"),
            @CacheEvict(cacheNames = "priceList", allEntries = true)
    })
    public void changePriceEverywhere(String sku, long newPrice) {
        database.put(sku, newPrice);
    }
}
