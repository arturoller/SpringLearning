package s22_extras.ext01_spring_ai;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Maskowanie danych osobowych PRZED wysłaniem tekstu do modelu (lekcja Ext01SpringAi, sekcja 6).
 * Ta wersja maskuje tylko adresy e-mail — ćwiczenie 3 rozszerza ją o telefony i PESEL.
 */
@Component
public class PersonalDataMasker {

    /** Prosty wzorzec adresu e-mail: znaki@domena.końcówka (wystarczy do maskowania, nie do walidacji). */
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

    public String mask(String text) {
        return EMAIL.matcher(text).replaceAll("[EMAIL]");
    }
}
