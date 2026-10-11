package s09_jpa_relations.rel01_many_to_one;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.List;

/**
 * <pre>
 * TEMAT: Relacja wiele-do-jednego — @ManyToOne i klucz obcy
 *        (relation/association = relacja, powiązanie między encjami; many-to-one = wiele do jednego;
 *         foreign key = klucz obcy; owning side = strona właścicielska; fetch = pobieranie; proxy = zastępca)
 *
 * W SKRÓCIE:
 *   W bazie produkt „należy” do kategorii dzięki kolumnie category_id (klucz obcy). W Javie chcemy pisać
 *   product.getCategory().getName(), a nie trzymać gołą liczbę categoryId. @ManyToOne mówi Hibernate: „to pole to
 *   inna encja, a w tabeli zapisuj tylko jej id w kolumnie z @JoinColumn”. Encja z tą adnotacją jest stroną
 *   WŁAŚCICIELSKĄ relacji — to jej tabela ma klucz obcy i to jej pole decyduje, co trafi do bazy.
 *   Domyślnie @ManyToOne jest EAGER (zachłanne): kategoria jest czytana zawsze razem z produktem. Dobra praktyka to
 *   fetch = LAZY (leniwe) — kategoria przyjdzie dopiero wtedy, gdy jej naprawdę użyjesz.
 *
 * ANALOGIA: metka na ubraniu w sklepie.
 *   Każda koszula (produkt) ma metkę „Dział: Męski” (klucz obcy). Dział nie nosi listy wszystkich koszul — to koszula
 *   wie, gdzie należy. Wiele koszul → jeden dział: to jest many-to-one. Gdy klient pyta tylko o cenę koszuli, nie
 *   musisz biec do kierownika działu (LAZY); idziesz tam dopiero, gdy klient zapyta o godziny otwarcia działu.
 *
 * JAK TO DZIAŁA:
 *   @ManyToOne(fetch = LAZY, optional = false)
 *   @JoinColumn(name = "category_id", nullable = false)
 *   private Category category;
 *     zapis:  INSERT INTO products (category_id, name, price, ...) VALUES (category.getId(), ...)
 *     odczyt LAZY:  SELECT ... FROM products WHERE id=?   → w pole category trafia proxy Category$HibernateProxy
 *                   category.getName()  → dopiero teraz SELECT ... FROM categories WHERE id=?
 *     odczyt EAGER: SELECT ... FROM eager_products LEFT JOIN categories ... → kategoria od razu w tym samym zapytaniu
 *   Kolumnę i klucz obcy tworzy migracja Flyway (db/migration/rel01); Hibernate z ddl-auto=validate tylko sprawdza,
 *   czy kolumna category_id istnieje i ma pasujący typ.
 *
 * SŁÓWKA:
 *   many-to-one = wiele do jednego; join column = kolumna złączenia (klucz obcy); owning side = strona właścicielska;
 *   inverse side = strona odwrotna (lustrzana); fetch type = sposób pobierania; eager = zachłanny (od razu);
 *   lazy = leniwy (na żądanie); optional = opcjonalny; nullable = dopuszczający NULL; transient = przejściowy
 *   (obiekt jeszcze niezapisany, nieznany Hibernate); reference = referencja, odnośnik; initialized = zainicjowany.
 *
 * ZOBACZ TEŻ: s08_data_jpa/Jpa01Entities (encje, persist, flush, clear), s08_data_jpa/Jpa02Repositories (repozytoria),
 *             t29_jdbc_databases/Jdbc06SqlJoins (klucze obce i JOIN w czystym SQL),
 *             s09_jpa_relations/Rel02OneToMany (druga strona relacji), s09_jpa_relations/Rel04LazyLoading (proxy od środka).
 * </pre>
 */
public final class Rel01ManyToOne {

    private Rel01ManyToOne() {
    }

    // =================================================================================================
    // 1. @ManyToOne + @JoinColumn — klucz obcy w tabeli „wiele”
    // =================================================================================================
    //
    // Tabela (migracja V1__categories_and_products.sql):
    //     create table products (..., category_id bigint not null,
    //         constraint fk_products_category foreign key (category_id) references categories (id));
    //
    // Encja (Product):
    //     @ManyToOne(fetch = FetchType.LAZY, optional = false)   // wiele produktów → jedna kategoria
    //     @JoinColumn(name = "category_id", nullable = false)    // join column = kolumna klucza obcego
    //     private Category category;                             // w Javie OBIEKT, w tabeli tylko jego id
    //
    // Bez @JoinColumn Hibernate też zgadnie nazwę: pole category + „_” + kolumna id kategorii = category_id.
    // Podawaj ją jednak jawnie — schemat tworzy Flyway, a jawna nazwa to jasna umowa między encją a migracją.
    //
    // Strona właścicielska (owning side) = encja, której tabela ma klucz obcy. Tu: Product. Tylko zmiana pola
    // product.category zmienia kolumnę category_id (więcej o tym w Rel02OneToMany).
    //
    // DOBRA PRAKTYKA: indeks na kolumnie klucza obcego (create index ix_products_category ...). Dlaczego? Zapytania
    //   „produkty z kategorii X” i każdy JOIN szukają po category_id. Na przykład PostgreSQL NIE zakłada takiego
    //   indeksu sam — bez niego przy dużej tabeli baza przegląda wszystkie wiersze.

    // =================================================================================================
    // 2. fetch: EAGER (domyślnie) kontra LAZY
    // =================================================================================================
    //
    // Specyfikacja JPA: @ManyToOne i @OneToOne są domyślnie EAGER, a @OneToMany i @ManyToMany — LAZY.
    // EagerProduct (bez fetch) — Hibernate przy find od razu łączy tabele (wypisany SQL, profil rel01):
    //
    //     select ep1_0.id, c1_0.id, c1_0.name, ep1_0.name
    //     from eager_products ep1_0
    //     left join categories c1_0 on c1_0.id=ep1_0.category_id
    //     where ep1_0.id=?
    //
    // Product (fetch = LAZY) — tylko jedna tabela, a w polu category siedzi PROXY (zastępca) klasy
    // Category$HibernateProxy (podklasa Category generowana przez Hibernate):
    //
    //     select p1_0.id, p1_0.category_id, p1_0.name, p1_0.price from products p1_0 where p1_0.id=?
    //
    // Proxy zna tylko id (Hibernate przeczytał je z kolumny category_id), więc getId() NIE idzie do bazy. Pierwsze
    // wywołanie innej metody (getName()) wysyła drugi SELECT do categories. Hibernate.isInitialized(obiekt)
    // (czy zainicjowany) mówi, czy proxy ma już dane.
    //
    // DOBRA PRAKTYKA: zawsze @ManyToOne(fetch = FetchType.LAZY). Dlaczego? EAGER działa ZAWSZE — także tam, gdzie
    //   kategorii nie potrzebujesz; w zapytaniu JPQL „select p from EagerProduct p” Hibernate dokłada osobny SELECT
    //   dla każdej kategorii: 3 produkty z 3 różnych kategorii = 4 zapytania (zmierzone statystykami Hibernate w teście
    //   eagerInJpqlCausesExtraSelects; to problem N+1 — Rel05NPlusOne). LAZY jest bezpieczne, a gdy kategorii naprawdę
    //   potrzebujesz, pobierasz ją świadomie (join fetch, @EntityGraph — Rel04LazyLoading).
    //
    // PUŁAPKA: w equals encji porównuj other.getId(), a nie other.id. Dlaczego? „other” może być proxy — jego POLA
    //   są puste (null), dane ma dopiero ukryty prawdziwy obiekt; getter proxy zwraca poprawne id. Z tego samego
    //   powodu hashCode zwraca Category.class.hashCode(), a nie getClass().hashCode() (klasa proxy jest inna).

    // =================================================================================================
    // 3. optional = false i nullable = false — produkt MUSI mieć kategorię
    // =================================================================================================
    //
    // optional = false — informacja dla Hibernate: relacja wymagana. Hibernate sprawdza ją już przy persist,
    //   ZANIM wyśle INSERT: PropertyValueException „not-null property references a null or transient value”.
    // nullable = false (w @JoinColumn) — opis kolumny: NOT NULL. Przy ddl-auto=create Hibernate dodałby „not null”;
    //   u nas schemat robi Flyway, więc tę samą regułę zapisujemy też w migracji (category_id bigint not null).
    //
    // DOBRA PRAKTYKA: wymaganą relację oznacz w TRZECH miejscach: optional = false, nullable = false i NOT NULL
    //   w migracji. Dlaczego? Encja wyłapie błąd wcześnie (czytelny komunikat), a baza i tak jest ostatnią linią
    //   obrony — gdy ktoś wstawi wiersz zwykłym SQL, z pominięciem Hibernate.

    // =================================================================================================
    // 4. Kategoria niezapisana (transient) kontra zapisana
    // =================================================================================================
    //
    // Category nowa = new Category("Nowa");                 // transient: brak id, Hibernate jej nie zna
    // em.persist(new Product("Kakao", ..., nowa));
    // em.flush();   → IllegalStateException z przyczyną TransientPropertyValueException:
    //                 „Instance of '...Product' references an unsaved transient instance of '...Category'
    //                  (persist the transient instance)”
    // To samo przez repozytorium (products.saveAndFlush) → InvalidDataAccessApiUsageException (Spring tłumaczy
    // wyjątki Hibernate na swoją hierarchię DataAccessException). Hibernate 7 dodatkowo ostrzega w logu:
    // HHH90032003 „Attempting to save one or more entities that have a non-nullable association with an unsaved
    // transient entity”.
    //
    // Dlaczego? Do kolumny category_id trzeba wpisać id kategorii, a niezapisana kategoria id jeszcze nie ma.
    // Hibernate NIE zapisuje jej „przy okazji”, bo na relacji nie ma kaskady (cascade — Rel02OneToMany).
    //
    // DOBRA PRAKTYKA: najpierw zapisz (albo wczytaj) kategorię, potem produkt. NIE dodawaj cascade do @ManyToOne.
    //   Dlaczego? Kategoria żyje własnym życiem — kaskada z produktu do kategorii (zwłaszcza REMOVE) to proszenie
    //   się o kłopoty: usunięcie jednego produktu próbowałoby usunąć kategorię wszystkich innych produktów.

    // =================================================================================================
    // 5. getReferenceById — ustaw klucz obcy BEZ czytania kategorii
    // =================================================================================================
    //
    // Chcesz przenieść produkt do kategorii o id 7. Masz tylko liczbę 7, a nie obiekt Category.
    //   PRZED: categories.findById(7).orElseThrow() → SELECT z categories tylko po to, żeby wpisać 7 do kolumny.
    //   PO:    categories.getReferenceById(7)   (EntityManager: em.getReference(Category.class, 7))
    //          → proxy z samym id, BEZ SELECT; product.setCategory(proxy); przy flush: UPDATE products SET category_id=7
    //
    // PUŁAPKA: getReferenceById NIE sprawdza, czy taki wiersz istnieje. Dlaczego to ważne? Przy złym id błąd poleci
    //   dopiero przy flush (naruszenie klucza obcego) albo przy pierwszym użyciu proxy (EntityNotFoundException).
    //   Gdy id pochodzi od użytkownika i chcesz ładny komunikat „nie ma takiej kategorii” — użyj findById.

    // =================================================================================================
    // 6. Zapytania „przez relację”
    // =================================================================================================
    //
    // ProductRepository.findByCategoryNameOrderByName("Kawa") — Spring Data czyta nazwę: Category.Name → pole
    // category, a w nim name. Wygenerowany SQL:
    //
    //     select p1_0.id, p1_0.category_id, p1_0.name, p1_0.price
    //     from products p1_0 join categories c1_0 on c1_0.id=p1_0.category_id
    //     where c1_0.name=? order by p1_0.name
    //
    // W JPQL piszesz to samo kropką: select p from Product p where p.category.name = :name — Hibernate sam dopisze
    // JOIN, bo zna relację z adnotacji. Kategorie produktów i tak przychodzą jako proxy (LAZY) — JOIN służy tu tylko
    // do filtrowania.

    /*
     * =================================================================================================
     * ŚCIĄGA:
     *   • @ManyToOne + @JoinColumn(name = "category_id") — klucz obcy w tabeli strony „wiele” (strona właścicielska).
     *   • Domyślnie @ManyToOne/@OneToOne = EAGER; @OneToMany/@ManyToMany = LAZY.
     *   • Zawsze fetch = FetchType.LAZY; kategorię pobieraj świadomie (join fetch, @EntityGraph).
     *   • LAZY → proxy Klasa$HibernateProxy; getId() bez SELECT; inne gettery → SELECT; Hibernate.isInitialized(x).
     *   • Wymagana relacja: optional = false + nullable = false + NOT NULL w migracji.
     *   • Niezapisana kategoria → TransientPropertyValueException (przez repozytorium: InvalidDataAccessApiUsageException).
     *   • Masz tylko id? getReferenceById(id) — proxy bez SELECT, do ustawienia klucza obcego.
     *   • Indeks na kolumnie klucza obcego; equals encji przez getId() (proxy!).
     *
     * PYTANIA KONTROLNE:
     *   1. Która tabela ma klucz obcy w relacji produkt → kategoria i dlaczego właśnie ta?
     *   2. Jaki jest domyślny fetch dla @ManyToOne, a jaki dla @OneToMany?
     *   3. Co się stanie? em.find(Product.class, id) (fetch = LAZY), a potem product.getCategory().getId() — ile
     *      zapytań SELECT pójdzie do bazy i czy proxy zostanie zainicjowane?
     *   4. ZNAJDŹ BŁĄD: {@code products.save(new Product("Kakao", cena, new Category("Napoje")));} — w module nie ma
     *      kaskady. Co się stanie i jak to naprawić?
     *   5. Co się stanie? em.persist(new Product("X", cena, null)) przy optional = false.
     *   6. Kiedy getReferenceById jest lepsze od findById, a kiedy gorsze?
     *   7. Dlaczego equals encji powinien wołać other.getId(), a nie czytać pole other.id?
     *   (odpowiedzi w zwiniętym bloku na samym końcu pliku)
     * =================================================================================================
     */

    // =================================================================================================
    // ĆWICZENIA — testy: Rel01ManyToOneExercisesTest
    // =================================================================================================

    /**
     * ĆWICZENIE 1 (łatwe): zapisz NOWĄ kategorię o nazwie categoryName i NOWY produkt w tej kategorii; zwróć id
     * produktu. Podpowiedź: kolejność ma znaczenie — najpierw categories.save(...), potem products.save(...).
     */
    static Long exercise1(CategoryRepository categories, ProductRepository products,
                          String categoryName, String productName, BigDecimal price) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 2 (średnie): zapytaniem JPQL zwróć nazwy produktów z kategorii o podanej nazwie, posortowane
     * alfabetycznie. Podpowiedź: em.createQuery("select p.name from Product p where p.category.name = :name order by
     * p.name", String.class).setParameter(...).getResultList().
     */
    static List<String> exercise2(EntityManager em, String categoryName) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * ĆWICZENIE 3 (trudniejsze): przenieś produkt productId do kategorii newCategoryId BEZ wczytywania nowej kategorii
     * z bazy (test sprawdza, że jej proxy zostało niezainicjowane). Podpowiedź: em.find dla produktu,
     * em.getReference dla kategorii, setter; zapis zrobi flush (dirty checking — nie trzeba persist).
     */
    static void exercise3(EntityManager em, Long productId, Long newCategoryId) {
        // TODO: twoje rozwiązanie
        throw new UnsupportedOperationException("TODO");
    }

    // <editor-fold desc="ODPOWIEDZI NA PYTANIA KONTROLNE" defaultstate="collapsed">
    /*
     * ODPOWIEDZI:
     *   1. Tabela products (strona „wiele”). Wiersz ma jedną kolumnę na jedną wartość — produkt ma jedną kategorię,
     *      więc mieści ją w jednej kolumnie category_id. Kategoria ma wiele produktów, nie zmieściłaby ich w kolumnie.
     *   2. @ManyToOne — EAGER (tak mówi specyfikacja JPA), @OneToMany — LAZY. Dlatego przy @ManyToOne trzeba LAZY
     *      ustawić samemu.
     *   3. Jeden SELECT (z products). getId() proxy zna bez bazy, więc proxy zostaje niezainicjowane.
     *      Sprawdzone testem lazyFetchGivesProxy w Rel01ManyToOneTest.
     *   4. Kategoria jest transient (niezapisana), a relacja nie ma kaskady → przy flush InvalidDataAccessApiUsageException
     *      z przyczyną TransientPropertyValueException („references an unsaved transient instance”). Naprawa: najpierw
     *      categories.save(kategoria), potem zapis produktu. Sprawdzone testem transientCategoryWithRepository.
     *   5. PropertyValueException „not-null property references a null or transient value” — już przy persist,
     *      przed wysłaniem INSERT. Sprawdzone testem requiredCategory.
     *   6. Lepsze, gdy masz pewne id i chcesz tylko ustawić klucz obcy (brak zbędnego SELECT). Gorsze, gdy id może być
     *      błędne — getReferenceById nie sprawdza istnienia wiersza, błąd wyjdzie później i mniej czytelnie.
     *   7. Bo „other” może być proxy Hibernate: jego pola są puste, a getter przekazuje wywołanie do prawdziwego
     *      obiektu (albo zna id bez bazy). Czytanie pola dałoby null i fałszywe „nie są równe”.
     */
    // </editor-fold>
}
