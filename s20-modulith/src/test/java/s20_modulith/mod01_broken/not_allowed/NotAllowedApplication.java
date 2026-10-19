package s20_modulith.mod01_broken.not_allowed;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CELOWO ZŁA aplikacja do lekcji Mod01Modules (tylko do analizy {@code ApplicationModules}). Błąd: moduł
 * {@code orders} deklaruje {@code allowedDependencies = "products"}, a mimo to używa modułu {@code customers}.
 */
@SpringBootApplication
public class NotAllowedApplication {
}
