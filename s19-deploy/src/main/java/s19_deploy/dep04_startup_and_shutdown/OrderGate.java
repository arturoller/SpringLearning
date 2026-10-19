package s19_deploy.dep04_startup_and_shutdown;

import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * „Bramka” udająca wolne przetwarzanie zamówienia: żądanie wchodzi i CZEKA, aż test (albo Ty) ją otworzy.
 * Dzięki zatrzaskom (CountDownLatch) test nie używa sleep ani pomiaru czasu — jest deterministyczny.
 */
@Component
public class OrderGate {

    private final CountDownLatch entered = new CountDownLatch(1);   // „żądanie jest w środku”
    private final CountDownLatch release = new CountDownLatch(1);   // „wolno dokończyć”

    /** Wywoływane przez kontroler: zgłasza wejście i czeka na zwolnienie (najwyżej 20 s — zabezpieczenie przed zawieszeniem). */
    public boolean enterAndWait() throws InterruptedException {
        entered.countDown();
        return release.await(20, TimeUnit.SECONDS);
    }

    public boolean awaitEntered(long seconds) throws InterruptedException {
        return entered.await(seconds, TimeUnit.SECONDS);
    }

    public void open() {
        release.countDown();
    }
}
