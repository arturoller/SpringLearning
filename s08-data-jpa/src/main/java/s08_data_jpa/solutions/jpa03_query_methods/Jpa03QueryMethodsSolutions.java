package s08_data_jpa.solutions.jpa03_query_methods;

import s08_data_jpa.jpa03_query_methods.Category;
import s08_data_jpa.jpa03_query_methods.Product;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa03QueryMethods (zapytania — w ProductSolutionRepository). */
public final class Jpa03QueryMethodsSolutions {

    private Jpa03QueryMethodsSolutions() {
    }

    public static List<Product> solution1(ProductSolutionRepository repository) {
        return repository.findByCategoryAndPriceLessThanOrderByPriceAsc(Category.KSIAZKI, new BigDecimal("100"));
    }

    public static long solution2(ProductSolutionRepository repository) {
        return repository.countByNameContainingIgnoreCase("kaw");
    }

    public static List<Product> solution3(ProductSolutionRepository repository) {
        return repository.findTop2ByCategoryAndStockGreaterThanOrderByPriceAsc(Category.ELEKTRONIKA, 0);
    }

    public static List<Product> solution4(ProductSolutionRepository repository, Collection<Category> categories) {
        return repository.findByCategoryInAndPriceBetweenOrderByNameAsc(
                categories, new BigDecimal("40"), new BigDecimal("500"));
    }
}
