package s18_aop.aop03_custom_annotations;

/**
 * Archiwum cen — INTERFEJS z adnotacją {@code @AuditLog} na metodzie. Pułapka z sekcji 1: adnotacje metod
 * interfejsu NIE są dziedziczone przez metody klasy, a {@code @annotation(...)} patrzy na metodę KLASY
 * (PriceArchiveService) — więc audytu nie będzie.
 */
public interface PriceArchive {

    @AuditLog(action = "ARCHIWIZACJA")
    String archive(String sku);
}
