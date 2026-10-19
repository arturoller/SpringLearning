package s17_events.solutions.evt02_transactional_events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import s17_events.evt02_transactional_events.Evt02TestTransactions;
import s17_events.evt02_transactional_events.OrderPlaced;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Evt02TransactionalEventsExercisesTest na rozwiązaniach wzorcowych. @Tag("wzorzec") — biegną zawsze. */
@Tag("wzorzec")
class Evt02TransactionalEventsSolutionsTest {

    private static OrderPlaced order(String id, String total) {
        return new OrderPlaced(id, "Jan Kowalski", new BigDecimal(total));
    }

    @Test
    @DisplayName("Rozwiązanie 1: zapis dopiero po zatwierdzeniu transakcji")
    void solution1() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEventsSolutions.Solution1ConfirmedListener.class)) {
            Evt02TransactionalEventsSolutions.Solution1ConfirmedListener listener =
                    context.getBean(Evt02TransactionalEventsSolutions.Solution1ConfirmedListener.class);
            List<Integer> sizeInsideTransaction = new ArrayList<>();

            context.getBean(TransactionTemplate.class).executeWithoutResult(status -> {
                context.publishEvent(order("ZAM-001", "349.90"));
                sizeInsideTransaction.add(listener.confirmed.size());
            });

            assertThat(sizeInsideTransaction).containsExactly(0);
            assertThat(listener.confirmed).containsExactly("ZAM-001");
        }
    }

    @Test
    @DisplayName("Rozwiązanie 2: zapis tylko po wycofaniu transakcji")
    void solution2() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEventsSolutions.Solution2CancelledListener.class)) {
            Evt02TransactionalEventsSolutions.Solution2CancelledListener listener =
                    context.getBean(Evt02TransactionalEventsSolutions.Solution2CancelledListener.class);
            TransactionTemplate template = context.getBean(TransactionTemplate.class);

            template.executeWithoutResult(status -> context.publishEvent(order("ZAM-001", "349.90")));
            template.executeWithoutResult(status -> {
                context.publishEvent(order("ZAM-002", "64.99"));
                status.setRollbackOnly();
            });

            assertThat(listener.cancelled).containsExactly("ZAM-002");
        }
    }

    @Test
    @DisplayName("Rozwiązanie 3: słuchacz reaguje po commicie i także przy publikacji bez transakcji")
    void solution3() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEventsSolutions.Solution3FallbackListener.class)) {
            Evt02TransactionalEventsSolutions.Solution3FallbackListener listener =
                    context.getBean(Evt02TransactionalEventsSolutions.Solution3FallbackListener.class);

            context.getBean(TransactionTemplate.class)
                    .executeWithoutResult(status -> context.publishEvent(order("ZAM-001", "349.90")));
            context.publishEvent(order("ZAM-002", "64.99"));

            assertThat(listener.received).containsExactly("ZAM-001", "ZAM-002");
        }
    }

    @Test
    @DisplayName("Rozwiązanie 4: weto w BEFORE_COMMIT wycofuje transakcję przy kwocie powyżej 10000 zł")
    void solution4() {
        try (AnnotationConfigApplicationContext context =
                     Evt02TestTransactions.contextWith(Evt02TransactionalEventsSolutions.Solution4LimitListener.class)) {
            TransactionTemplate template = context.getBean(TransactionTemplate.class);

            template.executeWithoutResult(status -> context.publishEvent(order("ZAM-001", "9999.00")));

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
