package s01_core_ioc.ioc03_stereotypes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.beans.Introspector;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Testy lekcji Ioc03Stereotypes — {@code @SpringBootTest} startuje kontekst Ioc03Application (skanowanie pakietu). */
@SpringBootTest
class Ioc03StereotypesTest {

    @Autowired
    ApplicationContext context;

    @Test
    @DisplayName("1. @Service, @Repository i @Controller są oznaczone @Component (metaadnotacja)")
    void stereotypesAreComponents() {
        assertThat(AnnotatedElementUtils.hasAnnotation(Service.class, Component.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(Repository.class, Component.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(Controller.class, Component.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(CatalogService.class, Component.class)).isTrue();
        assertThat(AnnotatedElementUtils.hasAnnotation(NotAComponent.class, Component.class)).isFalse();
    }

    @Test
    @DisplayName("2. Warstwy: kontroler → serwis → repozytorium są beanami i współpracują")
    void layers() {
        assertThat(context.getBean(CatalogController.class).list())
                .containsExactly("Czekolada", "Herbata", "Kawa");
        assertThat(context.getBean(CatalogRepository.class)).isNotNull();
        assertThat(context.getBean(PriceFormatter.class).format(2999)).isEqualTo("29,99 zł");
    }

    @Test
    @DisplayName("3. Domyślna nazwa: mała pierwsza litera, ale przy dwóch wielkich na początku bez zmian")
    void defaultNames() {
        assertThat(context.getBeanNamesForType(CatalogService.class)).containsExactly("catalogService");
        assertThat(context.getBeanNamesForType(URLShortener.class)).containsExactly("URLShortener");
        assertThat(Introspector.decapitalize("URLShortener")).isEqualTo("URLShortener");

        assertThatThrownBy(() -> context.getBean("uRLShortener"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> context.getBean("catalogservice"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("4. Jawna nazwa zastępuje domyślną")
    void explicitName() {
        assertThat(context.getBean("taxCalculator", VatCalculator.class).gross(10000)).isEqualTo(12300);
        assertThatThrownBy(() -> context.getBean("vatCalculator"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    @DisplayName("5. Własny stereotyp @ShopComponent działa jak @Component; klasa bez adnotacji nie jest beanem")
    void customStereotype() {
        assertThat(AnnotatedElementUtils.hasAnnotation(ShopComponent.class, Component.class)).isTrue();
        assertThat(context.getBean("loyaltyDiscount", LoyaltyDiscount.class).percentFor(25)).isEqualTo(10);
        assertThatThrownBy(() -> context.getBean(NotAComponent.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThat(List.of(context.getBeanNamesForAnnotation(Service.class)))
                .containsExactlyInAnyOrder("catalogService", "shippingService");
    }
}
