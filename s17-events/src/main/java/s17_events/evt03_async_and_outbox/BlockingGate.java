package s17_events.evt03_async_and_outbox;

import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * „Bramka” z zatrzaskiem ({@code CountDownLatch}): asynchroniczny słuchacz czeka na niej, dopóki test jej nie otworzy.
 * Dzięki temu test może SPRAWDZIĆ, że publikujący wrócił, zanim słuchacz skończył — bez {@code sleep} i bez zgadywania czasu.
 */
@Component
public class BlockingGate {

    private volatile CountDownLatch latch = new CountDownLatch(1);

    /** Zamyka bramkę na nowo (przed każdym testem). */
    public void close() {
        latch = new CountDownLatch(1);
    }

    public void open() {
        latch.countDown();
    }

    /** Czeka na otwarcie bramki, ale najwyżej 10 sekund (zabezpieczenie, żeby błąd w teście nie zawiesił budowania). */
    public boolean awaitOpen() throws InterruptedException {
        return latch.await(10, TimeUnit.SECONDS);
    }
}
