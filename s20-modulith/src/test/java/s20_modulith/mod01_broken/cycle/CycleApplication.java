package s20_modulith.mod01_broken.cycle;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CELOWO ZŁA aplikacja do lekcji Mod01Modules (tylko do analizy {@code ApplicationModules}). Błąd: cykl między
 * modułami — {@code orders} zależy od {@code products}, a {@code products} od {@code orders}.
 */
@SpringBootApplication
public class CycleApplication {
}
