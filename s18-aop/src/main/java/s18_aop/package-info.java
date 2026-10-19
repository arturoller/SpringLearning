/**
 * Dział s18_aop — PROGRAMOWANIE ASPEKTOWE (AOP — aspect-oriented programming = programowanie zorientowane na aspekty):
 * jak dołożyć do wielu metod to samo zachowanie (logowanie, pomiar czasu, sprawdzenie uprawnień, audyt) bez
 * dopisywania go w każdej metodzie — i jak to naprawdę działa pod spodem (proxy, czyli obiekt-pośrednik).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code aop01_aspects} — Aop01Aspects: {@code @Aspect} + {@code @Component}, pięć rodzajów porad
 *       ({@code @Before}, {@code @AfterReturning}, {@code @AfterThrowing}, {@code @After}, {@code @Around}),
 *       wyrażenia punktów przecięcia ({@code execution}, {@code within}, {@code @annotation}, {@code bean}, {@code args})
 *       i {@code @Pointcut} wielokrotnego użytku.</li>
 *   <li>{@code aop02_proxies} — Aop02Proxies: proxy JDK (interfejs) i CGLIB (podklasa), co ustawia Spring Boot
 *       ({@code spring.aop.proxy-target-class}), {@code @Proxyable} ze Spring Framework 7, wywołanie z tej samej klasy,
 *       metody {@code final}/prywatne, rzutowanie na klasę przy proxy JDK i obejścia.</li>
 *   <li>{@code aop03_custom_annotations} — Aop03CustomAnnotations: własne adnotacje ({@code @AuditLog},
 *       {@code @MeasureTime}) z atrybutami, kolejność kilku aspektów ({@code @Order}) i względem transakcji,
 *       aspekt połykający wyjątki, kiedy aspekt to przesada.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, więc aspekty jednej lekcji nie „łapią” beanów innej. Aspekty z ćwiczeń NIE mają
 * {@code @Component}: testy ćwiczeń zakładają je ręcznie (przez {@code AspectJProxyFactory} albo
 * {@code ApplicationContextRunner}), dzięki czemu nierozwiązane ćwiczenie nie psuje testów lekcji.
 * Moduł ma tylko starter {@code spring-boot-starter-aspectj} (w Boot 4 zastąpił dawny {@code spring-boot-starter-aop}).</p>
 */
package s18_aop;
