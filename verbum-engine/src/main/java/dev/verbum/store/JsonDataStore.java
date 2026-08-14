package dev.verbum.store;

import dev.verbum.error.VerbumError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A dependency-free JSON {@link DataStore} used for variable persistence.
 * Verbum variables are Double, String, Boolean, List and nested String-key
 * Maps, which is exactly what this tiny serializer supports.
 */
public final class JsonDataStore implements DataStore {

    private final Path file;

    public JsonDataStore(Path file) {
        this.file = file;
    }

    @SuppressWarnings("unchecked")
    @Override public Map<String, Object> load() {
        if (!Files.exists(file)) return new LinkedHashMap<>();
        try {
            Object parsed = parse(new Peek(new String(Files.readAllBytes(file))));
            if (parsed instanceof Map<?, ?> m) {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
                return out;
            }
            return new LinkedHashMap<>();
        } catch (Exception e) {
            throw new VerbumError("I could not read the saved data file " + file + "\n" + e.getMessage());
        }
    }

    @Override public void save(Map<String, Object> variables) {
        try {
            Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
            Files.writeString(file, render(variables, 0));
        } catch (IOException e) {
            throw new VerbumError("I could not write the saved data file " + file + "\n" + e.getMessage());
        }
    }

    // ------------------------------------------------------------- writing

    private static String render(Object v, int depth) {
        String indent = "  ".repeat(depth);
        String childIndent = "  ".repeat(depth + 1);
        if (v == null) return "null";
        if (v instanceof Boolean b) return b ? "true" : "false";
        if (v instanceof Double d) {
            double dv = d;
            return dv == Math.rint(dv) ? String.valueOf((long) dv) : String.valueOf(dv);
        }
        if (v instanceof Number n) return n.toString();
        if (v instanceof String s) return quote(s);
        if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sb.append(childIndent).append(quote(String.valueOf(e.getKey()))).append(": ")
                        .append(render(e.getValue(), depth + 1));
                if (++i < m.size()) sb.append(',');
                sb.append('\n');
            }
            return sb.append(indent).append('}').toString();
        }
        if (v instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < l.size(); i++) {
                sb.append(childIndent).append(render(l.get(i), depth + 1));
                if (i + 1 < l.size()) sb.append(',');
                sb.append('\n');
            }
            return sb.append(indent).append(']').toString();
        }
        return quote(String.valueOf(v));
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    // ------------------------------------------------------------- reading

    private static final class Peek {
        final String s;
        int i = 0;
        Peek(String s) { this.s = s; }
        char peek() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            return i < s.length() ? s.charAt(i) : '\0';
        }
        void next() { i++; }
    }

    private static Object parse(Peek p) {
        char c = p.peek();
        if (c == '{') return parseObject(p);
        if (c == '[') return parseArray(p);
        if (c == '"') return parseString(p);
        if (c == 't') { p.next(); return consume(p, "rue") ? Boolean.TRUE : null; }
        if (c == 'f') { p.next(); return consume(p, "alse") ? Boolean.FALSE : null; }
        if (c == 'n') { p.next(); consume(p, "ull"); return null; }
        return parseNumber(p);
    }

    private static Map<String, Object> parseObject(Peek p) {
        Map<String, Object> out = new LinkedHashMap<>();
            p.next();                    // {
        if (p.peek() == '}') { p.next(); return out; }
        while (true) {
            if (p.peek() == '}') { p.next(); return out; }
            String key = parseString(p);
            while (p.peek() != ':') p.next();
            p.next();                    // :
            out.put(key, parse(p));
            char c = p.peek();
            if (c == ',') p.next();
            else if (c == '}') { p.next(); return out; }
        }
    }

    private static List<Object> parseArray(Peek p) {
        List<Object> out = new ArrayList<>();
        p.next();                        // [
        if (p.peek() == ']') { p.next(); return out; }
        while (true) {
            out.add(parse(p));
            char c = p.peek();
            if (c == ',') p.next();
            else if (c == ']') { p.next(); return out; }
        }
    }

    private static String parseString(Peek p) {
        StringBuilder sb = new StringBuilder();
        p.next();                        // opening quote
        while (p.i < p.s.length()) {
            char c = p.s.charAt(p.i++);
            if (c == '"') break;
            if (c == '\\' && p.i < p.s.length()) {
                char n = p.s.charAt(p.i++);
                sb.append(switch (n) {
                    case 'n' -> '\n';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    default -> n;
                });
            } else sb.append(c);
        }
        return sb.toString();
    }

    private static boolean consume(Peek p, String rest) {
        for (int k = 0; k < rest.length(); k++) {
            if (p.i < p.s.length() && p.s.charAt(p.i) == rest.charAt(k)) p.i++; else return false;
        }
        return true;
    }

    private static Object parseNumber(Peek p) {
        StringBuilder sb = new StringBuilder();
        while (p.i < p.s.length()) {
            char c = p.s.charAt(p.i);
            if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c)) break;
            sb.append(c);
            p.i++;
        }
        String t = sb.toString();
        if (t.isEmpty()) return 0.0;
        if (t.indexOf('.') >= 0) return Double.parseDouble(t);
        return (double) Long.parseLong(t);
    }
}