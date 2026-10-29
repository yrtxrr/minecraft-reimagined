package dev.reimagined.browser.navigation;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Optional;

/** Bounded back navigation keeps the whole selection state, including the recipe page. */
public final class BrowserHistory<T> {
    private final ArrayDeque<T> entries = new ArrayDeque<>();
    private final int limit;
    public BrowserHistory(int limit) {
        if (limit < 1) throw new IllegalArgumentException("History must have capacity");
        this.limit = limit;
    }
    public void push(T state) {
        Objects.requireNonNull(state);
        if (entries.size() == limit) entries.removeFirst();
        entries.addLast(state);
    }
    public Optional<T> back() { return Optional.ofNullable(entries.pollLast()); }
    public int size() { return entries.size(); }
}
