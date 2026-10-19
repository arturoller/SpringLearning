package s13_rest_client.rc02_errors_and_timeouts;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * SlowSupplierGate = „szlaban” dla WOLNEGO dostawcy (gate = brama, szlaban). Udawany serwer, pytany o produkt WOLNY,
 * czeka przy szlabanie, aż test go podniesie ({@link #open()}) — najdłużej {@code maxWait}. Dzięki temu test limitu
 * czasu nie zależy od zegara: serwer NA PEWNO nie odpowie, zanim klientowi minie limit, bo czeka na zatrzask
 * (CountDownLatch — t21_concurrency/Concurrency07Synchronizers), a nie „śpi 2 sekundy”.
 */
@Component
public class SlowSupplierGate {

    private volatile CountDownLatch latch = new CountDownLatch(1);   // zamknięty od startu

    /** Opuszcza szlaban: kolejne zapytania o WOLNY będą czekać (wywołaj na początku testu). */
    public void close() {
        latch = new CountDownLatch(1);
    }

    /** Podnosi szlaban: czekające zapytania dostają odpowiedź od razu (wywołaj w finally testu). */
    public void open() {
        latch.countDown();
    }

    /** Wywoływane przez udawany serwer: czekaj na podniesienie szlabanu, najdłużej maxWait. */
    void awaitOpen(Duration maxWait) throws InterruptedException {
        latch.await(maxWait.toMillis(), TimeUnit.MILLISECONDS);
    }
}
