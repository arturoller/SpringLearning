package s09_jpa_relations.rel02_pitfalls;

import jakarta.persistence.EntityManager;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import s09_jpa_relations.SqlCapture;
import s09_jpa_relations.rel02_pitfalls.broken.BrokenRel02Application;
import s09_jpa_relations.rel02_pitfalls.broken.LoopOrder;
import s09_jpa_relations.rel02_pitfalls.broken.RemovingOrderLine;
import s09_jpa_relations.rel02_pitfalls.jointable.JoinTableApplication;
import s09_jpa_relations.rel02_pitfalls.jointable.Pallet;
import s09_jpa_relations.rel02_pitfalls.jointable.PalletItem;
import s09_jpa_relations.rel02_pitfalls.jointable.Parcel;
import s09_jpa_relations.rel02_pitfalls.jointable.ParcelItem;
import s09_jpa_relations.rel02_pitfalls.nomappedby.NoMappedByApplication;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PUŁAPKI lekcji Rel02OneToMany na CELOWO złych encjach — każda w osobnej aplikacji z pakietów rel02_pitfalls.*,
 * z własną bazą H2 (UUID w nazwie). Pełna aplikacja zamiast @DataJpaTest, bo transakcje są tu PRAWDZIWE
 * (TransactionTemplate — szablon transakcji — zatwierdza zmiany), a stan bazy sprawdzamy potem JdbcClientem.
 */
class Rel02BrokenMappingsTest {

    private static ConfigurableApplicationContext start(Class<?> application, String... extraArgs) {
        List<String> args = new java.util.ArrayList<>(List.of(
                "--spring.datasource.url=jdbc:h2:mem:rel02-pitfall-" + UUID.randomUUID(), "--" + SqlCapture.PROPERTY));
        args.addAll(List.of(extraArgs));
        return new SpringApplicationBuilder(application).web(WebApplicationType.NONE).run(args.toArray(String[]::new));
    }

    @Test
    @DisplayName("Sekcja 1: toString przechodzący po relacji dwukierunkowej → dwa SELECT-y i StackOverflowError")
    void toStringLoopInBidirectionalRelation() {
        try (ConfigurableApplicationContext app = start(BrokenRel02Application.class, "--spring.profiles.active=rel02")) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            SqlCapture.clear();

            assertThatThrownBy(() -> tx.executeWithoutResult(status -> em.find(LoopOrder.class, 1L).toString()))
                    .isInstanceOf(StackOverflowError.class);
            assertThat(SqlCapture.all()).as("zamówienie, potem jego pozycje — i nieskończona rekurencja w pamięci")
                    .containsExactly("select lo1_0.id,lo1_0.order_number from orders lo1_0 where lo1_0.id=?",
                            "select l1_0.order_id,l1_0.id,l1_0.quantity from order_line l1_0 where l1_0.order_id=?");
        }
    }

    @Test
    @DisplayName("Sekcja 3: CascadeType.REMOVE na @ManyToOne — usunięcie pozycji usuwa produkt albo łamie klucz obcy")
    void cascadeRemoveOnManyToOneDeletesParent() {
        try (ConfigurableApplicationContext app = start(BrokenRel02Application.class, "--spring.profiles.active=rel02")) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            JdbcClient jdbc = app.getBean(JdbcClient.class);
            SqlCapture.clear();

            // pozycja 5 = Kubek ×4 — jedyna pozycja z Kubkiem
            tx.executeWithoutResult(status -> em.remove(em.find(RemovingOrderLine.class, 5L)));

            assertThat(SqlCapture.all()).containsExactly(
                    "select rol1_0.id,rol1_0.product_id from order_line rol1_0 where rol1_0.id=?",
                    "delete from order_line where id=?",
                    "delete from product where id=?");
            assertThat(jdbc.sql("select name from product order by id").query(String.class).list())
                    .as("Kubek zniknął z katalogu razem z pozycją").containsExactly("Klawiatura", "Mysz", "Monitor");

            // pozycja 2 = Mysz ×2 — Mysz jest też w pozycji 4
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> em.remove(em.find(RemovingOrderLine.class, 2L))))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("FK_ORDER_LINE_PRODUCT");
            assertThat(jdbc.sql("select count(*) from order_line").query(Long.class).single())
                    .as("transakcja wycofana — pozycja 2 nadal jest").isEqualTo(4L);
        }
    }

    @Test
    @DisplayName("Sekcja 2: jednokierunkowe @OneToMany bez @JoinColumn → tabela łącząca parcel_items i INSERT na każdy element")
    void unidirectionalOneToManyUsesJoinTable() {
        try (ConfigurableApplicationContext app = start(JoinTableApplication.class,
                "--spring.jpa.hibernate.ddl-auto=create-drop")) {
            JdbcClient jdbc = app.getBean(JdbcClient.class);
            assertThat(jdbc.sql("select lower(table_name) from information_schema.tables where table_schema = 'PUBLIC' "
                    + "order by 1").query(String.class).list())
                    .containsExactly("pallet", "pallet_item", "parcel", "parcel_item", "parcel_items");
            assertThat(jdbc.sql("select lower(column_name) from information_schema.columns where table_name = 'PARCEL_ITEMS'")
                    .query(String.class).list())
                    .containsExactlyInAnyOrder("parcel_id", "items_id");

            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            SqlCapture.clear();
            tx.executeWithoutResult(status -> {
                Parcel parcel = new Parcel();
                parcel.getItems().add(new ParcelItem("książka"));
                parcel.getItems().add(new ParcelItem("kubek"));
                em.persist(parcel);
            });

            assertThat(SqlCapture.all()).containsExactly(
                    "insert into parcel (id) values (default)",
                    "insert into parcel_item (name,id) values (?,default)",
                    "insert into parcel_item (name,id) values (?,default)",
                    "insert into parcel_items (parcel_id,items_id) values (?,?)",
                    "insert into parcel_items (parcel_id,items_id) values (?,?)");
        }
    }

    @Test
    @DisplayName("Sekcja 2: @JoinColumn(nullable = false) — klucz obcy już w INSERT, ale UPDATE-y i tak zostają")
    void unidirectionalWithNotNullJoinColumnStillUpdates() {
        try (ConfigurableApplicationContext app = start(JoinTableApplication.class,
                "--spring.jpa.hibernate.ddl-auto=create-drop")) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            SqlCapture.clear();
            tx.executeWithoutResult(status -> {
                Pallet pallet = new Pallet();
                pallet.getItems().add(new PalletItem("karton A"));
                pallet.getItems().add(new PalletItem("karton B"));
                em.persist(pallet);
            });

            assertThat(SqlCapture.all()).containsExactly(
                    "insert into pallet (id) values (default)",
                    "insert into pallet_item (pallet_id,name,id) values (?,?,default)",
                    "insert into pallet_item (pallet_id,name,id) values (?,?,default)",
                    "update pallet_item set pallet_id=? where id=?",
                    "update pallet_item set pallet_id=? where id=?");
        }
    }

    @Test
    @DisplayName("Pytanie 3: @OneToMany bez mappedBy przy ddl-auto=validate → aplikacja nie startuje (brak tabeli orders_lines)")
    void missingMappedByExpectsJoinTable() {
        assertThatThrownBy(() -> start(NoMappedByApplication.class, "--spring.profiles.active=rel02").close())
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(SchemaManagementException.class)
                .rootCause().hasMessageContaining("missing table [orders_lines]");
    }
}
