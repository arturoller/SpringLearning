/**
 * Dział s10_transactions — TRANSAKCJE (transaction = transakcja: kilka operacji na bazie, które udają się wszystkie
 * albo żadna): jak Spring zarządza transakcjami przez {@code @Transactional} i gdzie ta „magia” przestaje działać.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code tx01_transactional} — Tx01Transactional: {@code @Transactional}, commit i rollback, które wyjątki
 *       wycofują transakcję ({@code rollbackFor}, {@code noRollbackFor}).</li>
 *   <li>{@code tx02_propagation} — Tx02Propagation: propagacja (REQUIRED, REQUIRES_NEW, NESTED, MANDATORY…),
 *       „rollback-only” i {@code UnexpectedRollbackException}.</li>
 *   <li>{@code tx03_proxy_pitfalls} — Tx03ProxyPitfalls: jak działa proxy transakcyjne i kiedy go nie ma
 *       (wywołanie z tej samej klasy, metoda prywatna, obiekt z {@code new}).</li>
 *   <li>{@code tx04_read_only_isolation} — Tx04ReadOnlyIsolation: {@code readOnly}, poziomy izolacji, zjawiska
 *       współbieżności (lost update, non-repeatable read), blokowanie optymistyczne i pesymistyczne.</li>
 *   <li>{@code tx05_programmatic_transactions} — Tx05ProgrammaticTransactions: {@code TransactionTemplate},
 *       {@code PlatformTransactionManager}, synchronizacja ({@code afterCommit}).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}; każda lekcja ma własną klasę
 * startową, własną bazę H2 ({@code jdbc:h2:mem:txNN}) i własne migracje Flyway.</p>
 */
package s10_transactions;
