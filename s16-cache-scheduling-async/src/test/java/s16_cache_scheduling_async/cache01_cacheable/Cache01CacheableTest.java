package s16_cache_scheduling_async.cache01_cacheable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Cache01Cacheable — „wynik” każdej sekcji.
 * Wąski wycinek? Tu potrzebny jest cały kontekst (@SpringBootTest), bo cache działa przez proxy tworzone przez kontener;
 * serwer HTTP nie jest potrzebny (domyślne środowisko MOCK), więc test jest szybki.
 */
@SpringBootTest
class Cache01CacheableTest {

    @Autowired
    ProductLookup lookup;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void cleanState() {
        // Kontekst jest wspólny dla wszystkich testów klasy — czyścimy pamięć i liczniki, żeby testy nie zależały od kolejności.
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        lookup.resetCounters();
    }

    @Test
    @DisplayName("1. Domyślny CacheManager Boota to ConcurrentMapCacheManager, a serwis jest owinięty proxy")
    void defaultCacheManager() {
        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
        assertThat(AopUtils.isAopProxy(lookup)).as("cache działa przez proxy").isTrue();
    }

    @Test
    @DisplayName("2. Drugie wywołanie z tym samym kluczem nie wykonuje metody — wynik z pamięci podręcznej")
    void secondCallComesFromCache() {
        Product first = lookup.findBySku("ELE-001");
        Product second = lookup.findBySku("ELE-001");

        assertThat(second).isSameAs(first);
        assertThat(lookup.calls("findBySku")).isEqualTo(1);

        lookup.findBySku("SPO-001"); // inny klucz = inny wpis = prawdziwe wywołanie
        assertThat(lookup.calls("findBySku")).isEqualTo(2);
        assertThat(cacheManager.getCache("products").get("ELE-001").get()).isEqualTo(first);
    }

    @Test
    @DisplayName("3. Klucz domyślny: jeden parametr = on sam, wiele parametrów = SimpleKey")
    void defaultKeys() {
        lookup.findBySku("ELE-001");
        lookup.findByCategory("ELEKTRONIKA", 0);

        assertThat(cacheManager.getCache("products").get("ELE-001")).isNotNull();
        assertThat(cacheManager.getCache("byCategory").get(new SimpleKey("ELEKTRONIKA", 0))).isNotNull();
        assertThat(new SimpleKey("ELEKTRONIKA", 0)).hasToString("SimpleKey [ELEKTRONIKA, 0]");

        lookup.findCategories();
        assertThat(cacheManager.getCache("categories").get(SimpleKey.EMPTY)).as("bez parametrów: SimpleKey.EMPTY").isNotNull();

        // PUŁAPKA: ta sama kategoria, inna strona = inny klucz = osobne wywołanie.
        lookup.findByCategory("ELEKTRONIKA", 1);
        assertThat(lookup.calls("findByCategory")).isEqualTo(2);
    }

    @Test
    @DisplayName("4. Własny klucz SpEL pomija parametry, które nie zmieniają wyniku")
    void customKey() {
        List<String> brief = lookup.search("KSIAZKI", 0, false);
        List<String> detailed = lookup.search("KSIAZKI", 0, true); // inny 'verbose', ten sam klucz "KSIAZKI:0"

        assertThat(detailed).isSameAs(brief);
        assertThat(lookup.calls("search")).isEqualTo(1);
        assertThat(cacheManager.getCache("search").get("KSIAZKI:0")).isNotNull();
        // Skutek uboczny wyboru klucza: „szczegółowa” odpowiedź to w rzeczywistości ta „krótka”.
        assertThat(detailed).containsExactly("KSI-001");
    }

    @Test
    @DisplayName("5. condition decyduje PRZED wywołaniem, unless PO wywołaniu (patrzy na wynik)")
    void conditionAndUnless() {
        // condition = false (ELE-… nie zaczyna się od SPO) → cache w ogóle nie jest używany
        lookup.findFoodOnly("ELE-001");
        lookup.findFoodOnly("ELE-001");
        assertThat(lookup.calls("findFoodOnly")).isEqualTo(2);

        // condition = true, wynik niepusty → zapamiętane
        lookup.findFoodOnly("SPO-001");
        lookup.findFoodOnly("SPO-001");
        assertThat(lookup.calls("findFoodOnly")).isEqualTo(3);

        // condition = true, ale wynik null → unless go odrzuca, więc kolejne wywołanie znów idzie do „bazy”
        lookup.findFoodOnly("SPO-999");
        lookup.findFoodOnly("SPO-999");
        assertThat(lookup.calls("findFoodOnly")).isEqualTo(5);
    }

    @Test
    @DisplayName("6. Domyślnie null też trafia do pamięci podręcznej (ConcurrentMapCacheManager pozwala na null)")
    void nullIsCachedByDefault() {
        assertThat(lookup.findBySku("NIE-MA")).isNull();
        assertThat(lookup.findBySku("NIE-MA")).isNull();

        assertThat(lookup.calls("findBySku")).isEqualTo(1);
        Cache.ValueWrapper wrapper = cacheManager.getCache("products").get("NIE-MA");
        assertThat(wrapper).as("wpis istnieje, a jego wartość to null").isNotNull();
        assertThat(wrapper.get()).isNull();
    }

    @Test
    @DisplayName("7. PUŁAPKA: wywołanie z tej samej klasy omija proxy, więc cache nie działa")
    void selfInvocationBypassesCache() {
        lookup.findTwiceFromInside("ELE-001");
        assertThat(lookup.calls("findBySku")).as("obie próby poszły do „bazy”").isEqualTo(2);
        assertThat(cacheManager.getCache("products").get("ELE-001")).isNull();

        // To samo z ZEWNĄTRZ (przez proxy) działa:
        lookup.resetCounters();
        lookup.findBySku("ELE-001");
        lookup.findBySku("ELE-001");
        assertThat(lookup.calls("findBySku")).isEqualTo(1);
    }

    @Test
    @DisplayName("8. PUŁAPKA: obiekt zmienny w cache — zmiana zwróconego obiektu zmienia pamięć podręczną, nie bazę")
    void mutableObjectsInCache() {
        MutableProduct first = lookup.findMutable("ELE-001");
        first.setPriceGrosze(1); // „niewinna” zmiana na własnej kopii... która wcale nie jest kopią

        MutableProduct second = lookup.findMutable("ELE-001");
        assertThat(second).isSameAs(first);
        assertThat(second.getPriceGrosze()).as("kolejny klient widzi cenę 1 grosz").isEqualTo(1);
        assertThat(lookup.priceInDatabase("ELE-001")).as("baza zmieniła się tylko dlatego, że obiekt jest wspólny")
                .isEqualTo(1);
        // (w tej lekcji „baza” trzyma ten sam obiekt; w prawdziwej bazie zostałaby cena 549999, a cache — 1)
    }

    @Test
    @DisplayName("9. PUŁAPKA: bez @EnableCaching adnotacja @Cacheable jest po cichu ignorowana")
    void withoutEnableCachingNothingHappens() {
        try (AnnotationConfigApplicationContext plain = new AnnotationConfigApplicationContext()) {
            plain.register(ProductLookup.class); // zwykły kontekst Springa, bez @EnableCaching i bez Boota
            plain.refresh();
            ProductLookup notCached = plain.getBean(ProductLookup.class);

            notCached.findBySku("ELE-001");
            notCached.findBySku("ELE-001");

            assertThat(AopUtils.isAopProxy(notCached)).isFalse();
            assertThat(notCached.calls("findBySku")).isEqualTo(2);
        }
    }
}
