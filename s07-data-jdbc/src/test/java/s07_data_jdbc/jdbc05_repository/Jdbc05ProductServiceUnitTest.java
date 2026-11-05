package s07_data_jdbc.jdbc05_repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sekcja 4: test JEDNOSTKOWY serwisu — bez bazy i bez Flyway. Repozytorium zastępuje atrapa (@MockitoBean),
 * więc sprawdzamy tylko logikę serwisu: co i w jakiej kolejności zapisuje. Kontekst Springa zawiera wyłącznie
 * ProductService (@SpringJUnitConfig) — nie ma menedżera transakcji, więc @Transactional nic tu nie robi.
 */
@SpringJUnitConfig(ProductService.class)
class Jdbc05ProductServiceUnitTest {

    @Autowired
    private ProductService service;

    @MockitoBean // atrapa (mock) repozytorium wstawiona w miejsce prawdziwego beana
    private ProductRepository repository;

    private static Product product(long id, String name, int stock) {
        return new Product(id, name, "dom", new BigDecimal("10.00"), stock, 0);
    }

    @Test
    @DisplayName("Sekcja 4: transferStock zmniejsza stan jednego produktu i zwiększa drugiego")
    void section4TransferSavesBoth() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1, "Kubek", 10)));
        when(repository.findById(2L)).thenReturn(Optional.of(product(2, "Lampka", 3)));

        service.transferStock(1, 2, 4);

        verify(repository).save(product(1, "Kubek", 6));
        verify(repository).save(product(2, "Lampka", 7));
    }

    @Test
    @DisplayName("Sekcja 4: za mało towaru → InsufficientStockException i żadnego zapisu")
    void section4InsufficientStock() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1, "Kubek", 2)));

        assertThatThrownBy(() -> service.transferStock(1, 2, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Kubek");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Sekcja 4: nieistniejący cel → ProductNotFoundException (po pierwszym zapisie — wycofa go transakcja)")
    void section4MissingTarget() {
        when(repository.findById(1L)).thenReturn(Optional.of(product(1, "Kubek", 10)));
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transferStock(1, 99, 4))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
        verify(repository).save(product(1, "Kubek", 6)); // atrapa nie ma transakcji — zapis 1 „poszedł”
    }
}
