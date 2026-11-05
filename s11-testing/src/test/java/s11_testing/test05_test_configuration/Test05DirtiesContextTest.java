package s11_testing.test05_test_configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sekcja 4 lekcji Test05TestConfiguration: {@code @DirtiesContext} (dirty = brudny) — „po tym teście wyrzuć
 * kontekst z pamięci podręcznej”. Tu {@code BEFORE_EACH_TEST_METHOD}: PRZED każdym testem Spring wyrzuca kontekst
 * z pamięci podręcznej, więc każdy test dostaje NOWY kontekst i nowy licznik ViewCounter od zera. Kolejność jest
 * ustalona CELOWO (@Order), żeby pokazać, co test 1 zostawiłby testowi 2.
 * <p>
 * Dlaczego BEFORE, a nie popularne AFTER_EACH_TEST_METHOD? Z AFTER pierwszy test tej klasy dostałby kontekst z pamięci
 * podręcznej, „pobrudzony” przez inną klasę o tej samej konfiguracji (sprawdzone: gdy wcześniej biegł Test05ResetStateTest,
 * który zostawia licznik na 2, test 1 widział 5 zamiast 3). AFTER sprząta PO SOBIE, BEFORE chroni PRZED innymi.
 * Koszt: każdy nowy kontekst to ponowny start aplikacji (Hibernate, pula połączeń…) — przy wielu testach minuty.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test05")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Test05DirtiesContextTest {

    private static ApplicationContext firstContext; // static: przetrwa między testami (każdy test to nowy obiekt klasy)

    @Autowired
    ViewCounter counter;

    @Autowired
    ApplicationContext context;

    @Test
    @Order(1)
    @DisplayName("Sekcja 4 (1 z 2): test nabija licznik do 3 — stan singletona w kontekście")
    void firstTestChangesState() {
        counter.register();
        counter.register();
        firstContext = context;

        assertThat(counter.register()).isEqualTo(3);
    }

    @Test
    @Order(2)
    @DisplayName("Sekcja 4 (2 z 2): po @DirtiesContext drugi test ma NOWY kontekst i licznik od zera")
    void secondTestGetsFreshContext() {
        assertThat(context).isNotSameAs(firstContext);
        assertThat(counter.count()).isZero();
    }
}
