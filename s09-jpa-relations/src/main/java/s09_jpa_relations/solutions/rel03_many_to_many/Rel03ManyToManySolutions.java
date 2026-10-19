package s09_jpa_relations.solutions.rel03_many_to_many;

import jakarta.persistence.EntityManager;
import s09_jpa_relations.rel03_many_to_many.Product;
import s09_jpa_relations.rel03_many_to_many.ProductTagCount;
import s09_jpa_relations.rel03_many_to_many.ProductTag;

import java.math.BigDecimal;
import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel03ManyToMany — zajrzyj dopiero po własnej próbie! */
public final class Rel03ManyToManySolutions {

    private Rel03ManyToManySolutions() {
    }

    static Long solution1(EntityManager em, String name, BigDecimal price, List<String> tagNames) {
        List<ProductTag> tags = em.createQuery("select t from ProductTag t where t.name in :names", ProductTag.class)
                .setParameter("names", tagNames)
                .getResultList();
        Product product = new Product(name, price);
        tags.forEach(product::addTag);                // obie strony relacji naraz
        em.persist(product);                          // INSERT produktu + wiersze product_tags przy flush
        return product.getId();
    }

    static List<String> solution2(EntityManager em, String tagName) {
        return em.createQuery(
                        "select p.name from Product p join p.tags t where t.name = :tag order by p.name", String.class)
                .setParameter("tag", tagName)
                .getResultList();
    }

    static void solution3(EntityManager em, String tagName) {
        ProductTag tag = em.createQuery("select t from ProductTag t where t.name = :name", ProductTag.class)
                .setParameter("name", tagName)
                .getSingleResult();
        List.copyOf(tag.getProducts()).forEach(product -> product.removeTag(tag));  // najpierw odpiąć (kopia!)
        em.remove(tag);                                                              // potem usunąć
    }

    static List<ProductTagCount> solution4(EntityManager em) {
        // LEFT JOIN: produkt bez tagów też daje wiersz (z t = null), a count(t) liczy tylko nie-nulle → 0.
        // Zwykły JOIN wyrzuciłby „Lampkę biurkową” z wyniku.
        return em.createQuery("""
                        select new s09_jpa_relations.rel03_many_to_many.ProductTagCount(p.name, count(t))
                        from Product p left join p.tags t
                        group by p.name
                        order by p.name""", ProductTagCount.class)
                .getResultList();
    }
}
