package s14_openapi.solutions.api03_groups_and_versions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Rozwiązania wzorcowe ćwiczeń lekcji Api03GroupsAndVersions — zajrzyj dopiero po własnej próbie! */
public final class Api03GroupsAndVersionsSolutions {

    private static final Set<String> HTTP_METHODS = Set.of("get", "put", "post", "delete", "patch");

    private Api03GroupsAndVersionsSolutions() {
    }

    private static JsonNode parse(String json) {
        return JsonMapper.builder().build().readTree(json);
    }

    public static List<String> solution1(String swaggerConfigJson) {
        JsonNode urls = parse(swaggerConfigJson).get("urls");
        List<String> names = new ArrayList<>();
        if (urls != null) {
            for (JsonNode entry : urls) {
                names.add(entry.get("name").asString());
            }
        }
        names.sort(null);
        return names;
    }

    public static List<String> solution2(String openApiJson) {
        JsonNode paths = parse(openApiJson).get("paths");
        List<String> secured = new ArrayList<>();
        for (String path : paths.propertyNames()) {
            JsonNode pathItem = paths.get(path);
            for (String method : pathItem.propertyNames()) {
                if (HTTP_METHODS.contains(method) && pathItem.get(method).get("security") != null) {
                    secured.add(method.toUpperCase() + " " + path);
                }
            }
        }
        secured.sort(null);
        return secured;
    }

    public static List<String> solution3(String openApiJson, String path, String method, String parameterName) {
        JsonNode pathItem = parse(openApiJson).get("paths").get(path);
        JsonNode operation = pathItem == null ? null : pathItem.get(method);
        if (operation == null) {
            throw new IllegalArgumentException("W dokumencie nie ma operacji " + method + " " + path);
        }
        List<String> values = new ArrayList<>();
        JsonNode parameters = operation.get("parameters");
        if (parameters != null) {
            for (JsonNode parameter : parameters) {
                if (parameterName.equals(parameter.get("name").asString())) {
                    JsonNode enumValues = parameter.get("schema") == null ? null : parameter.get("schema").get("enum");
                    if (enumValues != null) {
                        for (JsonNode value : enumValues) {
                            values.add(value.asString());
                        }
                    }
                }
            }
        }
        values.sort(null);
        return values;
    }
}
