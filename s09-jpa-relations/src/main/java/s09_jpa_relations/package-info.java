/**
 * Dział s09_jpa_relations — RELACJE MIĘDZY ENCJAMI JPA (Hibernate 7) od środka: jak relacje zapisują się w tabelach,
 * która strona relacji decyduje o kluczu obcym, kiedy Hibernate wysyła zapytania i jak nie dać się zaskoczyć
 * leniwemu ładowaniu. Wszystko mierzone: SQL z logu i liczba zapytań ze statystyk Hibernate.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code rel01_many_to_one} — Rel01ManyToOne: {@code @ManyToOne} i {@code @JoinColumn}, strona właścicielska,
 *       EAGER kontra LAZY, proxy, relacja wymagana, encja niezapisana (transient), {@code getReferenceById}.</li>
 *   <li>{@code rel02_one_to_many} — Rel02OneToMany: {@code @OneToMany(mappedBy)}, relacja dwukierunkowa, metody
 *       pomocnicze, kaskady, {@code orphanRemoval}, List kontra Set.</li>
 *   <li>{@code rel03_many_to_many} — Rel03ManyToMany: {@code @ManyToMany} i tabela łącząca, Set zamiast List
 *       (zmierzone), usuwanie ze strony odwrotnej, kaskada REMOVE jako pułapka, encja pośrednia z własnymi
 *       polami.</li>
 *   <li>{@code rel04_lazy_loading} — Rel04LazyLoading: proxy i leniwe kolekcje od środka,
 *       {@code LazyInitializationException}, jawne dociąganie (join fetch, {@code @EntityGraph}),
 *       {@code spring.jpa.open-in-view} (lekcja webowa).</li>
 *   <li>{@code rel05_n_plus_one} — Rel05NPlusOne: problem N+1 zmierzony statystykami Hibernate i cztery
 *       rozwiązania; stronicowanie z join fetch, {@code MultipleBagFetchException}.</li>
 *   <li>{@code rel06_entity_to_dto} — Rel06EntityToDto: dlaczego kontroler nie zwraca encji (wycieki, leniwe pola,
 *       cykle w JSON) i jak mapować na DTO w serwisie (lekcja webowa).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, własną bazę H2 ({@code jdbc:h2:mem:relNN}), własny profil
 * ({@code application-relNN.properties}) i własne migracje Flyway ({@code db/migration/relNN}); Hibernate działa
 * z {@code ddl-auto=validate} — schemat tworzy wyłącznie Flyway. Moduł ma Spring MVC (dla Rel04 i Rel06), więc
 * pozostałe lekcje wyłączają serwer WWW ustawieniem {@code spring.main.web-application-type=none}. Rozwiązania
 * ćwiczeń leżą w pakiecie {@code solutions}, obok lekcji.</p>
 */
package s09_jpa_relations;
