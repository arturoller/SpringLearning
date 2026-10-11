package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Controller;

import java.util.List;

/**
 * Kontroler (warstwa wejścia: przyjmuje żądania). {@code @Controller} jest w spring-context, więc to zwykły bean
 * także w aplikacji bez WWW; dopiero moduł webowy (s04_web_rest) nada mu znaczenie obsługi żądań HTTP.
 */
@Controller
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    public List<String> list() {
        return service.sortedProductNames();
    }
}
