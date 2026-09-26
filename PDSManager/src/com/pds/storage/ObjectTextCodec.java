package com.pds.storage;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic, reflection-based flat-text (de)serializer for the plain-field model
 * classes in com.pds.model. Produces "dotted.key=value" lines which are simple
 * to diff, checksum and encrypt. No external library is used.
 *
 * Supported field types: String, boolean/Boolean, enum, a nested plain object
 * (recursed), and List<String> or List<PlainObject>.
 */
public final class ObjectTextCodec {

    private ObjectTextCodec() {}

    // ---------- ENCODE ----------

    public static Map<String, String> encode(Object root) {
        Map<String, String> out = new LinkedHashMap<>();
        encodeObject(root, "", out);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void encodeObject(Object obj, String prefix, Map<String, String> out) {
        if (obj == null) return;
        for (Field f : obj.getClass().getFields()) {
            f.setAccessible(true);
            String key = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
            try {
                Object val = f.get(obj);
                Class<?> type = f.getType();
                if (type == String.class) {
                    out.put(key, val == null ? "" : (String) val);
                } else if (type == boolean.class) {
                    out.put(key, String.valueOf(val));
                } else if (type == Boolean.class) {
                    out.put(key, val == null ? "" : String.valueOf(val));
                } else if (type.isEnum()) {
                    out.put(key, val == null ? "" : ((Enum<?>) val).name());
                } else if (List.class.isAssignableFrom(type)) {
                    List<Object> list = (List<Object>) val;
                    int count = list == null ? 0 : list.size();
                    out.put(key + ".count", String.valueOf(count));
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            Object item = list.get(i);
                            String itemKey = key + "." + i;
                            if (item instanceof String) {
                                out.put(itemKey, (String) item);
                            } else {
                                encodeObject(item, itemKey, out);
                            }
                        }
                    }
                } else {
                    // nested plain object
                    encodeObject(val, key, out);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("codec encode failed on field " + key, e);
            }
        }
    }

    // ---------- DECODE ----------

    public static <T> T decode(Map<String, String> data, Class<T> rootType) {
        return decodeObject(data, "", rootType);
    }

    @SuppressWarnings("unchecked")
    private static <T> T decodeObject(Map<String, String> data, String prefix, Class<T> type) {
        try {
            T obj = type.getDeclaredConstructor().newInstance();
            for (Field f : type.getFields()) {
                f.setAccessible(true);
                String key = prefix.isEmpty() ? f.getName() : prefix + "." + f.getName();
                Class<?> ft = f.getType();
                if (ft == String.class) {
                    f.set(obj, data.getOrDefault(key, ""));
                } else if (ft == boolean.class) {
                    f.set(obj, Boolean.parseBoolean(data.getOrDefault(key, "false")));
                } else if (ft == Boolean.class) {
                    String v = data.get(key);
                    f.set(obj, (v == null || v.isEmpty()) ? null : Boolean.parseBoolean(v));
                } else if (ft.isEnum()) {
                    String v = data.get(key);
                    if (v != null && !v.isEmpty()) {
                        f.set(obj, Enum.valueOf((Class<Enum>) ft, v));
                    }
                } else if (List.class.isAssignableFrom(ft)) {
                    String countStr = data.get(key + ".count");
                    int count = countStr == null ? 0 : Integer.parseInt(countStr);
                    ParameterizedType pt = (ParameterizedType) f.getGenericType();
                    Class<?> elemType = (Class<?>) pt.getActualTypeArguments()[0];
                    List<Object> list = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        String itemKey = key + "." + i;
                        if (elemType == String.class) {
                            list.add(data.getOrDefault(itemKey, ""));
                        } else {
                            list.add(decodeObject(data, itemKey, elemType));
                        }
                    }
                    f.set(obj, list);
                } else {
                    f.set(obj, decodeObject(data, key, ft));
                }
            }
            return obj;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("codec decode failed for " + type + " at prefix '" + prefix + "'", e);
        }
    }

    // ---------- TEXT (LINES) <-> MAP ----------

    public static String toText(Map<String, String> data) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : data.entrySet()) {
            sb.append(e.getKey()).append('=').append(escape(e.getValue())).append('\n');
        }
        return sb.toString();
    }

    public static Map<String, String> fromText(String text) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty()) continue;
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq);
            String v = unescape(line.substring(eq + 1));
            map.put(k, v);
        }
        return map;
    }

    private static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
