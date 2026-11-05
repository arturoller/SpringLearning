package s10_transactions.solutions.tx01_transactional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import s10_transactions.tx01_transactional.FeeService;
import s10_transactions.tx01_transactional.InsufficientFundsException;
import s10_transactions.tx01_transactional.Wallet;
import s10_transactions.tx01_transactional.WalletNotFoundException;
import s10_transactions.tx01_transactional.WalletRepository;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Tx01Transactional (testy: Tx01TransactionalSolutionsTest). */
@Service
public class SolutionTransactionalExercises {

    private final WalletRepository wallets;

    public SolutionTransactionalExercises(WalletRepository wallets) {
        this.wallets = wallets;
    }

    /** ĆWICZENIE 1: jedna transakcja na całą metodę — brak drugiego portfela wycofuje też wypłatę. */
    @Transactional
    public void transferAll(Long fromId, Long toId) {
        Wallet from = find(fromId);
        BigDecimal all = from.getBalance();
        from.withdraw(all);
        Wallet to = find(toId);
        to.deposit(all);
        // save() zbędne: obie encje są zarządzane, UPDATE-y pójdą przy commicie (dirty checking)
    }

    /** ĆWICZENIE 2: rollbackFor — wyjątek sprawdzany też wycofuje (bez tego opłata zostałaby zatwierdzona). */
    @Transactional(rollbackFor = InsufficientFundsException.class)
    public void payInvoice(Long walletId, BigDecimal amount) throws InsufficientFundsException {
        Wallet wallet = find(walletId);
        wallet.withdraw(FeeService.FEE);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(walletId, amount, wallet.getBalance());
        }
        wallet.withdraw(amount);
    }

    /** ĆWICZENIE 3: brak środków → setRollbackOnly() i false; wcześniejsze wypłaty z listy znikają przy rollbacku. */
    @Transactional
    public boolean withdrawFromAll(List<Long> walletIds, BigDecimal amount) {
        for (Long id : walletIds) {
            Wallet wallet = find(id);
            if (wallet.getBalance().compareTo(amount) < 0) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return false;
            }
            wallet.withdraw(amount);
        }
        return true;
    }

    private Wallet find(Long id) {
        return wallets.findById(id).orElseThrow(() -> new WalletNotFoundException(id));
    }
}
