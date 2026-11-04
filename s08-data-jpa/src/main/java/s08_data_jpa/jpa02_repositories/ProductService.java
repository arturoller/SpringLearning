package s08_data_jpa.jpa02_repositories;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Serwis nad repozytorium (sekcja 4). Trzy metody z TYM SAMYM ciałem (findById + setter, bez save) różnią się
 * tylko adnotacją {@code @Transactional} — i dlatego dają różny wynik w bazie (test Jpa02ServiceTransactionTest).
 */
@Service // service = serwis, warstwa logiki biznesowej
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    /**
     * POPRAWNIE: jedna transakcja na całą metodę → encja z findById jest ZARZĄDZANA do końca metody → zmiana ceny
     * trafia do bazy przy commicie (dirty checking), bez wywołania save.
     */
    @Transactional // transactional = transakcyjny: proxy otwiera transakcję przed metodą, commit po niej
    public void changePrice(long id, BigDecimal newPrice) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setPrice(newPrice);
    }

    /**
     * BŁĄD (do nauki): bez {@code @Transactional} transakcja żyje tylko WEWNĄTRZ findById. Po powrocie encja jest
     * odłączona (detached), więc setter zmienia już tylko obiekt w pamięci — do bazy nic nie trafia.
     */
    public void changePriceWithoutTransaction(long id, BigDecimal newPrice) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setPrice(newPrice);
    }

    /**
     * BŁĄD (do nauki): transakcja tylko do odczytu. Spring przełącza wtedy sesję Hibernate na ręczne wypychanie
     * zmian (FlushMode.MANUAL), więc przy commicie nie ma flush — zmiana encji przepada.
     */
    @Transactional(readOnly = true) // read only = tylko do odczytu
    public void changePriceInReadOnlyTransaction(long id, BigDecimal newPrice) {
        Product product = repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        product.setPrice(newPrice);
    }
}
