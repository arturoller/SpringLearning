package s16_cache_scheduling_async.cache01_cacheable;

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

/** Testy ĆWICZEŃ lekcji Cache01Cacheable. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
@SpringBootTest
class Cache01CacheableExercisesTest {

    @Autowired
    ExerciseStockService stock;

    @Test
    @DisplayName("Ćwiczenie 1: wyrażenie SpEL na klucz kategoria:strona")
    void exercise1() {
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("category", "ELEKTRONIKA");
        context.setVariable("page", 2);
        String value = new SpelExpressionParser().parseExpression(Cache01Cacheable.exercise1()).getValue(context, String.class);
        assertThat(value).isEqualTo("ELEKTRONIKA:2");
    }

    @Test
    @DisplayName("Ćwiczenie 2: ręczne „pobierz albo policz i zapamiętaj”")
    void exercise2() {
        Map<String, Integer> cache = new HashMap<>();
        AtomicInteger loads = new AtomicInteger();

        assertThat(Cache01Cacheable.exercise2(cache, "kawa", k -> { loads.incrementAndGet(); return 5; })).isEqualTo(5);
        assertThat(Cache01Cacheable.exercise2(cache, "kawa", k -> { loads.incrementAndGet(); return 6; })).isEqualTo(5);
        assertThat(loads).hasValue(1);

        // null nie jest zapisywany, więc loader jest wołany ponownie
        assertThat(Cache01Cacheable.exercise2(cache, "brak", k -> { loads.incrementAndGet(); return null; })).isNull();
        assertThat(Cache01Cacheable.exercise2(cache, "brak", k -> { loads.incrementAndGet(); return null; })).isNull();
        assertThat(loads).hasValue(3);
        assertThat(cache).containsOnlyKeys("kawa");
    }

    @Test
    @DisplayName("Ćwiczenie 3: @Cacheable z unless dla zapasu zerowego")
    void exercise3() {
        stock.stockOf("ELE-001");
        stock.stockOf("ELE-001");
        assertThat(stock.loads()).as("ELE-001 ma zapas 7 — drugi odczyt z cache").isEqualTo(1);

        stock.stockOf("ELE-002");
        stock.stockOf("ELE-002");
        assertThat(stock.loads()).as("ELE-002 ma zapas 0 — nie wolno go zapamiętać").isEqualTo(3);
    }
}
