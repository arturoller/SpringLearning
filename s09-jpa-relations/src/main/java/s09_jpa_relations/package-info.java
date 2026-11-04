/**
 * Dział s09_jpa_relations — RELACJE MIĘDZY ENCJAMI (relationship = relacja, powiązanie): jak w JPA zapisać, że
 * zamówienie należy do klienta, ma wiele pozycji, a produkt ma wiele tagów — i jak nie zabić wydajności aplikacji.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code rel01_many_to_one} — Rel01ManyToOne: {@code @ManyToOne} + {@code @JoinColumn} (klucz obcy).</li>
 *   <li>{@code rel02_one_to_many} — Rel02OneToMany: {@code @OneToMany(mappedBy)}, relacja dwukierunkowa, strona
 *       właściciela, kaskady i {@code orphanRemoval}.</li>
 *   <li>{@code rel03_many_to_many} — Rel03ManyToMany: {@code @ManyToMany}, tabela łącząca, encja pośrednia.</li>
 *   <li>{@code rel04_lazy_loading} — Rel04LazyLoading: LAZY kontra EAGER, proxy, {@code LazyInitializationException}.</li>
 *   <li>{@code rel05_n_plus_one} — Rel05NPlusOne: problem N+1 i rozwiązania ({@code JOIN FETCH},
 *       {@code @EntityGraph}, batch fetching, projekcje).</li>
 *   <li>{@code rel06_entity_to_dto} — Rel06EntityToDto: dlaczego nie zwracamy encji na zewnątrz, mapowanie encja ↔ DTO
 *       (rekordy, ręcznie).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Klasa startowa lekcji skanuje
 * encje i repozytoria tylko ze swojego podpakietu, każda lekcja ma własną bazę H2 ({@code jdbc:h2:mem:relNN})
 * i własne migracje Flyway.</p>
 */
package s09_jpa_relations;
