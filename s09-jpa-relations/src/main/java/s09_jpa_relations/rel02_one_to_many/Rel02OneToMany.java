package s09_jpa_relations.rel02_one_to_many;

import jakarta.persistence.EntityManager;

/**
 * <pre>
 * TEMAT: Relacja jeden-do-wielu — @OneToMany(mappedBy), strona właścicielska, kaskady i sieroty
 *        (one-to-many = jeden do wielu; bidirectional = dwukierunkowa; mapped by = zmapowane przez;
 *         owning side = strona właścicielska; inverse side = strona odwrotna; cascade = kaskada;
 *         orphan removal = usuwanie sierot)
 *
 * W SKRÓCIE:
 *   Zamówienie ma wiele pozycji. W bazie to nadal JEDEN klucz obcy: order_lines.order_id. W Javie chcemy jednak iść
 *   w obie strony: line.getOrder() i order.getLines(). Relacja dwukierunkowa to dwa pola opisujące TEN SAM klucz
 *   obcy. Hibernate musi wiedzieć, które pole jest „prawdziwe” — to strona właścicielska (@ManyToOne w OrderLine).
 *   Druga strona (@OneToMany(mappedBy = "order") w ShopOrder) jest tylko lustrem: jej zmiany Hibernate POMIJA przy
 *   zapisie. Dlatego obie strony ustawiamy razem, w metodach pomocniczych addLine/removeLine. Kaskada (cascade)
 *   przenosi operacje z zamówienia na pozycje, a orphanRemoval usuwa pozycje wyjęte z listy.
 *
 * ANALOGIA: dziennik szkolny i legitymacje.
 *   O tym, do której klasy chodzi uczeń, decyduje wpis w JEGO legitymacji (klucz obcy — strona właścicielska).
 *   Lista uczniów w dzienniku klasy to tylko odbicie tych wpisów (strona odwrotna). Dopiszesz ucznia długopisem do
 *   dziennika, ale nie zmienisz legitymacji — w sekretariacie (bazie) uczeń dalej nie ma klasy. Porządny wychowawca
 *   (metoda addLine) zmienia oba dokumenty naraz.
 *
 * JAK TO DZIAŁA:
 *   ShopOrder:  @OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true) List{@code <OrderLine>} lines
 *   OrderLine:  @ManyToOne(fetch = LAZY, optional = false) @JoinColumn(name = "order_id") ShopOrder order
 *     flush → Hibernate patrzy na line.order (właściciel) i z niego bierze order_id do INSERT/UPDATE
 *           → lista order.lines służy tylko do: kaskad, orphanRemoval i odczytu (SELECT ... WHERE order_id=?)
 *     persist(order)      → cascade PERSIST: persist każdej pozycji z listy
 *     remove(order)       → cascade REMOVE: DELETE pozycji, potem DELETE zamówienia
 *     lines.remove(line)  → orphanRemoval: DELETE FROM order_lines WHERE id=?
 *
 * SŁÓWKA:
 *   one-to-many = jeden do wielu; mapped by = zmapowane przez (wskazuje pole po drugiej stronie); owning side = strona
 *   właścicielska; inverse side = strona odwrotna; cascade = kaskada (przenoszenie operacji); cascade type = rodzaj
 *   kaskady (PERSIST — utrwal, MERGE — scal, REMOVE — usuń, REFRESH — odśwież, DETACH — odłącz, ALL — wszystkie);
 *   orphan = sierota (pozycja bez zamówienia); helper method = metoda pomocnicza; bag = worek (lista bez kolejności);
 *   persistent bag = trwały worek (kolekcja Hibernate); unmodifiable = niemodyfikowalny.
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel01ManyToOne (strona właścicielska @ManyToOne), s08_data_jpa/Jpa01Entities
 *             (persist, flush, dirty checking), t29_jdbc_databases/Jdbc06SqlJoins (relacja 1:N w SQL),
 *             s09_jpa_relations/Rel03ManyToMany, s09_jpa_relations/Rel05NPlusOne (czytanie kolekcji wydajnie).
 * </pre>
 */
public final class Rel02OneToMany {

    private Rel02OneToMany() {
    }

    // =================================================================================================
    // 1. Strona właścicielska kontra strona odwrotna — co NAPRAWDĘ trafia do bazy
    // =================================================================================================
    //
    // W relacji dwukierunkowej mappedBy stoi po stronie, która NIE ma klucza obcego. mappedBy = "order" znaczy:
    // „ten związek opisuje już pole order w klasie OrderLine — ja jestem tylko jego lustrem”.
    //
    // NaiveOrder (PRZED) — dopisujemy pozycję tylko do listy:
    //     order.getLines().add(line);    // line.order == null
    //     em.persist(order);             // cascade PERSIST zapisuje też pozycję
    //     → INSERT INTO naive_order_lines (order_id, ...) VALUES (NULL, ...)
    // Wiersz pozycji jest w bazie, ale z order_id = NULL. Po ponownym odczycie zamówienie „nie ma” pozycji.
    // (W tabeli order_lines order_id jest NOT NULL — tam ten sam błąd skończyłby się wyjątkiem, co jest lepsze niż
    // cichy NULL; naive_order_lines celowo pozwala na NULL, żeby było widać skutek.)
    //
    // Odwrotnie: ustawiasz TYLKO line.setOrder(order) → baza jest poprawna (właściciel jest ustawiony), ale lista
    // order.getLines() w tym samym obiekcie w pamięci dalej jest pusta — aż do ponownego odczytu z bazy.
    //
    // PUŁAPKA: „dodałem pozycję do zamówienia, a w bazie jej nie ma przy zamówieniu”. Dlaczego? Zmieniłeś tylko stronę
    //   odwrotną (listę). Hibernate bierze klucz obcy WYŁĄCZNIE ze strony właścicielskiej — z pola line.order.
    //
    // PUŁAPKA: @OneToMany BEZ mappedBy (relacja jednokierunkowa od strony „jeden”) to dla Hibernate OSOBNA relacja,
    //   której właścicielem jest lista. Bez @JoinColumn Hibernate szuka wtedy TABELI ŁĄCZĄCEJ; Spring Boot
    //   (SpringImplicitNamingStrategy — domyślna strategia nazw) nazywa ją: tabela właściciela + „_” + nazwa pola.
    //   Sprawdzone uruchomieniem: zmiana w ShopOrder na @OneToMany(cascade = CascadeType.ALL) (bez mappedBy) przy
    //   ddl-auto=validate kończy start błędem
    //       org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: missing table [orders_lines]
    //   Dlaczego to ważne? Zapomniane mappedBy w relacji dwukierunkowej daje DWA niezależne powiązania i dodatkową
    //   tabelę. Przy ddl-auto=create/update Hibernate po cichu by ją UTWORZYŁ — dlatego validate + Flyway.

    // =================================================================================================
    // 2. Metody pomocnicze — obie strony zawsze razem
    // =================================================================================================
    //
    // public void addLine(OrderLine line) {          public void removeLine(OrderLine line) {
    //     lines.add(line);                               lines.remove(line);
    //     line.setOrder(this);                           line.setOrder(null);
    // }                                              }
    //
    // getLines() zwraca Collections.unmodifiableList(lines) — z zewnątrz NIE da się zrobić getLines().add(...),
    // a setOrder w OrderLine nie jest publiczny. Jedyna droga zmiany relacji to metody pomocnicze.
    //
    // DOBRA PRAKTYKA: w relacji dwukierunkowej dawaj metody add.../remove... po stronie „jeden” i chowaj gołe
    //   modyfikatory. Dlaczego? Obiekty w pamięci i baza zawsze mówią to samo; nie da się „zapomnieć” o drugiej
    //   stronie, bo nie ma jak jej ominąć.
    //
    // DOBRA PRAKTYKA: zastanów się, czy druga strona jest w ogóle potrzebna. Dlaczego? Relacja jednokierunkowa
    //   @ManyToOne (Rel01ManyToOne) jest prostsza: jedno pole, zero synchronizacji. @OneToMany dodawaj wtedy, gdy
    //   naprawdę traktujesz pozycje jako część zamówienia (kaskady, orphanRemoval, wygodna nawigacja).

    // =================================================================================================
    // 3. cascade = CascadeType.ALL — operacje „spływają” z zamówienia na pozycje
    // =================================================================================================
    //
    // orders.save(order)    → persist zamówienia i (kaskada PERSIST) każdej pozycji z listy: 1 + 2 INSERT-y
    // orders.delete(order)  → (kaskada REMOVE) DELETE pozycji, potem DELETE zamówienia — klucz obcy nie protestuje
    //
    // Kaskada ma sens tam, gdzie „dziecko” nie istnieje bez „rodzica”: pozycja zamówienia bez zamówienia nie ma
    // znaczenia. Mówi się, że zamówienie jest korzeniem agregatu (aggregate root), a pozycje — jego częścią.
    //
    // PUŁAPKA: cascade = ALL (albo REMOVE) na relacji do encji, która żyje SAMODZIELNIE (kategoria, klient, tag).
    //   Dlaczego? Usunięcie zamówienia usunęłoby klienta, a z nim... jego inne zamówienia (Rel03ManyToMany pokazuje
    //   to na @ManyToMany). Kaskady dawaj tylko od rodzica do dzieci, nigdy „w górę” ani „w bok”.

    // =================================================================================================
    // 4. orphanRemoval = true — sierota idzie do kosza
    // =================================================================================================
    //
    // order.removeLine(coffee); em.flush();  → DELETE FROM order_lines WHERE id=?
    // Pozycja wyjęta z kolekcji staje się „sierotą” (orphan) i Hibernate ją usuwa. Bez orphanRemoval (NaiveOrder)
    // usunięcie z listy NIE zmienia niczego w bazie — wiersz zostaje, a order_id dalej wskazuje zamówienie
    // (lista to strona odwrotna, sprawdzone testem withoutOrphanRemovalNothingHappens).
    //
    // orphanRemoval kontra CascadeType.REMOVE: REMOVE działa, gdy usuwasz RODZICA (pozycje idą razem z nim);
    // orphanRemoval — gdy rodzic zostaje, a z jego kolekcji znika dziecko. orphanRemoval = true obejmuje też REMOVE
    // (tak mówi specyfikacja JPA; sprawdzone uruchomieniem: z cascade = PERSIST + orphanRemoval = true usunięcie
    // zamówienia i tak wysyła DELETE pozycji).
    //
    // Kolejność SQL przy orders.delete(order) (z logu): najpierw dzieci, potem rodzic — klucz obcy nie protestuje:
    //     delete from order_lines where id=?    (raz na każdą pozycję)
    //     delete from orders where id=?
    //
    // PUŁAPKA: przy orphanRemoval nie podmieniaj całej kolekcji (this.lines = nowaLista). Dlaczego? Hibernate pilnuje
    //   SWOJEJ instancji kolekcji (po persist pole lines to już PersistentBag, a nie ArrayList); gdy zarządzana
    //   encja dostanie inną listę, flush w Hibernate 7 kończy się błędem
    //       org.hibernate.HibernateException: A collection with orphan deletion was no longer referenced by the
    //       owning entity instance: s09_jpa_relations.rel02_one_to_many.ShopOrder.lines
    //   (sprawdzone testem replacingOrphanRemovalCollectionFails). Czyść ją (clear) i dodawaj elementy, zamiast
    //   podmieniać.

    // =================================================================================================
    // 5. List czy Set?
    // =================================================================================================
    //
    // Kolekcję wczytaną z bazy Hibernate podmienia na własną: List bez @OrderColumn → PersistentBag (worek: bez
    // zapisanej kolejności, duplikaty dozwolone), Set → PersistentSet. Dlatego pole deklarujesz jako INTERFEJS
    // (List/Set), nigdy jako ArrayList/HashSet.
    //   • List — wygodna, w @OneToMany(mappedBy) działa dobrze; kolejność gwarantuje dopiero @OrderBy("productName")
    //     (sortowanie w SELECT) albo @OrderColumn (osobna kolumna z numerem pozycji).
    //   • Set — brak duplikatów, ale wymaga poprawnych equals/hashCode encji (stały hashCode — s08_data_jpa/Jpa01Entities).
    // Kiedy List boli: na stronie właścicielskiej @ManyToMany (Rel03ManyToMany) i przy pobieraniu dwóch list naraz
    // (MultipleBagFetchException — Rel05NPlusOne).

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dwukierunkowa 1:N = @OneToMany(mappedBy = "pole") po stronie „jeden” + @ManyToOne @JoinColumn po stronie „wiele”.
     *   • Do bazy trafia tylko strona WŁAŚCICIELSKA (ta z @JoinColumn); lista z mappedBy to lustro.
     *   • Metody addX/removeX ustawiają obie strony; getter oddaje listę niemodyfikowalną.
     *   • cascade = ALL — tylko od rodzica do dzieci, które bez niego nie istnieją.
     *   • orphanRemoval = true — dziecko wyjęte z kolekcji = DELETE; nie podmieniaj instancji kolekcji.
     *   • Brak mappedBy w @OneToMany → Hibernate szuka tabeli łączącej (rodzic_pole).
     *   • Pole kolekcji jako interfejs (List/Set), od razu zainicjowane: = new ArrayList{@code <>}().
     *
     * PYTANIA KONTROLNE:
     *   1. Która strona relacji ShopOrder–OrderLine jest właścicielska i po czym to poznać w kodzie?
     *   2. Co się stanie? order.getLines().add(line) w NaiveOrder (bez line.setOrder), potem persist(order) i flush —
     *      co będzie w kolumnie order_id?
     *   3. ZNAJDŹ BŁĄD: w ShopOrder ktoś zmienił adnotację na {@code @OneToMany(cascade = CascadeType.ALL)} (bez mappedBy).
     *      Co się stanie przy starcie aplikacji z ddl-auto=validate?
     *   4. Czym różni się orphanRemoval = true od cascade = CascadeType.REMOVE?
     *   5. Co się stanie? W NaiveOrder (bez orphanRemoval) usuwasz pozycję z listy i robisz flush.
     *   6. Dlaczego getLines() w ShopOrder zwraca listę niemodyfikowalną?
     *   7. Dlaczego nie dajemy cascade = ALL na @ManyToOne z OrderLine do ShopOrder?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rel02OneToManyExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): utwórz zamówienie klienta customerName z dwiema pozycjami: „Kawa” × 2 i „Herbata” × 1.
     * Zapisz je JEDNYM wywołaniem orders.save(...) i zwróć id zamówienia. Podpowiedź: addLine + kaskada.
     */
    static Long exercise1(ShopOrderRepository orders, String customerName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): usuń z zamówienia orderId pozycję o nazwie productName tak, żeby zniknęła z bazy.
     * Podpowiedź: em.find, w getLines() znajdź pozycję (stream + filter), removeLine — resztę zrobi orphanRemoval.
     */
    static void exercise2(EntityManager em, Long orderId, String productName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): w bazie jest „osierocona” pozycja NaiveOrderLine (order_id = NULL). Podepnij ją do
     * zamówienia NaiveOrder o id orderId tak, żeby (a) kolumna order_id została zapisana i (b) lista getLines() tego
     * zamówienia od razu ją zawierała. Podpowiedź: dwie strony — setOrder (właściciel) i getLines().add (lustro).
     */
    static void exercise3(EntityManager em, Long orderId, Long lineId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. OrderLine — ma @ManyToOne z @JoinColumn(name = "order_id"); jej tabela trzyma klucz obcy. Strona
     *      odwrotna ma mappedBy.
     *   2. Pozycja zostanie zapisana (kaskada PERSIST), ale z order_id = NULL; po ponownym odczycie zamówienie ma pustą
     *      listę. Sprawdzone testem inverseSideAloneDoesNotWriteForeignKey.
     *   3. Bez mappedBy lista staje się osobną relacją jednokierunkową z tabelą łączącą (orders_lines). Takiej tabeli
     *      nie ma w migracjach, więc ddl-auto=validate przerwie start: SchemaManagementException „Schema validation:
     *      missing table [orders_lines]”. Sprawdzone uruchomieniem (tymczasowa zmiana adnotacji w ShopOrder).
     *   4. REMOVE: usuwasz rodzica → usuwane są dzieci. orphanRemoval: rodzic zostaje, a dziecko wyjęte z jego kolekcji
     *      jest usuwane (orphanRemoval obejmuje też przypadek REMOVE).
     *   5. Nic w bazie się nie zmieni: wiersz pozycji zostaje, order_id dalej wskazuje zamówienie — lista to strona
     *      odwrotna. Sprawdzone testem withoutOrphanRemovalNothingHappens.
     *   6. Żeby nikt nie ominął addLine/removeLine i nie zmienił tylko jednej strony relacji.
     *   7. Kaskada od dziecka do rodzica: usunięcie jednej pozycji usunęłoby całe zamówienie (a kaskadą z niego —
     *      wszystkie inne pozycje). Kaskady idą od rodzica do dzieci.
     */
    // </editor-fold>
}
