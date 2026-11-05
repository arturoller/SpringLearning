package s09_jpa_relations.solutions.rel06_entity_to_dto;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import s09_jpa_relations.SqlCapture;
import s09_jpa_relations.rel06_entity_to_dto.CreateOrderRequest;
import s09_jpa_relations.rel06_entity_to_dto.CustomerDto;
import s09_jpa_relations.rel06_entity_to_dto.OrderDetailsDto;
import s09_jpa_relations.rel06_entity_to_dto.OrderLine;
import s09_jpa_relations.rel06_entity_to_dto.OrderLineDto;
import s09_jpa_relations.rel06_entity_to_dto.OrderMapper;
import s09_jpa_relations.rel06_entity_to_dto.OrderStatus;
import s09_jpa_relations.rel06_entity_to_dto.OrderSummaryDto;
import s09_jpa_relations.rel06_entity_to_dto.ShopOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Sprawdzenia z Rel06EntityToDtoExercisesTest na rozwiązaniach wzorcowych (SolutionDtoService).
 * @Tag("wzorzec") — biegną zawsze: dowód, że ćwiczenia da się rozwiązać, a ich testy są poprawne.
 */
@Tag("wzorzec")
@DataJpaTest(showSql = false, properties = SqlCapture.PROPERTY)
@ActiveProfiles("rel06")
@Import({SolutionDtoService.class, OrderMapper.class})
class Rel06EntityToDtoSolutionsTest {

    @Autowired
    private SolutionDtoService exercises;

    @Autowired
    private TestEntityManager tem;

    @Autowired
    private EntityManagerFactory emf;

    private Statistics stats;

    @BeforeEach
    void setUp() {
        stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        SqlCapture.clear();
    }

    @Test
    @DisplayName("Wzorzec 1: klient jako CustomerDto, bez skrótu hasła")
    void exercise1() {
        assertThat(exercises.customer(1L)).isEqualTo(new CustomerDto(1L, "Anna Nowak", "anna@sklep.pl"));
        assertThatThrownBy(() -> exercises.customer(99L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Wzorzec 2: podsumowania zamówień klienta — 1 zapytanie, 0 encji")
    void exercise2() {
        assertThat(exercises.summariesOfCustomer(1L)).containsExactly(
                new OrderSummaryDto("ZAM-001", "Anna Nowak", OrderStatus.NEW, new BigDecimal("267.90")),
                new OrderSummaryDto("ZAM-003", "Anna Nowak", OrderStatus.NEW, new BigDecimal("98.00")));
        assertThat(SqlCapture.all()).hasSize(1);
        assertThat(stats.getEntityLoadCount()).isZero();
    }

    @Test
    @DisplayName("Wzorzec 3: zmiana ilości przez zarządzaną encję — jeden UPDATE pozycji, bez save")
    void exercise3() {
        exercises.changeQuantity(1L, "Mysz", 5);
        SqlCapture.clear();
        tem.flush();

        assertThat(SqlCapture.all())
                .containsExactly("update order_line set order_id=?,product_id=?,quantity=?,unit_price=? where id=?");
        tem.clear();
        assertThat(tem.find(OrderLine.class, 2L).getQuantity()).isEqualTo(5);
        assertThatThrownBy(() -> exercises.changeQuantity(1L, "Monitor", 1)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("Wzorzec 4: szczegóły wszystkich zamówień klienta — 1 zapytanie, kompletne DTO")
    void exercise4() {
        List<OrderDetailsDto> details = exercises.detailsOfCustomer(1L);

        assertThat(details).extracting(OrderDetailsDto::orderNumber).containsExactly("ZAM-001", "ZAM-003");
        assertThat(details.getFirst().lines()).extracting(OrderLineDto::productName).containsExactly("Klawiatura", "Mysz");
        assertThat(details.getLast().total()).isEqualByComparingTo("98.00");
        assertThat(SqlCapture.all()).hasSize(1);
    }

    @Test
    @DisplayName("Wzorzec 5: dopisanie pozycji do zamówienia — ceny z bazy, produkty jednym zapytaniem")
    void exercise5() {
        OrderDetailsDto updated = exercises.addItems(2L,
                List.of(new CreateOrderRequest.Item(2L, 1), new CreateOrderRequest.Item(4L, 2)));
        tem.flush();

        assertThat(updated.lines()).extracting(OrderLineDto::productName).containsExactly("Monitor", "Mysz", "Kubek");
        assertThat(updated.total()).as("899.99 + 59.00 + 2 × 24.50").isEqualByComparingTo("1007.99");
        assertThat(SqlCapture.startingWith("select p1_0")).as("produkty żądania jednym zapytaniem z IN; drugi SELECT "
                + "to produkt ISTNIEJĄCEJ pozycji (Monitor), dociągnięty przez maper").containsExactly(
                "select p1_0.id,p1_0.name,p1_0.price from product p1_0 where p1_0.id in (?,?)",
                "select p1_0.id,p1_0.name,p1_0.price from product p1_0 where p1_0.id=?");
        assertThat(SqlCapture.startingWith("insert")).hasSize(2);

        assertThatThrownBy(() -> exercises.addItems(3L,
                List.of(new CreateOrderRequest.Item(1L, 1), new CreateOrderRequest.Item(99L, 1))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Nieznany produkt: 99");
        tem.flush();
        tem.clear();
        assertThat(tem.find(ShopOrder.class, 3L).getLines()).as("nic nie dodano").hasSize(1);
    }
}
