package s09_jpa_relations.rel02_one_to_many;

/**
 * <pre>
 * TEMAT: Relacja „jeden do wielu” — {@code @OneToMany(mappedBy)}, relacja dwukierunkowa, strona właściciela,
 *        kaskady i {@code orphanRemoval}
 *        (one to many = jeden do wielu; mapped by = zmapowane przez; bidirectional = dwukierunkowa;
 *         owning side = strona właściciela; cascade = kaskada; orphan removal = usuwanie sierot)
 *
 * W SKRÓCIE:
 *   Zamówienie ma listę pozycji. W bazie to nadal JEDEN klucz obcy: {@code order_line.order_id}. W Javie relację widać
 *   z dwóch stron: {@code OrderLine.order} ({@code @ManyToOne}, jak w Rel01) i {@code ShopOrder.lines}
 *   ({@code @OneToMany(mappedBy = "order")}). O tym, co trafi do bazy, decyduje TYLKO strona z kluczem obcym (pozycja);
 *   lista w zamówieniu to „lustro” — trzeba ją utrzymywać ręcznie, najlepiej metodami {@code addLine/removeLine}.
 *   {@code cascade = ALL} sprawia, że zapis/usunięcie zamówienia obejmuje pozycje, a {@code orphanRemoval = true} —
 *   że pozycja wyjęta z listy znika z bazy.
 *
 * ANALOGIA: segregator z kartkami.
 *   Na każdej kartce (pozycji) jest napisane, do którego segregatora należy (klucz obcy). Spis treści na okładce
 *   segregatora (lista lines) to tylko pomoc — jeśli dopiszesz kartkę do spisu, ale nie napiszesz na niej, gdzie
 *   należy, archiwista (Hibernate) jej nie odłoży. Kaskada to „wyrzucasz segregator — wyrzucasz kartki”.
 *   orphanRemoval to „kartka wyjęta z segregatora idzie do niszczarki”. A wyrzucenie kartki NIE może wyrzucać
 *   produktu z magazynu (CascadeType.REMOVE na @ManyToOne — PUŁAPKA z sekcji 3).
 *
 * JAK TO DZIAŁA:
 *   1. {@code mappedBy = "order"} mówi Hibernate: „tej listy nie zapisuj — jej stan wynika z pola order w OrderLine”.
 *      Przy flush Hibernate patrzy na {@code line.getOrder()} każdej pozycji, a NIE na zawartość listy.
 *   2. Lista jest leniwa (domyślne LAZY dla „do wielu”): w polu siedzi {@code PersistentBag} (kolekcja Hibernate),
 *      która przy pierwszym użyciu wysyła {@code select … from order_line where order_id=? order by id}.
 *   3. Kaskada: przy persist/remove zamówienia Hibernate przechodzi po liście i wykonuje to samo na pozycjach.
 *   4. orphanRemoval: przy flush Hibernate porównuje listę z jej stanem z wczytania; pozycje, których zabrakło,
 *      usuwa ({@code delete from order_line where id=?}). Dlatego musi to być WCIĄŻ TA SAMA kolekcja (sekcja 4).
 *
 * SŁÓWKA:
 *   bidirectional = dwukierunkowa; unidirectional = jednokierunkowa; owning side = strona właściciela (z kluczem obcym);
 *   inverse side = strona odwrotna (z mappedBy); helper method = metoda pomocnicza; cascade = kaskada (przeniesienie
 *   operacji na powiązane encje); orphan = sierota (dziecko bez rodzica); join table = tabela łącząca;
 *   bag = „worek” (lista Hibernate bez indeksu kolejności); aggregate = agregat (sum, count…).
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel01ManyToOne (strona „wiele”: @ManyToOne i klucz obcy),
 *             s09_jpa_relations/Rel03ManyToMany (relacja wiele do wielu),
 *             s09_jpa_relations/Rel04LazyLoading (leniwe kolekcje poza transakcją),
 *             s09_jpa_relations/Rel05NPlusOne (lista zamówień z pozycjami bez N+1),
 *             s08_data_jpa/Jpa01Entities (cykl życia encji, equals/hashCode),
 *             s08_data_jpa/Jpa04CustomQueries (agregaty w @Query),
 *             t20_lombok/Lombok03DataValueBuilder (@Data — dlaczego nie na encjach).
 * </pre>
 */
public final class Rel02OneToMany {

    private Rel02OneToMany() {
    }

    // =================================================================================================
    // 1. RELACJA DWUKIERUNKOWA — mappedBy, strona właściciela i metody pomocnicze (ShopOrder, OrderLine)
    // =================================================================================================
    //
    //   OrderLine (strona WŁAŚCICIELA — ma klucz obcy):       ShopOrder (strona ODWROTNA — mappedBy):
    //   {@code @ManyToOne(fetch = LAZY, optional = false)}            {@code @OneToMany(mappedBy = "order", cascade = ALL,}
    //   {@code @JoinColumn(name = "order_id", nullable = false)}      {@code            orphanRemoval = true)}
    //   {@code private ShopOrder order;}                              {@code private List<OrderLine> lines = new ArrayList<>();}
    //
    // Wartość {@code mappedBy} to NAZWA POLA po drugiej stronie ("order"), nie nazwa kolumny. Bez mappedBy Hibernate
    // uznałby, że to dwie NIEZALEŻNE relacje (i szukał tabeli łączącej — sekcja 2).
    //
    // Wczytanie listy (sprawdzone testem): {@code select l1_0.order_id,l1_0.id,l1_0.product_id,l1_0.quantity,l1_0.unit_price}
    // {@code from order_line l1_0 where l1_0.order_id=? order by l1_0.id} — dopiero przy pierwszym użyciu listy.
    //
    // PUŁAPKA: ustawienie tylko strony odwrotnej — {@code order.getLines().add(line)} bez {@code line.setOrder(order)}.
    //   Co dokładnie się dzieje (sprawdzone testem)? Kaskada każe zapisać nową pozycję, ale jej pole order jest null.
    //   Przy flush: {@code DataIntegrityViolationException} ← {@code org.hibernate.PropertyValueException}
    //   „not-null property references a null or transient value for entity …OrderLine.order” — do bazy nie idzie nic.
    //   Dlaczego? Lista ma mappedBy, więc Hibernate jej nie słucha — order_id bierze z line.getOrder().
    //
    // PUŁAPKA: odwrotnie — tylko {@code line.setOrder(order)} + save pozycji, bez dodania do listy. Baza jest w porządku
    //   (INSERT z order_id — sprawdzone testem), ale lista zamówienia wczytana WCZEŚNIEJ w tej samej transakcji nadal
    //   nie zawiera nowej pozycji (size 0) — aż do clear() i ponownego wczytania (size 1). Obiekty w pamięci kłamią.
    //
    // DOBRA PRAKTYKA: metody pomocnicze po stronie „jeden” — {@code addLine(...)} / {@code removeLine(...)} ustawiają
    //   OBIE strony naraz (ShopOrder.java). Dlaczego? Jedno miejsce, w którym relacja zmienia się poprawnie —
    //   baza (strona właściciela) i obiekty w pamięci (lista) są zawsze zgodne.
    //   PYTANIE REKRUTACYJNE: „Co to jest strona właściciela relacji i co robi mappedBy?”
    //
    // PUŁAPKA: toString/equals/hashCode po relacjach przy relacji DWUKIERUNKOWEJ. Dlaczego groźne? Zamówienie wypisuje
    //   listę pozycji, każda pozycja wypisuje zamówienie, które wypisuje listę… Sprawdzone testem (encje LoopOrder/
    //   LoopLine w pakiecie testowym rel02_pitfalls): dwa SELECT-y, a potem {@code java.lang.StackOverflowError}.
    //   To samo z hashCode/equals generowanymi „ze wszystkich pól”. Lombok {@code @Data} na encji robi DOKŁADNIE to
    //   (toString, equals i hashCode ze wszystkich pól) — dlatego na encjach nie używamy @Data
    //   (t20_lombok/Lombok03DataValueBuilder); jeśli już Lombok, to @Getter i ręczne equals/hashCode po id.

    // =================================================================================================
    // 2. JEDNOKIERUNKOWE @OneToMany — tabela łącząca albo @JoinColumn i dodatkowe UPDATE-y (Invoice)
    // =================================================================================================
    //
    // Gdy pozycja NIE ma pola z rodzicem, a rodzic ma listę, są dwa warianty (oba sprawdzone testem):
    //
    //   a) {@code @OneToMany} bez mappedBy i bez @JoinColumn → Hibernate chce TABELI ŁĄCZĄCEJ (join table), jak przy
    //      @ManyToMany. Test z ddl-auto=create-drop (encje Parcel/ParcelItem w rel02_pitfalls): powstają tabele
    //      parcel, parcel_item i {@code parcel_items (parcel_id, items_id)}. Zapis paczki z 2 elementami = 5 poleceń:
    //      {@code insert into parcel …; insert into parcel_item … ×2; insert into parcel_items (parcel_id,items_id) … ×2}.
    //      Trzecia tabela i dodatkowy JOIN przy każdym odczycie — dla zwykłego 1—N to marnotrawstwo.
    //
    //   b) {@code @OneToMany} + {@code @JoinColumn(name = "invoice_id")} (Invoice → InvoiceLine w tej lekcji) → klucz
    //      obcy w tabeli dzieci, ale zarządza nim LISTA rodzica. Zapis faktury z 2 pozycjami (sprawdzone testem):
    //        {@code insert into invoice (invoice_number,id) values (?,default)}
    //        {@code insert into invoice_line (amount,description,id) values (?,?,default)}      ← bez invoice_id!
    //        {@code insert into invoice_line (amount,description,id) values (?,?,default)}
    //        {@code update invoice_line set invoice_id=? where id=?}                           ← dopiero teraz
    //        {@code update invoice_line set invoice_id=? where id=?}
    //      Usunięcie pozycji z listy: {@code update invoice_line set invoice_id=null where invoice_id=? and id=?}, potem
    //      {@code delete from invoice_line where id=?}. Hibernate najpierw zapisuje encje, a potem „kolekcje” — stąd
    //      UPDATE-y, a kolumna invoice_id MUSI dopuszczać NULL. Z {@code @JoinColumn(nullable = false)} (Pallet
    //      w rel02_pitfalls) Hibernate 7 wpisuje klucz obcy już w INSERT, ale i tak wysyła potem te same UPDATE-y.
    //
    // DOBRA PRAKTYKA: domyślnie mapuj samo {@code @ManyToOne} po stronie dziecka (Rel01); listę po stronie rodzica dodaj
    //   (dwukierunkowo, z mappedBy), gdy naprawdę jej potrzebujesz — np. rodzic zarządza cyklem życia dzieci (kaskada,
    //   orphanRemoval), jak zamówienie pozycjami. Dlaczego? Strona z kluczem obcym zapisuje go jednym INSERT-em, bez
    //   tabel łączących i dodatkowych UPDATE-ów. A „wszystkie zamówienia klienta” to zwykle zapytanie, nie lista w encji
    //   (tysiące zamówień w jednej kolekcji to ryzyko wczytania ich wszystkich naraz).

    // =================================================================================================
    // 3. KASKADY i orphanRemoval — zamówienie zarządza życiem pozycji
    // =================================================================================================
    //
    //   zapis nowego zamówienia z 2 pozycjami: {@code orders.save(order)} (sprawdzone testem — 3 INSERT-y):
    //     {@code insert into orders (order_number,id) values (?,default)}
    //     {@code insert into order_line (order_id,product_id,quantity,unit_price,id) values (?,?,?,?,default)}  ×2
    //   Bez kaskady trzeba by zapisać każdą pozycję osobno ({@code lines.save(...)}).
    //
    //   {@code order.removeLine(line)} + flush (orphanRemoval):   {@code delete from order_line where id=?}
    //   {@code order.getLines().clear()} + flush:                  DELETE każdej pozycji
    //   {@code orders.deleteById(2L)} + flush (cascade REMOVE):
    //     {@code select … from orders …}                         ← wczytanie zamówienia
    //     {@code select … from order_line … where order_id=?}    ← Hibernate musi znać dzieci, żeby je usunąć
    //     {@code delete from order_line where id=?}  ×3          ← najpierw dzieci (klucz obcy!)
    //     {@code delete from orders where id=?}
    //   Uwaga na koszt: usuwanie kaskadowe to SELECT dzieci i DELETE NA KAŻDE dziecko — przy tysiącach dzieci lepszy
    //   jest masowy {@code @Modifying @Query("delete from OrderLine l where l.order.id = :id")} (s08_data_jpa/Jpa04CustomQueries).
    //
    // cascade = ALL to PERSIST, MERGE, REMOVE, REFRESH, DETACH. orphanRemoval to coś WIĘCEJ niż REMOVE: REMOVE usuwa
    // dzieci razem z rodzicem, orphanRemoval usuwa też dziecko odłączone od ŻYWEGO rodzica.
    //
    // DOBRA PRAKTYKA: kaskady tylko od rodzica do dzieci, które bez niego nie istnieją (zamówienie → pozycje,
    //   faktura → pozycje faktury). Dlaczego? Pozycja bez zamówienia nie ma sensu; ale klient bez zamówienia — tak.
    //
    // PUŁAPKA: {@code CascadeType.REMOVE} (albo ALL) na {@code @ManyToOne} — po stronie dziecka. Dlaczego groźne?
    //   Usunięcie POZYCJI każe Hibernate usunąć PRODUKT (sprawdzone testem — encja RemovingOrderLine w rel02_pitfalls):
    //     {@code delete from order_line where id=?}  →  {@code delete from product where id=?}
    //   Kubek występował w jednej pozycji — zniknął z katalogu sklepu po cichu. Mysz występuje w dwóch pozycjach —
    //   DELETE produktu narusza klucz obcy: DataIntegrityViolationException „… FK_ORDER_LINE_PRODUCT …” i cała
    //   transakcja się wycofuje. Kaskada płynie od rodzica do dziecka, nigdy odwrotnie.

    // =================================================================================================
    // 4. List czy Set, kolejność (@OrderBy) i PUŁAPKA podmiany kolekcji
    // =================================================================================================
    //
    // {@code List} bez {@code @OrderColumn} to dla Hibernate „worek” (bag): kolekcja z możliwymi duplikatami i bez
    // zapisanej kolejności. Przy @OneToMany(mappedBy) to w porządku: dodanie/usunięcie elementu to INSERT/DELETE
    // jednego wiersza (sekcja 3). Kłopoty z workami pojawiają się gdzie indziej:
    //   • przy @ManyToMany — usunięcie jednego elementu kasuje i wstawia od nowa całą tabelę łączącą (Rel03, sekcja 2);
    //   • przy {@code join fetch} DWÓCH list naraz — MultipleBagFetchException (s09_jpa_relations/Rel05NPlusOne).
    // {@code Set} nie ma tych problemów, ale wymaga poprawnych equals/hashCode encji (stały hashCode, equals po id —
    // s08_data_jpa/Jpa01Entities, sekcja 5); inaczej element „zgubi się” w zbiorze po nadaniu id przy zapisie.
    //
    // Kolejność: baza nie gwarantuje kolejności wierszy bez ORDER BY. {@code @OrderBy("id")} (nazwa POLA, nie kolumny)
    // dokleja {@code order by l1_0.id} do zapytania o kolekcję (sprawdzone testem). {@code @OrderColumn} (osobna kolumna
    // z numerem pozycji na liście) zapisuje kolejność ustawioną w Javie — droższe (UPDATE-y przy przesuwaniu).
    //
    // PUŁAPKA: podmiana kolekcji przy orphanRemoval — {@code order.setLines(new ArrayList<>())}. Dlaczego błąd?
    //   Hibernate śledzi zmiany SWOJEJ kolekcji (PersistentBag z encji); nowa ArrayList to obcy obiekt, a stara
    //   kolekcja „zniknęła” — Hibernate nie wie, czy jej elementy usunąć. Sprawdzone testem: przy flush przez
    //   repozytorium {@code JpaSystemException} ← {@code org.hibernate.HibernateException} „A collection with orphan
    //   deletion was no longer referenced by the owning entity instance: …ShopOrder.lines”. (W starszych wersjach
    //   Hibernate komunikat brzmiał „A collection with cascade="all-delete-orphan" was no longer referenced…” —
    //   tak go znajdziesz w internecie.) Naprawa: modyfikuj istniejącą listę ({@code clear()}, {@code addAll()},
    //   removeLine) i nie wystawiaj settera kolekcji.

    // =================================================================================================
    // 5. Wartość zamówienia — metoda w encji czy zapytanie agregujące?
    // =================================================================================================
    //
    //   {@code order.total()} (metoda w encji, liczy w Javie):     {@code lines.sumTotalOfOrder(id)} (@Query w OrderLineRepository):
    //     SELECT zamówienia + SELECT wszystkich pozycji             1 zapytanie, 0 wczytanych encji (sprawdzone testem)
    //     267.90 dla ZAM-001                                        {@code select coalesce(sum((ol1_0.quantity*ol1_0.unit_price)),0)}
    //                                                               {@code from order_line ol1_0 where ol1_0.order_id=?}
    //
    // Kiedy które?
    //   • metoda w encji — gdy zamówienie z pozycjami i tak jest w pamięci (pokazujesz szczegóły zamówienia, zmieniasz
    //     pozycje i chcesz od razu nową sumę — liczy też jeszcze NIEZAPISANE zmiany, bez żadnego SQL-a). Zapytanie
    //     zobaczy je również, ale za cenę flush: przy FlushMode.AUTO Hibernate przed zapytaniem o order_line wysyła
    //     UPDATE brudnej pozycji (sprawdzone testem: Mysz ×10 → oba sposoby dają 739.90, w logu jest UPDATE);
    //   • zapytanie agregujące — gdy potrzebujesz samej liczby (lista zamówień z sumami, raport, walidacja limitu).
    //     Wczytywanie 500 pozycji, żeby je zsumować, to 500 encji w pamięci zamiast jednej liczby.
    //   Pusta lista: total() daje 0 (reduce od ZERO); w SQL sum bez wierszy daje null — stąd {@code coalesce(…, 0)}.
    //
    // DOBRA PRAKTYKA: logikę biznesową, która dotyczy jednego obiektu i jego dzieci, trzymaj w encji (total(),
    //   addLine) — a agregaty po wielu wierszach licz w bazie. Dlaczego? Encja pilnuje swoich reguł niezależnie od
    //   tego, kto ją zmienia; baza liczy sumy szybciej i bez przesyłania wierszy.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • Dwukierunkowo: dziecko {@code @ManyToOne @JoinColumn} (WŁAŚCICIEL, klucz obcy) + rodzic
     *     {@code @OneToMany(mappedBy = "nazwaPolaWDziecku")}. Do bazy trafia TYLKO stan strony właściciela.
     *   • Metody pomocnicze addX/removeX ustawiają obie strony. Samo {@code getLines().add(line)} → PropertyValueException
     *     (null w order) przy flush; samo {@code line.setOrder(o)} → baza OK, lista w pamięci nieaktualna.
     *   • Jednokierunkowe @OneToMany: bez @JoinColumn → tabela łącząca; z @JoinColumn → INSERT + UPDATE na dziecko.
     *     Zwykle lepiej samo @ManyToOne albo dwukierunkowo.
     *   • {@code cascade = ALL} — zapis/usunięcie rodzica obejmuje dzieci; {@code orphanRemoval = true} — dziecko wyjęte
     *     z listy = DELETE. Usuwanie kaskadowe = SELECT dzieci + DELETE na każde.
     *   • Nigdy {@code cascade = REMOVE/ALL} na @ManyToOne (usuwa rodzica: produkt znika albo błąd FK).
     *   • List = bag (OK przy mappedBy); Set wymaga dobrego equals/hashCode; {@code @OrderBy("pole")} → ORDER BY.
     *   • Nie podmieniaj kolekcji z orphanRemoval: HibernateException „A collection with orphan deletion was no longer
     *     referenced…”. Używaj clear()/addAll().
     *   • toString/equals/hashCode bez relacji (dwukierunkowo → StackOverflowError); bez Lombok @Data na encjach.
     *   • Suma z pozycji: w encji, gdy pozycje są w pamięci; {@code coalesce(sum(...), 0)} w bazie, gdy potrzebna liczba.
     *
     * PYTANIA KONTROLNE:
     *   1. Która strona relacji zamówienie—pozycja jest stroną właściciela i co z tego wynika?
     *   2. Co się stanie? {@code order.getLines().add(new OrderLine(product, 1, price)); orders.flush();} — zamówienie
     *      z cascade = ALL, kolumna order_id NOT NULL.
     *   3. Co oznacza {@code mappedBy = "order"} — nazwę kolumny czy pola? Co się stanie, gdy je pominiesz?
     *   4. Ile poleceń SQL wyśle zapis faktury z dwiema pozycjami przy jednokierunkowym @OneToMany z @JoinColumn i dlaczego?
     *   5. ZNAJDŹ BŁĄD: {@code @ManyToOne(cascade = CascadeType.ALL) private Product product;} w encji OrderLine.
     *   6. Co się stanie? {@code order.setLines(new ArrayList<>()); orders.flush();} przy {@code orphanRemoval = true}.
     *   7. Czym różni się orphanRemoval od CascadeType.REMOVE?
     *   8. Kiedy policzysz wartość zamówienia metodą w encji, a kiedy zapytaniem {@code sum}? Dlaczego {@code coalesce}?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku OrderLineExercises.java (metody z TODO), testy: Rel02OneToManyExercisesTest.
    // Potrzebne zapytania dopisujesz w ExerciseOrderLineRepository.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. OrderLine — jej tabela ma klucz obcy order_id, a pole order ma @JoinColumn. Hibernate zapisuje relację
     *      wyłącznie na podstawie line.getOrder(); lista ShopOrder.lines (mappedBy) jest tylko odbiciem w pamięci.
     *   2. Kaskada każe zapisać pozycję, ale jej pole order jest null → przy flush DataIntegrityViolationException
     *      ← PropertyValueException „not-null property references a null or transient value … OrderLine.order”.
     *      Do bazy nic nie idzie. Sprawdzone testem. Naprawa: order.addLine(...) (ustawia obie strony).
     *   3. Nazwę POLA w encji po drugiej stronie. Bez mappedBy Hibernate traktuje listę jako osobną, jednokierunkową
     *      relację i szuka tabeli łączącej (orders_lines) — z ddl-auto=validate aplikacja by nie wstała, a z create
     *      powstałaby niepotrzebna tabela.
     *   4. Pięć: INSERT faktury, dwa INSERT-y pozycji BEZ invoice_id i dwa UPDATE-y ustawiające invoice_id
     *      (sprawdzone testem). Hibernate zapisuje najpierw encje, potem kolekcje — a kluczem obcym rządzi lista rodzica.
     *   5. Kaskada (w tym REMOVE) od dziecka do rodzica: usunięcie pozycji usunie produkt — po cichu, gdy był tylko w niej,
     *      albo z błędem FK (DataIntegrityViolationException), gdy jest w innych pozycjach. Sprawdzone testem.
     *      Na @ManyToOne nie dajemy kaskad.
     *   6. Przy flush HibernateException „A collection with orphan deletion was no longer referenced by the owning
     *      entity instance: …ShopOrder.lines” (przez repozytorium opakowany w JpaSystemException). Sprawdzone testem.
     *      Zamiast podmiany: order.getLines().clear() (i removeLine dla pojedynczych).
     *   7. REMOVE usuwa dzieci, gdy usuwasz rodzica. orphanRemoval dodatkowo usuwa dziecko, które wyjęto z kolekcji
     *      rodzica (rodzic zostaje) — np. removeLine → DELETE pozycji.
     *   8. Metodą, gdy zamówienie z pozycjami i tak jest w pamięci (liczy też niezapisane zmiany bez SQL); zapytaniem, gdy
     *      potrzebna jest sama liczba — 1 zapytanie bez wczytywania encji. sum bez wierszy zwraca null, a coalesce
     *      zamienia go na 0 (puste zamówienie ZAM-003 → 0).
     */
    // </editor-fold>
}
