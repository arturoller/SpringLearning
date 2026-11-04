package s12_security.sec06_security_testing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Panel pracownika "w przeglądarce" lekcji Sec06SecurityTesting ({@code /sec06/panel/**}, łańcuch {@code panelChain}:
 * sesja i ochrona CSRF — Sec04StatelessApi). Do testów z {@code with(csrf())}.
 */
@RestController
@RequestMapping("/sec06/panel")
public class PanelController {

    @GetMapping
    public String panel(Principal principal) {
        return "Panel pracownika: " + principal.getName();
    }

    @PostMapping("/price")
    public String changePrice(@RequestParam String product, @RequestParam String price) {
        return "Nowa cena " + product + ": " + price + " zł";
    }
}
