package s05_validation.val05_service_validation;

import org.springframework.stereotype.Service;

/**
 * SZKIELET ćwiczenia 1 z lekcji Val05ServiceValidation (opis: Val05ServiceValidation).
 * Test: Val05ServiceValidationExercisesTest.
 */
@Service
// TODO: ćwiczenie 1 — dodaj nad klasą adnotację, która włącza walidację metod tego beana
public class Exercise1Service {

    // TODO: ćwiczenie 1 — dopisz ograniczenia: priceGrosze co najmniej 0, percent od 0 do 100
    public int discountedPrice(int priceGrosze, int percent) {
        return priceGrosze - priceGrosze * percent / 100;
    }
}
