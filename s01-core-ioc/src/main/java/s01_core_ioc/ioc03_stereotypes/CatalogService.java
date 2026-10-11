package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Service;

import java.util.List;

/** Serwis katalogu (warstwa logiki biznesowej). Domyślna nazwa beana: "catalogService". */
@Service
public class CatalogService {

    private final CatalogRepository repository;

    public CatalogService(CatalogRepository repository) {
        this.repository = repository;
    }

    public List<String> sortedProductNames() {
        return repository.productNames().stream().sorted().toList();
    }
}
