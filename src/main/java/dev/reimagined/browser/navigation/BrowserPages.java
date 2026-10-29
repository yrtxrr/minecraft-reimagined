package dev.reimagined.browser.navigation;

/** Empty lists still have a safe first page; changing filters/window size cannot strand selection. */
public final class BrowserPages {
    private BrowserPages() {}
    public static int count(int entries, int size) {
        if (entries < 0 || size <= 0) throw new IllegalArgumentException("Invalid page dimensions");
        return (int) Math.max(1L, ((long) entries + size - 1) / size);
    }
    public static int clamp(int page, int entries, int size) {
        return Math.max(0, Math.min(page, count(entries, size) - 1));
    }
}
