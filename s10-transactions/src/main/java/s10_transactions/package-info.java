/**
 * Dział s10_transactions — TRANSAKCJE w Springu: {@code @Transactional}, rollback, propagacja, pułapki proxy,
 * readOnly i izolacja, blokowanie optymistyczne, TransactionTemplate.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet z własną klasą startową i własną bazą H2):</p>
 * <ol>
 *   <li>{@code tx01_transactional} — Tx01Transactional: czym jest transakcja (ACID), {@code @Transactional}, commit
 *       i rollback, wyjątki kontrolowane, rollbackFor/noRollbackFor, proxy i JpaTransactionManager.</li>
 *   <li>{@code tx02_propagation} — Tx02Propagation: propagacja (REQUIRED, REQUIRES_NEW, MANDATORY, NEVER, SUPPORTS,
 *       NOT_SUPPORTED, NESTED) i UnexpectedRollbackException.</li>
 *   <li>{@code tx03_proxy_pitfalls} — Tx03ProxyPitfalls: wywołanie this.metoda(), metody prywatne i final,
 *       {@code @PostConstruct}, połknięte wyjątki — i jak to naprawić.</li>
 *   <li>{@code tx04_read_only_isolation} — Tx04ReadOnlyIsolation: readOnly, timeout, poziomy izolacji,
 *       {@code @Version} i blokowanie optymistyczne.</li>
 *   <li>{@code tx05_programmatic_transactions} — Tx05ProgrammaticTransactions: TransactionTemplate, transakcja na
 *       element w pętli, {@code @Transactional} i {@code @Commit} w testach.</li>
 * </ol>
 *
 * <p>Izolacja lekcji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową (skanuje tylko swój podpakiet: swoje encje, repozytoria i serwisy), własny profil
 * {@code application-txNN.properties}, własną bazę {@code jdbc:h2:mem:txNN} i własne migracje Flyway
 * ({@code db/migration/txNN}); Hibernate tylko sprawdza schemat ({@code ddl-auto=validate}).</p>
 *
 * <p>Testy tej części kursu często NAPRAWDĘ zatwierdzają dane (inaczej nie widać różnicy między commitem
 * a rollbackiem), dlatego każdy test przywraca stan bazy w {@code @BeforeEach} i nie zależy od kolejności.</p>
 */
package s10_transactions;
