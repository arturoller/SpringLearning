package s09_jpa_relations.rel06_entity_to_dto;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PRZED: kontroler, który zwraca ENCJE prosto z serwisu. Jackson (biblioteka JSON) zamienia na JSON wszystko, co
 * ma getter — także pola wewnętrzne i leniwe relacje. Celowo zły przykład do sekcji 1–3 lekcji.
 */
@RestController
@RequestMapping("/api/rel06/entities")
public class EntityController {

    private final ShopService shopService;

    public EntityController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping("/products/{sku}")
    public Product product(@PathVariable String sku) {
        return shopService.productEntity(sku);
    }

    @GetMapping("/customers/{id}")
    public Customer customer(@PathVariable Long id) {
        return shopService.customerEntity(id);
    }
}
