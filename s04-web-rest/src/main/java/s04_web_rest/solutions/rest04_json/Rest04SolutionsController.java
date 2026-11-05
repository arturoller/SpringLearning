package s04_web_rest.solutions.rest04_json;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Rozwiązania wzorcowe ćwiczeń lekcji Rest04Json — zajrzyj dopiero po własnej próbie! */
@RestController
@RequestMapping("/rest04/cw")
public class Rest04SolutionsController {

    /** Rozwiązanie 1: @JsonProperty na komponencie rekordu. */
    public record GiftCard(String code, @JsonProperty("owner_email") String ownerEmail) {
    }

    /** Rozwiązanie 2: @JsonIgnore na komponencie rekordu. */
    public record Account(String login, @JsonIgnore String passwordHash) {
    }

    /** Rozwiązanie 3: @JsonInclude(NON_NULL) na komponencie (można też nad całym rekordem). */
    public record Coupon(String code, @JsonInclude(JsonInclude.Include.NON_NULL) String description) {
    }

    /** Rozwiązanie 4: @JsonFormat(pattern = ...) na komponencie z datą. */
    public record Invoice(String number, @JsonFormat(pattern = "dd.MM.yyyy") LocalDate issued, BigDecimal total) {
    }

    /** Rozwiązanie 5: @JsonProperty na stałych enuma. */
    public enum ParcelSize {
        @JsonProperty("small") SMALL,
        @JsonProperty("large") LARGE
    }

    public record Parcel(ParcelSize size) {
    }

    @GetMapping("/gift-card")
    public GiftCard giftCard() {
        return new GiftCard("GC-1", "ala@example.com");
    }

    @GetMapping("/account")
    public Account account() {
        return new Account("ala", "hash-123");
    }

    @GetMapping("/coupon")
    public Coupon coupon() {
        return new Coupon("PROMO10", null);
    }

    @GetMapping("/invoice")
    public Invoice invoice() {
        return new Invoice("FV/1/2026", LocalDate.of(2026, 1, 31), new BigDecimal("199.90"));
    }

    @GetMapping("/parcel")
    public Parcel parcel() {
        return new Parcel(ParcelSize.LARGE);
    }

    @PostMapping("/parcel")
    public String parcelSize(@RequestBody Parcel parcel) {
        return "rozmiar: " + parcel.size();
    }
}
