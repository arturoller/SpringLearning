/**
 * Dział s16_cache_scheduling_async — PAMIĘĆ PODRĘCZNA, ZADANIA CYKLICZNE I PRACA W TLE (cache = pamięć podręczna;
 * scheduling = harmonogram, planowanie zadań; async = asynchroniczność, praca w innym wątku): jak przyspieszyć aplikację
 * zapamiętanymi wynikami, jak uruchamiać zadania o określonych porach i jak nie blokować wątku żądania.
 *
 * <p>Kolejność czytania (każda lekcja to osobny podpakiet):</p>
 * <ol>
 *   <li>{@code cache01_cacheable} — Cache01Cacheable: {@code @EnableCaching}, {@code @Cacheable}, klucz, {@code condition}
 *       i {@code unless}, domyślny {@code CacheManager}, pułapki (wywołanie z tej samej klasy, obiekty zmienne).</li>
 *   <li>{@code cache02_cache_management} — Cache02CacheManagement: {@code @CachePut}, {@code @CacheEvict},
 *       {@code @Caching}, unieważnianie przy zmianie danych, {@code spring.cache.*}, rozmiar i czas życia wpisów.</li>
 *   <li>{@code sched03_scheduled} — Sched03Scheduled: {@code @EnableScheduling}, {@code @Scheduled}, wyrażenia cron
 *       Springa (6 pól), pula wątków harmonogramu, sprawdzanie zarejestrowanych zadań.</li>
 *   <li>{@code async04_async} — Async04Async: {@code @EnableAsync}, {@code @Async}, {@code CompletableFuture}, wykonawca
 *       zadań Boota, wyjątki w metodach {@code void}, utrata kontekstu (MDC) w innym wątku.</li>
 *   <li>{@code async05_virtual_threads} — Async05VirtualThreads: wątki wirtualne,
 *       {@code spring.threads.virtual.enabled=true}, co zmienia w Boot 4.1, kiedy pomagają, przypinanie (pinning).</li>
 * </ol>
 *
 * <p>Uwaga o izolacji: w pakiecie działu NIE ma klasy {@code @SpringBootApplication}. Każda lekcja ma własną klasę
 * startową w swoim podpakiecie, a adnotacje {@code @EnableCaching}, {@code @EnableScheduling} i {@code @EnableAsync}
 * stoją TYLKO w klasach startowych tych lekcji, które ich potrzebują — dzięki temu zadania cykliczne z lekcji trzeciej
 * nie ruszają w testach pozostałych lekcji.</p>
 */
package s16_cache_scheduling_async;
