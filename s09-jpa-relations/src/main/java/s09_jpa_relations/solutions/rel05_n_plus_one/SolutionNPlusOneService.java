package s09_jpa_relations.solutions.rel05_n_plus_one;

import org.springframework.stereotype.Service;
import s09_jpa_relations.rel05_n_plus_one.CustomerSpending;
import s09_jpa_relations.rel05_n_plus_one.ShopOrder;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel05NPlusOne (NPlusOneExercises) — zajrzyj dopiero po własnej próbie! */
@Service
public class SolutionNPlusOneService {

    private final SolutionShopOrderRepository orders;

    public SolutionNPlusOneService(SolutionShopOrderRepository orders) {
        this.orders = orders;
    }

    public Map<String, String> customerNameByOrderNumber() {
        return orders.findAllWithCustomer().stream()
                .collect(Collectors.toMap(ShopOrder::getOrderNumber, order -> order.getCustomer().getName(),
                        (a, b) -> a, TreeMap::new));
    }

    public Map<String, List<String>> productNamesByOrderNumber() {
        return orders.findAllWithLinesAndProducts().stream()
                .collect(Collectors.toMap(ShopOrder::getOrderNumber,
                        order -> order.getLines().stream().map(line -> line.getProduct().getName()).toList(),
                        (a, b) -> a, TreeMap::new));
    }

    public List<ShopOrder> ordersOfCustomerWithProducts(Long customerId) {
        return orders.findByCustomerIdOrderByOrderNumber(customerId);
    }

    public List<CustomerSpending> spendingPerCustomer() {
        return orders.spendingPerCustomer();
    }

    public List<ShopOrder> ordersWithLinesAndPayments() {
        List<ShopOrder> all = orders.findAllWithLines();
        orders.fetchPayments(all); // wynik pomijamy: te same obiekty z kontekstu dostały właśnie płatności
        return all;
    }
}
