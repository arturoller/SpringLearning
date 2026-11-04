package s10_transactions.solutions.tx03_proxy_pitfalls;

import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;
import s10_transactions.tx03_proxy_pitfalls.ProxyExercises.WorkerReport;

import java.util.concurrent.CompletableFuture;

/** Rozwiązania ćwiczeń 1 i 4 lekcji Tx03ProxyPitfalls. */
@Service
public class SolutionProxyExercises {

    private final SolutionProductService products;

    public SolutionProxyExercises(SolutionProductService products) {
        this.products = products;
    }

    /** ĆWICZENIE 1: AopUtils rozpoznaje proxy po klasie (CGLIB: znacznik $$ w nazwie; JDK: java.lang.reflect.Proxy). */
    public String describeProxy(Object bean) {
        if (AopUtils.isCglibProxy(bean)) {
            return "CGLIB";
        }
        if (AopUtils.isJdkDynamicProxy(bean)) {
            return "JDK";
        }
        return "BRAK";
    }

    /** ĆWICZENIE 4: transakcję otwiera proxy products W WĄTKU ROBOCZYM — tam, gdzie wykonuje się zmiana. */
    public WorkerReport renameInWorkerThread(Long productId, String newName) {
        return CompletableFuture.supplyAsync(() -> new WorkerReport(Thread.currentThread().getName(),
                products.renameAndReportState(productId, newName).active())).join();
    }
}
