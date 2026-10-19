package s20_modulith.mod01_broken.field_injection;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplikacja do lekcji Mod01Modules (tylko do analizy {@code ApplicationModules}): moduł {@code orders} wstrzykuje
 * API modułu {@code products} przez POLE ({@code @Autowired} na polu), a nie przez konstruktor.
 */
@SpringBootApplication
public class FieldInjectionApplication {
}
