package s09_jpa_relations.rel04_lazy_loading;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serwis zamówień. {@code @Transactional(readOnly = true)} (transakcja tylko do odczytu) na klasie — każda metoda
 * publiczna działa w transakcji, a razem z nią żyje EntityManager (kontekst utrwalania). Po powrocie z metody
 * transakcja się kończy i — bez open-in-view — EntityManager zostaje ZAMKNIĘTY. Leniwe pola, których metoda nie
 * dotknęła, zostają niezainicjowane na zawsze.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final ShopOrderRepository orders;

    public OrderService(ShopOrderRepository orders) {
        this.orders = orders;
    }

    /** Zwraca encję „tak jak jest” — klient i pozycje NIEZAINICJOWANE. */
    public ShopOrder findOrder(String number) {
        return orders.findByNumber(number).orElseThrow();
    }

    /** join fetch — klient wczytany w tym samym zapytaniu; bezpieczny także poza transakcją. */
    public ShopOrder findOrderWithCustomer(String number) {
        return orders.findWithCustomerByNumber(number).orElseThrow();
    }

    /** @EntityGraph — pozycje wczytane w tym samym zapytaniu. */
    public ShopOrder findOrderWithLines(String number) {
        return orders.findWithLinesByNumber(number).orElseThrow();
    }

    /** Leniwe pole użyte WEWNĄTRZ transakcji — działa (drugi SELECT, ale sesja jest otwarta). */
    public String customerNameOf(String number) {
        return findOrder(number).getCustomer().getName();
    }
}
