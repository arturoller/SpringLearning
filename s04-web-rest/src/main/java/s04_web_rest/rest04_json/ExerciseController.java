package s04_web_rest.rest04_json;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SZKIELET ĆWICZEŃ lekcji Rest04Json — uzupełniasz go sam (opis zadań: lekcja, sekcja ĆWICZENIA; testy:
 * Rest04JsonExercisesTest). W każdym ćwiczeniu robisz DWIE rzeczy: (1) dopisujesz adnotacje Jacksona do rekordu,
 * (2) piszesz ciało metody kontrolera. Mapowania są gotowe. Dopóki metody rzucają UnsupportedOperationException("TODO"),
 * testy ćwiczeń nie przechodzą (są wyłączone z domyślnego {@code ./mvnw test}).
 */
@RestController
@RequestMapping("/rest04/cw")
public class ExerciseController {

    /** Ćwiczenie 1: w JSON pole {@code ownerEmail} ma się nazywać {@code owner_email}. */
    public record GiftCard(String code, String ownerEmail) {
    }

    /** Ćwiczenie 2: {@code passwordHash} nie może pojawić się w JSON. */
    public record Account(String login, String passwordHash) {
    }

    /** Ćwiczenie 3: pole {@code description} równe null ma zniknąć z JSON. */
    public record Coupon(String code, String description) {
    }

    /** Ćwiczenie 4: data {@code issued} ma wyglądać w JSON tak: {@code "31.01.2026"}. */
    public record Invoice(String number, LocalDate issued, BigDecimal total) {
    }

    /** Ćwiczenie 5: w JSON rozmiary mają pisownię małymi literami: "small" i "large". */
    public enum ParcelSize {
        SMALL, LARGE
    }

    public record Parcel(ParcelSize size) {
    }

    /** Ćwiczenie 1: zwróć {@code new GiftCard("GC-1", "ala@example.com")}. */
    @GetMapping("/gift-card")
    public GiftCard giftCard() {
        // TODO: twoje rozwiązanie (i adnotacja nad rekordem GiftCard)
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 2: zwróć {@code new Account("ala", "hash-123")}. */
    @GetMapping("/account")
    public Account account() {
        // TODO: twoje rozwiązanie (i adnotacja nad rekordem Account)
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 3: zwróć kupon {@code "PROMO10"} z opisem null. */
    @GetMapping("/coupon")
    public Coupon coupon() {
        // TODO: twoje rozwiązanie (i adnotacja nad rekordem Coupon)
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 4: zwróć fakturę "FV/1/2026" z datą 2026-01-31 i kwotą 199.90. */
    @GetMapping("/invoice")
    public Invoice invoice() {
        // TODO: twoje rozwiązanie (i adnotacja nad polem issued)
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5 (część 1): zwróć paczkę {@code new Parcel(ParcelSize.LARGE)} — w JSON ma być {@code {"size":"large"}}. */
    @GetMapping("/parcel")
    public Parcel parcel() {
        // TODO: twoje rozwiązanie (i adnotacje przy stałych enuma ParcelSize)
        throw new UnsupportedOperationException("TODO");
    }

    /** Ćwiczenie 5 (część 2): przyjmij paczkę z JSON-a {@code {"size":"small"}} i odpowiedz tekstem "rozmiar: SMALL". */
    @PostMapping("/parcel")
    public String parcelSize(@RequestBody Parcel parcel) {
        // TODO: twoje rozwiązanie (i adnotacje przy stałych enuma ParcelSize)
        throw new UnsupportedOperationException("TODO");
    }
}
