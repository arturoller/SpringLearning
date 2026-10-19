package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PO: kontroler zwraca rekordy DTO zbudowane w serwisie (w transakcji). Kontroler nie wie nic o encjach,
 * Hibernate ani leniwym ładowaniu — dostaje gotowe, zwykłe obiekty.
 */
@RestController
@RequestMapping("/api/rel06")
public class DtoController {

    private final ShopService shopService;

    public DtoController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/products/{sku}")
    public ProductDto product(@PathVariable String sku) {
        return shopService.product(sku);
    }

    @GetMapping("/customers/{id}")
    public CustomerDto customer(@PathVariable Long id) {
        return shopService.customer(id);
    }
}
