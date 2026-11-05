package s11_testing.solutions.test05_test_configuration;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Rozwiązanie ćwiczenia 5 lekcji Test05TestConfiguration: zegar testowy, któremu test PRZESTAWIA czas
 * ({@code set}). Jeden kontekst, wiele chwil — bez nowego kontekstu dla każdej daty.
 */
public final class MutableClock extends Clock {

    private volatile Instant now; // volatile = zmiana widoczna od razu dla wszystkich wątków

    public MutableClock(Instant start) {
        this.now = start;
    }

    public void set(Instant instant) {
        this.now = instant;
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }
}
