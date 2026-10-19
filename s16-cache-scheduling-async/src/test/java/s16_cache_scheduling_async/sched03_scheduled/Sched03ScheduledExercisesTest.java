package s16_cache_scheduling_async.sched03_scheduled;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Testy ĆWICZEŃ lekcji Sched03Scheduled. @Tag("cwiczenie") — uruchamiasz sam (▶ obok klasy). */
@Tag("cwiczenie")
class Sched03ScheduledExercisesTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Test
    @DisplayName("Ćwiczenie 1: cron na „co poniedziałek o 6:30”")
    void exercise1() {
        CronExpression cron = CronExpression.parse(Sched03Scheduled.exercise1());

        ZonedDateTime sunday = ZonedDateTime.of(2026, 10, 18, 12, 0, 0, 0, WARSAW);
        ZonedDateTime monday = cron.next(sunday);
        assertThat(monday).isEqualTo(ZonedDateTime.of(2026, 10, 19, 6, 30, 0, 0, WARSAW));
        assertThat(cron.next(monday)).isEqualTo(ZonedDateTime.of(2026, 10, 26, 6, 30, 0, 0, WARSAW));
    }

    @Test
    @DisplayName("Ćwiczenie 2: n kolejnych terminów cron")
    void exercise2() {
        ZonedDateTime from = ZonedDateTime.of(2026, 10, 22, 9, 0, 0, 0, WARSAW); // czwartek

        assertThat(Sched03Scheduled.exercise2(from, "0 0 8 * * MON-FRI", 3)).containsExactly(
                ZonedDateTime.of(2026, 10, 23, 8, 0, 0, 0, WARSAW),
                ZonedDateTime.of(2026, 10, 26, 8, 0, 0, 0, WARSAW),
                ZonedDateTime.of(2026, 10, 27, 8, 0, 0, 0, WARSAW));
        assertThat(Sched03Scheduled.exercise2(from, "0 0 8 * * MON-FRI", 0)).isEmpty();
        assertThat(Sched03Scheduled.exercise2(ZonedDateTime.of(2026, 10, 23, 8, 0, 0, 0, WARSAW), "0 0 8 * * *", 1))
                .as("termin jest STRICTE późniejszy niż from")
                .containsExactly(ZonedDateTime.of(2026, 10, 24, 8, 0, 0, 0, WARSAW));
    }

    @Test
    @DisplayName("Ćwiczenie 3: najbliższy termin jako Instant w danej strefie")
    void exercise3() {
        Instant now = Instant.parse("2026-07-14T20:00:00Z");

        assertThat(Sched03Scheduled.exercise3(now, "0 0 8 * * *", WARSAW)).isEqualTo(Instant.parse("2026-07-15T06:00:00Z"));
        assertThat(Sched03Scheduled.exercise3(now, "0 0 8 * * *", ZoneId.of("UTC"))).isEqualTo(Instant.parse("2026-07-15T08:00:00Z"));
        assertThat(Sched03Scheduled.exercise3(Instant.parse("2026-01-14T20:00:00Z"), "0 0 8 * * *", WARSAW))
                .isEqualTo(Instant.parse("2026-01-15T07:00:00Z"));
    }
}
