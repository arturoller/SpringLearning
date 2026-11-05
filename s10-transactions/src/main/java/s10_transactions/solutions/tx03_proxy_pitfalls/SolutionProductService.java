package s10_transactions.solutions.tx03_proxy_pitfalls;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import s10_transactions.tx03_proxy_pitfalls.ExerciseProductService.Rename;
import s10_transactions.tx03_proxy_pitfalls.TxState;

import java.util.ArrayList;
import java.util.List;

/** Rozwiązania ćwiczeń 2 i 3 lekcji Tx03ProxyPitfalls (metody pomocnicze — jak w ExerciseProductService). */
@Service
public class SolutionProductService {

    private final JdbcClient jdbc;
    private final ObjectProvider<SolutionProductService> self;

    public SolutionProductService(JdbcClient jdbc, ObjectProvider<SolutionProductService> self) {
        this.jdbc = jdbc;
        this.self = self;
    }

    @Transactional
    public void renameOne(Long productId, String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Pusta nazwa produktu " + productId);
        }
        jdbc.sql("UPDATE product SET name = ? WHERE id = ?").params(newName, productId).update();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String renameOneInNewTransaction(Long productId, String newName) {
        renameOne(productId, newName);
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }

    @Transactional
    public TxState renameAndReportState(Long productId, String newName) {
        renameOne(productId, newName);
        return TxState.now();
    }

    /** ĆWICZENIE 2: adnotacja na metodzie wywoływanej Z ZEWNĄTRZ — this.renameOne w pętli dołącza do tej transakcji. */
    @Transactional
    public void renameAll(List<Rename> renames) {
        for (Rename rename : renames) {
            renameOne(rename.productId(), rename.newName());
        }
    }

    /** ĆWICZENIE 3: wywołanie przez proxy (self.getObject()) — REQUIRES_NEW działa, każda zmiana ma własną transakcję. */
    @Transactional
    public List<String> renameEachSeparately(List<Rename> renames) {
        List<String> transactionNames = new ArrayList<>();
        for (Rename rename : renames) {
            try {
                transactionNames.add(self.getObject().renameOneInNewTransaction(rename.productId(), rename.newName()));
            } catch (IllegalArgumentException e) {
                // pusta nazwa — pomijamy; wyjątek przeszedł przez proxy REQUIRES_NEW, więc NIE oznaczył tej transakcji
            }
        }
        return transactionNames;
    }
}
