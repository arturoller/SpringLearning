/**
 * Dział s21_capstone — PROJEKTY KOŃCOWE (capstone = projekt zwieńczający): trzy kompletne aplikacje, które łączą
 * wszystko z kursu. Każdy projekt ma specyfikację, architekturę, pełną implementację wzorcową, testy na każdym poziomie
 * (jednostkowe, wycinki, pełny kontekst), scenariusz w pliku .http i ćwiczenia, które ROZSZERZAJĄ projekt.
 *
 * <p>Kolejność czytania (każdy projekt to osobny podpakiet z własną klasą startową, profilem i bazą):</p>
 * <ol>
 *   <li>{@code cap01_shop_api} — Cap01ShopApi: REST API sklepu — produkty (CRUD, wyszukiwanie, strony), klienci,
 *       zamówienia z pozycjami i regułami statusów, walidacja, ProblemDetail, DTO, lista zamówień bez N+1.</li>
 *   <li>{@code cap02_reservations} — Cap02Reservations: rezerwacje stanowisk serwisowych — sloty czasowe, brak
 *       nakładania się (reguła w serwisie + ograniczenie w bazie), współbieżność, {@code Clock}, anulowanie z terminem,
 *       zdarzenie „potwierdzono” obsłużone po zatwierdzeniu transakcji.</li>
 *   <li>{@code cap03_library_with_auth} — Cap03LibraryWithAuth: biblioteka z logowaniem — książki, wypożyczenia,
 *       kary liczone z {@code Clock}, role BIBLIOTEKARZ i CZYTELNIK, JWT, BCrypt, {@code @PreAuthorize}.</li>
 * </ol>
 *
 * <p>Izolacja projektów: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każdy projekt ma własną klasę
 * startową (skanuje tylko swój podpakiet), własny profil {@code application-capNN.properties}, własną bazę
 * {@code jdbc:h2:mem:capNN} i własne migracje Flyway ({@code db/migration/capNN}). Rozwiązania ćwiczeń leżą
 * w {@code s21_capstone.solutions.capNN_...} — obok projektów, nie w nich.</p>
 *
 * <p>UWAGA: starter Spring Security jest na ścieżce klas CAŁEGO modułu (potrzebuje go Cap03). Dlatego Cap01 i Cap02
 * mają własny {@code SecurityFilterChain} z {@code permitAll()} — bez niego domyślne zabezpieczenia Boota zwracałyby
 * 401 na każde zapytanie (Cap01ShopApi, sekcja 7).</p>
 */
package s21_capstone;
