package s09_jpa_relations.rel05_n_plus_one;

import jakarta.persistence.EntityManager;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;
import s09_jpa_relations.SqlCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ustawienia Hibernate z lekcji Rel05NPlusOne, które działają na CAŁĄ aplikację — każda w osobno uruchomionej
 * aplikacji Rel05Application z własną bazą H2 (UUID w nazwie). Odczyty w {@code TransactionTemplate}
 * (szablon transakcji), bo leniwe relacje mają się dociągać.
 */
class Rel05HibernateSettingsTest {

    private static ConfigurableApplicationContext start(String... extraArgs) {
        List<String> args = new ArrayList<>(List.of("--spring.profiles.active=rel05",
                "--spring.datasource.url=jdbc:h2:mem:rel05-" + UUID.randomUUID(), "--" + SqlCapture.PROPERTY));
        args.addAll(List.of(extraArgs));
        return new SpringApplicationBuilder(Rel05Application.class).web(WebApplicationType.NONE)
                .run(args.toArray(String[]::new));
    }

    @Test
    @DisplayName("Sekcja 4: hibernate.default_batch_fetch_size=16 — klienci i pozycje 10 zamówień: 1 + 1 + 1 zapytania")
    void section4DefaultBatchFetchSize() {
        try (ConfigurableApplicationContext app =
                     start("--spring.jpa.properties.hibernate.default_batch_fetch_size=16")) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            ShopOrderRepository orders = app.getBean(ShopOrderRepository.class);
            SqlCapture.clear();

            tx.executeWithoutResult(status -> {
                List<ShopOrder> all = orders.findAllOrdered();
                all.forEach(order -> order.getCustomer().getName());
                all.forEach(order -> order.getLines().size());
            });

            assertThat(SqlCapture.all()).containsExactly(
                    "select so1_0.id,so1_0.customer_id,so1_0.order_number from orders so1_0 order by so1_0.order_number",
                    "select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id in (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    "select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.unit_price from order_line l1_0 "
                            + "where l1_0.order_id in (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) order by l1_0.id");
        }
    }

    @Test
    @DisplayName("Sekcja 2: fail_on_pagination_over_collection_fetch=true — stronicowanie w pamięci kończy się wyjątkiem")
    void section2FailOnPaginationOverCollectionFetch() {
        try (ConfigurableApplicationContext app =
                     start("--spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true")) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);

            assertThatThrownBy(() -> tx.executeWithoutResult(status -> em
                    .createQuery("select o from ShopOrder o left join fetch o.lines l order by l.id", ShopOrder.class)
                    .setMaxResults(3)
                    .getResultList()))
                    .isInstanceOf(HibernateException.class)
                    .hasMessage("setFirstResult() or setMaxResults() specified with collection fetch join (in-memory "
                            + "pagination was about to be applied, but 'hibernate.query.fail_on_pagination_over_collection_fetch' "
                            + "is enabled)");

            List<ShopOrder> page = tx.execute(status -> em
                    .createQuery("select o from ShopOrder o left join fetch o.lines order by o.orderNumber", ShopOrder.class)
                    .setMaxResults(3)
                    .getResultList());
            assertThat(page).as("stronicowanie w podzapytaniu SQL jest dozwolone").hasSize(3);
        }
    }
}
