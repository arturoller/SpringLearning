package s01_core_ioc.ioc03_stereotypes;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ShopComponent = WŁASNA adnotacja stereotypowa. Jest oznaczona {@code @Component}, więc każda klasa z
 * {@code @ShopComponent} jest skanowana i staje się beanem (ziarnem — obiektem zarządzanym przez Springa).
 * Tak samo zbudowane są {@code @Service}, {@code @Repository} i {@code @Controller}.
 */
@Target(ElementType.TYPE) // target = cel: adnotację można położyć na klasie
@Retention(RetentionPolicy.RUNTIME) // retention = zachowanie: adnotacja widoczna w czasie działania (Spring ją czyta)
@Documented
@Component
public @interface ShopComponent {
}
