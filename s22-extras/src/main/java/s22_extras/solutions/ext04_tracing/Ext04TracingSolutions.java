package s22_extras.solutions.ext04_tracing;

import s22_extras.ext04_tracing.Ext04Tracing.SpanInfo;
import s22_extras.ext04_tracing.Ext04Tracing.TraceParent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Rozwiązania wzorcowe ćwiczeń lekcji Ext04Tracing — zajrzyj dopiero po własnej próbie! */
public final class Ext04TracingSolutions {

    private static final String ROOT_PARENT = "0000000000000000";

    private Ext04TracingSolutions() {
    }

    static TraceParent solution1(String header) {
        String[] parts = header.split("-");
        if (parts.length != 4
                || !parts[0].matches("[0-9a-f]{2}")
                || !parts[1].matches("[0-9a-f]{32}")
                || !parts[2].matches("[0-9a-f]{16}")
                || !parts[3].matches("[0-9a-f]{2}")) {
            throw new IllegalArgumentException("Niepoprawny nagłówek traceparent: " + header);
        }
        boolean sampled = (Integer.parseInt(parts[3], 16) & 1) == 1;   // najmłodszy bit flag = „zapisz”
        return new TraceParent(parts[0], parts[1], parts[2], sampled);
    }

    static String solution2(String traceId, String spanId, boolean sampled) {
        if (traceId == null || !traceId.matches("[0-9a-f]{32}") || traceId.chars().allMatch(c -> c == '0')) {
            throw new IllegalArgumentException("Niepoprawny traceId: " + traceId);
        }
        if (spanId == null || !spanId.matches("[0-9a-f]{16}") || spanId.chars().allMatch(c -> c == '0')) {
            throw new IllegalArgumentException("Niepoprawny spanId: " + spanId);
        }
        return "00-" + traceId + "-" + spanId + "-" + (sampled ? "01" : "00");
    }

    static List<String> solution3(List<SpanInfo> spans) {
        Map<String, List<SpanInfo>> childrenByParent = spans.stream()
                .collect(Collectors.groupingBy(SpanInfo::parentSpanId));
        List<String> lines = new ArrayList<>();
        appendChildren(ROOT_PARENT, 0, childrenByParent, lines);
        return lines;
    }

    private static void appendChildren(String parentId, int depth, Map<String, List<SpanInfo>> childrenByParent,
                                       List<String> lines) {
        List<SpanInfo> children = childrenByParent.getOrDefault(parentId, List.of()).stream()
                .sorted(Comparator.comparing(SpanInfo::name))
                .toList();
        for (SpanInfo child : children) {
            lines.add("  ".repeat(depth) + child.name());
            appendChildren(child.spanId(), depth + 1, childrenByParent, lines);
        }
    }
}
