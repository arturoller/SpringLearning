package s03_boot_basics.boot04_starters;

import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.jar.JarFile;

/**
 * ClasspathInspector = „inspektor classpath”: małe narzędzia do sprawdzania, co jest na classpath (ścieżce klas)
 * TEGO modułu i z którego jara pochodzi dana klasa. Używają go lekcja Boot04Starters i jej testy.
 */
public final class ClasspathInspector {

    private ClasspathInspector() {
    }

    /** Czy klasa o pełnej nazwie jest na classpath? Sprawdza, ale nie inicjalizuje klasy (ClassUtils.isPresent). */
    public static boolean isPresent(String fullyQualifiedClassName) {
        return ClassUtils.isPresent(fullyQualifiedClassName, ClasspathInspector.class.getClassLoader());
    }

    /**
     * Ile plików .class ma jar o nazwie zaczynającej się od {@code jarFileName} (np. „spring-boot-starter-4.1.1.jar”)?
     * Szuka jara po pliku META-INF/MANIFEST.MF, który ma KAŻDY jar — także taki bez żadnej klasy.
     * Zwraca -1, gdy takiego jara nie ma na classpath.
     */
    public static long countClassesInJar(String jarFileName) {
        try {
            for (URL manifest : Collections.list(ClasspathInspector.class.getClassLoader().getResources("META-INF/MANIFEST.MF"))) {
                if ("jar".equals(manifest.getProtocol()) && manifest.getPath().contains("/" + jarFileName + "!")) {
                    JarURLConnection connection = (JarURLConnection) manifest.openConnection();
                    connection.setUseCaches(false);
                    try (JarFile jar = connection.getJarFile()) {
                        return jar.stream().filter(entry -> entry.getName().endsWith(".class")).count();
                    }
                }
            }
            return -1;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
