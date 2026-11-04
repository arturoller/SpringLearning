/**
 * Dział s07_data_jdbc — BAZA DANYCH PRZEZ JDBC W SPRINGU (JDBC — Java Database Connectivity, standardowe API Javy
 * do baz SQL): jak Spring upraszcza pracę z SQL-em i jak wersjonować strukturę bazy migracjami Flyway.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code jdbc01_jdbc_client} — Jdbc01JdbcClient: {@code JdbcClient} — zapytania, parametry, mapowanie wierszy
 *       na rekordy, wstawianie i klucze generowane.</li>
 *   <li>{@code jdbc02_jdbc_template} — Jdbc02JdbcTemplate: {@code JdbcTemplate} i {@code NamedParameterJdbcTemplate}
 *       — starsze API, które spotkasz w istniejącym kodzie; operacje wsadowe (batch).</li>
 *   <li>{@code jdbc03_sql_init} — Jdbc03SqlInit: {@code schema.sql}/{@code data.sql} i {@code spring.sql.init.*}.</li>
 *   <li>{@code jdbc04_flyway} — Jdbc04Flyway: migracje Flyway ({@code V1__…sql}) i dlaczego zastępują schema.sql.</li>
 *   <li>{@code jdbc05_repository} — Jdbc05Repository: repozytorium na JDBC (wzorzec repozytorium), wyjątki
 *       {@code DataAccessException}, transakcja — zapowiedź działu s10.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}; każda lekcja ma własną klasę
 * startową, własną bazę H2 ({@code jdbc:h2:mem:jdbcNN}) i własne skrypty/migracje w katalogu lekcji.</p>
 */
package s07_data_jdbc;
