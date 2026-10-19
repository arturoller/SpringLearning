package s14_openapi.solutions.api02_documenting_api;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Api02DocumentingApi — zajrzyj dopiero po własnej próbie! */
public final class Api02DocumentingApiSolutions {

    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch");

    private Api02DocumentingApiSolutions() {
    }

    private static JsonNode schema(String json, String schemaName) {
        return JsonMapper.builder().build().readTree(json).get("components").get("schemas").get(schemaName);
    }

    public static List<String> solution1(String openApiJson, String schemaName) {
        JsonNode required = schema(openApiJson, schemaName).get("required");
        List<String> names = new ArrayList<>();
        if (required != null) {
            for (JsonNode name : required) {
                names.add(name.asString());
            }
        }
        names.sort(null);
        return names;
    }

    public static List<String> solution2(String openApiJson, String schemaName) {
        JsonNode properties = schema(openApiJson, schemaName).get("properties");
        List<String> withoutDescription = new ArrayList<>();
        for (String name : properties.propertyNames()) {
            if (properties.get(name).get("description") == null) {
                withoutDescription.add(name);
            }
        }
        withoutDescription.sort(null);
        return withoutDescription;
    }

    public static List<String> solution3(String openApiJson) {
        JsonNode paths = JsonMapper.builder().build().readTree(openApiJson).get("paths");
        List<String> gaps = new ArrayList<>();
        for (String path : paths.propertyNames()) {
            JsonNode pathItem = paths.get(path);
            for (String method : pathItem.propertyNames()) {
                if (!HTTP_METHODS.contains(method)) {
                    continue;
                }
                JsonNode operation = pathItem.get(method);
                if (hasPathParameter(operation) && operation.get("responses").get("404") == null) {
                    gaps.add(method.toUpperCase() + " " + path);
                }
            }
        }
        gaps.sort(null);
        return gaps;
    }

    private static boolean hasPathParameter(JsonNode operation) {
        JsonNode parameters = operation.get("parameters");
        if (parameters == null) {
            return false;
        }
        for (JsonNode parameter : parameters) {
            if ("path".equals(parameter.get("in").asString())) {
                return true;
            }
        }
        return false;
    }
}
