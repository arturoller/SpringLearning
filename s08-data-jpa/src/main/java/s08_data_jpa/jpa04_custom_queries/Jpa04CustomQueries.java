package s08_data_jpa.jpa04_custom_queries;

/**
 * <pre>
 * TEMAT: Własne zapytania — {@code @Query} z JPQL i natywnym SQL, projekcje (interfejs, rekord, dynamiczne),
 *        {@code @Modifying} (masowe UPDATE/DELETE), agregaty
 *        (query = zapytanie; JPQL — Jakarta Persistence Query Language, język zapytań o ENCJE; native = natywny,
 *         czyli SQL konkretnej bazy; projection = projekcja, rzut — tylko wybrane kolumny; modifying = modyfikujący)
 *
 * W SKRÓCIE:
 *   Gdy nazwa metody z Jpa03 robi się za długa albo potrzebujesz czegoś, czego nazwa nie wyrazi (agregaty, złączenia,
 *   funkcje bazy, masowy UPDATE), piszesz zapytanie sam w adnotacji {@code @Query}. Domyślnie to JPQL: pytasz
 *   o encje i ich pola, a Hibernate tłumaczy to na SQL Twojej bazy. {@code nativeQuery = true} wysyła SQL bez
 *   tłumaczenia. Wynik nie musi być encją: projekcja (interfejs albo rekord) pobiera tylko potrzebne kolumny.
 *   UPDATE/DELETE wymaga {@code @Modifying} — i omija kontekst utrwalania, o czym trzeba pamiętać.
 *
 * ANALOGIA: zamówienie w restauracji.
 *   Metoda z nazwy (Jpa03) to wybór z karty: „zestaw nr 3” — szybko i bez pomyłek, ale tylko to, co w karcie.
 *   JPQL to rozmowa z kelnerem w języku restauracji („poproszę danie dnia bez sosu”) — kelner (Hibernate) sam
 *   przetłumaczy to kucharzowi (bazie). Natywny SQL to wejście do kuchni i mówienie gwarą kucharza — działa tylko
 *   w TEJ kuchni. Projekcja to „poproszę sam deser”, a nie cały zestaw. @Modifying to przemeblowanie sali, gdy
 *   kelner trzyma w notesie stary plan stolików (kontekst utrwalania) — trzeba mu go wyczyścić.
 *
 * JAK TO DZIAŁA:
 *   1. Przy STARCIE (tworzenie beana repozytorium) Spring Data dla metody z {@code @Query} tworzy obiekt zapytania.
 *      JPQL przekazuje do Hibernate, który go PARSUJE i sprawdza nazwy encji i pól — błąd = aplikacja nie startuje
 *      (sprawdzone testem: Jpa04BrokenQueriesTest). Natywnego SQL nikt przy starcie nie sprawdza.
 *   2. Przy wywołaniu: parametry metody wiązane są z {@code :nazwa} (po nazwie parametru Javy) albo z {@code ?1}
 *      (po pozycji). Hibernate zamienia JPQL na SQL (nazwy encji → tabele, pola → kolumny, wg mapowania).
 *   3. Typ zwracany decyduje o wyniku: encja (wczytana do kontekstu utrwalania, Jpa01 sekcja 3), interfejs
 *      (Spring Data tworzy proxy — obiekt-pośrednik), rekord/klasa DTO (wywołanie konstruktora), wartość skalarna.
 *   4. {@code @Modifying}: Spring Data wywołuje {@code executeUpdate()} zamiast {@code getResultList()} i zwraca
 *      liczbę zmienionych wierszy. UPDATE/DELETE idzie prosto do bazy — encje w pamięci o tym nie wiedzą.
 *
 * SŁÓWKA:
 *   named parameter = parametr nazwany; positional = pozycyjny; native = natywny (SQL tej bazy); portable = przenośny;
 *   projection = projekcja (rzut na wybrane kolumny); DTO (Data Transfer Object) = obiekt do przenoszenia danych;
 *   constructor expression = wyrażenie konstruktora ({@code select new …}); bulk = masowy; stale = nieaktualny
 *   (przeterminowany); clear = wyczyść; flush = wypchnij zmiany do bazy; aggregate = agregat (count, sum, avg…);
 *   group by = grupuj według; SpEL (Spring Expression Language) = język wyrażeń Springa.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa03QueryMethods (metody z nazw — zaczynaj od nich),
 *             s08_data_jpa/Jpa05PagingSorting (@Query + Pageable i countQuery),
 *             s07_data_jdbc/Jdbc01JdbcClient (zwykły SQL bez JPA — porównaj z natywnym @Query),
 *             s09_jpa_relations/Rel06EntityToDto (projekcje i DTO przy relacjach),
 *             t09_records/Records01Basics (rekordy — tu jako projekcje).
 * </pre>
 */
public final class Jpa04CustomQueries {

    private Jpa04CustomQueries() {
    }

    // =================================================================================================
    // 1. @Query Z JPQL — encje i pola, nie tabele i kolumny (ProductRepository.java, sekcja 1)
    // =================================================================================================
    //
    //   {@code @Query("select p from Product p where p.price > :minPrice order by p.price desc")}
    //   {@code List<Product> findMoreExpensiveThan(BigDecimal minPrice);}
    //
    // Product to nazwa KLASY encji (wielka litera!), p.price to POLE. SQL z logu (sprawdzone testem):
    //   {@code select p1_0.id,…,p1_0.price,p1_0.stock from product p1_0 where p1_0.price>? order by p1_0.price desc}
    // Hibernate sam dobrał tabelę product i kolumny (np. pole createdAt → kolumna created_at).
    //
    // Parametry:
    //   • nazwane {@code :minPrice} — dopasowane po NAZWIE parametru metody. {@code @Param("minPrice")} (param =
    //     parametr) jest potrzebne tylko wtedy, gdy kod skompilowano BEZ flagi javac {@code -parameters} (wtedy nazw
    //     parametrów nie ma w pliku .class i Spring Data zgłasza błąd z prośbą o @Param). spring-boot-starter-parent
    //     włącza {@code -parameters} w maven-compiler-plugin (sprawdzone w jego pom.xml), więc w tym kursie @Param
    //     jest zbędne. W starych tutorialach zobaczysz @Param wszędzie — to nie błąd, tylko nadmiar.
    //   • pozycyjne {@code ?1, ?2} — numer parametru metody od 1. Działają, ale przy zmianie kolejności parametrów
    //     łatwo o pomyłkę.
    //
    // Filtr OPCJONALNY — tego metody z nazw nie umieją (Jpa03, sekcja 4: null dawał IS NULL):
    //   {@code where (:category is null or p.category = :category) and p.price <= :maxPrice}
    //   search(null, 60) → wszystkie kategorie; search(HOME, 60) → tylko HOME (test).
    //
    // PUŁAPKA: nazwa TABELI w JPQL ({@code select p from product p}). Dlaczego to błąd? JPQL nie zna tabel, tylko
    //   encje: przy starcie aplikacja pada (sprawdzone testem) z BeanCreationException ← QueryCreationException
    //   „Query validation failed for 'select p from product p …'” ← UnknownEntityException „Could not resolve root
    //   entity 'product'”. To samo dla nazwy kolumny zamiast pola (created_at zamiast createdAt).
    //
    // DOBRA PRAKTYKA: dłuższe zapytania pisz w bloku tekstowym {@code """…"""} (text block, Java 15+) i z aliasem
    //   encji (p). Dlaczego? Zapytanie w kilku liniach czyta się jak SQL, a nie jak sklejony string z plusami.
    //
    // DOBRA PRAKTYKA: nazwa metody z @Query mówi CO biznesowo (findMoreExpensiveThan, search), a nie JAK.
    //   Dlaczego? Zapytanie i tak widać w adnotacji; nazwa ma być dla kodu, który z metody korzysta.

    // =================================================================================================
    // 2. NATYWNY SQL — nativeQuery = true: kiedy i za jaką cenę
    // =================================================================================================
    //
    //   {@code @Query(value = "SELECT * FROM product WHERE REGEXP_LIKE(name, :pattern) ORDER BY id", nativeQuery = true)}
    //
    // Hibernate NIE tłumaczy tego zapytania — idzie do bazy dokładnie tak, jak napisano (test porównuje SQL z logu
    // znak w znak). Wynik {@code SELECT *} Hibernate i tak zamienia na encje Product (kolumny muszą pasować do mapowania).
    // Natywne zapytanie może też zwrócić wartość: {@code SELECT COALESCE(SUM(stock), 0) … → long}.
    //
    // Kiedy natywnie? Gdy potrzebujesz czegoś, czego JPQL nie ma albo co jest specyficzne dla bazy: wyrażenia
    // regularne, funkcje okna (window functions), JSON, pełnotekstowe wyszukiwanie, podpowiedzi dla optymalizatora,
    // {@code WITH RECURSIVE}. Na co dzień — JPQL.
    //
    // PUŁAPKA: przenośność. Dlaczego? REGEXP_LIKE działa w H2 i Oracle, ale w PostgreSQL piszesz {@code name ~ :pattern}.
    //   Natywne zapytanie wiąże kod z jedną bazą — a testy na H2 mogą przechodzić, gdy produkcja (PostgreSQL) pada.
    //   Dlatego w s11_testing/Test06Testcontainers testujemy na prawdziwej bazie produkcyjnej.
    //
    // PUŁAPKA: natywny SQL NIE jest sprawdzany przy starcie (sprawdzone testem: literówka {@code stok} zamiast
    //   {@code stock} — aplikacja startuje, a wywołanie metody kończy się InvalidDataAccessResourceUsageException
    //   „Column "STOK" not found”). Dlaczego? Spring Data nie zna Twojej bazy na tyle, żeby rozebrać jej SQL.
    //   Każde natywne zapytanie musi mieć test, który je WYKONUJE.
    //
    // DOBRA PRAKTYKA: natywnie zwracaj raczej projekcje albo wartości niż encje z {@code SELECT *}. Dlaczego?
    //   SELECT * przestaje pasować do encji, gdy ktoś doda kolumnę o nieoczekiwanym typie; jawna lista kolumn nie.

    // =================================================================================================
    // 3. PROJEKCJE — tylko potrzebne dane, bez zarządzanych encji
    // =================================================================================================
    //
    //   rodzaj                     deklaracja                                              SQL / co dostajesz
    //   interfejsowa (z nazwy)     {@code List<ProductSummary> findByCategoryOrderByPriceAsc(c)}   select name, price → proxy JDK
    //   interfejsowa (@Query)      {@code select p.name as name, p.price as price …}               aliasy = nazwy getterów
    //   rekord z new               {@code select new s08_data_jpa.….ProductPrice(p.name, p.price)}  wywołanie konstruktora
    //   rekord BEZ new             {@code select p.name, p.price …} → {@code List<ProductPrice>}    ten sam SQL (test!)
    //   dynamiczna                 {@code <T> List<T> findByCategoryOrderByIdAsc(c, Class<T> type)} typ wybierasz przy wywołaniu
    //
    // Sprawdzone testem: każda z projekcji wysyła {@code select p1_0.name,p1_0.price from product p1_0 …} (dwie kolumny
    // zamiast sześciu), a statystyki Hibernate pokazują ZERO wczytanych encji (getEntityLoadCount() == 0).
    //
    // Rekord BEZ {@code new}: Spring Data 2026 (Spring Data JPA 4.x) widzi, że typ wyniku to rekord/klasa DTO, a nie
    // encja, i SAM przepisuje zapytanie na wyrażenie konstruktora (sprawdzone testem — SQL identyczny jak z new).
    // Kolumny dopasowuje do parametrów konstruktora po KOLEJNOŚCI (PUŁAPKA niżej). W starszych wersjach i tutorialach zobaczysz
    // wyłącznie {@code select new pelna.nazwa.pakietu.Klasa(…)} — to nadal działa i jest standardem JPA.
    //
    // Projekcja dynamiczna: findByCategoryOrderByIdAsc(OFFICE, ProductPrice.class) → rekordy (0 encji),
    // findByCategoryOrderByIdAsc(OFFICE, Product.class) → zwykłe encje (3 wczytane) — jedna metoda, różne kształty.
    //
    // Dlaczego projekcje?
    //   • mniej danych z bazy (tylko potrzebne kolumny — ważne przy szerokich tabelach i długich opisach);
    //   • brak encji w kontekście utrwalania: nie ma dirty checking (Jpa01, sekcja 4), nie ma kosztu pamięci,
    //     nie da się przypadkiem czegoś zmienić — projekcja jest tylko do ODCZYTU;
    //   • gotowy kształt dla API (rekord → JSON), bez wystawiania encji na zewnątrz (s09_jpa_relations/Rel06EntityToDto).
    //
    // PUŁAPKA: projekcja „otwarta” (open projection) — getter z {@code @Value("#{target.name + …}")} (ProductLabel).
    //   Dlaczego? Spring Data nie wie, których pól potrzebuje wyrażenie, więc wczytuje CAŁĄ encję (sprawdzone testem:
    //   pełny SELECT sześciu kolumn i 3 wczytane encje) — tracisz główną zaletę projekcji. Używaj projekcji
    //   „zamkniętych” (same gettery pól) albo rekordów i składaj tekst w Javie.
    //
    // PUŁAPKA: brak aliasów w @Query z projekcją interfejsową. Dlaczego groźne? {@code select p.name, p.price} bez
    //   {@code as name} / {@code as price} nie ma po czym dopasować getterów — i NIE ma wyjątku: getName() i getPrice()
    //   po cichu zwracają null (sprawdzone testem). Z rekordem liczy się KOLEJNOŚĆ kolumn: {@code select p.price, p.name}
    //   Spring Data przepisuje na {@code new ProductPrice(p.price, p.name)} i przy wywołaniu dostajesz
    //   InvalidDataAccessApiUsageException „Missing constructor for type 'ProductPrice'” (sprawdzone testem).
    //
    // DOBRA PRAKTYKA: do odczytu na ekran/do API — projekcja; do zmiany danych — encja.
    //   Dlaczego? Encja daje dirty checking i walidację relacji, projekcja — szybkość i bezpieczeństwo odczytu.
    //   PYTANIE REKRUTACYJNE: „Czym różni się projekcja interfejsowa od DTO-klasy w Spring Data JPA?”

    // =================================================================================================
    // 4. @Modifying — masowy UPDATE/DELETE i nieaktualny kontekst utrwalania
    // =================================================================================================
    //
    //   {@code @Modifying}
    //   {@code @Query("update Product p set p.price = p.price * :factor where p.category = :category")}
    //   {@code int changePrices(Category category, BigDecimal factor);}   // zwraca liczbę zmienionych wierszy
    //
    // JEDNO polecenie SQL zamiast „wczytaj każdą encję, zmień, zapisz”. Masowy DELETE ({@code delete from Product p
    // where p.stock = 0}) to też jedno polecenie (test: 1 polecenie, 0 wczytanych encji) — por. deleteBy… z Jpa03
    // (SELECT + osobny DELETE na każdą encję). Cena tej szybkości: brak zdarzeń cyklu życia (@PreUpdate, @PreRemove),
    // brak kaskad, brak wersjonowania (@Version, s10) — baza dostaje gołe polecenie.
    //
    // Bez {@code @Modifying} (sprawdzone testem): aplikacja STARTUJE, ale wywołanie kończy się
    // InvalidDataAccessApiUsageException „Query executed via 'getResultList()' or 'getSingleResult()' must be a
    // 'select' query” (przyczyna: org.hibernate.query.IllegalSelectQueryException). Spring Data wykonał UPDATE jak SELECT.
    //
    // Transakcja: UPDATE/DELETE wymaga transakcji (jak deleteBy… w Jpa03, sekcja 1). W testach @DataJpaTest jest nią
    // cały test; w aplikacji — metoda serwisu z @Transactional (s10_transactions/Tx01Transactional) albo
    // {@code @Transactional} na metodzie repozytorium.
    //
    // PUŁAPKA: kontekst utrwalania nie wie o masowym UPDATE. Dlaczego? UPDATE idzie prosto do bazy, a encje wczytane
    //   wcześniej siedzą w pamięci 1. poziomu (Jpa01, sekcja 3). Test (Kubek, 24.50, cena × 2):
    //     Product mug = findById(5);            // 24.50, encja w kontekście
    //     changePrices(HOME, 2);                // w bazie 49.00
    //     mug.getPrice()          → 24.50       // stary obiekt
    //     findById(5)             → TEN SAM obiekt (isSameAs) z ceną 24.50 — bez SELECT-a do bazy!
    //   Gorzej: gdyby potem zmienić inne pole tej encji, dirty checking zapisze UPDATE ze STARĄ ceną (nadpisze 49.00).
    //
    //   Rozwiązanie: {@code @Modifying(clearAutomatically = true)} — po UPDATE Spring Data woła {@code em.clear()}.
    //   Test: findById(5) zwraca NOWY obiekt z ceną 49.00; stary obiekt jest odłączony (detached) i nadal ma 24.50.
    //
    // flushAutomatically = true — PRZED UPDATE Spring Data woła {@code em.flush()}, żeby niezapisane zmiany z pamięci
    //   trafiły do bazy, zanim clear je wyrzuci. Sprawdzone testem: Hibernate z domyślnym trybem FlushMode.AUTO i tak
    //   robi flush przed masowym UPDATE (log: najpierw UPDATE zmienionej encji, potem masowy UPDATE), więc zmiana nie
    //   ginie nawet bez flushAutomatically. Atrybut ma znaczenie, gdy ktoś ustawił FlushMode.COMMIT albo przy innym
    //   dostawcy JPA niż Hibernate — {@code flushAutomatically = true, clearAutomatically = true} to bezpieczna para.
    //
    // DOBRA PRAKTYKA: masowe operacje wywołuj na początku transakcji albo w osobnej metodzie serwisu, a w @Modifying
    //   ustawiaj clearAutomatically = true. Dlaczego? Wtedy w pamięci nie ma encji, które mogłyby się „przeterminować”.
    //   PYTANIE REKRUTACYJNE: „Dlaczego po @Modifying @Query encja ma starą wartość i jak to naprawić?”

    // =================================================================================================
    // 5. AGREGATY i SpEL #{#entityName}
    // =================================================================================================
    //
    //   {@code select new s08_data_jpa.jpa04_custom_queries.CategoryStats(p.category, count(p), avg(p.price))}
    //   {@code from Product p group by p.category order by p.category}
    //
    // Typy wyników agregatów w JPQL (sprawdzone testem): count → Long, avg → Double (TAK, Double nawet dla pola
    // BigDecimal — dlatego CategoryStats ma Double averagePrice), max/min → typ pola, sum → BigDecimal dla BigDecimal,
    // ale Long dla pola int (sum(p.stock) to Long, nie Integer — suma może nie zmieścić się w int).
    // Agregat bez wierszy: sum/avg/max → null (maxPriceIn(GARDEN) == null), count → 0. Stąd typ obiektowy
    // (BigDecimal, nie double) albo {@code coalesce(sum(…), 0)} w zapytaniu.
    //
    // PUŁAPKA: avg pieniędzy jako Double. Dlaczego? Double jest niedokładny (t16_streams/Streams15BigDecimalMoney).
    //   Do pokazania średniej wystarczy, do dalszych obliczeń finansowych — licz sum i count, a dziel w BigDecimal.
    //
    // SpEL (język wyrażeń Springa) w @Query: {@code select p from #{#entityName} p} — Spring Data wstawia nazwę encji
    // repozytorium (tu Product). Przydaje się w repozytoriach BAZOWYCH (generycznych), wspólnych dla wielu encji;
    // w zwykłym repozytorium pisz po prostu nazwę encji.
    //
    // DOBRA PRAKTYKA: agregaty licz w bazie (count, sum, group by), a nie w Javie po wczytaniu wszystkich encji.
    //   Dlaczego? Baza zwraca kilka liczb zamiast tysięcy wierszy — mniej danych, mniej pamięci, mniej czasu.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @Query("select p from Product p where p.price > :min")} — JPQL: ENCJE i POLA, nie tabele i kolumny.
     *     Sprawdzany przy starcie (błąd → aplikacja nie startuje). @Param zbędne z -parameters (Boot je włącza).
     *   • Filtr opcjonalny: {@code (:category is null or p.category = :category)}.
     *   • {@code nativeQuery = true} — SQL bazy bez tłumaczenia; NIE sprawdzany przy starcie; nieprzenośny → test.
     *   • Projekcje: interfejs z getterami (aliasy w @Query — bez nich po cichu null!), rekord (select new … albo bez
     *     new w Spring Data 2026; kolejność kolumn = kolejność parametrów konstruktora), dynamiczna
     *     {@code <T> List<T> find…(…, Class<T>)}. Mniej kolumn, zero zarządzanych encji. @Value (otwarta) = cała encja.
     *   • {@code @Modifying} + update/delete → int (liczba wierszy), jedno polecenie SQL, wymaga transakcji;
     *     bez @Modifying → InvalidDataAccessApiUsageException przy wywołaniu.
     *   • Po masowym UPDATE encje w pamięci są nieaktualne → {@code clearAutomatically = true}
     *     (+ {@code flushAutomatically = true}, żeby clear nie wyrzucił niezapisanych zmian).
     *   • Agregaty: count → Long, avg → Double, max → typ pola, sum → BigDecimal/Long; bez wierszy null (coalesce).
     *   • {@code #{#entityName}} — nazwa encji w repozytoriach bazowych.
     *
     * PYTANIA KONTROLNE:
     *   1. Czym różni się JPQL od SQL? Dlaczego w JPQL piszesz {@code Product} i {@code p.createdAt}?
     *   2. ZNAJDŹ BŁĄD: {@code @Query("select p from product p where p.created_at > :since")}. Kiedy zobaczysz błąd?
     *   3. Co się stanie? Literówka w nazwie kolumny w zapytaniu z {@code nativeQuery = true} — kiedy i jaki błąd?
     *   4. Kiedy użyjesz natywnego SQL i jaki jest jego największy koszt?
     *   5. Co się stanie? {@code Product p = findById(5)} (cena 24.50), potem {@code changePrices(HOME, 2)} bez
     *      clearAutomatically, potem {@code findById(5).getPrice()}. Jaka cena i dlaczego?
     *   6. ZNAJDŹ BŁĄD: {@code @Query("update Product p set p.stock = 0 where p.id = :id") int reset(Long id);}
     *   7. Dlaczego projekcja jest szybsza od wczytania encji i czego z nią nie zrobisz?
     *   8. Jakiego typu jest wynik {@code avg(p.price)} i {@code max(p.price)} dla kategorii bez produktów?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ExerciseProductRepository.java (metody default z TODO), testy: Jpa04CustomQueriesExercisesTest.
    // Dopisujesz metody z @Query (i @Modifying) w interfejsie i wywołujesz je w ciałach metod default.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. SQL pyta o tabele i kolumny konkretnej bazy; JPQL pyta o encje i ich pola, niezależnie od bazy — Hibernate
     *      tłumaczy go na SQL według mapowania (Product → product, createdAt → created_at) i dialektu bazy.
     *   2. Dwa błędy: tabela {@code product} zamiast encji {@code Product} i kolumna {@code created_at} zamiast pola
     *      {@code createdAt}. Zobaczysz je przy STARCIE aplikacji: BeanCreationException ← QueryCreationException
     *      „Query validation failed” ← UnknownEntityException „Could not resolve root entity 'product'” (sprawdzone
     *      testem Jpa04BrokenQueriesTest).
     *   3. Aplikacja wystartuje normalnie; błąd dopiero przy wywołaniu metody: InvalidDataAccessResourceUsageException
     *      z komunikatem bazy (H2: „Column "STOK" not found”). Sprawdzone testem Jpa04BrokenQueriesTest.
     *   4. Gdy potrzebujesz funkcji specyficznych dla bazy (wyrażenia regularne, funkcje okna, JSON, WITH RECURSIVE).
     *      Koszt: przenośność (inna baza = inny SQL) i brak sprawdzania przy starcie — potrzebny test na właściwej bazie.
     *   5. 24.50. Masowy UPDATE zmienił tylko bazę; findById zwraca TEN SAM obiekt z kontekstu utrwalania (pamięci
     *      1. poziomu), bez zapytania do bazy. Naprawa: {@code @Modifying(clearAutomatically = true)} albo
     *      {@code em.clear()}/{@code em.refresh(p)}. Sprawdzone testem (sekcja 4).
     *   6. Brak {@code @Modifying}: Spring Data wykona UPDATE jak SELECT → InvalidDataAccessApiUsageException „… must be
     *      a 'select' query” przy wywołaniu (start działa). Do tego potrzebna transakcja. Sprawdzone testem (sekcja 4).
     *   7. Pobiera tylko potrzebne kolumny i nie tworzy encji w kontekście (brak dirty checking i kosztu pamięci).
     *      Nie zmienisz przez nią danych (nie jest zarządzana) i nie przejdziesz po relacjach jak po encji.
     *   8. avg → Double, max → BigDecimal; dla kategorii bez produktów oba dają null (count dałby 0). Sprawdzone
     *      testem (sekcja 5: maxPriceIn(GARDEN) == null, avg dla GARDEN == null).
     */
    // </editor-fold>
}
