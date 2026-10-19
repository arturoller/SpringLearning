package s16_cache_scheduling_async.sched03_scheduled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.FixedRateTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.scheduling.config.Task;
import org.springframework.scheduling.support.CronExpression;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Sched03Scheduled. Nigdzie nie czekamy na prawdziwy czas: konfigurację zadań czytamy z
 * ScheduledTaskHolder, wyrażenia cron liczymy dla ustalonej chwili, a logikę zadań wołamy wprost z zegarem Clock.fixed.
 */
@SpringBootTest
class Sched03ScheduledTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    ScheduledTaskHolder holder;

    @Autowired
    ThreadPoolTaskScheduler taskScheduler;

    @Test
    @DisplayName("1. @Scheduled rejestruje trzy zadania — ich ustawienia można odczytać bez czekania")
    void registeredTasks() {
        List<Task> tasks = holder.getScheduledTasks().stream()
                .map(ScheduledTask::getTask)
                .sorted(Comparator.comparing(Sched03ScheduledTest::methodName))
                .toList();

        assertThat(tasks).hasSize(3);
        assertThat(tasks.stream().map(Sched03ScheduledTest::methodName))
                .containsExactly("cleanTemporaryFiles", "morningReport", "refreshStock");

        FixedDelayTask delay = (FixedDelayTask) tasks.get(0);
        assertThat(delay.getIntervalDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(delay.getInitialDelayDuration()).isEqualTo(Duration.ofHours(1));

        CronTask cron = (CronTask) tasks.get(1);
        assertThat(cron.getExpression()).isEqualTo("0 0 8 * * MON-FRI");

        FixedRateTask rate = (FixedRateTask) tasks.get(2);
        assertThat(rate.getIntervalDuration()).isEqualTo(Duration.ofMinutes(30));
        assertThat(rate.getInitialDelayDuration()).isEqualTo(Duration.ofHours(1));
    }

    private static String methodName(Task task) {
        // Runnable zadania to opakowanie Task.OutcomeTrackingRunnable; jego toString() oddaje „Klasa.metoda”.
        String text = task.getRunnable().toString();
        return text.substring(text.lastIndexOf('.') + 1);
    }

    @Test
    @DisplayName("2. CronExpression.next liczy kolejny termin dla USTALONEJ chwili — bez czekania")
    void cronNextWithFixedTime() {
        CronExpression weekdays8 = CronExpression.parse("0 0 8 * * MON-FRI");

        ZonedDateTime mondayBefore = ZonedDateTime.of(2026, 10, 19, 7, 59, 0, 0, WARSAW);
        assertThat(mondayBefore.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(weekdays8.next(mondayBefore)).isEqualTo(ZonedDateTime.of(2026, 10, 19, 8, 0, 0, 0, WARSAW));

        // Piątek 8:00 → następny termin to poniedziałek (sobota i niedziela pominięte).
        ZonedDateTime fridayAt8 = ZonedDateTime.of(2026, 10, 23, 8, 0, 0, 0, WARSAW);
        assertThat(weekdays8.next(fridayAt8)).isEqualTo(ZonedDateTime.of(2026, 10, 26, 8, 0, 0, 0, WARSAW));
    }

    @Test
    @DisplayName("3. PUŁAPKA: cron Springa ma 6 pól — zapis z Linuksa (5 pól) jest odrzucany")
    void cronNeedsSixFields() {
        assertThatThrownBy(() -> CronExpression.parse("0 8 * * MON-FRI"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("6 fields");

        assertThat(CronExpression.isValidExpression("0 8 * * MON-FRI")).isFalse();
        assertThat(CronExpression.isValidExpression("0 0 8 * * MON-FRI")).isTrue();
        // przydatne zapisy z lekcji: makro @daily i „co 15 minut”
        ZonedDateTime from = ZonedDateTime.of(2026, 10, 19, 10, 7, 0, 0, WARSAW);
        assertThat(CronExpression.parse("@daily").next(from)).isEqualTo(ZonedDateTime.of(2026, 10, 20, 0, 0, 0, 0, WARSAW));
        assertThat(CronExpression.parse("0 */15 * * * *").next(from)).isEqualTo(ZonedDateTime.of(2026, 10, 19, 10, 15, 0, 0, WARSAW));
    }

    @Test
    @DisplayName("4. PUŁAPKA: gwiazdka w polu sekund oznacza „co sekundę” — '* * 8 * * *' to nie „raz o 8:00”")
    void starInSecondsField() {
        ZonedDateTime at8 = ZonedDateTime.of(2026, 10, 19, 8, 0, 0, 0, WARSAW);

        assertThat(CronExpression.parse("* * 8 * * *").next(at8)).as("co sekundę przez całą godzinę 8:xx")
                .isEqualTo(at8.plusSeconds(1));
        assertThat(CronExpression.parse("0 0 8 * * *").next(at8)).as("raz dziennie")
                .isEqualTo(at8.plusDays(1));
    }

    @Test
    @DisplayName("5. Strefa czasowa ma znaczenie: ta sama 8:00 to inna chwila w Warszawie latem i zimą")
    void cronZones() {
        CronExpression eight = CronExpression.parse("0 0 8 * * *");

        ZonedDateTime summer = eight.next(ZonedDateTime.of(2026, 7, 15, 0, 0, 0, 0, WARSAW));
        ZonedDateTime winter = eight.next(ZonedDateTime.of(2026, 1, 14, 0, 0, 0, 0, WARSAW));
        assertThat(summer.toInstant()).isEqualTo(Instant.parse("2026-07-15T06:00:00Z")); // czas letni UTC+2
        assertThat(winter.toInstant()).isEqualTo(Instant.parse("2026-01-14T07:00:00Z")); // czas zimowy UTC+1

        // Bez zone serwer w UTC uruchomiłby zadanie o 8:00 UTC, czyli o 9:00 lub 10:00 w Warszawie.
        ZonedDateTime utc = eight.next(ZonedDateTime.of(2026, 7, 15, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(utc.toInstant()).isEqualTo(Instant.parse("2026-07-15T08:00:00Z"));
    }

    @Test
    @DisplayName("6. Clock w logice zadania: test podaje zamrożony czas i woła metodę wprost")
    void clockInTaskLogic() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-19T06:00:00Z"), ZoneOffset.UTC);
        StockReportTasks tasks = new StockReportTasks(fixed);

        tasks.morningReport();

        assertThat(tasks.log()).containsExactly("raport poranny 2026-10-19T06:00:00Z");
    }

    @Test
    @DisplayName("7. Pula wątków harmonogramu ma domyślnie JEDEN wątek o nazwie scheduling-N")
    void schedulerPoolHasOneThread() throws Exception {
        assertThat(taskScheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
        assertThat(taskScheduler.getThreadNamePrefix()).isEqualTo("scheduling-");

        Future<String> threadName = taskScheduler.submit(() -> Thread.currentThread().getName());
        assertThat(threadName.get(10, TimeUnit.SECONDS)).startsWith("scheduling-"); // 10 s = tylko bezpiecznik
    }

    @Test
    @DisplayName("8. Wyjątek w zadaniu powtarzalnym jest zalogowany, ale NIE zatrzymuje kolejnych uruchomień")
    void exceptionDoesNotStopRepeatingTask() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch threeRuns = new CountDownLatch(3);

        Future<?> future = taskScheduler.scheduleAtFixedRate(() -> {
            int run = runs.incrementAndGet();
            threeRuns.countDown();
            if (run == 1) {
                throw new IllegalStateException("pierwsze uruchomienie się nie udało (celowo, w teście)");
            }
        }, Duration.ofMillis(5));

        assertThat(threeRuns.await(10, TimeUnit.SECONDS)).as("zadanie wykonało się co najmniej 3 razy mimo wyjątku").isTrue();
        future.cancel(false);
    }

    @Test
    @DisplayName("9. PUŁAPKA: metoda @Scheduled z parametrem kończy start kontekstu błędem")
    void scheduledMethodWithParameterFails() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(SchedulingOn.class, BadScheduledBean.class);
            assertThatThrownBy(context::refresh)
                    .isInstanceOf(BeanCreationException.class)
                    .hasMessageContaining("Could not create recurring task for @Scheduled method 'withParameter'")
                    .hasMessageContaining("Only no-arg methods may be annotated with @Scheduled");
        }
    }

    /** Minimalny zwykły kontekst Springa z włączonym planowaniem (bez Boota). */
    @EnableScheduling
    static class SchedulingOn {
    }
}
