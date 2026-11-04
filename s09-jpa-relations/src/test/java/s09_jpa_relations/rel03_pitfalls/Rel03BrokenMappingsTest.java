package s09_jpa_relations.rel03_pitfalls;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;
import s09_jpa_relations.SqlCapture;
import s09_jpa_relations.rel03_many_to_many.Tag;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PUŁAPKI lekcji Rel03ManyToMany na CELOWO złych encjach (BagProduct, CascadingProduct) — w osobnej aplikacji
 * BrokenRel03Application z własną bazą H2 na każdy test. Transakcje prawdziwe (TransactionTemplate — szablon
 * transakcji), stan bazy sprawdzamy JdbcClientem.
 */
class Rel03BrokenMappingsTest {

    private static final String SELECT_PRODUCT_TAGS = "select t1_0.product_id,t1_1.id,t1_1.name from product_tag t1_0 "
            + "join tag t1_1 on t1_1.id=t1_0.tag_id where t1_0.product_id=?";
    private static final String INSERT_LINK = "insert into product_tag (product_id,tag_id) values (?,?)";

    private static ConfigurableApplicationContext start() {
        return new SpringApplicationBuilder(BrokenRel03Application.class).web(WebApplicationType.NONE)
                .run("--spring.profiles.active=rel03",
                        "--spring.datasource.url=jdbc:h2:mem:rel03-pitfall-" + UUID.randomUUID(),
                        "--" + SqlCapture.PROPERTY);
    }

    @Test
    @DisplayName("Sekcja 2: List przy @ManyToMany — usunięcie JEDNEGO tagu kasuje wszystkie wiersze i wstawia 3 z powrotem")
    void bagRecreatesWholeJoinTableOnRemove() {
        try (ConfigurableApplicationContext app = start()) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            SqlCapture.clear();

            tx.executeWithoutResult(status -> em.find(BagProduct.class, 2L).getTags()   // Mysz, 4 tagi
                    .removeIf(tag -> tag.getName().equals("promocja")));

            assertThat(SqlCapture.all()).containsExactly(
                    "select bp1_0.id,bp1_0.name from product bp1_0 where bp1_0.id=?",
                    SELECT_PRODUCT_TAGS,
                    "delete from product_tag where product_id=?",
                    INSERT_LINK, INSERT_LINK, INSERT_LINK);
        }
    }

    @Test
    @DisplayName("Sekcja 2: List przy @ManyToMany — nawet DODANIE tagu to DELETE wszystkich + INSERT każdego")
    void bagRecreatesWholeJoinTableOnAdd() {
        try (ConfigurableApplicationContext app = start()) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            SqlCapture.clear();

            tx.executeWithoutResult(status -> em.find(BagProduct.class, 1L).getTags()   // Klawiatura, 2 tagi
                    .add(em.find(Tag.class, 5L)));                                       // + nowość

            assertThat(SqlCapture.all()).containsExactly(
                    "select bp1_0.id,bp1_0.name from product bp1_0 where bp1_0.id=?",
                    "select t1_0.id,t1_0.name from tag t1_0 where t1_0.id=?",
                    SELECT_PRODUCT_TAGS,
                    "delete from product_tag where product_id=?",
                    INSERT_LINK, INSERT_LINK, INSERT_LINK);
        }
    }

    @Test
    @DisplayName("Sekcja 3: cascade = ALL na @ManyToMany — usunięcie produktu próbuje usunąć współdzielony tag → błąd FK")
    void cascadeRemoveOnManyToManyHitsSharedTag() {
        try (ConfigurableApplicationContext app = start()) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            JdbcClient jdbc = app.getBean(JdbcClient.class);
            SqlCapture.clear();

            // Kubek ma tylko tag „promocja”, ale „promocja” jest też na Myszy
            assertThatThrownBy(() -> tx.executeWithoutResult(status -> em.remove(em.find(CascadingProduct.class, 4L))))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("FK_PRODUCT_TAG_TAG");
            assertThat(SqlCapture.all()).containsExactly(
                    "select cp1_0.id,cp1_0.name from product cp1_0 where cp1_0.id=?",
                    SELECT_PRODUCT_TAGS,
                    "delete from product_tag where product_id=?",
                    "delete from tag where id=?");
            assertThat(jdbc.sql("select count(*) from product where name = 'Kubek'").query(Long.class).single())
                    .as("transakcja wycofana — Kubka nie da się usunąć").isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("Sekcja 3: cascade = ALL na @ManyToMany — tag używany tylko przez usuwany produkt znika po cichu")
    void cascadeRemoveOnManyToManySilentlyDeletesTag() {
        try (ConfigurableApplicationContext app = start()) {
            TransactionTemplate tx = app.getBean(TransactionTemplate.class);
            EntityManager em = app.getBean(EntityManager.class);
            JdbcClient jdbc = app.getBean(JdbcClient.class);
            jdbc.sql("insert into product (name, price) values ('Termos', 50.00)").update();
            jdbc.sql("insert into tag (name) values ('outdoor')").update();
            jdbc.sql("insert into product_tag (product_id, tag_id) select p.id, t.id from product p, tag t "
                    + "where p.name = 'Termos' and t.name = 'outdoor'").update();
            Long thermos = jdbc.sql("select id from product where name = 'Termos'").query(Long.class).single();
            SqlCapture.clear();

            tx.executeWithoutResult(status -> em.remove(em.find(CascadingProduct.class, thermos)));

            assertThat(SqlCapture.all()).containsExactly(
                    "select cp1_0.id,cp1_0.name from product cp1_0 where cp1_0.id=?",
                    SELECT_PRODUCT_TAGS,
                    "delete from product_tag where product_id=?",
                    "delete from tag where id=?",
                    "delete from product where id=?");
            assertThat(jdbc.sql("select count(*) from tag where name = 'outdoor'").query(Long.class).single())
                    .as("tag „outdoor” zniknął razem z produktem").isZero();
        }
    }
}
