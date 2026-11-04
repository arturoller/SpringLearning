package s08_data_jpa.jpa04_pitfalls.jpql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import s08_data_jpa.jpa04_custom_queries.Product;

import java.util.List;

/** Sekcja 1 lekcji Jpa04CustomQueries: w JPQL nazwa TABELI (product) zamiast nazwy ENCJI (Product). */
public interface TableNameInJpqlRepository extends JpaRepository<Product, Long> {

    @Query("select p from product p where p.stock = 0") // BŁĄD: JPQL zna encję Product, nie tabelę product
    List<Product> findOutOfStock();
}
