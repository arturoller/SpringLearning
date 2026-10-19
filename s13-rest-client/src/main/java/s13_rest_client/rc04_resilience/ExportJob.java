package s13_rest_client.rc04_resilience;

import org.springframework.resilience.annotation.ConcurrencyLimit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ExportJob = eksport cennika do dostawcy — wolno uruchomić tylko JEDEN naraz, a kolejne próby mają być ODRZUCONE
 * od razu (nie czekać): {@code @ConcurrencyLimit(limit = 1, policy = REJECT)} (od Spring Framework 7.0.3).
 * Zatrzaski z parametrów pozwalają testowi trzymać pierwsze wywołanie „w środku” bez {@code sleep}.
 */
public class ExportJob {

    @ConcurrencyLimit(limit = 1, policy = ConcurrencyLimit.ThrottlePolicy.REJECT)
    public void export(CountDownLatch started, CountDownLatch finish) throws InterruptedException {
        started.countDown();                     // „jestem w środku”
        finish.await(5, TimeUnit.SECONDS);       // czekam, aż test pozwoli skończyć (najwyżej 5 s)
    }
}
