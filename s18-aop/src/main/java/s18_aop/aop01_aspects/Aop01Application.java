package s18_aop.aop01_aspects;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Klasa startowa lekcji Aop01Aspects. Aplikacja bez serwera WWW: zbuduje kontekst i zakończy się.
 * Skanuje tylko ten pakiet (aop01_aspects). Spring Boot sam włącza obsługę aspektów (autokonfiguracja
 * AopAutoConfiguration — szczegóły w lekcji, sekcja 2), więc NIE dopisujemy tu {@code @EnableAspectJAutoProxy}.
 * Aspekty z ćwiczeń (Aop01Exercise…Aspect) i BroadDeprecatedAspect nie mają {@code @Component}, więc nie są skanowane.
 */
@SpringBootApplication
public class Aop01Application {

    public static void main(String[] args) {
        SpringApplication.run(Aop01Application.class, args);
    }
}
