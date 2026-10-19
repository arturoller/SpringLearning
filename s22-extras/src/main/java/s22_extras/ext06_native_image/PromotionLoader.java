package s22_extras.ext06_native_image;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ładuje promocje z pliku ext06/promocje.txt (lekcja Ext06NativeImage, sekcja 3). DWIE rzeczy, których kompilator
 * obrazu natywnego nie przewidzi, czytając kod:
 * <ol>
 *   <li>plik z classpath (zasób) — wczytywany po nazwie w czasie działania,</li>
 *   <li>klasy tworzone refleksją — ich nazwy są w pliku, nie w kodzie.</li>
 * </ol>
 * Na JVM działa bez żadnych przygotowań. W obrazie natywnym — tylko z podpowiedziami z Ext06RuntimeHints.
 */
@Component
public class PromotionLoader {

    public static final String PROMOTIONS_FILE = "ext06/promocje.txt";

    public List<PromotionRule> loadPromotions() {
        return readClassNames().stream().map(PromotionLoader::instantiate).toList();
    }

    List<String> readClassNames() {
        try {
            String content = new ClassPathResource(PROMOTIONS_FILE).getContentAsString(StandardCharsets.UTF_8);
            return content.lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Nie mogę przeczytać " + PROMOTIONS_FILE, e);
        }
    }

    static PromotionRule instantiate(String className) {
        try {
            Class<?> type = Class.forName(className);                                  // refleksja: klasa po nazwie
            return (PromotionRule) type.getDeclaredConstructor().newInstance();        // refleksja: wywołanie konstruktora
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nie mogę utworzyć promocji " + className, e);
        }
    }
}
