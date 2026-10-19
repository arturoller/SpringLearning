package s16_cache_scheduling_async.cache01_cacheable;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wyszukiwanie produktów z pamięcią podręczną. Każda metoda ZLICZA swoje prawdziwe wywołania ({@link #calls(String)}) —
 * dzięki temu test widzi, czy odpowiedź przyszła z cache (licznik stoi) czy z „bazy” (licznik rośnie).
 * "Baza" to tu zwykła lista w pamięci (sklep z AGENT_KIT: kilka produktów).
 */
@Service
public class ProductLookup {

    private static final List<Product> DATABASE = List.of(
            new Product("ELE-001", "Laptop Pro 14", "ELEKTRONIKA", 549999),
            new Product("ELE-003", "Słuchawki BT", "ELEKTRONIKA", 34990),
            new Product("SPO-001", "Kawa ziarnista 1kg", "SPOZYWCZE", 6499),
            new Product("SPO-002", "Czekolada gorzka", "SPOZYWCZE", 749),
            new Product("KSI-001", "Czysty kod", "KSIAZKI", 7900));

    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
    private final Map<String, MutableProduct> mutableDatabase = new ConcurrentHashMap<>(
            Map.of("ELE-001", new MutableProduct("ELE-001", 549999)));

    /** Ile razy PRAWDZIWA metoda o tej nazwie została wykonana (wywołania obsłużone z cache się nie liczą). */
    public int calls(String method) {
        return calls.computeIfAbsent(method, k -> new AtomicInteger()).get();
    }

    public void resetCounters() {
        calls.clear();
    }

    private void count(String method) {
        calls.computeIfAbsent(method, k -> new AtomicInteger()).incrementAndGet();
    }

    // ---- 1. Najprostszy @Cacheable: nazwa pamięci "products", klucz = jedyny parametr (sku) ----------------------

    @Cacheable("products") // cacheNames = nazwa pamięci podręcznej ("szuflada"); klucz domyślny = parametr sku
    public Product findBySku(String sku) {
        count("findBySku");
        return DATABASE.stream().filter(p -> p.sku().equals(sku)).findFirst().orElse(null);
    }

    // ---- 2. Klucz domyślny z WIELU parametrów: SimpleKey(category, page) -----------------------------------------

    @Cacheable("byCategory")
    public List<Product> findByCategory(String category, int page) {
        count("findByCategory");
        return DATABASE.stream().filter(p -> p.category().equals(category)).toList();
    }

    /** Metoda bez parametrów: klucz domyślny to SimpleKey.EMPTY. */
    @Cacheable("categories")
    public List<String> findCategories() {
        count("findCategories");
        return DATABASE.stream().map(Product::category).distinct().sorted().toList();
    }

    // ---- 3. Własny klucz (SpEL — język wyrażeń Springa): parametr 'verbose' NIE wchodzi do klucza ----------------

    @Cacheable(cacheNames = "search", key = "#category + ':' + #page")
    public List<String> search(String category, int page, boolean verbose) {
        count("search");
        return DATABASE.stream()
                .filter(p -> p.category().equals(category))
                .map(p -> verbose ? p.sku() + " " + p.name() : p.sku())
                .toList();
    }

    // ---- 4. condition (PRZED wywołaniem, patrzy na parametry) i unless (PO wywołaniu, patrzy na wynik) -----------

    @Cacheable(cacheNames = "conditional",
            condition = "#sku.startsWith('SPO')",  // zapamiętuj tylko produkty spożywcze
            unless = "#result == null")            // ...i nie zapamiętuj „braku produktu” (null)
    public Product findFoodOnly(String sku) {
        count("findFoodOnly");
        return DATABASE.stream().filter(p -> p.sku().equals(sku)).findFirst().orElse(null);
    }

    // ---- 5. Pułapka: wywołanie z tej samej klasy omija proxy, więc omija też cache -------------------------------

    /** Wywołuje {@code this.findBySku} dwa razy — adnotacja NIE zadziała, bo wołamy obiekt, nie proxy. */
    public Product findTwiceFromInside(String sku) {
        findBySku(sku);
        return findBySku(sku);
    }

    // ---- 6. Pułapka: obiekt ZMIENNY w cache — dostajemy TĘ SAMĄ instancję co w pamięci podręcznej -----------------

    @Cacheable("mutable")
    public MutableProduct findMutable(String sku) {
        count("findMutable");
        return mutableDatabase.get(sku);
    }

    /** Odczyt wprost z „bazy” (bez cache) — do porównania w teście. */
    public long priceInDatabase(String sku) {
        return mutableDatabase.get(sku).getPriceGrosze();
    }
}
