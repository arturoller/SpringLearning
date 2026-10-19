package s16_cache_scheduling_async.solutions.sched03_scheduled;

import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Sched03Scheduled — zajrzyj dopiero po własnej próbie! */
public final class Sched03ScheduledSolutions {

    private Sched03ScheduledSolutions() {
    }

    static String solution1() {
        return "0 30 6 * * MON"; // sekundy minuty godziny dzień-miesiąca miesiąc dzień-tygodnia
    }

    static List<ZonedDateTime> solution2(ZonedDateTime from, String cron, int n) {
        CronExpression expression = CronExpression.parse(cron);
        List<ZonedDateTime> result = new ArrayList<>();
        ZonedDateTime current = from;
        for (int i = 0; i < n; i++) {
            current = expression.next(current);
            result.add(current);
        }
        return result;
    }

    static Instant solution3(Instant now, String cron, ZoneId zone) {
        return CronExpression.parse(cron).next(now.atZone(zone)).toInstant();
    }
}
