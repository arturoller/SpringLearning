package s09_jpa_relations.rel06_entity_to_dto;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.LazyInitializationException;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import s09_jpa_relations.SqlCapture;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel06EntityToDto na wycinku {@code @DataJpaTest} z dołączonym serwisem i maperem ({@code @Import}).
 * Dane: 2 klientów, 4 produkty, 3 zamówienia (db/migration/rel06/V2); każdy test jest wycofywany — poza testem
 * z {@code NOT_SUPPORTED} (bez transakcji testu, tylko odczyt).
 */
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel06")
@Import({OrderService.class, OrderMapper.class})
class Rel06EntityToDtoTest {

    @Autowired
    private OrderService service;

    @Autowired
    private ShopOrderRepository orders;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        SqlCapture.clear();
    }

    private static List<String> readableProperties(Class<?> type) throws Exception {
        return Arrays.stream(Introspector.getBeanInfo(type, Object.class).getPropertyDescriptors())
                .filter(property -> property.getReadMethod() != null).map(PropertyDescriptor::getName).toList();
    }

    // ---- 1 --------------------------------------------------------------------------------------

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // bez transakcji testu — jak kontroler
    @DisplayName("Sekcja 1: encja poza transakcją wybucha (LazyInitializationException), DTO z serwisu — nie")
    void section1EntityOutsideTransactionVsDto() {
        ShopOrder entity = orders.findByOrderNumber("ZAM-001").orElseThrow();
        assertThatThrownBy(() -> entity.getLines().size()).isInstanceOf(LazyInitializationException.class);
        assertThatThrownBy(() -> entity.getCustomer().getName()).isInstanceOf(LazyInitializationException.class);

        OrderDetailsDto dto = service.details("ZAM-001");
        assertThat(dto.customerName()).isEqualTo("Anna Nowak");
        assertThat(dto.lines()).hasSize(2);
    }

    @Test
    @DisplayName("Sekcja 1: gettery encji (to, co wypisze serializator) — skrót hasła i cykl zamówienie ↔ pozycja")
    void section1EntityExposesFieldsAndCycles() throws Exception {
        assertThat(readableProperties(Customer.class)).containsExactly("email", "id", "name", "passwordHash");
        assertThat(Arrays.stream(CustomerDto.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("id", "name", "email");

        assertThat(readableProperties(ShopOrder.class)).contains("lines");
        assertThat(Introspector.getBeanInfo(OrderLine.class).getPropertyDescriptors())
                .filteredOn(property -> property.getName().equals("order"))
                .extracting(PropertyDescriptor::getPropertyType).containsExactly(ShopOrder.class);
        assertThat(Arrays.stream(OrderLineDto.class.getRecordComponents()).map(RecordComponent::getName))
                .as("DTO pozycji nie wskazuje na zamówienie — cyklu nie ma")
                .containsExactly("productName", "quantity", "unitPrice", "lineTotal");
    }

    @Test
    @DisplayName("Sekcja 1: mass assignment — żądanie nie ma pól ceny/statusu, a encja ma setter tylko do uwagi")
    void section1NoMassAssignment() throws Exception {
        assertThat(Arrays.stream(CreateOrderRequest.Item.class.getRecordComponents()).map(RecordComponent::getName))
                .containsExactly("productId", "quantity");
        assertThat(Arrays.stream(Introspector.getBeanInfo(ShopOrder.class, Object.class).getPropertyDescriptors())
                .filter(property -> property.getWriteMethod() != null).map(PropertyDescriptor::getName))
                .containsExactly("note");
    }

    // ---- 2 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 2: szczegóły zamówienia jako OrderDetailsDto — kompletne, 1 zapytanie")
    void section2DetailsDto() {
        OrderDetailsDto dto = service.details("ZAM-001");

        assertThat(dto).isEqualTo(new OrderDetailsDto("ZAM-001", "Anna Nowak", OrderStatus.NEW,
                "Proszę dzwonić przed dostawą",
                List.of(new OrderLineDto("Klawiatura", 1, new BigDecimal("149.90"), new BigDecimal("149.90")),
                        new OrderLineDto("Mysz", 2, new BigDecimal("59.00"), new BigDecimal("118.00"))),
                new BigDecimal("267.90")));
        assertThat(SqlCapture.all()).containsExactly("select so1_0.id,so1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,"
                + "c1_0.password_hash,l1_0.order_id,l1_0.id,l1_0.product_id,p1_0.id,p1_0.name,p1_0.price,l1_0.quantity,"
                + "l1_0.unit_price,so1_0.note,so1_0.order_number,so1_0.status from orders so1_0 "
                + "join customer c1_0 on c1_0.id=so1_0.customer_id left join order_line l1_0 on so1_0.id=l1_0.order_id "
                + "left join product p1_0 on p1_0.id=l1_0.product_id where so1_0.order_number=? order by l1_0.id");
    }

    @Test
    @DisplayName("Sekcja 2: lista OrderSummaryDto projekcją — 1 zapytanie, 0 encji; CustomerDto bez skrótu hasła")
    void section2SummariesAndCustomerDto() {
        assertThat(service.summaries()).containsExactly(
                new OrderSummaryDto("ZAM-001", "Anna Nowak", OrderStatus.NEW, new BigDecimal("267.90")),
                new OrderSummaryDto("ZAM-002", "Jan Kowalski", OrderStatus.SHIPPED, new BigDecimal("899.99")),
                new OrderSummaryDto("ZAM-003", "Anna Nowak", OrderStatus.NEW, new BigDecimal("98.00")));
        assertThat(SqlCapture.all()).hasSize(1);
        assertThat(stats.getEntityLoadCount()).isZero();

        assertThat(service.customer(2L)).isEqualTo(new CustomerDto(2L, "Jan Kowalski", "jan@sklep.pl"));
    }

    // ---- 3 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 3: CreateOrderRequest → encja — ceny z bazy, produkty jednym zapytaniem, numer i status od serwera")
    void section3PlaceOrder() {
        CreateOrderRequest request = new CreateOrderRequest(2L, "Na prezent",
                List.of(new CreateOrderRequest.Item(1L, 2), new CreateOrderRequest.Item(4L, 1)));

        OrderDetailsDto placed = service.placeOrder(request);
        orders.flush();

        assertThat(placed.orderNumber()).isEqualTo("ZAM-004");
        assertThat(placed.status()).isEqualTo(OrderStatus.NEW);
        assertThat(placed.customerName()).isEqualTo("Jan Kowalski");
        assertThat(placed.total()).as("2 × 149.90 + 24.50 — z cen produktów").isEqualByComparingTo("324.30");
        assertThat(SqlCapture.all()).containsExactly(
                "select c1_0.id,c1_0.email,c1_0.name,c1_0.password_hash from customer c1_0 where c1_0.id=?",
                "select p1_0.id,p1_0.name,p1_0.price from product p1_0 where p1_0.id in (?,?)",
                "select count(*) from orders so1_0",
                "insert into orders (customer_id,note,order_number,status,id) values (?,?,?,?,default)",
                "insert into order_line (order_id,product_id,quantity,unit_price,id) values (?,?,?,?,default)",
                "insert into order_line (order_id,product_id,quantity,unit_price,id) values (?,?,?,?,default)");
    }

    @Test
    @DisplayName("Sekcja 3: nieznany produkt albo zła ilość w żądaniu — IllegalArgumentException")
    void section3InvalidRequest() {
        assertThatThrownBy(() -> service.placeOrder(new CreateOrderRequest(1L, null,
                List.of(new CreateOrderRequest.Item(99L, 1)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nieznany produkt: 99");
        assertThatThrownBy(() -> service.placeOrder(new CreateOrderRequest(1L, null,
                List.of(new CreateOrderRequest.Item(1L, 0)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Ilość musi być dodatnia: 0");
    }

    // ---- 4 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 4: aktualizacja przez zmianę zarządzanej encji — bez save, dirty checking wysyła UPDATE")
    void section4UpdateManagedEntity() {
        service.changeNote("ZAM-003", new UpdateNoteRequest("Zostawić u sąsiada"));
        orders.flush();

        assertThat(SqlCapture.all()).containsExactly(
                "select so1_0.id,so1_0.customer_id,so1_0.note,so1_0.order_number,so1_0.status from orders so1_0 "
                        + "where so1_0.order_number=?",
                "update orders set customer_id=?,note=?,order_number=?,status=? where id=?");
        assertThat(orders.findByOrderNumber("ZAM-003").orElseThrow().getNote()).isEqualTo("Zostawić u sąsiada");
    }

    @Test
    @DisplayName("Sekcja 4: PUŁAPKA — nowa encja zbudowana z DTO + save to INSERT, nie UPDATE (tu: naruszenie UNIQUE)")
    void section4NewEntityFromDtoIsInsert() {
        ShopOrder fromDto = new ShopOrder("ZAM-001", customers.getReferenceById(1L), "nowa uwaga");

        // IDENTITY: INSERT idzie od razu przy save (baza nadaje id) — więc wyjątek leci już z save
        assertThatThrownBy(() -> orders.save(fromDto))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("UK_ORDERS_ORDER_NUMBER");
        assertThat(SqlCapture.startingWith("insert")).hasSize(1);
        assertThat(SqlCapture.startingWith("update")).isEmpty();
    }

    // ---- 5 --------------------------------------------------------------------------------------

    @Test
    @DisplayName("Sekcja 5: ręczny maper to zwykła klasa — da się go przetestować bez bazy i bez Springa")
    void section5MapperWithoutDatabase() {
        ShopOrder order = new ShopOrder("ZAM-T", new Customer("Ewa", "ewa@sklep.pl", "test-only-hash"), null);
        order.addLine(new Product("Kubek", new BigDecimal("24.50")), 2, new BigDecimal("24.50"));

        OrderDetailsDto dto = new OrderMapper().toDetails(order);

        assertThat(dto.customerName()).isEqualTo("Ewa");
        assertThat(dto.lines()).containsExactly(
                new OrderLineDto("Kubek", 2, new BigDecimal("24.50"), new BigDecimal("49.00")));
        assertThat(dto.total()).isEqualByComparingTo("49.00");
        assertThat(SqlCapture.all()).isEmpty();
    }
}
