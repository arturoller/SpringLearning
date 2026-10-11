package s03_boot_basics.boot02_writing_auto_configuration;

/**
 * LoyaltyPolicy = zasady programu lojalnościowego (loyalty = lojalność, policy = zasady): stały rabat procentowy.
 * Używana w ćwiczeniu 3 lekcji Boot02WritingAutoConfiguration.
 *
 * @param percent rabat w procentach (0–100)
 */
public record LoyaltyPolicy(int percent) {

    /** Cena po rabacie w groszach (zaokrąglenie w dół). */
    public int discounted(int priceGrosze) {
        return priceGrosze * (100 - percent) / 100;
    }
}
