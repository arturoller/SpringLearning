package s09_jpa_relations.rel01_many_to_one;

/**
 * <pre>
 * TEMAT: Relacja „wiele do jednego” — {@code @ManyToOne} i {@code @JoinColumn}: zamówienie należy do klienta
 *        (many to one = wiele do jednego; join column = kolumna złączenia, czyli klucz obcy; relationship = relacja)
 *
 * W SKRÓCIE:
 *   W bazie relacja to KLUCZ OBCY (foreign key): kolumna {@code orders.customer_id} wskazuje wiersz w {@code customer}.
 *   W Javie ta sama relacja to POLE z obiektem: {@code ShopOrder.customer}. {@code @ManyToOne} mówi Hibernate:
 *   „to pole to klient, a jego id trzymasz w kolumnie z {@code @JoinColumn}”. Domyślnie JPA pobiera takie pole
 *   ZACHŁANNIE (EAGER) — przy liście zamówień to oznacza dodatkowe zapytanie na każdego klienta (N+1). Dlatego
 *   zawsze piszemy {@code fetch = FetchType.LAZY}. Przypisanie istniejącego klienta nie wymaga SELECT-a
 *   ({@code getReferenceById}), a usunięcie klienta, który ma zamówienia, blokuje baza (klucz obcy).
 *
 * ANALOGIA: paragon i karta klienta.
 *   Na każdym paragonie (zamówieniu) jest NUMER karty klienta — nie cała karta z adresem i zdjęciem. To klucz obcy.
 *   Kasjer, który chce wiedzieć, jak klient się nazywa, idzie do kartoteki (osobny SELECT). EAGER to kasjer, który
 *   przy KAŻDYM paragonie leci do kartoteki, choć nikt nie pytał o nazwisko. LAZY to kasjer, który idzie tylko wtedy,
 *   gdy ktoś naprawdę zapyta. getReferenceById to „wiem, że karta nr 2 istnieje — wpisuję numer i nie sprawdzam”.
 *   A wyrzucenie karty klienta, do której są paragony, blokuje szef (baza): „najpierw paragony!”.
 *
 * JAK TO DZIAŁA:
 *   1. Przy starcie Hibernate czyta mapowanie: {@code ShopOrder.customer} → kolumna {@code customer_id} → klucz
 *      główny encji {@code Customer}. Z {@code ddl-auto=validate} sprawdza, że kolumna istnieje (typ BIGINT).
 *   2. Wczytanie zamówienia ({@code find}): Hibernate czyta wiersz {@code orders} RAZEM z wartością customer_id.
 *      EAGER: od razu dociąga klienta (JOIN albo osobny SELECT). LAZY: wkłada w pole PROXY (obiekt-pośrednik,
 *      podklasa {@code Customer$HibernateProxy}), które zna tylko id i wczyta resztę przy pierwszym getterze.
 *   3. Zapis: Hibernate bierze id z obiektu w polu {@code customer} i wpisuje do {@code customer_id}. Obiekt musi być
 *      ZARZĄDZANY (managed) albo być proxy — nowego, niezapisanego (transient) klienta Hibernate nie zapisze sam.
 *   4. Zapytania: JPQL {@code o.customer.email} = JOIN do customer; {@code o.customer.id} = sama kolumna customer_id.
 *
 * SŁÓWKA:
 *   foreign key = klucz obcy; owning side = strona właściciela (ta z kluczem obcym); fetch = pobieranie;
 *   eager = zachłannie (od razu); lazy = leniwie (przy pierwszym użyciu); proxy = obiekt-pośrednik (zastępca);
 *   reference = referencja (odwołanie); transient = przejściowy (nowy, jeszcze nie zapisany obiekt);
 *   managed = zarządzany (w kontekście utrwalania); optional = opcjonalny; cascade = kaskada (przenoszenie operacji);
 *   constraint = ograniczenie (reguła w bazie); integrity = spójność; N+1 = jedno zapytanie + N dodatkowych.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa01Entities (encja, kontekst utrwalania, equals/hashCode encji),
 *             s08_data_jpa/Jpa03QueryMethods (metody z nazw — tu z przejściem po relacji),
 *             s08_data_jpa/Jpa06FlywayWithJpa (migracje + validate),
 *             s07_data_jdbc/Jdbc02JdbcTemplate (relacja ręcznie: JOIN i ResultSetExtractor),
 *             s09_jpa_relations/Rel02OneToMany (druga strona: lista zamówień w kliencie),
 *             s09_jpa_relations/Rel04LazyLoading (proxy i LazyInitializationException),
 *             s09_jpa_relations/Rel05NPlusOne (N+1 i jak go usunąć).
 * </pre>
 */
public final class Rel01ManyToOne {

    private Rel01ManyToOne() {
    }

    // =================================================================================================
    // 1. @ManyToOne + @JoinColumn — klucz obcy jako pole z obiektem (ShopOrder.java, migracja V1)
    // =================================================================================================
    //
    //   W bazie (db/migration/rel01/V1):                  W Javie (ShopOrder):
    //     orders.customer_id BIGINT NOT NULL                {@code @ManyToOne(fetch = FetchType.LAZY, optional = false)}
    //     FOREIGN KEY (customer_id) REFERENCES customer     {@code @JoinColumn(name = "customer_id", nullable = false)}
    //                                                       {@code private Customer customer;}
    //
    // Klucz obcy jest po stronie „wiele” (orders) — tak działa każda baza relacyjna: jeden klient nie ma kolumny
    // „lista zamówień”, to każde zamówienie mówi, czyje jest. Encja z polem odpowiadającym kluczowi obcemu to STRONA
    // WŁAŚCICIELA (owning side): TYLKO jej pole decyduje, co Hibernate wpisze do customer_id (ważne w Rel02).
    //
    // Klient (Customer) nic nie wie o zamówieniach — relacja JEDNOKIERUNKOWA (unidirectional). To w zupełności
    // wystarcza w większości przypadków; listę zamówień klienta dostaniesz zapytaniem (sekcja 4).
    //
    // Bez @JoinColumn Hibernate wymyśliłby nazwę kolumny sam: pole + "_" + kolumna id celu → {@code customer_id}.
    // Tu by się zgadzało, ale DOBRA PRAKTYKA: pisz @JoinColumn(name = …) jawnie. Dlaczego? Nazwa kolumny jest częścią
    // umowy z migracją Flyway — zmiana nazwy pola w Javie nie może po cichu zmienić nazwy kolumny.
    //
    // Po co {@code nullable = false} w @JoinColumn, skoro NOT NULL jest w migracji? Hibernate używa go przy
    // generowaniu schematu (ddl-auto=create), a — tak jak {@code optional = false} — mówi mu, że klient ZAWSZE jest:
    // może dociągać klienta przez INNER JOIN ({@code join customer}, nie {@code left join} — widać w SQL sekcji 2)
    // i sprawdzić null przed wysłaniem INSERT-a (sekcja 3).
    //
    // DOBRA PRAKTYKA: indeks na kolumnie klucza obcego (V1: {@code CREATE INDEX ix_orders_customer_id}). Dlaczego?
    //   „Zamówienia klienta X” i sprawdzanie FK przy usuwaniu klienta szukają po customer_id. H2 i MySQL zakładają
    //   indeks same, PostgreSQL NIE — bez niego każde takie zapytanie czyta całą tabelę.

    // =================================================================================================
    // 2. POBIERANIE: domyślne EAGER (JPA) kontra LAZY — SQL i N+1 (EagerShopOrder kontra ShopOrder)
    // =================================================================================================
    //
    // Specyfikacja JPA: relacje „do jednego” (@ManyToOne, @OneToOne) mają domyślnie {@code FetchType.EAGER},
    // relacje „do wielu” (@OneToMany, @ManyToMany) — LAZY. Porównanie (SQL sprawdzony testem z SqlCapture):
    //
    //   EagerShopOrder (domyślne EAGER), em.find(…, 1) — JEDNO zapytanie z JOIN:
    //     {@code select eso1_0.id,eso1_0.customer_id,c1_0.id,c1_0.email,c1_0.name,eso1_0.order_number}
    //     {@code from orders eso1_0 join customer c1_0 on c1_0.id=eso1_0.customer_id where eso1_0.id=?}
    //
    //   EagerShopOrder, JPQL {@code select o from EagerShopOrder o order by o.orderNumber} — 1 + 3 zapytania:
    //     {@code select eso1_0.id,eso1_0.customer_id,eso1_0.order_number from orders eso1_0 order by …}
    //     {@code select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?}   ← klient 1
    //     {@code select … from customer c1_0 where c1_0.id=?}                              ← klient 2
    //     {@code select … from customer c1_0 where c1_0.id=?}                              ← klient 3
    //
    // Dlaczego JPQL nie zrobił JOIN-a? find() Hibernate buduje sam i może dołożyć JOIN. Zapytanie JPQL wykonuje
    // DOKŁADNIE tak, jak napisano (bez joina), a dopiero potem widzi, że EAGER każe mieć klientów „od razu” — więc
    // dociąga ich osobnymi SELECT-ami. To jest problem N+1 (1 zapytanie o listę + N o powiązane obiekty), i to
    // dla relacji, której nikt nie użył! Pięć zamówień dało 3 dodatkowe SELECT-y, nie 5 — bo klient wczytany raz
    // siedzi w kontekście utrwalania (Jpa01, sekcja 3). Przy 1000 zamówień od 800 klientów: 801 zapytań.
    //
    //   ShopOrder (LAZY), em.find(…, 1) — bez joina:
    //     {@code select so1_0.id,so1_0.customer_id,so1_0.order_number,so1_0.ordered_on from orders so1_0 where so1_0.id=?}
    //   order.getCustomer()           → obiekt klasy {@code Customer$HibernateProxy}, Hibernate.isInitialized = false
    //   order.getCustomer().getId()   → 0 zapytań (proxy ZNA id — wzięło je z customer_id)
    //   order.getCustomer().getName() → teraz {@code select c1_0.id,c1_0.email,c1_0.name from customer c1_0 where c1_0.id=?}
    //   lista zamówień JPQL           → 1 zapytanie (klientów nikt nie dotknął, więc nikt ich nie czyta)
    //
    // A gdy klienci SĄ potrzebni przy liście? Wtedy mówisz to jawnie w zapytaniu: {@code join fetch o.customer}
    // (ShopOrderRepository.findAllWithCustomer) → jedno zapytanie z JOIN. Szczegóły: s09_jpa_relations/Rel05NPlusOne.
    //
    // DOBRA PRAKTYKA: {@code @ManyToOne(fetch = FetchType.LAZY)} i {@code @OneToOne(fetch = FetchType.LAZY)} ZAWSZE.
    //   Dlaczego? (1) EAGER nie da się „wyłączyć” w konkretnym zapytaniu — LAZY da się „włączyć” (join fetch,
    //   @EntityGraph). (2) EAGER zamienia każde zapytanie JPQL o listę w N+1. (3) EAGER na łańcuchu relacji
    //   (zamówienie → klient → adres → kraj…) potrafi wczytać pół bazy przy jednym find().
    //   PYTANIE REKRUTACYJNE: „Jaki jest domyślny FetchType @ManyToOne i dlaczego warto go zmienić?”
    //
    // PUŁAPKA: {@code toString()} (albo logowanie) wypisujące relację. Dlaczego groźne? {@code "" + order.getCustomer()}
    //   wywołuje toString na proxy → proxy się inicjalizuje → dodatkowy SELECT (sprawdzone testem). Log
    //   „Zapisano zamówienie {}” z takim toString robi zapytania do bazy, a poza transakcją rzuca
    //   LazyInitializationException (Rel04). Dlatego ShopOrder.toString celowo pomija klienta. To samo dotyczy
    //   equals/hashCode: tylko id, nigdy pola relacji. Lombok {@code @Data}/{@code @ToString} generuje toString,
    //   equals i hashCode ze WSZYSTKICH pól, łącznie z relacjami — na encjach go nie używamy
    //   (t20_lombok/Lombok03DataValueBuilder; w Rel02 zobaczysz, jak kończy się to przy relacji dwukierunkowej).

    // =================================================================================================
    // 3. ZAPIS: istniejący klient przez getReferenceById, nowy (transient) klient bez kaskady
    // =================================================================================================
    //
    //   {@code Customer jan = customers.getReferenceById(2L);}       // 0 zapytań — samo proxy z id = 2
    //   {@code orders.save(new ShopOrder("ZAM-100", date, jan));}
    //   SQL (sprawdzone testem — JEDNO polecenie, żadnego SELECT-a z customer):
    //   {@code insert into orders (customer_id,order_number,ordered_on,id) values (?,?,?,default)}
    //
    // Do zapisu zamówienia Hibernate potrzebuje tylko ID klienta. {@code findById(2L)} zrobiłby niepotrzebny SELECT
    // całego klienta. {@code getReferenceById} (dawniej {@code getOne}/{@code getById} — przestarzałe, zobaczysz je
    // w starych tutorialach) zwraca proxy BEZ zapytania. Jeśli klienta o tym id nie ma, dowiesz się dopiero przy
    // INSERT (naruszenie klucza obcego) albo przy pierwszym getterze proxy (EntityNotFoundException) — dlatego używaj
    // go, gdy id pochodzi z zaufanego miejsca (np. z bazy, z zalogowanego użytkownika), a przy danych od klienta API
    // sprawdź istnienie (findById / existsById) i zwróć czytelny błąd.
    //
    // Sprawdzone testem: nieistniejące id ({@code getReferenceById(99L)}) przy zapisie zamówienia → INSERT idzie do bazy
    // i dostajesz DataIntegrityViolationException (naruszenie FK_ORDERS_CUSTOMER); przy getterze proxy
    // ({@code getReferenceById(99L).getName()}) → SELECT i {@code jakarta.persistence.EntityNotFoundException}
    // „No row with the given identifier exists for entity […Customer with id '99']”.
    //
    // {@code optional = false}: dla Hibernate to obietnica „klient zawsze jest”. Zapis zamówienia z {@code customer = null}
    // kończy się błędem jeszcze PRZED wysłaniem INSERT-a (sprawdzone testem: do bazy nie poszło nic):
    // DataIntegrityViolationException ← {@code org.hibernate.PropertyValueException} „not-null property references
    // a null or transient value for entity …ShopOrder.customer”. Hibernate sam pilnuje NOT NULL — zamiast błędu bazy.
    //
    // PUŁAPKA: przypisanie NOWEGO, niezapisanego klienta ({@code new Customer(…)}) i save zamówienia. Dlaczego błąd?
    //   Relacja nie ma kaskady (cascade — Rel02), więc Hibernate nie zapisze klienta za Ciebie, a bez jego id nie ma
    //   czego wpisać do customer_id. Sprawdzone testem: już {@code save()} (nie dopiero flush — przy IDENTITY persist
    //   wysyła INSERT od razu) rzuca {@code InvalidDataAccessApiUsageException} (tłumaczenie Springa) ← IllegalStateException
    //   ← {@code org.hibernate.TransientPropertyValueException}: „Instance of '…ShopOrder' references an unsaved
    //   transient instance of '…Customer' (persist the transient instance)”. Do bazy nie poszło nic.
    //   Rozwiązanie: najpierw {@code customers.save(customer)}, potem zamówienie. Kaskady na @ManyToOne
    //   (cascade = PERSIST) NIE dodawaj — „dziecko” nie powinno zarządzać życiem „rodzica” (Rel02, sekcja 3).

    // =================================================================================================
    // 4. ZAPYTANIA PO RELACJI — kiedy JOIN, a kiedy sama kolumna (ShopOrderRepository)
    // =================================================================================================
    //
    //   metoda                                          SQL (sprawdzony testem)
    //   findByCustomerEmailOrderByOrderNumber(email)    {@code … from orders so1_0 join customer c1_0 on c1_0.id=so1_0.customer_id}
    //                                                   {@code where c1_0.email=? order by so1_0.order_number}
    //   {@code @Query("… where o.customer.id = :customerId")}   {@code … from orders so1_0 where so1_0.customer_id=? order by …}
    //   findByCustomerIdOrderByOrderNumber(id)          ten sam SQL co wyżej — bez joina
    //
    // Hibernate jest sprytny: {@code o.customer.id} to wartość, która JUŻ jest w tabeli orders (customer_id), więc
    // nie łączy tabel. Każde inne pole klienta ({@code email}, {@code name}) wymaga JOIN-a. Zwróć uwagę, że nawet
    // findByCustomerEmail… NIE wczytuje klienta (w SELECT są tylko kolumny orders) — JOIN służy tylko do warunku
    // WHERE, a customer w wyniku nadal jest leniwym proxy.
    //
    // DOBRA PRAKTYKA: gdy masz id rodzica, filtruj po id ({@code o.customer.id = :id}), nie po obiekcie wczytanym
    //   specjalnie w tym celu. Dlaczego? Oszczędzasz SELECT klienta i JOIN.

    // =================================================================================================
    // 5. USUWANIE klienta, który ma zamówienia — klucz obcy pilnuje spójności
    // =================================================================================================
    //
    //   {@code customers.deleteById(1L);}   // SELECT klienta (deleteById najpierw wczytuje encję)
    //   {@code customers.flush();}          // {@code delete from customer where id=?} → baza mówi NIE
    //
    // Sprawdzone testem: {@code DataIntegrityViolationException} (Spring) ← {@code org.hibernate.exception.ConstraintViolationException}
    // ← H2: „Referential integrity constraint violation: "FK_ORDERS_CUSTOMER: PUBLIC.ORDERS FOREIGN KEY(CUSTOMER_ID)
    // REFERENCES PUBLIC.CUSTOMER(ID) …"”. Hibernate NIE usuwa zamówień sam — relacja jest w ShopOrder, a klient
    // o zamówieniach nic nie wie. Błąd pojawia się przy flush (zwykle przy końcu transakcji), nie przy deleteById.
    //
    // PUŁAPKA: {@code TestEntityManager.flush()} / {@code EntityManager.flush()} zamiast {@code repository.flush()}.
    //   Dlaczego inny wyjątek? Tłumaczenie wyjątków na DataAccessException robi proxy repozytorium Springa (s07, s08).
    //   Bezpośredni flush na EntityManagerze daje „surowy” {@code org.hibernate.exception.ConstraintViolationException}
    //   (sprawdzone testem). W serwisach łap DataIntegrityViolationException — i flush rób przez repozytorium.
    //
    // Co zrobić, gdy klienta trzeba usunąć?
    //   • najpierw zamówienia (albo zakaz — w sklepie zamówień zwykle się nie usuwa: księgowość!), potem klient;
    //   • {@code ON DELETE CASCADE} w kluczu obcym (w migracji) — baza sama usunie zamówienia. Hibernate ma do tego
    //     {@code @OnDelete(action = OnDeleteAction.CASCADE)} na polu @ManyToOne: przy ddl-auto=create generuje taki
    //     klucz obcy i wie, że baza usuwa dzieci sama. Przy Flyway decyduje i tak migracja — tylko wzmianka;
    //   • „miękkie usuwanie” (soft delete — kolumna active/deleted_at) — klient znika z ekranów, historia zostaje.
    //
    // DOBRA PRAKTYKA: zostaw klucze obce w bazie, nawet gdy „Hibernate i tak pilnuje”. Dlaczego? Baza jest ostatnią
    //   linią obrony: chroni dane przed błędem w kodzie, ręcznym SQL-em i innymi aplikacjami na tej samej bazie.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • {@code @ManyToOne(fetch = FetchType.LAZY, optional = false)} + {@code @JoinColumn(name = "customer_id", nullable = false)}
     *     — pole z obiektem po stronie „wiele”; klucz obcy w JEJ tabeli; to strona właściciela.
     *   • Domyślnie @ManyToOne/@OneToOne = EAGER: find → JOIN, JPQL o listę → N+1 (osobny SELECT na każdego rodzica).
     *     Zawsze LAZY; gdy rodzic potrzebny — {@code join fetch} w zapytaniu.
     *   • LAZY → proxy ({@code Customer$HibernateProxy}); getId() bez zapytania, inny getter → SELECT.
     *   • toString/equals/hashCode bez pól relacji (dodatkowe zapytania, pętle). Lombok @Data na encji — nie.
     *   • {@code getReferenceById(id)} → proxy bez SELECT; zapis zamówienia = tylko INSERT.
     *   • Nowy (transient) rodzic bez kaskady → InvalidDataAccessApiUsageException ← TransientPropertyValueException.
     *   • {@code o.customer.id = :id} → sama kolumna customer_id (bez JOIN); {@code o.customer.email} → JOIN.
     *   • Usunięcie rodzica z dziećmi → DataIntegrityViolationException przy flush; rozwiązania: najpierw dzieci,
     *     ON DELETE CASCADE w migracji (@OnDelete), soft delete. Indeks na kolumnie FK.
     *
     * PYTANIA KONTROLNE:
     *   1. W której tabeli jest klucz obcy relacji klient 1—N zamówienie i dlaczego właśnie tam?
     *   2. Co się stanie? Encja z {@code @ManyToOne} bez atrybutu fetch, zapytanie JPQL o 5 zamówień trzech klientów.
     *      Ile zapytań SQL i dlaczego nie 1?
     *   3. Czym różni się {@code findById(2L)} od {@code getReferenceById(2L)} przy przypisywaniu klienta do zamówienia?
     *   4. Co się stanie? {@code orders.save(new ShopOrder("ZAM-9", date, new Customer("Nowy", "n@x.pl")))} — relacja
     *      bez kaskady. Kiedy i jaki wyjątek?
     *   5. ZNAJDŹ BŁĄD: {@code public String toString() { return "Order " + id + " klienta " + customer; }} w encji
     *      z leniwym {@code customer}.
     *   6. Które z zapytań zrobi JOIN: {@code where o.customer.id = :id} czy {@code where o.customer.email = :email}? Dlaczego?
     *   7. Co się stanie? {@code customers.deleteById(1L); customers.flush();}, gdy klient 1 ma zamówienia.
     *   8. Dlaczego warto ustawić {@code optional = false} i założyć indeks na kolumnie klucza obcego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku OrderExercises.java (metody z TODO), testy: Rel01ManyToOneExercisesTest.
    // Potrzebne zapytania dopisujesz w ExerciseShopOrderRepository.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. W tabeli orders (strona „wiele”): każde zamówienie ma dokładnie jednego klienta, więc jedna kolumna
     *      customer_id wystarcza. Klient ma wiele zamówień — w jego wierszu nie da się zapisać listy.
     *   2. 1 + 3 = 4 zapytania (sprawdzone testem): SELECT zamówień bez joina, potem osobny SELECT dla każdego
     *      RÓŻNEGO klienta (EAGER każe mieć go od razu, a JPQL nie miał joina). Drugi raz ten sam klient nie jest
     *      czytany — jest już w kontekście utrwalania. To N+1.
     *   3. findById robi SELECT całego klienta; getReferenceById zwraca proxy z samym id, bez zapytania — do zapisu
     *      klucza obcego to wystarczy (test: tylko INSERT). Nieistniejące id wyjdzie dopiero przy INSERT/getterze.
     *   4. Wyjątek już przy save (IDENTITY → INSERT od razu): InvalidDataAccessApiUsageException ← IllegalStateException
     *      ← TransientPropertyValueException „references an unsaved transient instance … (persist the transient
     *      instance)”. Do bazy nic nie poszło. Najpierw zapisz klienta. Sprawdzone testem.
     *   5. Doklejenie {@code customer} woła toString leniwego proxy → dodatkowy SELECT przy każdym logu (a poza
     *      transakcją LazyInitializationException); przy relacji dwukierunkowej — nieskończona rekurencja. W toString
     *      tylko proste pola (ewentualnie id klienta).
     *   6. {@code o.customer.email} — e-mail jest w tabeli customer. {@code o.customer.id} to kolumna customer_id
     *      w orders, więc Hibernate nie łączy tabel (sprawdzone testem — SQL bez join).
     *   7. deleteById wczytuje klienta (SELECT), flush wysyła DELETE, baza odrzuca go (klucz obcy):
     *      DataIntegrityViolationException ← ConstraintViolationException „Referential integrity constraint violation:
     *      FK_ORDERS_CUSTOMER …”. Zamówienia nie są usuwane automatycznie. Sprawdzone testem.
     *   8. optional = false: Hibernate wie, że rodzic zawsze jest — sprawdzi null przed INSERT i może użyć INNER JOIN.
     *      Indeks: wyszukiwanie zamówień klienta i sprawdzanie FK przy usuwaniu klienta nie czytają całej tabeli
     *      (PostgreSQL nie tworzy go sam).
     */
    // </editor-fold>
}
