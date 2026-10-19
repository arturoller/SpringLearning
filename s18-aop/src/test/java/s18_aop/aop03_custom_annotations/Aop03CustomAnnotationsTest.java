package s18_aop.aop03_custom_annotations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * Testy lekcji Aop03CustomAnnotations. {@code @SpringBootTest} — aspekty działają tylko w kontekście Springa.
 * {@code @MockitoBean Clock clock} podmienia bean zegara z Aop03Application na atrapę (mock): test sam mówi, jaki czas
 * „pokaże” zegar przy kolejnych odczytach, więc wynik pomiaru jest zawsze ten sam (żadnych prawdziwych pomiarów).
 */
@SpringBootTest
class Aop03CustomAnnotationsTest {

    @MockitoBean
    Clock clock;

    @Autowired
    PriceService priceService;

    @Autowired
    OrderService orderService;

    @Autowired
    ReportService reportService;

    @Autowired
    PriceArchive priceArchive;

    @Autowired
    AuditJournal journal;

    @BeforeEach
    void clearJournal() {
        journal.clear();
    }

    @Test
    @DisplayName("1. @AuditLog: aspekt odczytuje atrybut action adnotacji i zapisuje start, argumenty i wynik")
    void auditReadsAnnotationAttribute() {
        BigDecimal result = priceService.changePrice("SPO-001", new BigDecimal("59.99"));

        assertThat(result).isEqualByComparingTo("59.99");
        assertThat(journal.entries()).containsExactly(
                "AUDYT start ZMIANA_CENY [SPO-001, 59.99]",
                "METODA changePrice(SPO-001, 59.99)",
                "AUDYT OK ZMIANA_CENY");
    }

    @Test
    @DisplayName("1. PUŁAPKA: adnotacji bez @Retention(RUNTIME) i adnotacji z metody interfejsu aspekt nie widzi")
    void invisibleAnnotations() {
        priceService.previewPrice("KSI-001");
        priceArchive.archive("KSI-001");

        assertThat(journal.entries()).as("żadnego wpisu AUDYT").containsExactly(
                "METODA previewPrice(KSI-001)",
                "METODA archive(KSI-001)");
        assertThat(AopUtils.isAopProxy(priceArchive)).as("do PriceArchiveService nic nie pasuje — brak proxy").isFalse();
    }

    @Test
    @DisplayName("2. @MeasureTime: próg z atrybutu, czas z podmienionego zegara; adnotacja metody wygrywa z adnotacją klasy")
    void measureTimeOnClassAndMethod() {
        given(clock.millis()).willReturn(1_000L, 1_050L, 2_000L, 2_050L);

        reportService.dailyReport();
        reportService.monthlyReport();

        assertThat(journal.entries()).as("po jednym pomiarze na metodę").containsExactly(
                "POMIAR dailyReport: 50 ms (OK, próg 1000 ms)",
                "POMIAR monthlyReport: 50 ms (WOLNO, próg 10 ms)");
    }

    @Test
    @DisplayName("3. Kolejność: AuditAspect z @Order(1) na zewnątrz, MeasureTimeAspect z @Order(2) wewnątrz")
    void orderOfAspects() {
        given(clock.millis()).willReturn(1_000L, 1_040L);

        String result = orderService.placeOrder("SPO-001", 2);

        assertThat(result).isEqualTo("Zamówienie przyjęte: 2 x SPO-001");
        assertThat(journal.entries()).containsExactly(
                "AUDYT start NOWE_ZAMOWIENIE [SPO-001, 2]",
                "METODA placeOrder(SPO-001, 2)",
                "POMIAR placeOrder: 40 ms (OK, próg 100 ms)",
                "AUDYT OK NOWE_ZAMOWIENIE");
    }

    @Test
    @DisplayName("3. Przy wyjątku pomiar i audyt zapisują błąd, a ten sam wyjątek dociera do wołającego")
    void orderOfAspectsOnError() {
        given(clock.millis()).willReturn(1_000L, 1_150L);

        assertThatThrownBy(() -> orderService.placeOrder("ELE-002", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Brak towaru: ELE-002 (stan 0, zamówiono 1)")
                .hasNoCause();

        assertThat(journal.entries()).containsExactly(
                "AUDYT start NOWE_ZAMOWIENIE [ELE-002, 1]",
                "METODA placeOrder(ELE-002, 1)",
                "POMIAR placeOrder: 150 ms (WOLNO, próg 100 ms)",
                "AUDYT BŁĄD NOWE_ZAMOWIENIE: Brak towaru: ELE-002 (stan 0, zamówiono 1)");
    }

    @Test
    @DisplayName("3. PUŁAPKA: @Order na metodzie porady jest ignorowany — liczy się @Order na klasie aspektu")
    void orderOnAdviceMethodIsIgnored() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
                .withBean(AuditJournal.class)
                .withBean(AuditAspect.class)
                .withBean(OrderOnMethodAspect.class)
                .withBean(PriceService.class)
                .run(context -> {
                    context.getBean(PriceService.class).changePrice("KSI-001", new BigDecimal("75.00"));

                    assertThat(context.getBean(AuditJournal.class).entries()).containsExactly(
                            "AUDYT start ZMIANA_CENY [KSI-001, 75.00]",
                            "PORADA z @Order(-100) na metodzie",
                            "METODA changePrice(KSI-001, 75.00)",
                            "AUDYT OK ZMIANA_CENY");
                });
    }

    @Test
    @DisplayName("4. PUŁAPKA: aspekt połykający wyjątki — wołający dostaje null zamiast błędu")
    void swallowingAspectHidesErrors() {
        String result = orderService.placeOrderSafely("ELE-002", 1);

        assertThat(result).isNull();
        assertThat(journal.entries()).containsExactly(
                "METODA placeOrderSafely(ELE-002, 1)",
                "POŁKNIĘTY IllegalStateException: Brak towaru: ELE-002 (stan 0, zamówiono 1)");
        // błąd wychodzi dopiero dalej, w kodzie, który ufa wynikowi — daleko od prawdziwej przyczyny
        assertThatThrownBy(() -> result.length()).isInstanceOf(NullPointerException.class);
    }
}
