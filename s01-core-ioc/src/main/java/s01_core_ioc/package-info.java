/**
 * Dział s01_core_ioc — kontener IoC (Inversion of Control — odwrócenie sterowania): beany, wstrzykiwanie zależności,
 * stereotypy, metody {@code @Bean}, wiele implementacji jednego interfejsu, zasięgi i cykl życia beana.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code ioc01_container} — Ioc01Container: czym jest kontener, PRZED/PO, getBean.</li>
 *   <li>{@code ioc02_constructor_injection} — Ioc02ConstructorInjection: konstruktor, pole, setter; cykl zależności.</li>
 *   <li>{@code ioc03_stereotypes} — Ioc03Stereotypes: {@code @Component}, {@code @Service}, {@code @Repository}, nazwy beanów.</li>
 *   <li>{@code ioc04_bean_methods} — Ioc04BeanMethods: {@code @Configuration} i {@code @Bean}, bean Clock.</li>
 *   <li>{@code ioc05_multiple_candidates} — Ioc05MultipleCandidates: {@code @Primary}, {@code @Qualifier}, listy beanów.</li>
 *   <li>{@code ioc06_scopes} — Ioc06Scopes: singleton i prototype, ObjectProvider.</li>
 *   <li>{@code ioc07_lifecycle} — Ioc07Lifecycle: {@code @PostConstruct}, {@code @PreDestroy}, kolejność zdarzeń.</li>
 * </ol>
 *
 * <p>Moduł ma tylko spring-boot-starter — bez serwera WWW. Kontener IoC działa tak samo w każdej aplikacji Springa.</p>
 *
 * <p>Uwaga o klasach „demo” i ćwiczeniach-konfiguracjach: klasy BEZ adnotacji {@code @Component}/{@code @Configuration}
 * nie są skanowane. Testy rejestrują je ręcznie (ApplicationContextRunner), dzięki czemu przykłady błędów nie psują
 * aplikacji lekcji.</p>
 */
package s01_core_ioc;
