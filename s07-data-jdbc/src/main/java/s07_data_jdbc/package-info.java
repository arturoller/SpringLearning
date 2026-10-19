/**
 * Dział s07_data_jdbc — BAZY DANYCH PRZEZ JDBC W SPRINGU (JDBC = Java DataBase Connectivity, standardowy sposób
 * rozmowy Javy z bazą): jak z aplikacji Springa wysyłać zapytania SQL, przygotować bazę do pracy i testować kod, który
 * z niej korzysta. Bez weba, na bazie H2 w pamięci.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code jdbc01_jdbc_client} — Jdbc01JdbcClient: nowoczesne API {@code JdbcClient}: zapytania, parametry
 *       pozycyjne i nazwane, mapowanie na rekordy, {@code single()} i {@code optional()}, klucze generowane,
 *       wstrzykiwanie SQL.</li>
 *   <li>{@code jdbc02_jdbc_template} — Jdbc02JdbcTemplate: starsze API ({@code JdbcTemplate},
 *       {@code NamedParameterJdbcTemplate}) z tutoriali, {@code RowMapper}, {@code batchUpdate}, hierarchia
 *       {@code DataAccessException}.</li>
 *   <li>{@code jdbc03_sql_init} — Jdbc03SqlInit: {@code schema.sql} i {@code data.sql}, tryby inicjalizacji,
 *       błędy w skryptach i powód, dla którego to nie wystarcza w produkcji.</li>
 *   <li>{@code jdbc04_flyway} — Jdbc04Flyway: wersjonowane migracje bazy (Flyway), suma kontrolna, migracje
 *       powtarzalne, historia zmian. Od tej lekcji działy z bazą w kursie używają Flyway.</li>
 *   <li>{@code jdbc05_repository} — Jdbc05Repository: repozytorium na {@code JdbcClient}, wyjątek domenowy dla
 *       naruszenia unikalności i test wycinkowy {@code @JdbcTest}.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, własną bazę H2 ({@code jdbc:h2:mem:jdbcNN}), własny profil
 * ({@code application-jdbcNN.properties}) i własne skrypty ({@code db/jdbcNN} albo {@code db/migration/jdbcNN}).
 * Starter Flyway jest na ścieżce klas całego modułu, więc lekcje, które go nie uczą, wyłączają go ustawieniem
 * {@code spring.flyway.enabled=false}. Rozwiązania ćwiczeń leżą w pakiecie {@code solutions}, obok lekcji.</p>
 */
package s07_data_jdbc;
