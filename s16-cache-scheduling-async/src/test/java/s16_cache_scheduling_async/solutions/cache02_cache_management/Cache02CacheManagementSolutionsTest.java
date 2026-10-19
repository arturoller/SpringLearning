package s16_cache_scheduling_async.solutions.cache02_cache_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Cache02CacheManagementExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@SpringBootTest
class Cache02CacheManagementSolutionsTest {

    @Autowired
    SolutionPriceStore store;

    @Test
    @DisplayName("Wzorzec 1: czy pamięć ma wpis o danym kluczu")
    void solution1() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.getCache("prices").put("ELE-001", 1L);
        manager.getCache("prices").put("NULL-1", null);

        assertThat(Cache02CacheManagementSolutions.solution1(manager, "prices", "ELE-001")).isTrue();
        assertThat(Cache02CacheManagementSolutions.solution1(manager, "prices", "NULL-1")).isTrue();
        assertThat(Cache02CacheManagementSolutions.solution1(manager, "prices", "SPO-001")).isFalse();
        assertThat(Cache02CacheManagementSolutions.solution1(new ConcurrentMapCacheManager("prices"), "inna", "x")).isFalse();
    }

    @Test
    @DisplayName("Wzorzec 2: wyczyść pamięć i zwróć liczbę usuniętych wpisów")
    void solution2() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.getCache("prices").put("ELE-001", 1L);
        manager.getCache("prices").put("SPO-001", 2L);

        assertThat(Cache02CacheManagementSolutions.solution2(manager, "prices")).isEqualTo(2);
        assertThat(Cache02CacheManagementSolutions.solution2(manager, "prices")).isZero();
        assertThat(Cache02CacheManagementSolutions.solution2(new ConcurrentMapCacheManager("prices"), "inna")).isZero();
    }

    @Test
    @DisplayName("Wzorzec 3: @CachePut odświeża wpis po zmianie ceny")
    void solution3() {
        assertThat(store.priceOf("ELE-001")).isEqualTo(549999);
        store.changePrice("ELE-001", 499999);

        assertThat(store.priceOf("ELE-001")).isEqualTo(499999);
        assertThat(store.loads()).isEqualTo(1);
    }
}
