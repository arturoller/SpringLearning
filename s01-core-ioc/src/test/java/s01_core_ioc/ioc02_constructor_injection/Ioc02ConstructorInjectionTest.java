package s01_core_ioc.ioc02_constructor_injection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testy lekcji Ioc02ConstructorInjection. {@code @SpringBootTest} startuje kontekst Ioc02Application; zepsute
 * konfiguracje (cykle, niejednoznaczne konstruktory) budujemy w {@code ApplicationContextRunner} (runner = „uruchamiacz”
 * małych kontekstów w testach), żeby aplikacja lekcji zawsze startowała.
 */
@SpringBootTest
class Ioc02ConstructorInjectionTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. Konstruktor: jedyny konstruktor wystarcza, pole jest final, a test działa przez zwykłe new")
    void constructorInjection() throws Exception {
        CustomerService fromContainer = context.getBean(CustomerService.class);
        assertThat(fromContainer.displayName(1)).isEqualTo("Anna Kowalska");

        assertThat(Modifier.isFinal(CustomerService.class.getDeclaredField("repository").getModifiers())).isTrue();

        // bez Springa: dubler jako lambda
        CustomerService byHand = new CustomerService(id -> Optional.of(new Customer(id, "Ola", "Test")));
        assertThat(byHand.displayName(7)).isEqualTo("Ola Test");
    }

    @Test
    @DisplayName("2. Pole @Autowired: w Springu działa, ale po zwykłym new pole jest null → NullPointerException")
    void fieldInjection() {
        assertThat(context.getBean(FieldInjectedCustomerService.class).displayName(2)).isEqualTo("Jan Nowak");

        assertThatThrownBy(() -> new FieldInjectedCustomerService().displayName(2))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("3. Setter: brak beana VipPolicy nie przeszkadza, a ręcznie podana zasada działa")
    void setterInjection() {
        SetterInjectedCustomerService fromContainer = context.getBean(SetterInjectedCustomerService.class);
        assertThat(context.getBeanNamesForType(VipPolicy.class)).isEmpty();
        assertThat(fromContainer.label(1)).isEqualTo("Anna Kowalska");

        SetterInjectedCustomerService byHand = new SetterInjectedCustomerService(
                new CustomerService(new InMemoryCustomerRepository()));
        byHand.setVipPolicy(id -> id == 1);
        assertThat(byHand.label(1)).isEqualTo("★ Anna Kowalska");
        assertThat(byHand.label(2)).isEqualTo("Jan Nowak");
    }

    @Test
    @DisplayName("4. Dwa konstruktory: z @Autowired działa, bez wskazania Spring nie wybierze")
    void twoConstructors() {
        new ApplicationContextRunner()
                .withBean(InMemoryCustomerRepository.class)
                .withBean(TwoConstructorsService.class)
                .run(ctx -> assertThat(ctx.getBean(TwoConstructorsService.class).describe(1))
                        .isEqualTo("Klient Kowalska"));

        new ApplicationContextRunner()
                .withBean(InMemoryCustomerRepository.class)
                .withBean(AmbiguousConstructorsService.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).isInstanceOf(BeanCreationException.class);
                    assertThat(rootCause(ctx.getStartupFailure())).isInstanceOf(NoSuchMethodException.class);
                });
    }

    @Test
    @DisplayName("5. Cykl przez konstruktory kończy się BeanCurrentlyInCreationException")
    void circularConstructors() {
        new ApplicationContextRunner()
                .withBean(CycleA.class)
                .withBean(CycleB.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(hasCause(ctx.getStartupFailure(), BeanCurrentlyInCreationException.class)).isTrue();
                });
    }

    @Test
    @DisplayName("5. Cykl przez pola: przechodzi tylko po jawnym zezwoleniu na cykle (domyślnie, jak w Bootcie, są zabronione)")
    void circularFields() {
        new ApplicationContextRunner()
                .withAllowCircularReferences(true)
                .withBean(FieldCycleA.class)
                .withBean(FieldCycleB.class)
                .run(ctx -> assertThat(ctx).hasNotFailed());

        new ApplicationContextRunner() // domyślnie cykle są zabronione
                .withBean(FieldCycleA.class)
                .withBean(FieldCycleB.class)
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(hasCause(ctx.getStartupFailure(), BeanCurrentlyInCreationException.class)).isTrue();
                });
    }

    private static Throwable rootCause(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }
}
