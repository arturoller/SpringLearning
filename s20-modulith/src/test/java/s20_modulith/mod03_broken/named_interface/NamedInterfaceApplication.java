package s20_modulith.mod03_broken.named_interface;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CELOWO ZŁA aplikacja do lekcji Mod03Documentation (tylko do analizy {@code ApplicationModules}). Błąd: moduł
 * {@code inventory} może używać z modułu {@code orders} tylko nazwanego interfejsu {@code events}, a sięga też po
 * {@code OrderManagement} z interfejsu nienazwanego (pakietu bazowego).
 */
@SpringBootApplication
public class NamedInterfaceApplication {
}
