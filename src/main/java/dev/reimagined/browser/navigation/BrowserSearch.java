package dev.reimagined.browser.navigation;

import java.util.Locale;

/** Every space-separated term must match; @namespace restricts the item's registry namespace. */
public final class BrowserSearch {
    private BrowserSearch() {}
    public static boolean matches(String name, String id, String query) {
        String display = name.toLowerCase(Locale.ROOT);
        String registry = id.toLowerCase(Locale.ROOT);
        String namespace = registry.contains(":") ? registry.substring(0, registry.indexOf(':')) : registry;
        for (String term : query.toLowerCase(Locale.ROOT).strip().split("\\s+")) {
            if (term.isEmpty()) continue;
            if (term.startsWith("@")) {
                if (term.length() == 1 || !namespace.contains(term.substring(1))) return false;
            } else if (!display.contains(term) && !registry.contains(term)) return false;
        }
        return true;
    }
}
