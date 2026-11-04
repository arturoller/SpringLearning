package s08_data_jpa.jpa03_query_methods;

/**
 * Kategoria produktu ({@code @Enumerated(EnumType.STRING)} w Product). GARDEN (ogród) celowo nie ma w danych
 * startowych żadnego produktu — sekcja 2 pokazuje na niej wynik „zero wierszy”.
 */
public enum Category {
    COMPUTERS, HOME, OFFICE, GARDEN
}
