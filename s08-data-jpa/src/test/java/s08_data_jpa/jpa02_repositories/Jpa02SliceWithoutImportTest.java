package s08_data_jpa.jpa02_repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 5: wycinek {@code @DataJpaTest} BEZ {@code @Import} — repozytorium jest, serwisu (@Service) nie ma. */
@DataJpaTest(showSql = false)
@ActiveProfiles("jpa02")
class Jpa02SliceWithoutImportTest {

    @Autowired
    private ObjectProvider<ProductService> service; // object provider = dostawca, który nie wymaga istnienia beana

    @Autowired
    private ObjectProvider<ProductRepository> repository;

    @Test
    @DisplayName("Sekcja 5: @DataJpaTest tworzy repozytoria Spring Data, ale nie skanuje @Service")
    void sliceHasRepositoriesButNoServices() {
        assertThat(repository.getIfAvailable()).isNotNull();
        assertThat(service.getIfAvailable()).isNull();
    }
}
