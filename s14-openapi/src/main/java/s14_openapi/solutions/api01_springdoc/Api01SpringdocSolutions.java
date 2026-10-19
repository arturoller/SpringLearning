package s14_openapi.solutions.api01_springdoc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Api01Springdoc — zajrzyj dopiero po własnej próbie! */
public final class Api01SpringdocSolutions {

    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch");

    private Api01SpringdocSolutions() {
    }

    private static JsonNode parse(String json) {
        return JsonMapper.builder().build().readTree(json);
    }

    public static List<String> solution1(String openApiJson) {
        List<String> paths = new ArrayList<>(parse(openApiJson).get("paths").propertyNames());
        paths.sort(null);
        return paths;
    }

    public static List<String> solution2(String openApiJson) {
        JsonNode paths = parse(openApiJson).get("paths");
        List<String> operations = new ArrayList<>();
        for (String path : paths.propertyNames()) {
            for (String key : paths.get(path).propertyNames()) {
                if (HTTP_METHODS.contains(key)) {
                    operations.add(key.toUpperCase() + " " + path);
                }
            }
        }
        operations.sort(null);
        return operations;
    }

    public static List<String> solution3(String openApiJson, String path, String method, Set<String> expected) {
        JsonNode pathNode = parse(openApiJson).get("paths").get(path);
        JsonNode operation = pathNode == null ? null : pathNode.get(method);
        if (operation == null) {
            throw new IllegalArgumentException("W dokumencie nie ma operacji " + method + " " + path);
        }
        JsonNode responses = operation.get("responses");
        List<String> missing = new ArrayList<>();
        for (String code : expected) {
            if (responses == null || responses.get(code) == null) {
                missing.add(code);
            }
        }
        missing.sort(null);
        return missing;
    }
}
