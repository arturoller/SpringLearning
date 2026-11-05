package s09_jpa_relations.solutions.rel04_lazy_loading;

import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s09_jpa_relations.rel04_lazy_loading.Customer;
import s09_jpa_relations.rel04_lazy_loading.CustomerRepository;
import s09_jpa_relations.rel04_lazy_loading.ShopOrder;

import java.util.List;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel04LazyLoading (LazyLoadingExercises) — zajrzyj dopiero po własnej próbie! */
@Service
public class SolutionLazyLoadingService {

    private final SolutionShopOrderRepository orders;
    private final CustomerRepository customers;

    public SolutionLazyLoadingService(SolutionShopOrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @Transactional(readOnly = true) // sesja otwarta do końca metody → proxy klienta może się wczytać
    public String customerNameOfOrder(Long orderId) {
        return orders.findById(orderId).orElseThrow().getCustomer().getName();
    }

    public ShopOrder orderWithLinesAndProducts(Long orderId) {
        return orders.findWithLinesAndProductsById(orderId).orElseThrow();
    }

    public List<ShopOrder> allOrdersWithCustomers() {
        return orders.findAllByOrderByOrderNumber();
    }

    @Transactional(readOnly = true)
    public Customer customerWithOrders(Long customerId) {
        Customer customer = customers.findById(customerId).orElseThrow();
        Hibernate.initialize(customer.getOrders()); // SELECT zamówień teraz, póki sesja jest otwarta
        return customer;
    }
}
