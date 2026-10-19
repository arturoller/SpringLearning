package s20_modulith.mod01_broken.internal_access;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CELOWO ZŁA aplikacja do lekcji Mod01Modules (leży w testach, nigdy się nie uruchamia — służy tylko do analizy
 * {@code ApplicationModules}). Błąd: moduł {@code orders} sięga do {@code products.internal} — do środka innego
 * modułu, zamiast przez jego API.
 */
@SpringBootApplication
public class InternalAccessApplication {
}
