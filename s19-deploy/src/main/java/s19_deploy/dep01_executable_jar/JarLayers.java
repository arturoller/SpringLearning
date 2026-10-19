package s19_deploy.dep01_executable_jar;

/**
 * Pokazowa (demo) klasa lekcji Dep01ExecutableJar: do której WARSTWY (layer) trafia wpis z wykonywalnego JAR-a.
 * Odtwarza regułę, według której spring-boot-maven-plugin zapisuje plik BOOT-INF/layers.idx.
 * (Ćwiczenie 3 prosi o napisanie własnej wersji — ta jest tylko dowodem dla testu lekcji.)
 */
public final class JarLayers {

    public static final String DEPENDENCIES = "dependencies";
    public static final String SPRING_BOOT_LOADER = "spring-boot-loader";
    public static final String SNAPSHOT_DEPENDENCIES = "snapshot-dependencies";
    public static final String APPLICATION = "application";

    private JarLayers() {
    }

    /** Nazwa warstwy dla ścieżki wpisu w JAR-ze, np. {@code BOOT-INF/lib/logback-core-1.5.38.jar}. */
    public static String layerOf(String entryPath) {
        if (entryPath.startsWith("org/springframework/boot/loader/")) {
            return SPRING_BOOT_LOADER;
        }
        if (entryPath.startsWith("BOOT-INF/lib/")) {
            return entryPath.endsWith("SNAPSHOT.jar") ? SNAPSHOT_DEPENDENCIES : DEPENDENCIES;
        }
        return APPLICATION;   // BOOT-INF/classes, META-INF, classpath.idx, layers.idx
    }
}
