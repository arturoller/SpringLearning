package s16_cache_scheduling_async.solutions.cache01_cacheable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprawdzenia z Cache01CacheableExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
@SpringBootTest
class Cache01CacheableSolutionsTest {

    @Autowired
    SolutionStockService stock;

    @Test
    @DisplayName("Wzorzec 1: wyrażenie SpEL na klucz kategoria:strona")
    void solution1() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("category", "ELEKTRONIKA");
        context.setVariable("page", 2);
        String value = new SpelExpressionParser().parseExpression(Cache01CacheableSolutions.solution1())
                .getValue(context, String.class);
        assertThat(value).isEqualTo("ELEKTRONIKA:2");
    }

    @Test
    @DisplayName("Wzorzec 2: ręczne „pobierz albo policz i zapamiętaj”")
    void solution2() {
        Map<String, Integer> cache = new HashMap<>();
        AtomicInteger loads = new AtomicInteger();

        assertThat(Cache01CacheableSolutions.solution2(cache, "kawa", k -> { loads.incrementAndGet(); return 5; })).isEqualTo(5);
        assertThat(Cache01CacheableSolutions.solution2(cache, "kawa", k -> { loads.incrementAndGet(); return 6; })).isEqualTo(5);
        assertThat(loads).hasValue(1);
        assertThat(Cache01CacheableSolutions.solution2(cache, "brak", k -> { loads.incrementAndGet(); return null; })).isNull();
        assertThat(Cache01CacheableSolutions.solution2(cache, "brak", k -> { loads.incrementAndGet(); return null; })).isNull();
        assertThat(loads).hasValue(3);
        assertThat(cache).containsOnlyKeys("kawa");
    }

    @Test
    @DisplayName("Wzorzec 3: @Cacheable z unless dla zapasu zerowego")
    void solution3() {
        stock.stockOf("ELE-001");
        stock.stockOf("ELE-001");
        assertThat(stock.loads()).isEqualTo(1);

        stock.stockOf("ELE-002");
        stock.stockOf("ELE-002");
        assertThat(stock.loads()).isEqualTo(3);
    }
}
