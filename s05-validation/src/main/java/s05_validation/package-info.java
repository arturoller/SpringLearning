/**
 * Dział s05_validation — WALIDACJA DANYCH (validation = sprawdzanie poprawności): jak odrzucić błędne dane, zanim
 * dotrą do logiki biznesowej, i jak powiedzieć klientowi API, co dokładnie jest nie tak.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code val01_bean_validation} — Val01BeanValidation: adnotacje Jakarta Bean Validation ({@code @NotBlank},
 *       {@code @Size}, {@code @Min}, {@code @Email}…) i {@code Validator} bez Springa i ze Springiem.</li>
 *   <li>{@code val02_valid_in_controllers} — Val02ValidInControllers: {@code @Valid} przy {@code @RequestBody},
 *       walidacja parametrów ścieżki i zapytania, odpowiedź 400 i jej treść.</li>
 *   <li>{@code val03_nested_and_groups} — Val03NestedAndGroups: obiekty zagnieżdżone, listy, grupy walidacji
 *       ({@code @Validated(Grupa.class)}), kolejność grup.</li>
 *   <li>{@code val04_custom_validators} — Val04CustomValidators: własna adnotacja + {@code ConstraintValidator},
 *       walidacja na poziomie klasy (kilka pól naraz), komunikaty.</li>
 *   <li>{@code val05_service_validation} — Val05ServiceValidation: {@code @Validated} na serwisie (walidacja metod
 *       przez proxy), {@code ConstraintViolationException}, gdzie walidować i czego NIE walidować adnotacjami.</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie.</p>
 */
package s05_validation;
