package s15_actuator_logging.obs04_logging;

import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.core.env.StandardEnvironment;

/**
 * Pomocnik testów logowania. PO CO: Boot konfiguruje system logowania (Logback) tylko RAZ w całym JVM-ie — kolejne
 * konteksty testowe widzą już gotowy układ appenderów i NIE stosują ponownie wzorca ani formatu strukturalnego
 * (poziomy {@code logging.level.*} stosują się za każdym razem). Bez tego pomocnika wynik testów zależałby od tego,
 * który test wystartował pierwszy. {@link #reset()} przywraca stan „jak przed startem aplikacji”: następny kontekst
 * konfiguruje logowanie od nowa, ze swoimi właściwościami.
 */
final class LoggingTestSupport {

    private LoggingTestSupport() {
    }

    static void reset() {
        // Boot zapisuje ustawienia logowania (wzorzec, format JSON, plik) jako WŁAŚCIWOŚCI SYSTEMOWE JVM-a i nigdy ich
        // nie czyści — bez tego format ECS z jednego testu „przeciekałby” do następnych.
        for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
            System.clearProperty(property.getEnvironmentVariableName());
        }
        LoggingSystem system = LoggingSystem.get(LoggingTestSupport.class.getClassLoader());
        system.cleanUp();           // cleanUp = sprzątanie: oznacza logowanie jako niezainicjowane
        system.beforeInitialize();  // przygotowanie systemu do ponownej inicjalizacji
        // domyślna konfiguracja Boota: konsola, wzorzec domyślny, poziom INFO dla wszystkich rejestratorów
        system.initialize(new LoggingInitializationContext(new StandardEnvironment()), null, null);
    }
}
