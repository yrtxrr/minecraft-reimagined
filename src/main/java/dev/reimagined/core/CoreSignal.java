package dev.reimagined.core;

import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

/** Common-side quest/ecology/civilization bridge. No live world references or client packets. */
public final class CoreSignal extends Event {
    public enum Kind { RESEARCH, ERA, CIVILIZATION, ECOLOGY }
    private final Kind kind;
    private final ResourceLocation subject;
    private final UUID actor;
    public CoreSignal(Kind kind, ResourceLocation subject, UUID actor) {
        this.kind=java.util.Objects.requireNonNull(kind); this.subject=java.util.Objects.requireNonNull(subject); this.actor=actor;
    }
    public Kind kind() { return kind; }
    public ResourceLocation subject() { return subject; }
    /** Null for a world event without an individual actor. */
    public UUID actor() { return actor; }
}
