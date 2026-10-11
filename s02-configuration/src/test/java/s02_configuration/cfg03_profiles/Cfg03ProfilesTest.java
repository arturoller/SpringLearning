package s02_configuration.cfg03_profiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testy lekcji Cfg03Profiles. Klasa testowa ma {@code @ActiveProfiles({"cfg03", "cfg03-dev"})} — to profile
 * „jak w IntelliJ”. Pozostałe scenariusze (produkcja, brak profilu, grupy) uruchamiają aplikację jak z wiersza poleceń:
 * {@code --spring.profiles.active=...}, żeby porównać je ze sobą w jednym teście.
 */
@SpringBootTest
@ActiveProfiles({"cfg03", "cfg03-dev"})
class Cfg03ProfilesTest {

    @Autowired
    PaymentGateway gateway;

    @Autowired
    GatewaySettings settings;

    @Autowired
    Environment environment;

    @Profile("a & b | c")
    static class MixedWithoutParentheses {
    }

    @Profile("(a & b) | c")
    static class MixedWithParentheses {
    }

    /** Startuje Cfg03Application (bez serwera WWW) z podanymi argumentami wiersza poleceń. */
    private static ConfigurableApplicationContext start(String... args) {
        return new SpringApplicationBuilder(Cfg03Application.class).web(WebApplicationType.NONE).run(args);
    }

    @Test
    @DisplayName("1. @Profile wybiera bean: dev i brak profilu = FakePaymentGateway, cfg03-prod = RealPaymentGateway")
    void profileSelectsBean() {
        // ta klasa testowa: cfg03 + cfg03-dev
        assertThat(gateway).isInstanceOf(FakePaymentGateway.class);
        assertThat(gateway.charge(new BigDecimal("10.00"))).isEqualTo("FAKE: przyjęto 10.00");

        try (ConfigurableApplicationContext prod = start("--spring.profiles.active=cfg03,cfg03-prod")) {
            assertThat(prod.getBean(PaymentGateway.class)).isInstanceOf(RealPaymentGateway.class);
            assertThat(prod.getBeansOfType(FakePaymentGateway.class)).isEmpty();
            assertThat(prod.getBean(PaymentGateway.class).charge(new BigDecimal("10.00")))
                    .isEqualTo("REAL: obciążono 10.00 przez https://pay.example.com/api");
        }
    }

    @Test
    @DisplayName("2. application-{profil}.properties nadpisuje plik bazowy; wygrywa profil wymieniony PÓŹNIEJ")
    void profileFilesOverrideBase() {
        // dev: adres z pliku dev, timeout z pliku bazowego
        assertThat(settings.url()).isEqualTo("http://localhost:9090/fake-pay");
        assertThat(settings.timeout()).isEqualTo(Duration.ofSeconds(10));

        try (ConfigurableApplicationContext prod = start("--spring.profiles.active=cfg03,cfg03-prod")) {
            GatewaySettings prodSettings = prod.getBean(GatewaySettings.class);
            assertThat(prodSettings.url()).isEqualTo("https://pay.example.com/api");
            assertThat(prodSettings.timeout()).isEqualTo(Duration.ofSeconds(3));
        }

        // PUŁAPKA: odwrócona kolejność — cfg03 (baza) wymieniony na końcu wygrywa z cfg03-prod
        try (ConfigurableApplicationContext reversed = start("--spring.profiles.active=cfg03-prod,cfg03")) {
            assertThat(reversed.getBean(GatewaySettings.class).url()).isEqualTo("http://localhost:8080/pay");
        }
    }

    @Test
    @DisplayName("3. Profil „default”: aktywny tylko gdy nie włączono żadnego innego profilu")
    void defaultProfile() {
        try (ConfigurableApplicationContext none = start()) {
            assertThat(none.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(none.getEnvironment().getDefaultProfiles()).containsExactly("default");
            assertThat(none.getBeansOfType(DefaultProfileMarker.class)).hasSize(1);
            assertThat(none.getBean(PaymentGateway.class)).isInstanceOf(FakePaymentGateway.class);   // !cfg03-prod
            assertThat(none.getBean(GatewaySettings.class).url()).isEqualTo("brak");   // plików cfg03* nikt nie wczytał
        }
        // włączony jakikolwiek profil (tu: ten test ma cfg03 i cfg03-dev) → „default” znika
        assertThat(environment.getActiveProfiles()).containsExactly("cfg03", "cfg03-dev");
        assertThat(environment.matchesProfiles("default")).isFalse();
    }

    @Test
    @DisplayName("4. Wyrażenia profilowe: AuditLog istnieje w cfg03-prod lub cfg03-audit, nie w dev")
    void profileExpressions() {
        assertThat(environment.matchesProfiles("cfg03-prod | cfg03-audit")).isFalse();   // ta klasa: dev

        try (ConfigurableApplicationContext prod = start("--spring.profiles.active=cfg03,cfg03-prod")) {
            assertThat(prod.getBeansOfType(AuditLog.class)).hasSize(1);
        }
        try (ConfigurableApplicationContext audit = start("--spring.profiles.active=cfg03,cfg03-audit")) {
            assertThat(audit.getBeansOfType(AuditLog.class)).hasSize(1);
            assertThat(audit.getBean(PaymentGateway.class)).isInstanceOf(FakePaymentGateway.class);
        }
        try (ConfigurableApplicationContext dev = start("--spring.profiles.active=cfg03,cfg03-dev")) {
            assertThat(dev.getBeansOfType(AuditLog.class)).isEmpty();
        }
        // PUŁAPKA: mieszanie & i | bez nawiasów jest odrzucane (z nawiasami jest dobrze)
        new ApplicationContextRunner().withUserConfiguration(MixedWithoutParentheses.class)
                .run(context -> assertThat(context).hasFailed());
        new ApplicationContextRunner().withUserConfiguration(MixedWithParentheses.class)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    @DisplayName("5. spring.profiles.active i grupy profili: jeden profil włącza kilka innych")
    void profileGroups() {
        try (ConfigurableApplicationContext full = start(
                "--spring.profiles.group.cfg03-full=cfg03-dev,cfg03-audit",
                "--spring.profiles.active=cfg03,cfg03-full")) {
            assertThat(full.getEnvironment().getActiveProfiles())
                    .contains("cfg03", "cfg03-full", "cfg03-dev", "cfg03-audit");
            assertThat(full.getBeansOfType(AuditLog.class)).hasSize(1);
            assertThat(full.getBean(GatewaySettings.class).url()).isEqualTo("http://localhost:9090/fake-pay");
        }
    }
}
