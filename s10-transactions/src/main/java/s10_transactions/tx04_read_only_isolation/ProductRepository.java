package s10_transactions.tx04_read_only_isolation;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

/**
 * Repozytorium produktów (Spring Data JPA). Gotowa implementacja (SimpleJpaRepository) ma nad klasą
 * {@code @Transactional(readOnly = true)}, a nad metodami zapisującymi (save, delete…) zwykłe {@code @Transactional}
 * — sprawdzone testem sekcji 1.
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    /**
     * Odczyt z BLOKADĄ PESYMISTYCZNĄ do zapisu (pessimistic write lock): wiersz zostaje zablokowany od odczytu do końca
     * transakcji — inni, którzy chcą go zmienić, muszą czekać. Działa tylko w transakcji.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.sku = :sku")
    Optional<Product> findForUpdate(String sku);
}
