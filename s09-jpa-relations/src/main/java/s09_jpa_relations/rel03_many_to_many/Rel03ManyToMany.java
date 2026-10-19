package s09_jpa_relations.rel03_many_to_many;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;

/**
 * <pre>
 * TEMAT: Relacja wiele-do-wielu — @ManyToMany, tabela łącząca i encja pośrednia
 *        (many-to-many = wiele do wielu; join table = tabela łącząca; owning side = strona właścicielska;
 *         inverse side = strona odwrotna; bag = worek; link entity = encja pośrednia/łącząca)
 *
 * W SKRÓCIE:
 *   Produkt ma wiele tagów („bio”, „promocja”), a tag należy do wielu produktów. Takiej relacji NIE da się zapisać
 *   jedną kolumną klucza obcego — potrzebna jest trzecia tabela, product_tags, z parami (product_id, tag_id).
 *   @ManyToMany + @JoinTable opisują tę tabelę po stronie WŁAŚCICIELSKIEJ (Product), a druga strona (ProductTag) ma
 *   mappedBy. Kolekcję trzymamy w Set (zbiorze), nie w List — przy List Hibernate przy KAŻDEJ zmianie kasuje
 *   wszystkie wiersze produktu z tabeli łączącej i wstawia je od nowa. Na @ManyToMany nie dajemy kaskady REMOVE.
 *   Gdy powiązanie potrzebuje własnych danych (ilość, cena w chwili zakupu) — zamieniamy @ManyToMany na encję
 *   pośrednią (OrderLine) z dwiema relacjami @ManyToOne.
 *
 * ANALOGIA: lista obecności na zajęciach dodatkowych.
 *   Uczeń chodzi na wiele kółek, kółko ma wielu uczniów. Ani legitymacja ucznia, ani dziennik kółka nie mieszczą tej
 *   wiedzy — prowadzi się osobny zeszyt zapisów: „Ola — szachy”, „Ola — chór”, „Adam — szachy” (tabela łącząca).
 *   Gdy zapis zaczyna mieć własne dane (data zapisu, opłata), zeszyt staje się pełnoprawnym dokumentem — „deklaracją
 *   uczestnictwa” z własnym numerem (encja pośrednia).
 *
 * JAK TO DZIAŁA:
 *   Product: @ManyToMany @JoinTable(name = "product_tags", joinColumns = product_id, inverseJoinColumns = tag_id)
 *            Set{@code <ProductTag>} tags                            ← strona właścicielska
 *   ProductTag: @ManyToMany(mappedBy = "tags") Set{@code <Product>} products ← strona odwrotna (lustro)
 *     product.addTag(tag) + flush  → insert into product_tags (product_id,tag_id) values (?,?)
 *     product.removeTag(tag) + flush → delete from product_tags where product_id=? and tag_id=?
 *     odczyt product.getTags()  → select ... from product_tags t1_0 join tags t1_1 on t1_1.id=t1_0.tag_id
 *                                  where t1_0.product_id=?
 *   Encja ProductTag NIE ma żadnej kolumny o produktach, Product też nie — wszystko jest w product_tags.
 *
 * SŁÓWKA:
 *   product tag = tag (etykieta) produktu — encja ProductTag (nie „Tag”: tak nazywa się adnotacja JUnita @Tag);
 *   many-to-many = wiele do wielu; join table = tabela łącząca (pośrednicząca); join columns = kolumny złączenia
 *   (wskazują TĘ encję); inverse join columns = odwrotne kolumny złączenia (wskazują drugą stronę); bag = worek
 *   (lista bez zapisanej kolejności, z możliwymi duplikatami); recreate = odtworzenie (skasuj wszystko i wstaw od nowa);
 *   link entity = encja pośrednia; aggregate = funkcja agregująca (sum, count); constructor expression = wyrażenie
 *   konstruktora (select new ...); unlink = odpiąć.
 *
 * ZOBACZ TEŻ: s09_jpa_relations/Rel02OneToMany (strona właścicielska, metody pomocnicze, kaskady),
 *             s08_data_jpa/Jpa01Entities (equals/hashCode encji — potrzebne w Set),
 *             t12_collections/Collections04Sets (jak działa zbiór), t29_jdbc_databases/Jdbc06SqlJoins (JOIN w SQL),
 *             s09_jpa_relations/Rel05NPlusOne (wydajne czytanie kolekcji).
 * </pre>
 */
public final class Rel03ManyToMany {

    private Rel03ManyToMany() {
    }

    // =================================================================================================
    // 1. @ManyToMany + @JoinTable — trzecia tabela z parami kluczy
    // =================================================================================================
    //
    // Migracja (V1__products_tags_orders.sql):
    //     create table product_tags (
    //         product_id bigint not null, tag_id bigint not null,
    //         primary key (product_id, tag_id),                       ← para jest kluczem: brak duplikatów
    //         foreign key (product_id) references products (id), foreign key (tag_id) references tags (id));
    //
    // Encja Product (strona właścicielska — ma @JoinTable):
    //     @ManyToMany
    //     @JoinTable(name = "product_tags",
    //             joinColumns = @JoinColumn(name = "product_id"),        // kolumna wskazująca Product
    //             inverseJoinColumns = @JoinColumn(name = "tag_id"))     // kolumna wskazująca ProductTag
    //     private Set<ProductTag> tags = new HashSet<>();
    //
    // Encja ProductTag (strona odwrotna): @ManyToMany(mappedBy = "tags") private Set<Product> products = new HashSet<>();
    //
    // Zasada jak w Rel02OneToMany: do bazy trafia TYLKO to, co jest w kolekcji strony właścicielskiej. Dopisanie
    // produktu do tag.products (bez product.tags) nie wstawi żadnego wiersza (test inverseSideAloneWritesNothing:
    // po flush product_tags jest pusta). Dlatego metody pomocnicze
    // Product.addTag/removeTag zmieniają OBIE kolekcje, a gettery oddają zbiory niemodyfikowalne.
    //
    // Którą stronę wybrać na właściciela? Tę, od której częściej zaczynasz zmiany („dodaj tag do produktu”).
    // Dla bazy to obojętne — tabela łącząca wygląda tak samo.

    // =================================================================================================
    // 2. Set czy List? Zmierzone liczbą instrukcji SQL
    // =================================================================================================
    //
    // ListProduct ma te same tagi w List (tabela list_product_tags BEZ klucza głównego — worek dopuszcza duplikaty).
    // Produkt z tagami a, b, c; usuwamy „a” i robimy flush. SQL z logu (Hibernate 7.4):
    //
    //   Set (Product):                                   List (ListProduct):
    //     delete from product_tags                         delete from list_product_tags where list_product_id=?
    //       where product_id=? and tag_id=?                insert into list_product_tags (...) values (?,?)   ← b
    //                                                      insert into list_product_tags (...) values (?,?)   ← c
    //   = 1 instrukcja                                   = 3 instrukcje (usuń WSZYSTKO, wstaw resztę od nowa)
    //
    // Co gorsza, przy List tak samo kończy się DODANIE tagu: produkt z 3 tagami + 1 nowy = 1 DELETE + 4 INSERT-y
    // (sprawdzone testem addToBagRecreatesCollection). Dlaczego? Worek nie ma klucza: Hibernate nie umie wskazać
    // JEDNEGO wiersza („który z dwóch identycznych wierszy?”), więc odtwarza (recreate) całą kolekcję. W Set para
    // (product_id, tag_id) jest unikalna, więc DELETE/INSERT dotyczy dokładnie jednego wiersza.
    //
    // DOBRA PRAKTYKA: kolekcja strony właścicielskiej @ManyToMany to Set. Dlaczego? Precyzyjne DELETE/INSERT
    //   zamiast kasowania i wstawiania wszystkich wierszy, a do tego brak duplikatów, które i tak nie mają sensu
    //   („produkt ma tag bio dwa razy”). Set wymaga poprawnych equals/hashCode encji (stały hashCode,
    //   s08_data_jpa/Jpa01Entities).
    //
    // PUŁAPKA: metoda pomocnicza dotyka DRUGIEJ strony. product.removeTag(tag) wywołuje tag.productsInternal()
    //   .remove(this), a to zmusza Hibernate do wczytania leniwej kolekcji tag.products (dodatkowy SELECT —
    //   test helperLoadsInverseCollection: 1 SELECT + 1 DELETE). Dlaczego to ważne? Tag „promocja” może mieć
    //   10 000 produktów — odpięcie go od JEDNEGO produktu wczyta wszystkie 10 000. Jeśli nie potrzebujesz
    //   nawigacji tag → produkty, zrób relację JEDNOKIERUNKOWĄ (bez ProductTag.products), a produkty tagu pobieraj
    //   zapytaniem: select p from Product p join p.tags t where t.name = :name.

    // =================================================================================================
    // 3. Usuwanie encji ze strony odwrotnej (ProductTag) — najpierw odpiąć
    // =================================================================================================
    //
    // em.remove(tag) usuwa wiersz z tags, ale NIE rusza product_tags — tabelę łączącą opisuje strona
    // właścicielska (Product), a o niej nikt nie powiedział. Skutek zależy od tego, co jest w pamięci:
    //   • produktu NIE ma w kontekście utrwalania → delete from tags where id=? → baza odrzuca:
    //       org.hibernate.exception.ConstraintViolationException (klucz obcy fk_product_tags_tag);
    //   • produkt JEST w pamięci i ma ten tag w kolekcji → Hibernate łapie to przy flush, zanim wyśle DELETE:
    //       java.lang.IllegalStateException: org.hibernate.TransientPropertyValueException: Persistent instance of
    //       's09_jpa_relations.rel03_many_to_many.Product' references an unsaved transient instance of
    //       's09_jpa_relations.rel03_many_to_many.ProductTag' (persist the transient instance before flushing) [...]
    //     (usunięty tag jest dla Hibernate już „nieznany”, a produkt dalej go wskazuje).
    //
    // Poprawnie: odepnij tag od wszystkich produktów, potem go usuń:
    //     List.copyOf(tag.getProducts()).forEach(p -> p.removeTag(tag));   // kopia — nie zmieniamy zbioru w pętli
    //     em.remove(tag);
    // Przy tysiącach produktów szybciej jest odpiąć tag jednym zapytaniem SQL (JdbcClient — s07_data_jdbc):
    //     delete from product_tags where tag_id = ?        a potem em.remove(tag)
    // Uwaga: SQL „obok” Hibernate nie zmienia obiektów w pamięci — działa bezpiecznie, gdy produktów tego tagu nie
    // ma w kontekście utrwalania (np. na początku osobnej transakcji).

    // =================================================================================================
    // 4. PUŁAPKA: cascade = REMOVE (albo ALL) na @ManyToMany
    // =================================================================================================
    //
    // DangerousProduct ma @ManyToMany(cascade = CascadeType.REMOVE). Usuwamy produkt „Kawa” z tagiem „promocja”,
    // który ma też „Herbata”. SQL z logu:
    //     delete from dangerous_product_tags where dangerous_product_id=?   ← wiersze łączące „Kawy” — OK
    //     delete from tags where id=?                                       ← kaskada: usuń TAG „promocja”!
    //     → ConstraintViolationException: „Herbata” dalej wskazuje ten tag (fk_dangerous_product_tags_tag).
    // A gdy tag nie ma innych produktów — kaskada usuwa go po cichu i tag znika z całego sklepu
    // (test cascadeRemoveSilentlyDeletesTag).
    //
    // PUŁAPKA: cascade REMOVE/ALL na @ManyToMany. Dlaczego? Druga strona to SAMODZIELNE encje współdzielone przez
    //   wielu „rodziców”. Usuwanie produktu ma usuwać POWIĄZANIA, a nie tagi. W najlepszym razie dostaniesz
    //   wyjątek, w najgorszym — cichą utratę danych.
    //
    // A powiązania? Te Hibernate usuwa sam, BEZ żadnej kaskady — tabela łącząca należy do strony właścicielskiej.
    // em.remove(produkt) dla zwykłego Product (SQL z logu, test removingOwnerDeletesOnlyLinks):
    //     delete from product_tags where product_id=?
    //     delete from products where id=?                   ← tagi zostają w tabeli tags

    // =================================================================================================
    // 5. Powiązanie z własnymi danymi → encja pośrednia zamiast @ManyToMany
    // =================================================================================================
    //
    // Zamówienie ↔ produkt to też „wiele do wielu”. Ale pozycja zamówienia ma ILOŚĆ (i zwykle cenę z chwili
    // zakupu). @ManyToMany obsługuje w tabeli łączącej tylko dwie kolumny kluczy — dodatkowej kolumny (quantity)
    // nie da się przez nią ani zapisać, ani odczytać.
    //
    //   ŹLE:    ShopOrder: @ManyToMany Set<Product> products       → gdzie zapisać „2 sztuki”?
    //   DOBRZE: OrderLine (własne id, własna tabela order_lines):
    //               @ManyToOne ShopOrder order;  @ManyToOne Product product;  int quantity;
    //
    // Encja pośrednia to zwykła encja: ma repozytorium/zapytania, walidację, własne pola, a w JPQL przechodzi się
    // przez nią kropką:
    //     select new s09_jpa_relations.rel03_many_to_many.ProductSales(l.product.name, sum(l.quantity))
    //     from OrderLine l group by l.product.name order by l.product.name
    // select new ... (wyrażenie konstruktora) od razu buduje rekord ProductSales — bez wczytywania encji.
    //
    // DOBRA PRAKTYKA: jeśli przewidujesz, że powiązanie dostanie kiedyś własne pole (data dodania tagu, kto go
    //   dodał), od razu zrób encję pośrednią. Dlaczego? Przejście z @ManyToMany na encję pośrednią później to
    //   zmiana schematu (nowa kolumna id, nowy klucz główny), mapowania i całego kodu, który używał kolekcji.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @ManyToMany + @JoinTable(name, joinColumns, inverseJoinColumns) po stronie właścicielskiej; druga strona:
     *     @ManyToMany(mappedBy = "pole").
     *   • Tabela łącząca: tylko dwie kolumny kluczy obcych, klucz główny = para (bez duplikatów).
     *   • Set, nie List: List (worek) przy każdej zmianie = DELETE wszystkich wierszy + INSERT wszystkich od nowa.
     *   • Metody pomocnicze addX/removeX zmieniają obie strony — ale wczytują leniwą kolekcję drugiej strony.
     *   • Relacja jednokierunkowa, gdy nie potrzebujesz nawigacji z drugiej strony (zapytanie zamiast kolekcji).
     *   • Usuwanie encji ze strony odwrotnej: najpierw odpiąć od właścicieli, potem remove.
     *   • NIGDY cascade REMOVE/ALL na @ManyToMany.
     *   • Powiązanie z własnymi danymi (ilość, data) → encja pośrednia z dwiema @ManyToOne.
     *
     * PYTANIA KONTROLNE:
     *   1. Dlaczego relacji produkt–tag nie da się zapisać kolumną klucza obcego w jednej z tabel?
     *   2. Która encja jest stroną właścicielską w parze Product–ProductTag i po czym to poznać?
     *   3. Co się stanie? ListProduct ma tagi a, b, c (kolekcja już wczytana). Usuwasz „a” z listy i robisz flush —
     *      ile instrukcji SQL wyśle Hibernate i jakich?
     *   4. ZNAJDŹ BŁĄD: {@code ProductTag bio = em.find(ProductTag.class, id); em.remove(bio); em.flush();} —
     *      tag „bio” jest przypięty do produktu, którego nie ma w pamięci. Co się stanie i jak to poprawić?
     *   5. Co się stanie? Dwa DangerousProduct mają wspólny tag; usuwasz jeden z nich (cascade = REMOVE) i robisz flush.
     *   6. Dlaczego product.removeTag(tag) może być kosztowne, choć usuwa tylko jeden wiersz?
     *   7. Kiedy @ManyToMany trzeba zamienić na encję pośrednią? Podaj przykład ze sklepu.
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rel03ManyToManyExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): utwórz i zapisz NOWY produkt (name, price) z tagami o podanych nazwach. Tagi JUŻ są
     * w bazie — znajdź je zapytaniem JPQL, nie twórz nowych. Zwróć id produktu.
     * Podpowiedź: {@code em.createQuery("select t from ProductTag t where t.name in :names", ProductTag.class)},
     * potem addTag dla każdego tagu i em.persist(produkt).
     */
    static Long exercise1(EntityManager em, String name, BigDecimal price, List<String> tagNames) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zwróć nazwy produktów, które mają tag o nazwie tagName, posortowane alfabetycznie.
     * Podpowiedź: JPQL z JOIN po kolekcji — {@code select p.name from Product p join p.tags t where t.name = :tag
     * order by p.name}.
     */
    static List<String> exercise2(EntityManager em, String tagName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (średnie): usuń tag o nazwie tagName ze sklepu — tak, żeby nie poleciał wyjątek klucza obcego,
     * a produkty zostały (bez tego tagu). Podpowiedź: znajdź tag, odepnij go od KOPII zbioru jego produktów
     * (removeTag), dopiero potem em.remove(tag).
     */
    static void exercise3(EntityManager em, String tagName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 4 (trudniejsze): dla KAŻDEGO produktu (także bez tagów!) zwróć liczbę jego tagów jako listę
     * ProductTagCount, posortowaną po nazwie produktu. Podpowiedź: {@code select new
     * s09_jpa_relations.rel03_many_to_many.ProductTagCount(p.name, count(t)) from Product p left join p.tags t
     * group by p.name order by p.name} — dlaczego LEFT JOIN, a nie JOIN?
     */
    static List<ProductTagCount> exercise4(EntityManager em) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Kolumna mieści jedną wartość w wierszu. Produkt ma WIELE tagów (kolumna tag_id w products nie wystarczy),
     *      a tag WIELE produktów (product_id w tags też nie). Potrzebna jest trzecia tabela z parami.
     *   2. Product — ma @JoinTable (opisuje tabelę łączącą). ProductTag ma mappedBy = "tags", czyli jest lustrem.
     *   3. Trzy: delete from list_product_tags where list_product_id=? (wszystkie wiersze produktu), potem dwa
     *      INSERT-y (b i c). Worek nie ma klucza, więc Hibernate odtwarza całą kolekcję. Sprawdzone testem
     *      removeFromBagRecreatesCollection (getPrepareStatementCount() == 3).
     *   4. delete from tags łamie klucz obcy product_tags.tag_id → ConstraintViolationException przy flush.
     *      em.remove na stronie odwrotnej nie czyści tabeli łączącej. Poprawka: najpierw odpiąć tag od produktów
     *      (removeTag na kopii zbioru), potem em.remove. Sprawdzone testem removingLinkedTagViolatesForeignKey.
     *   5. Hibernate usuwa wiersze łączące usuwanego produktu, a potem (kaskada) sam tag — drugi produkt dalej go
     *      wskazuje, więc baza odrzuca DELETE: ConstraintViolationException. Sprawdzone testem
     *      cascadeRemoveOnManyToManyIsDangerous.
     *   6. Bo removeTag zmienia też tag.products, a to leniwa kolekcja — Hibernate wczytuje WSZYSTKIE produkty tagu
     *      (dodatkowy SELECT). Przy popularnym tagu to tysiące wierszy. Sprawdzone testem helperLoadsInverseCollection.
     *   7. Gdy powiązanie ma własne dane: pozycja zamówienia (zamówienie–produkt) z ilością i ceną zakupu. Tabela
     *      łącząca @ManyToMany mieści tylko dwa klucze; encja pośrednia OrderLine ma własne id i pola.
     */
    // </editor-fold>
}
