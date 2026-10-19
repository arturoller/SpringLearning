package s09_jpa_relations.solutions.rel01_many_to_one;

import jakarta.persistence.EntityManager;
import s09_jpa_relations.rel01_many_to_one.Category;
import s09_jpa_relations.rel01_many_to_one.CategoryRepository;
import s09_jpa_relations.rel01_many_to_one.Product;
import s09_jpa_relations.rel01_many_to_one.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel01ManyToOne — zajrzyj dopiero po własnej próbie! */
public final class Rel01ManyToOneSolutions {

    private Rel01ManyToOneSolutions() {
    }

    static Long solution1(CategoryRepository categories, ProductRepository products,
                          String categoryName, String productName, BigDecimal price) {
        Category category = categories.save(new Category(categoryName));      // najpierw strona „jeden”
        return products.save(new Product(productName, price, category)).getId();
    }

    static List<String> solution2(EntityManager em, String categoryName) {
        return em.createQuery(
                        "select p.name from Product p where p.category.name = :name order by p.name", String.class)
                .setParameter("name", categoryName)
                .getResultList();
    }

    static void solution3(EntityManager em, Long productId, Long newCategoryId) {
        Product product = em.find(Product.class, productId);
        product.setCategory(em.getReference(Category.class, newCategoryId));  // proxy — bez SELECT
        // brak persist/merge: produkt jest zarządzany, flush wyśle UPDATE (dirty checking)
    }
}
