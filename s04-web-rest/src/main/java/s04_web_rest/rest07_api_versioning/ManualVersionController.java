package s04_web_rest.rest07_api_versioning;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * ManualVersionController = wersjonowanie „po staremu”: wersja jest częścią adresu (/api/v1/..., /api/v2/...),
 * a każda wersja to osobne, zwykłe mapowanie bez atrybutu version. Tak robi większość API sprzed Spring Framework 7
 * i tak spotkasz to w tutorialach. Porównanie z wersjonowaniem wbudowanym — sekcja 1 lekcji.
 */
@RestController
public class ManualVersionController {

    @GetMapping("/api/v1/catalog/{sku}")
    public String catalogV1(@PathVariable String sku) {
        return "v1: " + sku;
    }

    @GetMapping("/api/v2/catalog/{sku}")
    public String catalogV2(@PathVariable String sku) {
        return "v2: " + sku + " (z cenami netto i brutto)";
    }
}
