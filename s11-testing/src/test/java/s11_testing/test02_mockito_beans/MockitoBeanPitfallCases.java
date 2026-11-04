package s11_testing.test02_mockito_beans;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Pomocnicze „klasy testowe” BEZ metod testowych — JUnit ich nie uruchamia; Test02PitfallsTest ładuje ich konteksty
 * ręcznie, żeby pokazać, co się dzieje, gdy atrapa dotyczy beana, którego w aplikacji NIE MA.
 */
final class MockitoBeanPitfallCases {

    private MockitoBeanPitfallCases() {
    }

    /** Interfejs bez żadnej implementacji i bez @Component — w kontekście aplikacji go nie ma. */
    interface MissingPort {
        String ping();
    }

    @SpringBootTest
    static class MockOfMissingBean {
        @MockitoBean
        MissingPort missing;
    }

    @SpringBootTest
    static class StrictMockOfMissingBean {
        @MockitoBean(enforceOverride = true)   // enforce = wymuś: ma ZASTĄPIĆ istniejący bean
        MissingPort missing;
    }

    @SpringBootTest
    static class SpyOfMissingBean {
        @MockitoSpyBean
        MissingPort missing;
    }
}
