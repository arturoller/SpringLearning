package s17_events.evt02_transactional_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Evt02TransactionalEvents. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy).
 * Mały kontekst Springa z prawdziwą transakcją (Evt02TestTransactions) — pełny Boot jest tu zbędny.
 * Uwaga: wyjątek ze słuchacza AFTER_COMMIT/AFTER_ROLLBACK trafia tylko do logu, więc źle napisane ćwiczenie 1–3
 * zobaczysz jako błąd asercji (pusta lista), a w logu — komunikat „threw exception”.
 */
@Tag("cwiczenie")
class Evt02TransactionalEventsExercisesTest {

    private static OrderPlaced order(String id, String total) {
        return new OrderPlaced(id, "Jan Kowalski", new BigDecimal(total));
    }

    @Test
    @DisplayName("Ćwiczenie 1: zapis dopiero po zatwierdzeniu transakcji")
    void exercise1() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEvents.Exercise1ConfirmedListener.class)) {
            Evt02TransactionalEvents.Exercise1ConfirmedListener listener =
                    context.getBean(Evt02TransactionalEvents.Exercise1ConfirmedListener.class);
            List<Integer> sizeInsideTransaction = new ArrayList<>();

            context.getBean(TransactionTemplate.class).executeWithoutResult(status -> {
                context.publishEvent(order("ZAM-001", "349.90"));
                sizeInsideTransaction.add(listener.confirmed.size());   // jeszcze w transakcji — lista ma być pusta
            });

            assertThat(sizeInsideTransaction).containsExactly(0);
            assertThat(listener.confirmed).containsExactly("ZAM-001");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 2: zapis tylko po wycofaniu transakcji")
    void exercise2() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEvents.Exercise2CancelledListener.class)) {
            Evt02TransactionalEvents.Exercise2CancelledListener listener =
                    context.getBean(Evt02TransactionalEvents.Exercise2CancelledListener.class);
            TransactionTemplate template = context.getBean(TransactionTemplate.class);

            template.executeWithoutResult(status -> context.publishEvent(order("ZAM-001", "349.90")));   // commit
            template.executeWithoutResult(status -> {
                context.publishEvent(order("ZAM-002", "64.99"));
                status.setRollbackOnly();                                                                  // rollback
            });

            assertThat(listener.cancelled).containsExactly("ZAM-002");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 3: słuchacz reaguje po commicie i także przy publikacji bez transakcji")
    void exercise3() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEvents.Exercise3FallbackListener.class)) {
            Evt02TransactionalEvents.Exercise3FallbackListener listener =
                    context.getBean(Evt02TransactionalEvents.Exercise3FallbackListener.class);

            context.getBean(TransactionTemplate.class)
                    .executeWithoutResult(status -> context.publishEvent(order("ZAM-001", "349.90")));
            context.publishEvent(order("ZAM-002", "64.99"));   // bez transakcji

            assertThat(listener.received).containsExactly("ZAM-001", "ZAM-002");
        }
    }

    @Test
    @DisplayName("Ćwiczenie 4: weto w BEFORE_COMMIT wycofuje transakcję przy kwocie powyżej 10000 zł")
    void exercise4() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEvents.Exercise4LimitListener.class)) {
            TransactionTemplate template = context.getBean(TransactionTemplate.class);

            template.executeWithoutResult(status -> context.publishEvent(order("ZAM-001", "9999.00")));   // bez błędu

            List<Integer> completionStatuses = new ArrayList<>();
            assertThatThrownBy(() -> template.executeWithoutResult(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int completionStatus) {
                        completionStatuses.add(completionStatus);
                    }
                });
                context.publishEvent(order("ZAM-002", "10500.00"));
            })).isInstanceOf(IllegalStateException.class).hasMessage("Limit przekroczony");

            assertThat(completionStatuses).containsExactly(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
    }
}
