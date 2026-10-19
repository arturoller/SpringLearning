package s09_jpa_relations.solutions.rel05_n_plus_one;

import jakarta.persistence.EntityManager;
import s09_jpa_relations.rel05_n_plus_one.Customer;
import s09_jpa_relations.rel05_n_plus_one.OrderLineCount;
import s09_jpa_relations.rel05_n_plus_one.ShopOrder;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel05NPlusOne — zajrzyj dopiero po własnej próbie! */
public final class Rel05NPlusOneSolutions {

    private Rel05NPlusOneSolutions() {
    }

    static List<ShopOrder> solution1(EntityManager em, String status) {
        return em.createQuery("""
                        select o from ShopOrder o
                        join fetch o.customer
                        where o.status = :status
                        order by o.number""", ShopOrder.class)
                .setParameter("status", status)
                .getResultList();
    }

    static List<OrderLineCount> solution2(EntityManager em) {
        // left join — zamówienie bez pozycji też by się pojawiło (z liczbą 0); count(l) liczy tylko istniejące pozycje.
        return em.createQuery("""
                        select new s09_jpa_relations.rel05_n_plus_one.OrderLineCount(o.number, count(l))
                        from ShopOrder o left join o.lines l
                        group by o.number
                        order by o.number""", OrderLineCount.class)
                .getResultList();
    }

    static List<ShopOrder> solution3(EntityManager em, int page, int size) {
        // Krok 1: stronicujemy SAME id — tu nie ma złączenia, więc limit dotyczy zamówień.
        List<Long> ids = em.createQuery("select o.id from ShopOrder o order by o.number", Long.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        // Krok 2: dociągamy pozycje tylko dla tych id — bez stronicowania złączenia.
        return em.createQuery("""
                        select o from ShopOrder o
                        left join fetch o.lines
                        where o.id in :ids
                        order by o.number""", ShopOrder.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    static List<Customer> solution4(EntityManager em) {
        // Zapytanie 1: klienci z zamówieniami (jeden worek w join fetch — dozwolone).
        List<Customer> customers = em.createQuery(
                        "select c from Customer c join fetch c.orders order by c.name", Customer.class)
                .getResultList();
        // Zapytanie 2: pozycje tych zamówień. Zamówienia są już w kontekście utrwalania — Hibernate uzupełnia
        // kolekcje lines w TYCH SAMYCH obiektach, które wiszą w customer.getOrders().
        em.createQuery("select o from ShopOrder o left join fetch o.lines where o.customer in :customers",
                        ShopOrder.class)
                .setParameter("customers", customers)
                .getResultList();
        return customers;
    }
}
