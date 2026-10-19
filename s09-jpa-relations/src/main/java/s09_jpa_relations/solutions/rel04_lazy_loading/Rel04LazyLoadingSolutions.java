package s09_jpa_relations.solutions.rel04_lazy_loading;

import jakarta.persistence.EntityManager;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.transaction.support.TransactionTemplate;
import s09_jpa_relations.rel04_lazy_loading.ShopOrder;
import s09_jpa_relations.rel04_lazy_loading.ShopOrderRepository;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel04LazyLoading — zajrzyj dopiero po własnej próbie! */
public final class Rel04LazyLoadingSolutions {

    private Rel04LazyLoadingSolutions() {
    }

    static String solution1(TransactionTemplate tx, ShopOrderRepository orders, String number) {
        // Cała praca w jednej transakcji: sesja otwarta, więc leniwe proxy klienta da się zainicjować.
        return tx.execute(status -> orders.findByNumber(number).orElseThrow().getCustomer().getName());
    }

    static ShopOrder solution2(EntityManager em, String number) {
        // join fetch — klient (relacja wymagana, zwykły JOIN); left join fetch — pozycje (zamówienie może być puste).
        return em.createQuery("""
                        select o from ShopOrder o
                        join fetch o.customer
                        left join fetch o.lines
                        where o.number = :number""", ShopOrder.class)
                .setParameter("number", number)
                .getSingleResult();
    }

    static Class<?> solution3(Object entity) {
        // getPersistentClass() czyta klasę z metadanych proxy — bez bazy i bez sesji.
        // Hibernate.getClass(entity) inicjalizowałby proxy, a poza sesją rzuciłby LazyInitializationException.
        if (entity instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getPersistentClass();
        }
        return entity.getClass();
    }
}
