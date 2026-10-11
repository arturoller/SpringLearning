/**
 * Dział s07_data_jdbc — BAZA DANYCH PRZEZ JDBC w Springu (bez JPA): JdbcClient, JdbcTemplate, skrypty startowe,
 * migracje Flyway i repozytorium. Baza: H2 w pamięci (nic nie trzeba instalować).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code jdbc01_jdbc_client} — Jdbc01JdbcClient: DataSource z autokonfiguracji i nowoczesny JdbcClient.</li>
 *   <li>{@code jdbc02_jdbc_template} — Jdbc02JdbcTemplate: JdbcTemplate, NamedParameterJdbcTemplate, RowMapper, batch.</li>
 *   <li>{@code jdbc03_sql_init} — Jdbc03SqlInit: schema.sql, data.sql i właściwości {@code spring.sql.init.*}.</li>
 *   <li>{@code jdbc04_flyway} — Jdbc04Flyway: wersjonowane migracje bazy (Flyway).</li>
 *   <li>{@code jdbc05_repository} — Jdbc05Repository: repozytorium na JdbcClient, klucze generowane, wyjątki, @JdbcTest.</li>
 * </ol>
 *
 * <p>Izolacja lekcji w jednym module: każda lekcja ma własną klasę {@code @SpringBootApplication}, własny profil
 * ({@code application-jdbcNN.properties}) i własne skrypty w {@code src/main/resources/db/...}. Wspólny
 * {@code application.properties} wyłącza Flyway i {@code spring.sql.init} — lekcja włącza je u siebie.
 * W pakiecie działu NIE ma klasy {@code @SpringBootApplication}.</p>
 */
package s07_data_jdbc;
