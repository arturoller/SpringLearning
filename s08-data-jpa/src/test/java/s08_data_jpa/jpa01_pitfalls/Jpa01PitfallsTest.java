package s08_data_jpa.jpa01_pitfalls;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.InstantiationException;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import s08_data_jpa.jpa01_entities.Category;
import s08_data_jpa.jpa01_entities.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy sekcji 5 lekcji Jpa01Entities — pułapki encji. Pakiet jpa01_pitfalls leży POZA jpa01_entities, żeby
 * klasa startowa lekcji nie zeskanowała błędnych encji. Bez Springa: każdy test buduje własną, małą
 * EntityManagerFactory standardowym API JPA 3.2 (PersistenceConfiguration; tu jej wersja z Hibernate) z jawną
 * listą encji i osobną bazą H2.
 */
class Jpa01PitfallsTest {

    private static EntityManagerFactory factory(Class<?>... entities) {
        String name = "jpa01-pitfalls-" + UUID.randomUUID();
        HibernatePersistenceConfiguration configuration = new HibernatePersistenceConfiguration(name)
                .jdbcUrl("jdbc:h2:mem:" + name)
                .jdbcCredentials("sa", "")
                .property("hibernate.hbm2ddl.auto", "create-drop")
                .property("hibernate.generate_statistics", "true");
        for (Class<?> entity : entities) {
            configuration.managedClass(entity);
        }
        return configuration.createEntityManagerFactory();
    }

    /** persist w osobnej transakcji (zatwierdzonej), potem pusty kontekst — następny odczyt idzie do bazy. */
    private static void saveAndClear(EntityManager em, Object... entities) {
        em.getTransaction().begin();
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.getTransaction().commit();
        em.clear();
    }

    @Test
    @DisplayName("Sekcja 5a: bez konstruktora bez argumentów start i zapis działają, a wybucha dopiero ODCZYT (InstantiationException)")
    void noDefaultConstructor() {
        try (EntityManagerFactory emf = factory(NoDefaultConstructorProduct.class); // start — bez błędu
             EntityManager em = emf.createEntityManager()) {
            saveAndClear(em, new NoDefaultConstructorProduct(1L, "Mysz")); // zapis — bez błędu

            assertThatThrownBy(() -> em.find(NoDefaultConstructorProduct.class, 1L))
                    .isInstanceOf(InstantiationException.class)
                    .hasMessageContaining("No default constructor for entity");
        }
    }

    @Test
    @DisplayName("Sekcja 5b: getReference daje leniwe proxy dla zwykłej klasy, a dla klasy final — od razu SELECT i prawdziwy obiekt")
    void finalClassHasNoProxy() {
        try (EntityManagerFactory emf = factory(FinalProduct.class, OpenProduct.class);
             EntityManager em = emf.createEntityManager()) {
            saveAndClear(em, new FinalProduct(1L, "Kubek"), new OpenProduct(1L, "Lampka", new BigDecimal("79.00")));
            Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
            stats.clear();

            OpenProduct open = em.getReference(OpenProduct.class, 1L);
            assertThat(open.getClass().getSimpleName()).as("podklasa-proxy").isEqualTo("OpenProduct$HibernateProxy");
            assertThat(stats.getPrepareStatementCount()).as("proxy: jeszcze bez SELECT").isZero();
            assertThat(open.getName()).isEqualTo("Lampka");
            assertThat(stats.getPrepareStatementCount()).as("SELECT dopiero przy pierwszym użyciu").isEqualTo(1);

            stats.clear();
            FinalProduct fin = em.getReference(FinalProduct.class, 1L);
            assertThat(fin.getClass()).as("final: bez proxy").isEqualTo(FinalProduct.class);
            assertThat(stats.getPrepareStatementCount()).as("final: SELECT od razu").isEqualTo(1);
        }
    }

    @Test
    @DisplayName("Sekcja 2 (DOBRA PRAKTYKA): BigDecimal bez precision/scale → Hibernate wybiera numeric(38,2)")
    void bigDecimalWithoutPrecision() {
        try (EntityManagerFactory emf = factory(OpenProduct.class);
             EntityManager em = emf.createEntityManager()) {
            Object[] column = (Object[]) em.createNativeQuery("""
                            SELECT numeric_precision, numeric_scale FROM information_schema.columns
                            WHERE table_name = 'OPENPRODUCT' AND column_name = 'PRICE'""")
                    .getSingleResult();
            assertThat(column).containsExactly(38, 2);
        }
    }

    @Test
    @DisplayName("Sekcja 5c: equals/hashCode na wszystkich polach — po persist encja „ginie” w HashSet; wersja po id ze stałym hashCode działa")
    void equalsHashCodeInHashSet() {
        try (EntityManagerFactory emf = factory(AllFieldsEqualsProduct.class, Product.class);
             EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            AllFieldsEqualsProduct bad = new AllFieldsEqualsProduct("Mysz");
            Set<AllFieldsEqualsProduct> badSet = new HashSet<>();
            badSet.add(bad);
            em.persist(bad); // nadaje id → zmienia hashCode
            assertThat(badSet.contains(bad)).as("hashCode zmienił się po persist").isFalse();
            assertThat(badSet).as("a obiekt nadal tam jest").hasSize(1);

            Product good = new Product("Mysz", Category.COMPUTERS, new BigDecimal("59.00"), 30, Instant.parse("2026-10-01T08:00:00Z"));
            Set<Product> goodSet = new HashSet<>();
            goodSet.add(good);
            em.persist(good);
            good.setPrice(new BigDecimal("49.00"));
            assertThat(goodSet.contains(good)).as("stały hashCode — znajdzie po persist i zmianie pola").isTrue();

            em.getTransaction().rollback();
        }
    }
}
