package s18_aop.solutions.aop03_custom_annotations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import s18_aop.aop03_custom_annotations.AuditJournal;
import s18_aop.aop03_custom_annotations.CurrentUser;
import s18_aop.aop03_custom_annotations.DiscountAdmin;
import s18_aop.aop03_custom_annotations.FlakyWarehouseClient;
import s18_aop.aop03_custom_annotations.OrderService;
import s18_aop.aop03_custom_annotations.PermissionDeniedException;
import s18_aop.aop03_custom_annotations.TemporaryFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sprawdzenia z Aop03CustomAnnotationsExercisesTest na rozwiązaniach wzorcowych. {@code @Tag("wzorzec")} — biegną zawsze. */
@Tag("wzorzec")
class Aop03CustomAnnotationsSolutionsTest {

    static <T> T proxied(T target, Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    @DisplayName("Wzorzec 1: @RequiresRole wpuszcza tylko użytkownika z wymaganą rolą")
    void solution1() {
        CurrentUser user = new CurrentUser("KLIENT");
        DiscountAdmin admin = proxied(new DiscountAdmin(), new Aop03Exercise1RoleAspectSolution(user));

        assertThat(admin.showDiscounts()).startsWith("Rabaty:");
        assertThatThrownBy(() -> admin.grantDiscount("Jan Kowalski", 10))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessage("Brak uprawnień: wymagana rola KIEROWNIK, a użytkownik ma rolę KLIENT");

        user.setRole("KIEROWNIK");
        assertThat(admin.grantDiscount("Jan Kowalski", 10)).isEqualTo("Rabat 10% dla Jan Kowalski");
    }

    @Test
    @DisplayName("Wzorzec 2: aspekt zapisuje wynik wywołania i przepuszcza ten sam wyjątek")
    void solution2() {
        List<String> log = new ArrayList<>();
        OrderService orders = proxied(new OrderService(new AuditJournal()), new Aop03Exercise2ErrorLogAspectSolution(log));

        assertThat(orders.placeOrder("SPO-001", 2)).isEqualTo("Zamówienie przyjęte: 2 x SPO-001");
        assertThatThrownBy(() -> orders.placeOrder("ELE-002", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Brak towaru: ELE-002 (stan 0, zamówiono 1)")
                .hasNoCause();

        assertThat(log).containsExactly("OK placeOrder", "BŁĄD placeOrder: IllegalStateException");
    }

    @Test
    @DisplayName("Wzorzec 3: @RetryOnFailure ponawia tylko chwilowe awarie i najwyżej attempts razy")
    void solution3() {
        FlakyWarehouseClient twoFailures = proxied(new FlakyWarehouseClient(2), new Aop03Exercise3RetryAspectSolution());
        assertThat(twoFailures.fetchStock("SPO-001")).isEqualTo(120);
        assertThat(twoFailures.attempts()).isEqualTo(3);

        FlakyWarehouseClient fiveFailures = proxied(new FlakyWarehouseClient(5), new Aop03Exercise3RetryAspectSolution());
        assertThatThrownBy(() -> fiveFailures.fetchStock("SPO-001"))
                .isInstanceOf(TemporaryFailureException.class)
                .hasMessage("Magazyn chwilowo niedostępny (próba 3)");
        assertThat(fiveFailures.attempts()).isEqualTo(3);

        FlakyWarehouseClient unknown = proxied(new FlakyWarehouseClient(0), new Aop03Exercise3RetryAspectSolution());
        assertThatThrownBy(() -> unknown.fetchStockOfUnknown("XYZ-999")).isInstanceOf(IllegalArgumentException.class);
        assertThat(unknown.attempts()).isEqualTo(1);
    }
}
