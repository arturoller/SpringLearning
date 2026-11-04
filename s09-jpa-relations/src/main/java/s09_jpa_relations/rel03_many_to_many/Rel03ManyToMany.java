package s09_jpa_relations.rel03_many_to_many;

/**
 * <pre>
 * TEMAT: Relacja „wiele do wielu” — {@code @ManyToMany} i {@code @JoinTable}, Set kontra List, kaskady
 *        i encja pośrednia, gdy powiązanie ma własne dane
 *        (many to many = wiele do wielu; join table = tabela łącząca; tag = etykieta; supplier = dostawca)
 *
 * W SKRÓCIE:
 *   Produkt ma wiele tagów, a tag jest na wielu produktach. Jednej kolumny klucza obcego tu nie wystarczy — potrzebna
 *   jest TABELA ŁĄCZĄCA {@code product_tag (product_id, tag_id)}: jeden wiersz = jedno powiązanie. {@code @ManyToMany}
 *   z {@code @JoinTable} ukrywa tę tabelę: w Javie widzisz {@code Set<Tag>} w produkcie. Strona z @JoinTable jest
 *   właścicielem, druga ma {@code mappedBy}. Kolekcją jest {@code Set} (List kasuje i wstawia wszystkie wiersze przy
 *   każdej zmianie), bez kaskad (usunięcie produktu nie może usuwać tagów). Gdy powiązanie ma dane (cena zakupu
 *   u dostawcy), zamiast @ManyToMany robisz encję pośrednią z dwoma {@code @ManyToOne}.
 *
 * ANALOGIA: lista obecności na zajęciach.
 *   Uczniów jest wielu, zajęć jest wiele. Nie dopiszesz do ucznia jednej rubryki „zajęcia”, ani do zajęć jednej
 *   rubryki „uczeń” — prowadzisz osobny zeszyt obecności: „Ania — matematyka”, „Ania — chemia”, „Bartek —
 *   matematyka”. To tabela łącząca. Gdy przy wpisie trzeba zapisać ocenę albo datę, zeszyt przestaje być „samą listą
 *   par” i staje się dziennikiem z własnymi rubrykami — to encja pośrednia. A skreślenie ucznia z dziennika nie może
 *   kasować przedmiotu z planu lekcji (kaskada REMOVE na @ManyToMany).
 *
 * JAK TO DZIAŁA:
 *   1. {@code @JoinTable(name = "product_tag", joinColumns = product_id, inverseJoinColumns = tag_id)}: Hibernate wie,
 *      że kolekcja Product.tags to wiersze product_tag z product_id = id produktu, połączone z tag po tag_id.
 *   2. Wczytanie kolekcji (leniwe): {@code select t1_0.product_id,t1_1.id,t1_1.name from product_tag t1_0
 *      join tag t1_1 on t1_1.id=t1_0.tag_id where t1_0.product_id=?} — jedno zapytanie z JOIN-em.
 *   3. Przy flush Hibernate porównuje kolekcję ze stanem z wczytania: Set → INSERT/DELETE pojedynczych wierszy
 *      łączących; List (bag — worek) → nie umie wskazać jednego wiersza, więc kasuje wszystkie i wstawia od nowa.
 *   4. Tabelę łączącą zmienia TYLKO strona właściciela (Product.tags). Zmiana Tag.products (mappedBy) do bazy nie trafia.
 *
 * SŁÓWKA:
 *   join table = tabela łącząca; owning side = strona właściciela; inverse side = strona odwrotna (mappedBy);
 *   bag = worek (lista bez indeksu); set = zbiór; cascade = kaskada; intermediate entity = encja pośrednia;
 *   composite key = klucz złożony; embedded id = osadzony identyfikator; distinct = bez powtórzeń;
 *   left join = złączenie lewostronne (także wiersze bez dopasowania); having = warunek na grupach.
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel01ManyToOne (@ManyToOne — z niego składa się encja pośrednia),
 *             s09_jpa_relations/Rel02OneToMany (strona właściciela, mappedBy, metody pomocnicze, kaskady),
 *             s09_jpa_relations/Rel05NPlusOne (tagi wielu produktów bez N+1),
 *             s08_data_jpa/Jpa01Entities (equals/hashCode encji — warunek działania Set),
 *             s08_data_jpa/Jpa04CustomQueries (agregaty i projekcje w @Query),
 *             t12_collections/Collections04Sets (Set i equals/hashCode w czystej Javie).
 * </pre>
 */
public final class Rel03ManyToMany {

    private Rel03ManyToMany() {
    }

    // =================================================================================================
    // 1. @ManyToMany + @JoinTable — tabela łącząca, strona właściciela, relacja dwukierunkowa (Product, Tag)
    // =================================================================================================
    //
    //   Product (WŁAŚCICIEL):                                  Tag (strona ODWROTNA):
    //   {@code @ManyToMany}                                            {@code @ManyToMany(mappedBy = "tags")}
    //   {@code @JoinTable(name = "product_tag",}                       {@code private Set<Product> products = new HashSet<>();}
    //   {@code     joinColumns = @JoinColumn(name = "product_id"),}
    //   {@code     inverseJoinColumns = @JoinColumn(name = "tag_id"))}
    //   {@code private Set<Tag> tags = new HashSet<>();}
    //
    // Tabela product_tag nie ma własnej encji — Hibernate zarządza nią sam. Klucz główny (product_id, tag_id) w migracji
    // pilnuje, że para wystąpi raz. Którą stronę wybrać na właściciela? Tę, od której zwykle zmieniasz relację
    // („otaguj produkt”) — tylko jej zmiany trafiają do bazy. Druga strona (mappedBy) jest opcjonalna: dodaj ją, gdy
    // potrzebujesz nawigacji od tagu do produktów (tu: zapytanie z left join t.products — sekcja 5).
    //
    // SQL wczytania kolekcji (sprawdzone testem):
    //   Product.tags:  {@code select t1_0.product_id,t1_1.id,t1_1.name from product_tag t1_0 join tag t1_1 on t1_1.id=t1_0.tag_id}
    //                  {@code where t1_0.product_id=?}
    //   Tag.products:  {@code select p1_0.tag_id,p1_1.id,p1_1.name,p1_1.price from product_tag p1_0 join product p1_1}
    //                  {@code on p1_1.id=p1_0.product_id where p1_0.tag_id=?}
    //
    // DOBRA PRAKTYKA: metody pomocnicze addTag/removeTag po stronie właściciela, które zmieniają OBIE kolekcje
    //   (jak addLine w Rel02), a gettery zwracają widok tylko do odczytu ({@code Collections.unmodifiableSet}).
    //   Dlaczego? Inaczej ktoś zmieni tylko tag.getProducts() i zdziwi się, że w bazie nic się nie stało.
    //   Koszt, o którym warto wiedzieć (sprawdzone testem): addTag/removeTag dotykają tag.products, więc Hibernate
    //   wczytuje CAŁY zbiór produktów tego tagu (dodatkowy SELECT przed INSERT-em do product_tag). Przy tagu na 100 000 produktach to za drogo — wtedy relację
    //   robi się jednokierunkową (bez Tag.products), a „produkty z tagiem” czyta się zapytaniem.

    // =================================================================================================
    // 2. Set kontra List przy @ManyToMany — ile poleceń SQL kosztuje jedna zmiana
    // =================================================================================================
    //
    //   Mysz ma 4 tagi. Usuwamy JEDEN (sprawdzone testem z SqlCapture):
    //
    //   Set<Tag> (Product, lekcja):                           List<Tag> (BagProduct w rel03_pitfalls):
    //     {@code delete from product_tag}                             {@code delete from product_tag where product_id=?}   ← WSZYSTKIE!
    //     {@code   where product_id=? and tag_id=?}                   {@code insert into product_tag (product_id,tag_id) …}  ×3
    //
    //   Dodanie jednego tagu do Klawiatury (2 tagi): Set → 1 INSERT; List → DELETE wszystkich + 3 INSERT-y.
    //
    // Dlaczego? List bez {@code @OrderColumn} to dla Hibernate „worek” (bag): może mieć duplikaty i nie ma klucza
    // wiersza, więc Hibernate nie potrafi wskazać „tego jednego” wiersza do usunięcia — kasuje kolekcję i odtwarza ją.
    // Set nie ma duplikatów: para (product_id, tag_id) jednoznacznie wskazuje wiersz.
    //
    // PUŁAPKA: {@code List} przy @ManyToMany. Dlaczego groźne? Przy 50 tagach jedna zmiana = 1 DELETE + 49 INSERT-ów,
    //   a baza (indeksy, wyzwalacze — triggers, audyt) obsługuje usunięcie i ponowne wstawienie WSZYSTKICH powiązań.
    //   Testy na 2–3 elementach tego nie pokażą — zobaczysz to dopiero w logu SQL.
    //
    // DOBRA PRAKTYKA: {@code Set} przy @ManyToMany + equals/hashCode encji po id ze STAŁYM hashCode
    //   (s08_data_jpa/Jpa01Entities, sekcja 5). Dlaczego stały? Nowy tag ma id = null, a po zapisie dostaje id; gdyby
    //   hashCode zależał od id, obiekt „przeniósłby się” do innego kubełka HashSet i contains/remove by go nie znalazły.

    // =================================================================================================
    // 3. Kaskady przy @ManyToMany — PUŁAPKA CascadeType.REMOVE / ALL
    // =================================================================================================
    //
    // Usunięcie zwykłego produktu (Product bez kaskady) usuwa jego wiersze w product_tag — tabelą łączącą zarządza
    // właściciel — ale tagi zostają. Tak ma być: tag to osobny byt, używany przez inne produkty.
    //
    // PUŁAPKA: {@code @ManyToMany(cascade = CascadeType.ALL)} (albo REMOVE). Dlaczego groźne? Usunięcie produktu każe
    //   Hibernate usunąć też jego TAGI (encja CascadingProduct w rel03_pitfalls, sprawdzone testem):
    //     {@code delete from product_tag where product_id=?}   ← powiązania produktu
    //     {@code delete from tag where id=?}                   ← tag „promocja”, którego używa też Mysz!
    //   → DataIntegrityViolationException „… FK_PRODUCT_TAG_TAG …” (wiersz Myszy w product_tag wciąż wskazuje tag),
    //   transakcja się wycofuje i NIE DA SIĘ usunąć produktu. Gorzej, gdy tag był tylko na tym produkcie (test: Termos
    //   z tagiem „outdoor”): tag znika po cichu — razem ze słownikiem tagów, który ktoś budował.
    //   Bez klucza obcego w bazie (np. tabela łącząca bez FK) zostałyby wiersze wskazujące nieistniejący tag.
    //
    // Kaskada PERSIST („nowy tag zapisz razem z produktem”) bywa wygodna, ale też jej unikamy: tag zwykle już
    // istnieje (findByName) i nowe tworzymy świadomie (ćwiczenie 1). Zasada z Rel02: kaskady tylko od rodzica do
    // dzieci, które bez niego nie istnieją. Przy N—N nikt nie jest niczyim rodzicem.

    // =================================================================================================
    // 4. ENCJA POŚREDNIA zamiast @ManyToMany — gdy powiązanie ma dane (ProductSupplier)
    // =================================================================================================
    //
    // „Klawiaturę kupujemy w Hurtowni Alfa za 110.00, a w Hurtowni Beta za 105.00.” Cena zakupu nie należy ani do
    // produktu, ani do dostawcy — należy do PARY. @ManyToMany nie ma gdzie jej trzymać (tabela łącząca ma tylko dwa
    // klucze obce). Rozwiązanie — encja pośrednia:
    //
    //   {@code @Entity @Table(name = "product_supplier")}
    //   {@code class ProductSupplier {}
    //   {@code     @Id @GeneratedValue Long id;}                                   ← własne id
    //   {@code     @ManyToOne(fetch = LAZY, optional = false) Product product;}    ← zwykłe @ManyToOne (Rel01)
    //   {@code     @ManyToOne(fetch = LAZY, optional = false) Supplier supplier;}
    //   {@code     BigDecimal purchasePrice;}                                      ← dane powiązania
    //   {@code }}
    //   + w migracji {@code CONSTRAINT uk_product_supplier UNIQUE (product_id, supplier_id)}
    //
    // Relacja N—N rozpada się na dwie relacje N—1 — wszystko z Rel01 i Rel02 działa bez niespodzianek: zapytania
    // ({@code findByProductIdOrderByPurchasePrice}), zmiana ceny przez dirty checking, kolejne pola (data umowy,
    // minimalna ilość). Podwójną parę odrzuca baza (sprawdzone testem): DataIntegrityViolationException „Unique index
    // or primary key violation: "PUBLIC.UK_PRODUCT_SUPPLIER …"”.
    //
    // Druga droga: klucz złożony — klasa {@code @Embeddable} (osadzalna) z polami productId i supplierId
    // (z equals/hashCode, Serializable) jako {@code @EmbeddedId}, a przy polach @ManyToOne adnotacja
    // {@code @MapsId("productId")} (mapuje id — klucz obcy jest jednocześnie częścią klucza głównego). Działa, ale
    // każde wyszukanie po id wymaga zbudowania obiektu klucza, a referencja do takiego wiersza z innej tabeli to dwie
    // kolumny zamiast jednej.
    //
    // DOBRA PRAKTYKA: encja pośrednia z WŁASNYM id + UNIQUE na parze kluczy obcych. Dlaczego? Proste id (jedna kolumna)
    //   do findById, URL-i w API i odwołań z innych tabel; UNIQUE w bazie i tak pilnuje unikalności pary.
    //   I ogólniej: jeśli podejrzewasz, że powiązanie kiedyś dostanie dane — od razu zrób encję pośrednią. Przejście
    //   z @ManyToMany na encję pośrednią później to zmiana mapowania, zapytań i migracja danych.
    //   PYTANIE REKRUTACYJNE: „Jak zamapować relację wiele do wielu z dodatkowym atrybutem?”
    //
    // PUŁAPKA: kontynuowanie pracy z EntityManagerem po wyjątku z bazy. Dlaczego? Po nieudanym INSERT-cie encja
    //   zostaje w kontekście bez id — kolejne zapytanie (auto flush) kończy się {@code org.hibernate.AssertionFailure}
    //   „… has a null identifier (this can happen if the session is flushed after an exception occurs)” (sprawdzone
    //   testem). Po wyjątku z bazy transakcję wycofuj i zaczynaj od nowa — nie „próbuj dalej” w tej samej.

    // =================================================================================================
    // 5. ZAPYTANIA po relacji N—N: produkty z tagiem, duplikaty i distinct, liczba produktów na tag
    // =================================================================================================
    //
    //   metoda                                       SQL (sprawdzony testem)
    //   findByTagsNameOrderByName("biuro")           {@code select p1_0.… from product p1_0 left join product_tag t1_0 on p1_0.id=t1_0.product_id}
    //                                                {@code left join tag t1_1 on t1_1.id=t1_0.tag_id where t1_1.name=? order by p1_0.name}
    //   {@code @Query("… join p.tags t where t.name in :tagNames")}  {@code … from product p1_0 join product_tag t1_0 on … join tag t1_1 on …}
    //                                                {@code where t1_1.name in (?,?) order by p1_0.name}
    //
    // Metoda z nazwy robi LEFT JOIN (Spring Data tak tłumaczy przejście po kolekcji), JPQL z {@code join} — INNER JOIN.
    // Wynik ten sam, bo warunek WHERE na t.name i tak odrzuca produkty bez tagów.
    //
    // Duplikaty: produkt z DWOMA pasującymi tagami (Klawiatura: biuro i gaming) daje w SQL DWA wiersze. Sprawdzone testem
    // dla {@code t.name in ('biuro', 'gaming')}:
    //   • natywny SQL i JPQL {@code select p.name …}     → [Klawiatura, Klawiatura, Lampka, Monitor, Mysz, Mysz] (6)
    //   • JPQL {@code select distinct p.name …}          → [Klawiatura, Lampka, Monitor, Mysz] (4)
    //   • JPQL {@code select p …} (ENCJE), bez distinct  → 4 produkty, każdy raz!
    // Hibernate 6+ (więc i 7) sam usuwa powtórzenia ENCJI-korzeni z wyniku (ta sama encja = ten sam obiekt w kontekście).
    // W starych tutorialach (Hibernate 5) zobaczysz, że bez {@code distinct} lista encji ma duplikaty — to już nieaktualne.
    // {@code distinct} w zapytaniu o encje nadal ma sens: trafia do SQL ({@code select distinct p1_0.id,…}), więc
    // baza nie przesyła powtórzonych wierszy (ważne przy stronicowaniu — s08_data_jpa/Jpa05PagingSorting).
    //
    // PUŁAPKA: brak distinct w zapytaniu o WARTOŚCI (nazwy, projekcje) po kolekcji. Dlaczego? Deduplikacja Hibernate
    //   dotyczy tylko encji; nazwy z JOIN-a po tagach wracają tyle razy, ile tagów pasuje (test: 6 zamiast 4).
    //
    // Liczba produktów na tag — z tagami bez produktów:
    //   {@code select t.name, count(p) from Tag t left join t.products p group by t.name order by t.name}
    //   SQL: {@code select t1_0.name,count(p1_0.product_id) from tag t1_0 left join product_tag p1_0 on t1_0.id=p1_0.tag_id}
    //        {@code group by t1_0.name order by t1_0.name}
    //   → bezprzewodowe 1, biuro 3, gaming 3, nowość 0, promocja 2. Hibernate liczy {@code count(p1_0.product_id)} prosto
    //   z tabeli łączącej, nawet nie dołączając tabeli product. Z {@code join} zamiast {@code left join} tag „nowość”
    //   zniknąłby z wyniku — count(…) liczy tylko istniejące dopasowania.
    //
    // DOBRA PRAKTYKA: „produkty z tagiem X” przy dużych danych pobieraj zapytaniem od strony produktu (jak wyżej),
    //   a nie przez {@code tag.getProducts()}. Dlaczego? Zapytanie może filtrować, sortować i stronicować w bazie;
    //   kolekcja zawsze wczytuje WSZYSTKIE produkty tagu do pamięci.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • N—N = tabela łącząca {@code product_tag (product_id, tag_id)}, PK z obu kolumn, dwa klucze obce.
     *   • Właściciel: {@code @ManyToMany @JoinTable(name, joinColumns = ta encja, inverseJoinColumns = druga)};
     *     druga strona: {@code @ManyToMany(mappedBy = "tags")}. Do bazy trafia tylko stan właściciela.
     *   • Metody pomocnicze addTag/removeTag (obie strony); gettery tylko do odczytu; helpery wczytują tag.products.
     *   • {@code Set}, nie {@code List}: List (bag) = DELETE wszystkich wierszy + INSERT każdego przy każdej zmianie.
     *     Set wymaga equals po id i stałego hashCode.
     *   • Żadnych kaskad REMOVE/ALL na @ManyToMany: usuwają współdzielone tagi (błąd FK albo cicha utrata danych).
     *   • Powiązanie z danymi → encja pośrednia: własne id + 2 × @ManyToOne + UNIQUE(product_id, supplier_id).
     *     Alternatywa: @EmbeddedId + @MapsId (klucz złożony).
     *   • Zapytania: findByTagsName → LEFT JOIN; {@code join p.tags t} → JOIN. Encje-korzenie bez duplikatów (Hibernate 6+);
     *     wartości (p.name) — z duplikatami, potrzebny {@code distinct}. {@code left join} + {@code count} → także zera.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego relacji N—N nie da się zapisać jednym kluczem obcym? Jak wygląda tabela łącząca?
     *   2. Która strona jest właścicielem relacji Product—Tag i co się stanie, gdy zmienisz tylko {@code tag.products}?
     *   3. Co się stanie? Produkt ma 4 tagi w {@code List<Tag>} (@ManyToMany); usuwasz jeden i robisz flush. Ile poleceń SQL?
     *   4. ZNAJDŹ BŁĄD: {@code @ManyToMany(cascade = CascadeType.ALL) private Set<Tag> tags;} w encji Product.
     *   5. Jak zamapujesz „produkt u dostawcy z ceną zakupu”? Dlaczego nie @ManyToMany?
     *   6. Co się stanie? {@code select p.name from Product p join p.tags t where t.name in ('biuro', 'gaming')} — ile razy
     *      pojawi się Klawiatura (tagi: biuro, gaming)? A gdy zamiast {@code p.name} wybierzesz {@code p}?
     *   7. Dlaczego licząc produkty na tag, używasz {@code left join}, a nie {@code join}?
     *   8. Własne id czy {@code @EmbeddedId} w encji pośredniej — co wybierasz i dlaczego?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — w pliku ProductTagExercises.java (metody z TODO), testy: Rel03ManyToManyExercisesTest.
    // Potrzebne zapytania dopisujesz w ExerciseProductRepository.
    // =================================================================================================

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Klucz obcy w wierszu wskazuje JEDEN wiersz innej tabeli, a tu po obu stronach jest „wiele”. Tabela łącząca
     *      product_tag ma dwie kolumny (product_id, tag_id), oba są kluczami obcymi, razem tworzą klucz główny;
     *      jeden wiersz = jedno powiązanie.
     *   2. Product (ma @JoinTable); Tag ma mappedBy. Zmiana samego tag.products nie zmieni product_tag — Hibernate
     *      tej kolekcji nie zapisuje; tylko obiekty w pamięci będą niezgodne z bazą. Dlatego addTag/removeTag.
     *   3. Cztery: DELETE wszystkich wierszy produktu z product_tag i 3 INSERT-y pozostałych tagów (sprawdzone testem
     *      na BagProduct). Z Set — jedno polecenie: delete … where product_id=? and tag_id=?.
     *   4. Kaskada REMOVE na N—N: usunięcie produktu usuwa jego tagi. Gdy tag ma inne produkty — błąd FK
     *      (DataIntegrityViolationException, produktu nie da się usunąć); gdy nie ma — tag znika po cichu. Sprawdzone
     *      testem. Na @ManyToMany nie dajemy kaskad.
     *   5. Encja pośrednia ProductSupplier: własne id, @ManyToOne do Product i do Supplier, pole purchasePrice, UNIQUE
     *      (product_id, supplier_id) w migracji. @ManyToMany nie ma gdzie trzymać ceny — tabela łącząca to same klucze.
     *   6. Dwa razy (jeden wiersz na każdy pasujący tag; sprawdzone testem: 6 wyników zamiast 4). Z {@code select p}
     *      Hibernate 7 zwraca każdą encję raz. Na wartościach trzeba {@code distinct}.
     *   7. {@code join} odrzuca tagi bez produktów (nie ma pary do złączenia), więc „nowość” z liczbą 0 zniknęłaby
     *      z wyniku. {@code left join} zostawia tag z NULL po stronie produktów, a count pomija NULL-e, więc daje 0.
     *   8. Zwykle własne id + UNIQUE: jedna kolumna klucza (prostsze findById, odwołania z innych tabel, API), a baza
     *      i tak pilnuje unikalności pary. @EmbeddedId + @MapsId, gdy schemat jest narzucony (istniejąca tabela
     *      z kluczem złożonym) albo para naprawdę jest naturalnym identyfikatorem.
     */
    // </editor-fold>
}
