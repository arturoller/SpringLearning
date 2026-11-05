package s09_jpa_relations.solutions.rel01_many_to_one;

import org.springframework.stereotype.Service;
import s09_jpa_relations.rel01_many_to_one.CustomerOrderCount;
import s09_jpa_relations.rel01_many_to_one.CustomerRepository;
import s09_jpa_relations.rel01_many_to_one.ShopOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel01ManyToOne (OrderExercises) — zajrzyj dopiero po własnej próbie! */
@Service
public class SolutionOrderService {

    private final SolutionShopOrderRepository orders;
    private final CustomerRepository customers;

    public SolutionOrderService(SolutionShopOrderRepository orders, CustomerRepository customers) {
        this.orders = orders;
        this.customers = customers;
    }

    public List<String> orderNumbersOfCustomerNamed(String customerName) {
        return orders.findByCustomerNameOrderByOrderNumber(customerName).stream()
                .map(ShopOrder::getOrderNumber)
                .toList();
    }

    public ShopOrder placeOrder(Long customerId, String orderNumber, LocalDate orderedOn) {
        // proxy z samym id — bez SELECT-a klienta; INSERT wpisze customer_id
        return orders.save(new ShopOrder(orderNumber, orderedOn, customers.getReferenceById(customerId)));
    }

    public long countOrdersOf(String email) {
        return orders.countByCustomerEmail(email);
    }

    public Map<String, Long> orderCountByCustomerName() {
        return orders.countOrdersPerCustomer().stream()
                .collect(Collectors.toMap(CustomerOrderCount::customerName, CustomerOrderCount::orderCount,
                        (a, b) -> a, TreeMap::new));
    }

    public ShopOrder reassign(Long orderId, Long newCustomerId) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        order.setCustomer(customers.getReferenceById(newCustomerId)); // dirty checking → UPDATE orders przy flush
        return order;
    }
}
