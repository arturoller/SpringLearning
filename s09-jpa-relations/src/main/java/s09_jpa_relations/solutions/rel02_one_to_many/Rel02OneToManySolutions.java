package s09_jpa_relations.solutions.rel02_one_to_many;

import jakarta.persistence.EntityManager;
import s09_jpa_relations.rel02_one_to_many.NaiveOrder;
import s09_jpa_relations.rel02_one_to_many.NaiveOrderLine;
import s09_jpa_relations.rel02_one_to_many.OrderLine;
import s09_jpa_relations.rel02_one_to_many.ShopOrder;
import s09_jpa_relations.rel02_one_to_many.ShopOrderRepository;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel02OneToMany — zajrzyj dopiero po własnej próbie! */
public final class Rel02OneToManySolutions {

    private Rel02OneToManySolutions() {
    }

    static Long solution1(ShopOrderRepository orders, String customerName) {
        ShopOrder order = new ShopOrder(customerName);
        order.addLine(new OrderLine("Kawa", 2));
        order.addLine(new OrderLine("Herbata", 1));
        return orders.save(order).getId();          // kaskada PERSIST zapisze obie pozycje
    }

    static void solution2(EntityManager em, Long orderId, String productName) {
        ShopOrder order = em.find(ShopOrder.class, orderId);
        order.getLines().stream()
                .filter(line -> line.getProductName().equals(productName))
                .findFirst()
                .ifPresent(order::removeLine);       // orphanRemoval → DELETE przy flush
    }

    static void solution3(EntityManager em, Long orderId, Long lineId) {
        NaiveOrder order = em.find(NaiveOrder.class, orderId);
        NaiveOrderLine line = em.find(NaiveOrderLine.class, lineId);
        line.setOrder(order);                        // strona właścicielska → kolumna order_id
        order.getLines().add(line);                  // lustro → spójny obiekt w pamięci
    }
}
