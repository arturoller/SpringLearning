package s03_boot_basics.boot03_spring_application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * ShopExitCode = kod wyjścia aplikacji sklepu. ExitCodeGenerator (generator kodu wyjścia) to bean, o który pyta
 * SpringApplication.exit(...): pierwszy niezerowy kod wygrywa. Kod pochodzi z właściwości {@code boot03.exit-code}
 * (domyślnie 0 = sukces) — np. zadanie nocne może zwrócić 3, gdy część danych się nie zaimportowała.
 */
@Component
public class ShopExitCode implements ExitCodeGenerator {

    private final int exitCode;

    public ShopExitCode(@Value("${boot03.exit-code:0}") int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
