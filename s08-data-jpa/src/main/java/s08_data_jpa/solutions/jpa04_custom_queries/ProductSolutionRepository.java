package s08_data_jpa.solutions.jpa04_custom_queries;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.ProductSummary;

import java.util.List;

/**
 * Rozwiązania wzorcowe ćwiczeń lekcji Jpa04CustomQueries — zajrzyj dopiero po własnej próbie!
 * Pakiet solutions nie jest skanowany przez Jpa04Application; test tworzy repozytorium fabryką JpaRepositoryFactory.
 */
public interface ProductSolutionRepository extends JpaRepository<Product, Long> {

    @Query("select p from Product p where lower(p.name) like lower(concat('%', :fragment, '%')) order by p.name")
    List<Product> searchByName(String fragment);

    @Query("select new s08_data_jpa.jpa04_custom_queries.ProductSummary(p.name, p.price) "
            + "from Product p where p.category = :category order by p.price desc")
    List<ProductSummary> summariesByPriceDesc(String category);

    @Modifying(clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock + :amount where p.category = :category")
    int addStock(String category, int amount);

    default List<Product> solution1(String fragment) {
        return searchByName(fragment);
    }

    default List<ProductSummary> solution2(String category) {
        return summariesByPriceDesc(category);
    }

    default int solution3(String category, int amount) {
        return addStock(category, amount);
    }
}
