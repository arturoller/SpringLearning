package s18_aop.aop03_custom_annotations;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy ĆWICZEŃ lekcji Aop03CustomAnnotations. {@code @Tag("cwiczenie")} — uruchamiasz sam (▶ obok klasy).
 * Bez kontekstu Springa: AspectJProxyFactory zakłada aspekt ćwiczenia na zwykły obiekt.
 */
@Tag("cwiczenie")
class Aop03CustomAnnotationsExercisesTest {

    static <T> T proxied(T target, Object aspect) {
        AspectJProxyFactory factory = new AspectJProxyFactory(target);
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    @Test
    @DisplayName("Ćwiczenie 1: @RequiresRole wpuszcza tylko użytkownika z wymaganą rolą")
    void exercise1() {
        CurrentUser user = new CurrentUser("KLIENT");
        DiscountAdmin admin = proxied(new DiscountAdmin(), new Aop03Exercise1RoleAspect(user));

        assertThat(admin.showDiscounts()).startsWith("Rabaty:");
        assertThatThrownBy(() -> admin.grantDiscount("Jan Kowalski", 10))
                .isInstanceOf(PermissionDeniedException.class)
                .hasMessage("Brak uprawnień: wymagana rola KIEROWNIK, a użytkownik ma rolę KLIENT");

        user.setRole("KIEROWNIK");
        assertThat(admin.grantDiscount("Jan Kowalski", 10)).isEqualTo("Rabat 10% dla Jan Kowalski");
    }

    @Test
    @DisplayName("Ćwiczenie 2: aspekt zapisuje wynik wywołania i przepuszcza ten sam wyjątek")
    void exercise2() {
        List<String> log = new ArrayList<>();
        OrderService orders = proxied(new OrderService(new AuditJournal()), new Aop03Exercise2ErrorLogAspect(log));

        assertThat(orders.placeOrder("SPO-001", 2)).isEqualTo("Zamówienie przyjęte: 2 x SPO-001");
        assertThatThrownBy(() -> orders.placeOrder("ELE-002", 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Brak towaru: ELE-002 (stan 0, zamówiono 1)")
                .hasNoCause();

        assertThat(log).containsExactly("OK placeOrder", "BŁĄD placeOrder: IllegalStateException");
    }

    @Test
    @DisplayName("Ćwiczenie 3: @RetryOnFailure ponawia tylko chwilowe awarie i najwyżej attempts razy")
    void exercise3() {
        FlakyWarehouseClient twoFailures = proxied(new FlakyWarehouseClient(2), new Aop03Exercise3RetryAspect());
        assertThat(twoFailures.fetchStock("SPO-001")).isEqualTo(120);
        assertThat(twoFailures.attempts()).as("2 awarie + 1 sukces").isEqualTo(3);

        FlakyWarehouseClient fiveFailures = proxied(new FlakyWarehouseClient(5), new Aop03Exercise3RetryAspect());
        assertThatThrownBy(() -> fiveFailures.fetchStock("SPO-001"))
                .isInstanceOf(TemporaryFailureException.class)
                .hasMessage("Magazyn chwilowo niedostępny (próba 3)");
        assertThat(fiveFailures.attempts()).as("nie więcej niż attempts = 3").isEqualTo(3);

        FlakyWarehouseClient unknown = proxied(new FlakyWarehouseClient(0), new Aop03Exercise3RetryAspect());
        assertThatThrownBy(() -> unknown.fetchStockOfUnknown("XYZ-999")).isInstanceOf(IllegalArgumentException.class);
        assertThat(unknown.attempts()).as("innych wyjątków nie ponawiamy").isEqualTo(1);
    }
}
