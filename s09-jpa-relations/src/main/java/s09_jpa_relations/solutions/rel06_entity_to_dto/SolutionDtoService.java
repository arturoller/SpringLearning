package s09_jpa_relations.solutions.rel06_entity_to_dto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s09_jpa_relations.rel06_entity_to_dto.CreateOrderRequest;
import s09_jpa_relations.rel06_entity_to_dto.CustomerDto;
import s09_jpa_relations.rel06_entity_to_dto.CustomerRepository;
import s09_jpa_relations.rel06_entity_to_dto.OrderDetailsDto;
import s09_jpa_relations.rel06_entity_to_dto.OrderLine;
import s09_jpa_relations.rel06_entity_to_dto.OrderMapper;
import s09_jpa_relations.rel06_entity_to_dto.OrderSummaryDto;
import s09_jpa_relations.rel06_entity_to_dto.Product;
import s09_jpa_relations.rel06_entity_to_dto.ProductRepository;
import s09_jpa_relations.rel06_entity_to_dto.ShopOrder;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rel06EntityToDto (DtoExercises) — zajrzyj dopiero po własnej próbie! */
@Service
@Transactional(readOnly = true)
public class SolutionDtoService {

    private final SolutionShopOrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;
    private final OrderMapper mapper;

    public SolutionDtoService(SolutionShopOrderRepository orders, CustomerRepository customers,
                              ProductRepository products, OrderMapper mapper) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
        this.mapper = mapper;
    }

    public CustomerDto customer(Long customerId) {
        return customers.findById(customerId).map(CustomerDto::from).orElseThrow();
    }

    public List<OrderSummaryDto> summariesOfCustomer(Long customerId) {
        return orders.findSummariesOfCustomer(customerId);
    }

    @Transactional
    public void changeQuantity(Long orderId, String productName, int quantity) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        OrderLine line = order.getLines().stream()
                .filter(l -> l.getProduct().getName().equals(productName))
                .findFirst()
                .orElseThrow(); // NoSuchElementException, gdy brak pozycji
        line.setQuantity(quantity); // bez save — dirty checking
    }

    public List<OrderDetailsDto> detailsOfCustomer(Long customerId) {
        return orders.findDetailsOfCustomer(customerId).stream().map(mapper::toDetails).toList();
    }

    @Transactional
    public OrderDetailsDto addItems(Long orderId, List<CreateOrderRequest.Item> items) {
        ShopOrder order = orders.findById(orderId).orElseThrow();
        Map<Long, Product> byId = products.findAllById(items.stream().map(CreateOrderRequest.Item::productId).toList())
                .stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (CreateOrderRequest.Item item : items) { // najpierw sprawdzenie WSZYSTKICH — potem zmiany
            if (!byId.containsKey(item.productId())) {
                throw new IllegalArgumentException("Nieznany produkt: " + item.productId());
            }
        }
        for (CreateOrderRequest.Item item : items) {
            Product product = byId.get(item.productId());
            order.addLine(product, item.quantity(), product.getPrice()); // kaskada PERSIST przy flush
        }
        return mapper.toDetails(order);
    }
}
