package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Sekcja 4: @JdbcTest bez @Import — wycinek nie skanuje komponentów, więc nie zna repozytorium ani serwisu. */
@JdbcTest
@ActiveProfiles("jdbc05")
class Jdbc05SliceWithoutImportTest {

    @Autowired
    private ObjectProvider<ProductRepository> repository;

    @Autowired
    private ObjectProvider<ProductService> service;

    @Test
    @DisplayName("Sekcja 4: bez @Import wycinek @JdbcTest nie zawiera beana @Repository ani @Service")
    void section4SliceDoesNotScanComponents() {
        assertThat(repository.getIfAvailable()).isNull();
        assertThat(service.getIfAvailable()).isNull();
    }
}
