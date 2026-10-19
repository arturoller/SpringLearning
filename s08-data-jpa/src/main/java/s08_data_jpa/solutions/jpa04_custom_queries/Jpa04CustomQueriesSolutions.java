package s08_data_jpa.solutions.jpa04_custom_queries;

import s08_data_jpa.jpa04_custom_queries.Category;
import s08_data_jpa.jpa04_custom_queries.Product;
import s08_data_jpa.jpa04_custom_queries.StockValue;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa04CustomQueries (zapytania — w ProductSolutionRepository). */
public final class Jpa04CustomQueriesSolutions {

    private Jpa04CustomQueriesSolutions() {
    }

    public static List<Product> solution1(ProductSolutionRepository repository, int threshold) {
        return repository.findStockBelow(threshold);
    }

    public static List<StockValue> solution2(ProductSolutionRepository repository, Category category) {
        return repository.findStockValues(category);
    }

    public static List<Product> solution3(ProductSolutionRepository repository, String category, int limit) {
        return repository.findCheapestNative(category, limit);
    }

    public static int solution4(ProductSolutionRepository repository, Category category, int percent) {
        BigDecimal factor = BigDecimal.valueOf(100 - percent).movePointLeft(2);   // 10 → 0.90
        return repository.multiplyPrices(factor, category);
    }
}
