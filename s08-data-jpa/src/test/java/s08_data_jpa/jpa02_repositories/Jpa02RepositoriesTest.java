package s08_data_jpa.jpa02_repositories;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Jpa02Repositories. {@code @DataJpaTest} — najwęższy test, który ma repozytoria i Hibernate'a.
 * {@link TestEntityManager} przygotowuje dane i robi flush/clear, a {@link OutputCaptureExtension} pozwala
 * sprawdzić, jaki SQL naprawdę poszedł do bazy (linie {@code Hibernate: ...} z show-sql).
 */
@DataJpaTest
@ActiveProfiles("jpa02")
@ExtendWith(OutputCaptureExtension.class)
class Jpa02RepositoriesTest {

    private static final String SELECT_PRODUCT_BY_ID =
            "Hibernate: select p1_0.id,p1_0.category,p1_0.name,p1_0.price,p1_0.sku,p1_0.stock from products p1_0 where p1_0.id=?";

    @Autowired
    ProductRepository productRepository;

    @Autowired
    DiscountCodeRepository discountCodeRepository;

    @Autowired
    ProductCatalog productCatalog;

    @Autowired
    TestEntityManager entityManager;

    private static String since(CapturedOutput output, int start) {
        return output.toString().substring(start);
    }

    private static Product laptop() {
        return new Product("ELE-001", "Laptop Pro 14", Category.ELEKTRONIKA, new BigDecimal("5499.99"), 7);
    }

    private static Product smartphone() {
        return new Product("ELE-002", "Smartfon X", Category.ELEKTRONIKA, new BigDecimal("2999.00"), 0);
    }

    @Test
    @DisplayName("1. Repozytorium to proxy JDK, które przekazuje wywołania do SimpleJpaRepository")
    void repositoryIsProxy() {
        assertThat(AopUtils.isJdkDynamicProxy(productRepository)).isTrue();
        assertThat(AopProxyUtils.ultimateTargetClass(productRepository)).isEqualTo(SimpleJpaRepository.class);

        productRepository.saveAll(List.of(laptop(), smartphone()));
        List<Product> all = productRepository.findAll();               // ListCrudRepository: List, nie Iterable
        assertThat(all).extracting(Product::getSku).containsExactlyInAnyOrder("ELE-001", "ELE-002");
    }

    @Test
    @DisplayName("2. save: nowa encja → persist (ten sam obiekt), odłączona → merge (SELECT i KOPIA); pracuj na wyniku save")
    void saveNewVersusDetached(CapturedOutput output) {
        Product laptop = laptop();
        Product saved = productRepository.save(laptop);
        assertThat(saved).isSameAs(laptop);                             // persist — ten sam obiekt
        entityManager.flush();
        entityManager.clear();                                          // laptop jest teraz odłączony

        int start = output.length();
        laptop.setStock(5);
        Product merged = productRepository.save(laptop);
        assertThat(merged).isNotSameAs(laptop);                         // merge — zarządzana KOPIA
        assertThat(since(output, start)).contains(SELECT_PRODUCT_BY_ID);
        assertThat(entityManager.getEntityManager().contains(laptop)).isFalse();
        assertThat(entityManager.getEntityManager().contains(merged)).isTrue();
        entityManager.flush();
        assertThat(since(output, start)).contains("Hibernate: update products set");

        // pytanie kontrolne 2: zmiana obiektu odłączonego PO save przepada
        Long id = laptop.getId();
        entityManager.clear();
        Product p = productRepository.findById(id).orElseThrow();
        entityManager.clear();
        p.setStock(0);
        productRepository.save(p);                                      // wynik zignorowany!
        p.setName("Nowa nazwa");
        entityManager.flush();
        entityManager.clear();
        Product fromDb = entityManager.find(Product.class, id);
        assertThat(fromDb.getStock()).isZero();
        assertThat(fromDb.getName()).isEqualTo("Laptop Pro 14");
    }

    @Test
    @DisplayName("3. Ręcznie nadane id: save nowego kodu rabatowego robi merge — najpierw SELECT, potem INSERT")
    void saveWithAssignedId(CapturedOutput output) {
        int start = output.length();
        discountCodeRepository.save(new DiscountCode("LATO10", 10));
        entityManager.flush();
        String sql = since(output, start);
        int select = sql.indexOf("Hibernate: select dc1_0.code,dc1_0.percent from discount_codes dc1_0 where dc1_0.code=?");
        int insert = sql.indexOf("Hibernate: insert into discount_codes (percent,code) values (?,?)");
        assertThat(select).as("zbędny SELECT").isNotNegative();
        assertThat(insert).isGreaterThan(select);
    }

    @Test
    @DisplayName("4. Odczyt: Optional z findById, count/existsById jako count(*), getReferenceById bez SELECT, flush przed zapytaniem")
    void reading(CapturedOutput output) {
        Long id = entityManager.persistAndGetId(laptop(), Long.class);
        entityManager.flush();
        entityManager.clear();

        assertThat(productRepository.findById(id)).isPresent();
        assertThat(productRepository.findById(-1L)).isEmpty();          // brak = pusty Optional, nie null
        assertThat(productRepository.findAllById(List.of(id, -1L))).hasSize(1);

        int start = output.length();
        assertThat(productRepository.existsById(id)).isTrue();
        assertThat(productRepository.count()).isEqualTo(1);
        assertThat(since(output, start))
                .contains("Hibernate: select count(*) from products p1_0 where p1_0.id=?")
                .contains("Hibernate: select count(*) from products p1_0");

        // findById zaraz po save — z kontekstu utrwalania, bez SELECT
        Product phone = productRepository.save(smartphone());
        start = output.length();
        assertThat(productRepository.findById(phone.getId()).orElseThrow()).isSameAs(phone);
        assertThat(since(output, start)).doesNotContain("Hibernate: select");

        // count() najpierw wypycha oczekujący INSERT (flush automatyczny), dopiero potem liczy
        start = output.length();
        assertThat(productRepository.count()).isEqualTo(2);
        String sql = since(output, start);
        assertThat(sql.indexOf("Hibernate: insert into products")).isNotNegative()
                .isLessThan(sql.indexOf("Hibernate: select count(*) from products p1_0"));

        // getReferenceById: pośrednik bez SELECT; brak wiersza wychodzi dopiero przy getterze
        entityManager.clear();
        start = output.length();
        Product reference = productRepository.getReferenceById(12345L);
        assertThat(reference.getClass().getSimpleName()).contains("HibernateProxy");
        assertThat(since(output, start)).doesNotContain("Hibernate: select");
        assertThatThrownBy(reference::getName)
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No row with the given identifier exists");

        assertThatThrownBy(() -> productRepository.findById(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("The given id must not be null");
    }

    @Test
    @DisplayName("5. Usuwanie: deleteById = SELECT + DELETE, brak wiersza bez wyjątku; deleteAllInBatch omija kontekst")
    void deleting(CapturedOutput output) {
        Long id = entityManager.persistAndGetId(laptop(), Long.class);
        entityManager.flush();
        entityManager.clear();

        int start = output.length();
        productRepository.deleteById(id);
        entityManager.flush();
        assertThat(since(output, start))
                .contains(SELECT_PRODUCT_BY_ID)
                .contains("Hibernate: delete from products where id=?");
        assertThat(productRepository.existsById(id)).isFalse();

        productRepository.deleteById(-1L);                              // brak wiersza — po prostu nic

        // PUŁAPKA: deleteAllInBatch — jedno DELETE w bazie, ale wczytana encja dalej „żyje” w kontekście
        Product phone = productRepository.saveAndFlush(smartphone());
        start = output.length();
        productRepository.deleteAllInBatch();
        assertThat(since(output, start)).contains("Hibernate: delete from products p1_0");
        assertThat(productRepository.count()).isZero();                    // w bazie pusto...
        assertThat(productRepository.findById(phone.getId())).isPresent();  // ...a findById zwraca obiekt z pamięci
    }

    @Test
    @DisplayName("6. @DataJpaTest: transakcja wycofywana, save bez flush nie dotyka bazy, repozytorium tłumaczy wyjątki")
    void dataJpaTestBasics() {
        assertThat(TestTransaction.isActive()).isTrue();
        assertThat(TestTransaction.isFlaggedForRollback()).isTrue();

        Product laptop = laptop();
        Product found = entityManager.persistFlushFind(laptop);         // persist + flush + clear + find
        assertThat(found).isNotSameAs(laptop).isEqualTo(laptop);         // nowy obiekt z bazy, ten sam sku

        // PUŁAPKA: save bez flush nie dotyka bazy — za długie sku (limit 20 znaków) przechodzi bez błędu...
        Product tooLong = productRepository.save(
                new Product("ELE-004-MONITOR-27-CALI", "Monitor 27 cali", Category.ELEKTRONIKA, new BigDecimal("1299.00"), 4));
        assertThat(productRepository.findById(tooLong.getId())).isPresent();   // ...findById „znajduje” go w pamięci...
        assertThatThrownBy(entityManager::flush)                                // ...dopiero flush wysyła INSERT
                .isInstanceOf(org.hibernate.exception.DataException.class);
        entityManager.clear();                                                  // wyrzucamy nieudany INSERT

        assertThatThrownBy(() -> productRepository.saveAndFlush(
                new Product("ELE-001", "Podróbka", Category.ELEKTRONIKA, BigDecimal.ONE, 1)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasCauseInstanceOf(org.hibernate.exception.ConstraintViolationException.class);
    }

    @Test
    @DisplayName("7. Repozytorium tylko do odczytu: goły Repository z wybranymi metodami nie ma save ani delete")
    void readOnlyRepository() {
        productRepository.saveAll(List.of(laptop(), smartphone()));
        assertThat(productCatalog.count()).isEqualTo(2);
        assertThat(productCatalog.findAll()).hasSize(2);
        assertThat(Arrays.stream(ProductCatalog.class.getMethods()).map(Method::getName))
                .containsExactlyInAnyOrder("findById", "findAll", "count");
    }
}
