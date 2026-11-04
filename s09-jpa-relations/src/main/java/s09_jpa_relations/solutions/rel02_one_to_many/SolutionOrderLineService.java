package s09_jpa_relations.solutions.rel02_one_to_many;

import org.springframework.stereotype.Service;
import s09_jpa_relations.rel02_one_to_many.OrderLine;
import s09_jpa_relations.rel02_one_to_many.Product;
import s09_jpa_relations.rel02_one_to_many.ProductQuantity;
import s09_jpa_relations.rel02_one_to_many.ProductRepository;
import s09_jpa_relations.rel02_one_to_many.ShopOrder;
import s09_jpa_relations.rel02_one_to_many.ShopOrderRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel02OneToMany (OrderLineExercises) — zajrzyj dopiero po własnej próbie! */
@Service
public class SolutionOrderLineService {

    private final ShopOrderRepository orders;
    private final ProductRepository products;
    private final SolutionOrderLineRepository lines;

    public SolutionOrderLineService(ShopOrderRepository orders, ProductRepository products,
                                    SolutionOrderLineRepository lines) {
        this.orders = orders;
        this.products = products;
        this.lines = lines;
    }

    public OrderLine addProduct(Long orderId, Long productId, int quantity) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        Product product = products.findById(productId).orElseThrow(); // potrzebna cena → findById, nie referencja
        return order.addLine(product, quantity); // kaskada PERSIST zapisze pozycję przy flush
    }

    public void removeProduct(Long orderId, Long productId) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        order.getLines().stream()
                .filter(line -> line.getProduct().getId().equals(productId)) // getId() proxy — bez zapytania
                .findFirst()
                .ifPresent(order::removeLine); // strumień już zakończony → bezpieczna zmiana listy
    }

    public BigDecimal orderTotal(Long orderId) {
        return lines.totalOfOrder(orderId);
    }

    public Map<String, Long> quantitySoldByProduct() {
        return lines.sumQuantityPerProduct().stream()
                .collect(Collectors.toMap(ProductQuantity::productName, ProductQuantity::quantity,
                        (a, b) -> a, TreeMap::new));
    }

    public ShopOrder copyOrder(Long sourceOrderId, String newOrderNumber) {
        ShopOrder source = orders.findById(sourceOrderId).orElseThrow();
        ShopOrder copy = new ShopOrder(newOrderNumber);
        for (OrderLine line : source.getLines()) { // iterujemy po źródle, zmieniamy kopię — bez kolizji
            copy.addLine(new OrderLine(line.getProduct(), line.getQuantity(), line.getUnitPrice()));
        }
        return orders.save(copy); // cascade = ALL → INSERT zamówienia + INSERT każdej pozycji
    }
}
