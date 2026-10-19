package s16_cache_scheduling_async.cache02_cache_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Cache02CacheManagement. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Cache02CacheManagementExercisesTest {

    @Autowired
    ExercisePriceStore store;

    @Test
    @DisplayName("Ćwiczenie 1: czy pamięć ma wpis o danym kluczu")
    void exercise1() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.getCache("prices").put("ELE-001", 1L);
        manager.getCache("prices").put("NULL-1", null);

        assertThat(Cache02CacheManagement.exercise1(manager, "prices", "ELE-001")).isTrue();
        assertThat(Cache02CacheManagement.exercise1(manager, "prices", "NULL-1")).as("wpis z null też jest wpisem").isTrue();
        assertThat(Cache02CacheManagement.exercise1(manager, "prices", "SPO-001")).isFalse();
        // dla menedżera ze stałą listą nazw nieznana pamięć = getCache zwraca null
        assertThat(Cache02CacheManagement.exercise1(fixed(), "inna", "x")).isFalse();
    }

    @Test
    @DisplayName("Ćwiczenie 2: wyczyść pamięć i zwróć liczbę usuniętych wpisów")
    void exercise2() {
        ConcurrentMapCacheManager manager = new ConcurrentMapCacheManager();
        manager.getCache("prices").put("ELE-001", 1L);
        manager.getCache("prices").put("SPO-001", 2L);

        assertThat(Cache02CacheManagement.exercise2(manager, "prices")).isEqualTo(2);
        assertThat(Cache02CacheManagement.exercise2(manager, "prices")).isZero();
        assertThat(Cache02CacheManagement.exercise2(fixed(), "inna")).isZero();
    }

    @Test
    @DisplayName("Ćwiczenie 3: @CachePut odświeża wpis po zmianie ceny")
    void exercise3() {
        assertThat(store.priceOf("ELE-001")).isEqualTo(549999);
        store.changePrice("ELE-001", 499999);

        assertThat(store.priceOf("ELE-001")).isEqualTo(499999);
        assertThat(store.loads()).as("po zmianie ceny nie było drugiego odczytu z bazy").isEqualTo(1);
    }

    private static ConcurrentMapCacheManager fixed() {
        return new ConcurrentMapCacheManager("prices"); // stała lista nazw: nieznana pamięć = null
    }
}
