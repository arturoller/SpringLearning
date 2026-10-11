/**
 * Dział s08_data_jpa — JPA i Spring Data JPA: encje, repozytoria, zapytania, stronicowanie i Flyway z Hibernate.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code jpa01_entities} — Jpa01Entities: ORM, encja ({@code @Entity}), klucz główny i jego generowanie,
 *       kolumny, enumy, equals/hashCode encji, kontekst utrwalania i automatyczne wykrywanie zmian (dirty checking).</li>
 *   <li>{@code jpa02_repositories} — Jpa02Repositories: {@code JpaRepository} i jego rodzina interfejsów, CRUD,
 *       {@code save} (persist czy merge?), {@code findById}, {@code deleteById}, {@code @DataJpaTest}.</li>
 *   <li>{@code jpa03_query_methods} — Jpa03QueryMethods: zapytania wyprowadzane z nazw metod
 *       ({@code findByNameContainingIgnoreCase}, {@code countBy}, {@code existsBy}, {@code findTop3By...}).</li>
 *   <li>{@code jpa04_custom_queries} — Jpa04CustomQueries: {@code @Query} (JPQL i natywny SQL), {@code @Modifying},
 *       projekcje (interfejsy, rekordy, {@code select new}).</li>
 *   <li>{@code jpa05_paging_sorting} — Jpa05PagingSorting: {@code Pageable}, {@code Page} kontra {@code Slice},
 *       sortowanie po kilku polach i stabilna kolejność.</li>
 *   <li>{@code jpa06_flyway_with_jpa} — Jpa06FlywayWithJpa: schemat z migracji Flyway, Hibernate tylko sprawdza
 *       ({@code ddl-auto=validate}), co się dzieje przy niezgodności encji z tabelą.</li>
 * </ol>
 *
 * <p>Jak lekcje są od siebie odizolowane w JEDNYM module? Każda lekcja ma własną klasę {@code @SpringBootApplication}
 * w swoim podpakiecie. Spring Boot szuka encji ({@code @Entity}) i repozytoriów tylko w pakiecie klasy startowej
 * (i jego podpakietach), więc lekcja widzi wyłącznie swoje encje. Ustawienia lekcji są w jej profilu
 * {@code application-jpaNN.properties} (osobna baza H2 {@code jdbc:h2:mem:jpaNN}), a wspólne
 * {@code application.properties} tylko wyłącza Flyway.</p>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication} — skanowałaby wszystkie lekcje naraz.</p>
 */
package s08_data_jpa;
