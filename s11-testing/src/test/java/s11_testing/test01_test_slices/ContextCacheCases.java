package s11_testing.test01_test_slices;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Pomocnicze „klasy testowe” BEZ metod testowych (JUnit ich nie uruchamia) — różnią się konfiguracją kontekstu,
 * dokładnie tak, jak różniłyby się prawdziwe klasy testowe. Test01ContextCacheTest ładuje ich konteksty ręcznie
 * i porównuje: ten sam obiekt czy nowy.
 */
final class ContextCacheCases {

    private ContextCacheCases() {
    }

    @SpringBootTest
    @ActiveProfiles("test01")
    static class SameConfigA {
    }

    @SpringBootTest
    @ActiveProfiles("test01")
    static class SameConfigB {
    }

    @SpringBootTest(properties = "demo.flag=true")   // inna właściwość = inny klucz w pamięci podręcznej
    @ActiveProfiles("test01")
    static class OtherProperty {
    }

    @SpringBootTest
    @ActiveProfiles("test01")
    static class WithMockA {
        @MockitoBean
        ProductService service;
    }

    @SpringBootTest
    @ActiveProfiles("test01")
    static class WithMockB {
        @MockitoBean
        ProductService service;
    }

    @SpringBootTest
    @ActiveProfiles("test01")
    static class WithOtherMock {
        @MockitoBean
        ProductRepository repository;   // atrapa INNEGO beana = inny zestaw atrap = inny kontekst
    }

    @SpringBootTest(properties = "demo.dirty=true")   // osobna konfiguracja, żeby unieważnienie nie zepsuło innych
    @ActiveProfiles("test01")
    static class ForDirtying {
    }
}
