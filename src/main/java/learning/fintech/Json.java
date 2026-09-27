package learning.fintech;

import java.util.LinkedHashMap;
import java.util.Map;

final class Json {
    private final String input;
    private int cursor;

    private Json(String input) { this.input = input; }

    static Object parse(String input) {
        Json reader = new Json(input);
        Object result = reader.value();
        reader.space();
        if (reader.cursor != input.length()) throw new IllegalArgumentException("Trailing JSON");
        return result;
    }

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            StringBuilder out = new StringBuilder("{");
            for (var entry : map.entrySet()) {
                if (out.length() > 1) out.append(',');
                out.append(write(entry.getKey().toString())).append(':').append(write(entry.getValue()));
            }
            return out.append('}').toString();
        }
        StringBuilder out = new StringBuilder("\"");
        for (char ch : value.toString().toCharArray()) {
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 32) out.append(String.format("\\u%04x", (int) ch));
                    else out.append(ch);
                }
            }
        }
        return out.append('"').toString();
    }

    private void space() {
        while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) cursor++;
    }

    private char take() {
        if (cursor >= input.length()) throw new IllegalArgumentException("Incomplete JSON");
        return input.charAt(cursor++);
    }

    private Object value() {
        space();
        char ch = take();
        if (ch == '{') {
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (input.charAt(cursor) == '}') { cursor++; return result; }
            do {
                space();
                String key = string();
                space();
                if (take() != ':') throw new IllegalArgumentException("Expected colon");
                result.put(key, value());
                space();
                ch = take();
            } while (ch == ',');
            if (ch != '}') throw new IllegalArgumentException("Expected object end");
            return result;
        }
        if (ch == '"') { cursor--; return string(); }
        if (ch == 't' && input.startsWith("rue", cursor)) { cursor += 3; return true; }
        if (ch == 'f' && input.startsWith("alse", cursor)) { cursor += 4; return false; }
        if (ch == 'n' && input.startsWith("ull", cursor)) { cursor += 3; return null; }
        if (ch == '-' || Character.isDigit(ch)) {
            int start = cursor - 1;
            while (cursor < input.length() && "0123456789.eE+-".indexOf(input.charAt(cursor)) >= 0) cursor++;
            return Double.valueOf(input.substring(start, cursor));
        }
        throw new IllegalArgumentException("Unexpected JSON value");
    }

    private String string() {
        if (take() != '"') throw new IllegalArgumentException("Expected string");
        StringBuilder result = new StringBuilder();
        while (true) {
            char ch = take();
            if (ch == '"') return result.toString();
            if (ch != '\\') { result.append(ch); continue; }
            ch = take();
            switch (ch) {
                case '"', '\\', '/' -> result.append(ch);
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'u' -> {
                    result.append((char) Integer.parseInt(input.substring(cursor, cursor + 4), 16));
                    cursor += 4;
                }
                default -> throw new IllegalArgumentException("Invalid escape");
            }
        }
    }
}
