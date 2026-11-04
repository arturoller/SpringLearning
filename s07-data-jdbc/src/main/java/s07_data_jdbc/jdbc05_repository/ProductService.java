package s07_data_jdbc.jdbc05_repository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis (service) — logika biznesowa nad repozytorium. Przenoszenie towaru między produktami to DWA zapisy
 * (zmniejsz jeden stan, zwiększ drugi), które muszą się udać razem albo wcale — dlatego transakcja.
 */
@Service
public class ProductService {

    private final ProductRepository products;

    public ProductService(ProductRepository products) {
        this.products = products;
    }

    /**
     * PO: jedna transakcja ({@code @Transactional}) — wyjątek w środku wycofuje OBA zapisy.
     * Celowo najpierw zapisujemy zmniejszenie, a dopiero potem sprawdzamy, czy cel istnieje, żeby pokazać wycofanie.
     */
    @Transactional // transactional = transakcyjny: metoda działa w jednej transakcji bazy
    public void transferStock(long fromId, long toId, int quantity) {
        doTransfer(fromId, toId, quantity);
    }

    /** PRZED: ta sama logika bez transakcji — każdy zapis zatwierdza się osobno (auto-commit). */
    public void transferStockWithoutTransaction(long fromId, long toId, int quantity) {
        doTransfer(fromId, toId, quantity);
    }

    private void doTransfer(long fromId, long toId, int quantity) {
        Product from = products.findById(fromId).orElseThrow(() -> new ProductNotFoundException(fromId));
        if (from.stock() < quantity) {
            throw new InsufficientStockException(from.name(), from.stock(), quantity);
        }
        products.save(from.withStock(from.stock() - quantity)); // zapis 1
        Product to = products.findById(toId).orElseThrow(() -> new ProductNotFoundException(toId));
        products.save(to.withStock(to.stock() + quantity));     // zapis 2
    }
}
