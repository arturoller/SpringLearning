/**
 * Dział s11_testing — TESTOWANIE APLIKACJI SPRING BOOT: który rodzaj testu wybrać, jak podmienić zależności,
 * jak testować kontrolery, bazę i całą aplikację — także na prawdziwym PostgreSQL w kontenerze Dockera.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code test01_test_slices} — Test01TestSlices: piramida testów, {@code @SpringBootTest} kontra wycinki
 *       ({@code @WebMvcTest}, {@code @DataJpaTest}, {@code @JsonTest}…), pamięć podręczna kontekstów.</li>
 *   <li>{@code test02_mockito_beans} — Test02MockitoBeans: {@code @MockitoBean}, {@code @MockitoSpyBean}, Mockito
 *       w testach Springa.</li>
 *   <li>{@code test03_mock_mvc_tester} — Test03MockMvcTester: {@code MockMvcTester} — asercje AssertJ dla kontrolerów.</li>
 *   <li>{@code test04_data_jpa_test} — Test04DataJpaTest: {@code @DataJpaTest}, {@code TestEntityManager},
 *       transakcja testu i jej pułapki.</li>
 *   <li>{@code test05_test_configuration} — Test05TestConfiguration: {@code @TestConfiguration}, {@code @Import},
 *       właściwości i profile testów, {@code @DynamicPropertySource}, {@code @DirtiesContext}.</li>
 *   <li>{@code test06_testcontainers} — Test06Testcontainers: PostgreSQL w kontenerze, {@code @ServiceConnection}.</li>
 *   <li>{@code test07_docker_compose} — Test07DockerCompose: {@code compose.yaml} i aplikacja uruchamiana lokalnie
 *       razem z bazą.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}; każda lekcja ma własną klasę
 * startową. Testy wymagające Dockera mają {@code @Testcontainers(disabledWithoutDocker = true)} — bez Dockera są
 * pomijane (żółty wynik), a nie czerwone.</p>
 */
package s11_testing;
