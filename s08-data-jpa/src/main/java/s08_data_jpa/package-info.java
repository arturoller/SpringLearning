/**
 * Dział s08_data_jpa — SPRING DATA JPA (JPA — Jakarta Persistence API, standard mapowania obiektów Javy na tabele;
 * Hibernate — jego implementacja; Spring Data — warstwa, która pisze za Ciebie typowe repozytoria).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code jpa01_entities} — Jpa01Entities: encja ({@code @Entity}, {@code @Id}, {@code @GeneratedValue},
 *       {@code @Column}), {@code EntityManager}, kontekst utrwalania (persistence context) i stany encji.</li>
 *   <li>{@code jpa02_repositories} — Jpa02Repositories: {@code JpaRepository} — CRUD bez implementacji,
 *       {@code save} (persist kontra merge), {@code findById} i {@code Optional}.</li>
 *   <li>{@code jpa03_query_methods} — Jpa03QueryMethods: zapytania z nazw metod ({@code findByCategoryAndPriceLessThan}…).</li>
 *   <li>{@code jpa04_custom_queries} — Jpa04CustomQueries: {@code @Query} (JPQL i SQL natywny), projekcje,
 *       {@code @Modifying}.</li>
 *   <li>{@code jpa05_paging_sorting} — Jpa05PagingSorting: {@code Pageable}, {@code Page}, {@code Slice}, {@code Sort}.</li>
 *   <li>{@code jpa06_flyway_with_jpa} — Jpa06FlywayWithJpa: Flyway tworzy schemat, Hibernate go tylko sprawdza
 *       ({@code ddl-auto=validate}); dlaczego nie {@code update} na produkcji.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Klasa startowa lekcji skanuje
 * encje i repozytoria tylko ze swojego podpakietu, każda lekcja ma własną bazę H2 ({@code jdbc:h2:mem:jpaNN}).</p>
 */
package s08_data_jpa;
