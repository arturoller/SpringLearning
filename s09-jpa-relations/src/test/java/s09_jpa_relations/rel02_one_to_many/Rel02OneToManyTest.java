package s09_jpa_relations.rel02_one_to_many;

import org.hibernate.collection.spi.PersistentBag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Rel02OneToMany — „WYNIK” każdej sekcji. {@code @DataJpaTest} + profil rel02 (migracje Flyway,
 * ddl-auto=validate). JdbcClient zagląda do tabel „obok” Hibernate — widzimy, co NAPRAWDĘ jest w kolumnie order_id.
 */
@DataJpaTest
@ActiveProfiles("rel02")
class Rel02OneToManyTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    ShopOrderRepository orders;

    @Test
    @DisplayName("1. Dopisanie pozycji TYLKO do listy (strona odwrotna) — w bazie order_id zostaje NULL")
    void inverseSideAloneDoesNotWriteForeignKey() {
        NaiveOrder order = new NaiveOrder("Anna");
        NaiveOrderLine line = new NaiveOrderLine("Kawa", 2);
        order.getLines().add(line);                 // tylko lista — line.order nadal null
        em.persist(order);                          // cascade PERSIST zapisze też pozycję
        em.flush();

        Long fk = jdbc.sql("select order_id from naive_order_lines where id = ?").param(line.getId())
                .query(Long.class).optional().orElse(null);
        assertThat(fk).as("klucz obcy nieustawiony").isNull();

        em.clear();
        assertThat(em.find(NaiveOrder.class, order.getId()).getLines())
                .as("po ponownym odczycie zamówienie „nie ma” pozycji").isEmpty();
    }

    @Test
    @DisplayName("1. Ustawienie TYLKO strony właścicielskiej — baza poprawna, ale lista w pamięci nieaktualna")
    void owningSideAloneWritesForeignKeyButListIsStale() {
        NaiveOrder order = em.persist(new NaiveOrder("Anna"));
        NaiveOrderLine line = new NaiveOrderLine("Kawa", 2);
        line.setOrder(order);                       // tylko strona właścicielska
        em.persist(line);
        em.flush();

        assertThat(jdbc.sql("select order_id from naive_order_lines where id = ?").param(line.getId())
                .query(Long.class).single()).isEqualTo(order.getId());
        assertThat(order.getLines()).as("ten sam obiekt w pamięci — lista o niczym nie wie").isEmpty();
    }

    @Test
    @DisplayName("2. Metoda pomocnicza addLine ustawia obie strony — klucz obcy zapisany, lista aktualna")
    void helperMethodKeepsBothSidesInSync() {
        ShopOrder order = new ShopOrder("Anna");
        order.addLine(new OrderLine("Kawa", 2));
        order.addLine(new OrderLine("Herbata", 1));
        em.persist(order);
        em.flush();

        assertThat(order.getLines()).hasSize(2);
        assertThat(jdbc.sql("select count(*) from order_lines where order_id = ?").param(order.getId())
                .query(Long.class).single()).isEqualTo(2);
    }

    @Test
    @DisplayName("3. cascade = ALL: save zamówienia zapisuje pozycje, delete zamówienia usuwa pozycje")
    void cascadeAll() {
        ShopOrder order = new ShopOrder("Piotr");
        order.addLine(new OrderLine("Kawa", 1));
        order.addLine(new OrderLine("Kakao", 3));
        orders.saveAndFlush(order);
        assertThat(countRows("order_lines")).isEqualTo(2);

        orders.delete(order);
        orders.flush();

        assertThat(countRows("orders")).isZero();
        assertThat(countRows("order_lines")).as("pozycje usunięte kaskadą").isZero();
    }

    @Test
    @DisplayName("4. orphanRemoval = true: pozycja wyjęta z listy znika z bazy (DELETE)")
    void orphanRemovalDeletesRow() {
        ShopOrder order = new ShopOrder("Ola");
        OrderLine coffee = new OrderLine("Kawa", 1);
        order.addLine(coffee);
        order.addLine(new OrderLine("Herbata", 2));
        em.persist(order);
        em.flush();

        order.removeLine(coffee);
        em.flush();

        assertThat(countRows("order_lines")).isEqualTo(1);
        assertThat(jdbc.sql("select product_name from order_lines").query(String.class).single()).isEqualTo("Herbata");
    }

    @Test
    @DisplayName("4. Bez orphanRemoval: usunięcie z listy NIC nie zmienia w bazie — wiersz i klucz obcy zostają")
    void withoutOrphanRemovalNothingHappens() {
        NaiveOrder order = em.persist(new NaiveOrder("Ola"));
        NaiveOrderLine line = new NaiveOrderLine("Kawa", 1);
        line.setOrder(order);
        order.getLines().add(line);
        em.persist(line);
        em.flush();

        order.getLines().remove(line);
        em.flush();

        assertThat(jdbc.sql("select order_id from naive_order_lines where id = ?").param(line.getId())
                .query(Long.class).single()).isEqualTo(order.getId());
    }

    @Test
    @DisplayName("4. PUŁAPKA: podmiana instancji kolekcji z orphanRemoval kończy flush błędem")
    void replacingOrphanRemovalCollectionFails() {
        ShopOrder order = new ShopOrder("Ola");
        order.addLine(new OrderLine("Kawa", 1));
        em.persist(order);
        em.flush();

        // Symulujemy „this.lines = new ArrayList<>()” — ShopOrder celowo nie ma takiej metody.
        ReflectionTestUtils.setField(order, "lines", new ArrayList<OrderLine>());

        assertThatThrownBy(() -> em.flush())
                .hasMessageContaining("A collection with orphan deletion was no longer referenced by the owning entity instance");
    }

    @Test
    @DisplayName("5. Kolekcja wczytana z bazy to PersistentBag (List bez kolejności i duplikatów pod kontrolą Hibernate)")
    void loadedListIsPersistentBag() {
        NaiveOrder order = em.persist(new NaiveOrder("Anna"));
        em.flush();
        em.clear();

        assertThat(em.find(NaiveOrder.class, order.getId()).getLines()).isInstanceOf(PersistentBag.class);
    }

    private long countRows(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }
}
