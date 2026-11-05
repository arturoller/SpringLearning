package s10_transactions.tx03_pitfalls.expose_proxy;

import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Wywołanie własnej metody transakcyjnej przez {@code AopContext.currentProxy()} zamiast przez {@code this}. */
@Service
public class ExposingService {

    public boolean viaCurrentProxy() {
        return ((ExposingService) AopContext.currentProxy()).inTransaction();
    }

    public boolean viaThis() {
        return inTransaction();
    }

    @Transactional
    public boolean inTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
}
