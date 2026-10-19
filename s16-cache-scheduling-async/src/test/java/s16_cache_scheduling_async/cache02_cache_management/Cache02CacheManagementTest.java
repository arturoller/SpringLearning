package s16_cache_scheduling_async.cache02_cache_management;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Cache02CacheManagement. Cały kontekst (@SpringBootTest), bo operacje cache działają przez proxy;
 * bez serwera HTTP (środowisko MOCK).
 */
@SpringBootTest
class Cache02CacheManagementTest {

    @Autowired
    PriceBoard board;

    @Autowired
    CacheManager cacheManager;

    @BeforeEach
    void cleanState() {
        board.resetDatabase();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    @DisplayName("1. PUŁAPKA: zmiana w bazie bez unieważnienia cache — klienci widzą starą cenę")
    void staleDataWithoutEviction() {
        assertThat(board.priceOf("ELE-001")).isEqualTo(549999);

        board.changePriceBehindCachesBack("ELE-001", 499999);

        assertThat(board.databasePrice("ELE-001")).isEqualTo(499999);
        assertThat(board.priceOf("ELE-001")).as("ConcurrentMapCache nigdy sam nie wygasa").isEqualTo(549999);
    }

    @Test
    @DisplayName("2. @CachePut: metoda zawsze się wykonuje, a jej wynik od razu trafia do cache")
    void cachePutRefreshesEntry() {
        board.priceOf("ELE-001");               // 1 odczyt z bazy
        board.changePrice("ELE-001", 499999);   // @CachePut: wykonana ZAWSZE, wpis nadpisany

        assertThat(board.priceOf("ELE-001")).isEqualTo(499999);
        assertThat(board.loads()).as("kolejny odczyt poszedł z cache — nie było drugiego odczytu z bazy").isEqualTo(1);
    }

    @Test
    @DisplayName("3. @CacheEvict z kluczem wyrzuca jeden wpis — następny odczyt idzie do bazy")
    void cacheEvictByKey() {
        board.priceOf("ELE-001");
        board.priceOf("SPO-001");
        assertThat(board.loads()).isEqualTo(2);

        board.removeProduct("SPO-001");

        assertThat(cacheManager.getCache("prices").get("SPO-001")).isNull();
        assertThat(cacheManager.getCache("prices").get("ELE-001")).as("pozostałe wpisy zostają").isNotNull();
    }

    @Test
    @DisplayName("4. allEntries = true czyści całą pamięć")
    void cacheEvictAllEntries() {
        board.priceOf("ELE-001");
        board.priceOf("SPO-001");
        ConcurrentMap<Object, Object> nativeMap = nativeMapOf("prices");
        assertThat(nativeMap).hasSize(2);

        board.reloadAll();

        assertThat(nativeMap).isEmpty();
    }

    @Test
    @DisplayName("5. @Caching: jedna metoda unieważnia kilka pamięci naraz")
    void cachingCombinesOperations() {
        board.priceOf("ELE-001");
        assertThat(board.priceList()).contains("ELE-001=549999");
        assertThat(cacheManager.getCache("priceList").get(org.springframework.cache.interceptor.SimpleKey.EMPTY)).isNotNull();

        board.changePriceEverywhere("ELE-001", 499999);

        assertThat(cacheManager.getCache("prices").get("ELE-001")).isNull();
        assertThat(nativeMapOf("priceList")).isEmpty();
        assertThat(board.priceList()).as("lista zbudowana od nowa, z nową ceną").contains("ELE-001=499999");
    }

    @Test
    @DisplayName("6. Wyjątek: domyślny @CacheEvict nie wyrzuca wpisu, beforeInvocation = true wyrzuca")
    void evictAndExceptions() {
        board.priceOf("ELE-001");
        board.priceOf("SPO-001");

        assertThatThrownBy(() -> board.failingEvictAfter("ELE-001")).isInstanceOf(IllegalStateException.class);
        assertThat(cacheManager.getCache("prices").get("ELE-001")).as("wyjątek = nic nie wyrzucono").isNotNull();

        assertThatThrownBy(() -> board.failingEvictBefore("SPO-001")).isInstanceOf(IllegalStateException.class);
        assertThat(cacheManager.getCache("prices").get("SPO-001")).as("wyrzucone PRZED wywołaniem").isNull();
    }

    @Test
    @DisplayName("7. PUŁAPKA: @CachePut zwracający inny typ niż @Cacheable psuje pamięć")
    void cachePutWithWrongReturnType() {
        board.priceOf("ELE-001");
        assertThat(board.changePriceWrongType("ELE-001", 499999)).isEqualTo("OK");

        assertThat(cacheManager.getCache("prices").get("ELE-001").get()).as("w cache jest teraz String").isEqualTo("OK");
        assertThatThrownBy(() -> board.priceOf("ELE-001")).isInstanceOf(ClassCastException.class);
    }

    @Test
    @DisplayName("8. CacheManager: nazwy pamięci, natywna mapa, put/evict/clear ręcznie")
    void inspectingWithCacheManager() {
        board.priceOf("ELE-001");

        assertThat(cacheManager.getCacheNames()).contains("prices");
        Cache prices = cacheManager.getCache("prices");
        assertThat(prices).isInstanceOf(ConcurrentMapCache.class);
        assertThat(nativeMapOf("prices")).containsKey("ELE-001");

        prices.put("KSI-001", 1L);                       // ręczny zapis (bez wywołania metody serwisu)
        assertThat(board.priceOf("KSI-001")).isEqualTo(1L);
        assertThat(prices.evictIfPresent("KSI-001")).isTrue();
        prices.clear();
        assertThat(nativeMapOf("prices")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<Object, Object> nativeMapOf(String cacheName) {
        return (ConcurrentMap<Object, Object>) ((ConcurrentMapCache) cacheManager.getCache(cacheName)).getNativeCache();
    }
}
