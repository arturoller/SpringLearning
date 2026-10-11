package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.EntityManager;
import s08_data_jpa.jpa01_entities.Category;
import s08_data_jpa.jpa01_entities.Product;

import java.math.BigDecimal;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa01Entities — zajrzyj dopiero po własnej próbie! */
public final class Jpa01EntitiesSolutions {

    private Jpa01EntitiesSolutions() {
    }

    static Long solution1(EntityManager em, String name) {
        Category category = new Category(name);
        em.persist(category);                       // SEQUENCE: id znane od razu, INSERT przy flush
        return category.getId();
    }

    static void solution2(EntityManager em, Long productId, BigDecimal newPrice) {
        Product product = em.find(Product.class, productId);   // encja zarządzana
        product.setPrice(newPrice);                             // UPDATE przy flush — bez persist/merge
    }

    static Product solution3(EntityManager em, Product detached, BigDecimal newPrice) {
        detached.setPrice(newPrice);
        return em.merge(detached);                  // merge kopiuje stan do zarządzanej kopii i ją zwraca
    }
}
