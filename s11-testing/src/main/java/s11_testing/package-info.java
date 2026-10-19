/**
 * Dział s11_testing — TESTOWANIE APLIKACJI SPRING BOOT: jak sprawdzać każdą warstwę aplikacji testem, który jest
 * szybki, wiarygodny i nie zależy od kolejności ani od komputera. Od testów jednostkowych z Mockito, przez wycinki
 * ({@code @WebMvcTest}, {@code @DataJpaTest}), po prawdziwą bazę PostgreSQL w kontenerze Dockera.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code test01_test_slices} — Test01TestSlices: piramida testów, wycinki ({@code @WebMvcTest},
 *       {@code @DataJpaTest}, {@code @JdbcTest}, {@code @JsonTest}), {@code @SpringBootTest} i środowisko webowe,
 *       pamięć podręczna kontekstów testowych.</li>
 *   <li>{@code test02_mockito_beans} — Test02MockitoBeans: czysty Mockito bez Springa, {@code @MockitoBean},
 *       {@code @MockitoSpyBean}, {@code @TestBean}, zerowanie atrap między testami.</li>
 *   <li>{@code test03_mock_mvc_tester} — Test03MockMvcTester: testy kontrolerów w stylu AssertJ
 *       ({@code MockMvcTester}), porównanie z klasycznym MockMvc, {@code RestTestClient}.</li>
 *   <li>{@code test04_data_jpa_test} — Test04DataJpaTest: wycinek JPA dokładniej — transakcja i wycofanie,
 *       {@code TestEntityManager}, {@code flush()}/{@code clear()}, {@code @Sql}, podmiana bazy, Flyway.</li>
 *   <li>{@code test05_test_configuration} — Test05TestConfiguration: {@code @TestConfiguration},
 *       {@code @TestPropertySource}, {@code @DynamicPropertySource}, {@code @ActiveProfiles}, stały zegar w testach.</li>
 *   <li>{@code test06_testcontainers} — Test06Testcontainers: prawdziwy PostgreSQL w kontenerze,
 *       {@code @ServiceConnection}, różnice H2 i PostgreSQL, uruchamianie aplikacji z kontenerami.</li>
 *   <li>{@code test07_docker_compose} — Test07DockerCompose: Boot sam uruchamia usługi z {@code compose.yaml}
 *       przy starcie aplikacji; Docker Compose a Testcontainers.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, a jej testy znajdują ją same (szukają w górę drzewa pakietów). Wspólne
 * {@code application.properties} wyłącza Flyway i obsługę Docker Compose — włączają je tylko profile lekcji, które
 * ich uczą ({@code application-test04.properties}, {@code application-test07.properties}). Bazy H2 dostają
 * unikalne nazwy, więc konteksty testów nie widzą nawzajem swoich tabel.</p>
 *
 * <p>Docker: testy z kontenerami (Test06) są POMIJANE, gdy Docker nie działa
 * ({@code @Testcontainers(disabledWithoutDocker = true)}) — {@code ./mvnw test} przechodzi u każdego. Testy Test07
 * Dockera nie potrzebują (w testach Boot pomija Docker Compose); Docker jest potrzebny tylko do ręcznego uruchomienia
 * aplikacji lekcji 06 i 07 (pliki {@code s11-testing/http/*.http}). Pliki Compose leżą w {@code s11-testing/compose}.</p>
 *
 * <p>Ćwiczenia: w tym dziale ćwiczeniem jest najczęściej DOPISANIE kawałka testu — szkielety są w
 * {@code src/test/java} (klasy {@code …ExercisesTest}, {@code @Tag("cwiczenie")}), opisy w klasach lekcji. Rozwiązania
 * wzorcowe: pakiet {@code solutions} (testy {@code …SolutionsTest}, {@code @Tag("wzorzec")}), obok lekcji.</p>
 */
package s11_testing;
