/**
 * Dział s05_validation — WALIDACJA DANYCH (validation = sprawdzanie poprawności): jak nie wpuścić do aplikacji danych
 * pustych, za długich, ujemnych albo w złym formacie, i jak odpowiedzieć na nie czytelnym błędem.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code val01_bean_validation} — Val01BeanValidation: adnotacje {@code jakarta.validation.constraints},
 *       {@code Validator}, {@code ConstraintViolation}, różnice {@code @NotNull}/{@code @NotEmpty}/{@code @NotBlank},
 *       null a reguły, komunikaty i ValidationMessages.properties.</li>
 *   <li>{@code val02_valid_in_controllers} — Val02ValidInControllers: {@code @Valid @RequestBody} i walidacja
 *       parametrów w Spring MVC — kod 400 i wyjątki.</li>
 *   <li>{@code val03_nested_and_groups} — Val03NestedAndGroups: obiekty zagnieżdżone, elementy kolekcji, grupy.</li>
 *   <li>{@code val04_custom_validators} — Val04CustomValidators: własna adnotacja i {@code ConstraintValidator}.</li>
 *   <li>{@code val05_service_validation} — Val05ServiceValidation: {@code @Validated} na serwisie (przez proxy).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, więc skanuje tylko swoje klasy. Wspólny plik {@code ValidationMessages.properties}
 * leży w korzeniu classpath — klucze w nim mają prefiks lekcji ({@code val01.…}).</p>
 */
package s05_validation;
