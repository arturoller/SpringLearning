package s08_data_jpa.jpa04_pitfalls.nativesql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s08_data_jpa.jpa04_custom_queries.Product;

import java.util.List;

/** Sekcja 2 lekcji Jpa04CustomQueries: literówka w nazwie kolumny w natywnym SQL (stok zamiast stock). */
public interface NativeTypoRepository extends JpaRepository<Product, Long> {

    @Query(value = "SELECT * FROM product WHERE stok = 0", nativeQuery = true) // BŁĄD: kolumna nazywa się stock
    List<Product> findOutOfStock();
}
