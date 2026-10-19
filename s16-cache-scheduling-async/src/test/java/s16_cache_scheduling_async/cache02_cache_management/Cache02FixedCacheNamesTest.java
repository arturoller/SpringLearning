package s16_cache_scheduling_async.cache02_cache_management;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Osobna klasa testowa, bo zmienia ustawienia kontekstu: spring.cache.cache-names. */
@SpringBootTest(properties = "spring.cache.cache-names=prices")
class Cache02FixedCacheNamesTest {

    @Autowired
    PriceBoard board;

    @Autowired
    CacheManager cacheManager;

    @Test
    @DisplayName("9. spring.cache.cache-names zamyka listę pamięci — nieznana nazwa to błąd")
    void fixedNamesRejectUnknownCache() {
        assertThat(cacheManager.getCacheNames()).containsExactly("prices");
        assertThat(board.priceOf("ELE-001")).isEqualTo(549999);

        // metoda używa pamięci "priceList", której nie ma na liście:
        assertThatThrownBy(board::priceList)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot find cache named 'priceList'");
    }
}
