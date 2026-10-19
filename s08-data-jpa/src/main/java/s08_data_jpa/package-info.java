/**
 * Dział s08_data_jpa — SPRING DATA JPA: zamiast pisać SQL ręcznie (s07_data_jdbc) opisujesz klasy adnotacjami,
 * a Hibernate (implementacja JPA — Jakarta Persistence API) sam układa zapytania; Spring Data dokłada repozytoria,
 * które piszesz jako interfejsy bez implementacji. Bez warstwy webowej — wszystko sprawdzają testy.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code jpa01_entities} — Jpa01Entities: {@code @Entity}, {@code @Id}, {@code @GeneratedValue}
 *       (sekwencja kontra IDENTITY), {@code @Column}, wymagania encji, ddl-auto, podgląd SQL, kontekst utrwalania,
 *       equals/hashCode encji.</li>
 *   <li>{@code jpa02_repositories} — Jpa02Repositories: {@code JpaRepository}, save (nowa kontra istniejąca encja),
 *       findById, deleteById, {@code @DataJpaTest} i {@code TestEntityManager}.</li>
 *   <li>{@code jpa03_query_methods} — Jpa03QueryMethods: zapytania z nazw metod (findBy…, OrderBy, Top, exists, count)
 *       i błąd startu przy literówce.</li>
 *   <li>{@code jpa04_custom_queries} — Jpa04CustomQueries: {@code @Query} (JPQL i natywne SQL), projekcje
 *       (interfejs, rekord DTO), {@code @Modifying} i nieaktualne encje w kontekście.</li>
 *   <li>{@code jpa05_paging_sorting} — Jpa05PagingSorting: {@code Pageable}, {@code Sort}, {@code Page} kontra
 *       {@code Slice}, stała kolejność przy stronicowaniu.</li>
 *   <li>{@code jpa06_flyway_with_jpa} — Jpa06FlywayWithJpa: schemat z migracji Flyway + ddl-auto=validate,
 *       błąd startu przy niezgodności encji i tabeli, dlaczego nie „update” na produkcji.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie — Hibernate skanuje encje i Spring Data repozytoria tylko w pakiecie tej klasy,
 * więc lekcje nie widzą nawzajem swoich tabel. Każda lekcja ma też własny profil
 * ({@code application-jpaNN.properties}): własną bazę H2 ({@code jdbc:h2:mem:jpaNN}) i albo wyłączony Flyway,
 * albo własny katalog migracji ({@code db/migration/jpaNN}). Rozwiązania ćwiczeń leżą w {@code solutions.*}
 * (rodzeństwo lekcji) z własnymi klasami startowymi.</p>
 */
package s08_data_jpa;
