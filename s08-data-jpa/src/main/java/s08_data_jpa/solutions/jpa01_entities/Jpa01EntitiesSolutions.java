package s08_data_jpa.solutions.jpa01_entities;

import jakarta.persistence.EntityManager;
import s08_data_jpa.jpa01_entities.Product;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Jpa01Entities — zajrzyj dopiero po własnej próbie! */
public final class Jpa01EntitiesSolutions {

    private Jpa01EntitiesSolutions() {
    }

    /** Rozwiązanie 1: persist; przy IDENTITY INSERT idzie od razu, więc id jest już ustawione. */
    public static Long solution1Persist(EntityManager em, Product product) {
        em.persist(product);
        return product.getId();
    }

    /** Rozwiązanie 2: find daje encję zarządzaną (albo null); setter wystarczy — UPDATE zrobi dirty checking. */
    public static boolean solution2ChangePrice(EntityManager em, long id, BigDecimal newPrice) {
        Product product = em.find(Product.class, id);
        if (product == null) {
            return false;
        }
        product.setPrice(newPrice);
        return true;
    }

    /** Rozwiązanie 3: merge kopiuje stan do encji zarządzanej i zwraca TĘ kopię. */
    public static Product solution3SaveDetached(EntityManager em, Product detached) {
        return em.merge(detached);
    }

    /** Rozwiązanie 4: find (null = pomiń) + remove tylko encji zarządzanej z zerowym stanem. */
    public static int solution4RemoveOutOfStock(EntityManager em, List<Long> ids) {
        int removed = 0;
        for (Long id : ids) {
            Product product = em.find(Product.class, id);
            if (product != null && product.getStock() == 0) {
                em.remove(product);
                removed++;
            }
        }
        return removed;
    }
}
