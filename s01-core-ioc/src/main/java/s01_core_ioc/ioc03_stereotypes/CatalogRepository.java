package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Repository;

import java.util.List;

/** Repozytorium katalogu (warstwa dostępu do danych). Domyślna nazwa beana: "catalogRepository". */
@Repository
public class CatalogRepository {

    public List<String> productNames() {
        return List.of("Kawa", "Herbata", "Czekolada");
    }
}
