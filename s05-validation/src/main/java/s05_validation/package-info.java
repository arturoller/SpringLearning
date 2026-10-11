/**
 * Dział s05_validation — WALIDACJA (validation = sprawdzanie poprawności danych).
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code val01_bean_validation} — Val01BeanValidation: adnotacje Bean Validation i programowy {@code Validator}.</li>
 *   <li>{@code val02_valid_in_controllers} — Val02ValidInControllers: {@code @Valid @RequestBody}, walidacja parametrów
 *       żądania, zamiana błędów na czytelny JSON.</li>
 *   <li>{@code val03_nested_and_groups} — Val03NestedAndGroups: obiekty zagnieżdżone, elementy list, grupy walidacji.</li>
 *   <li>{@code val04_custom_validators} — Val04CustomValidators: własne adnotacje, walidacja między polami,
 *       beany Springa w walidatorze.</li>
 *   <li>{@code val05_service_validation} — Val05ServiceValidation: walidacja w serwisach ({@code @Validated}) i
 *       w {@code @ConfigurationProperties}.</li>
 * </ol>
 *
 * <p>Uwaga: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Lekcje z kontekstem Springa mają własne
 * klasy startowe w swoich podpakietach, a rozwiązania ćwiczeń leżą w {@code s05_validation.solutions}.</p>
 */
package s05_validation;
