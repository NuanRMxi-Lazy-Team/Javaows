package cn.moerain.javaows.misc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal localization manager that loads a JSON translations file
 * and provides lookup with fallback to provided default.
 *
 * Usage:
 *   String s = Localization.t("menu.file", "文件(F)");
 *
 * Language selection:
 *   - System property: -Dapp.lang=en_us (preferred)
 *   - Else Locale default (en_* -> en_us)
 *   - Else falls back to built-in defaults (Chinese literals in code)
 */
public final class Localization {
    private static final Map<String, String> STRINGS = new HashMap<>();
    private static String currentLang = null;

    static {
        // initialize on class load
        String lang = System.getProperty("app.lang");
        if (lang == null || lang.isBlank()) {
            Locale loc = Locale.getDefault();
            String language = loc.getLanguage();
            String country = loc.getCountry();
            if ("en".equalsIgnoreCase(language)) {
                lang = "en_us"; // map all English to en_us
            } else if ("zh".equalsIgnoreCase(language)) {
                // default Chinese uses hardcoded strings as fallback; optionally support zh_cn file later
                lang = ""; // will result in no external load
            } else {
                // default to English if not Chinese
                lang = "en_us";
            }
        }
        load(lang);
    }

    private Localization() {}

    public static synchronized void load(String lang) {
        if (lang == null || lang.isBlank() || lang.equalsIgnoreCase(currentLang)) {
            return;
        }
        Map<String, String> loaded = new HashMap<>();
        boolean ok = false;

        // Try classpath resource first
        String resourcePath = "/translations/" + lang.toLowerCase(Locale.ROOT) + ".json";
        try (InputStream is = Localization.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(is);
                flattenJson("", root, loaded);
                ok = true;
            }
        } catch (IOException ignored) {}

        // Fallback: try file system (useful during dev)
        if (!ok) {
            File file = new File("src\\translations\\" + lang.toLowerCase(Locale.ROOT) + ".json");
            if (file.exists()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(file);
                    flattenJson("", root, loaded);
                    ok = true;
                } catch (IOException ignored) {}
            }
        }

        // Apply if loaded
        if (ok) {
            STRINGS.clear();
            STRINGS.putAll(loaded);
            currentLang = lang.toLowerCase(Locale.ROOT);
        }
    }

    private static void flattenJson(String prefix, JsonNode node, Map<String, String> out) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String key = prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey();
                flattenJson(key, e.getValue(), out);
            }
        } else if (node.isTextual()) {
            out.put(prefix, node.asText());
        } else {
            // ignore non-text nodes for now
        }
    }

    /**
     * Translate by key, with fallback default if not found.
     */
    public static String t(String key, String defaultValue) {
        String v = STRINGS.get(key);
        return v != null ? v : defaultValue;
    }

    /**
     * Translate and format with String.format when args provided.
     */
    public static String tf(String key, String defaultPattern, Object... args) {
        String pattern = t(key, defaultPattern);
        try {
            return String.format(pattern, args);
        } catch (Exception e) {
            // if formatting fails, return pattern plus args
            return pattern;
        }
    }

    public static String getCurrentLang() {
        return currentLang == null ? "" : currentLang;
    }
}

