package org.axostudio.axonpcs.api.model;

import java.util.Objects;

/**
 * Public immutable skin payload.
 */
public final class NPCSkin {
    private final NPCSkinMode mode;
    private final String source;
    private final String value;
    private final String signature;

    public NPCSkin(NPCSkinMode mode, String source, String value, String signature) {
        this.mode = mode == null ? NPCSkinMode.NONE : mode;
        this.source = source == null ? "" : source;
        this.value = value == null ? "" : value;
        this.signature = signature == null ? "" : signature;
    }

    public static NPCSkin none() {
        return new NPCSkin(NPCSkinMode.NONE, "", "", "");
    }

    public static NPCSkin mirror() {
        return new NPCSkin(NPCSkinMode.MIRROR, "@mirror", "", "");
    }

    public static NPCSkin named(String name) {
        return new NPCSkin(NPCSkinMode.NAME, name, "", "");
    }

    public static NPCSkin texture(String source, String value, String signature) {
        return new NPCSkin(NPCSkinMode.TEXTURE, source, value, signature == null ? "" : signature);
    }

    public NPCSkinMode mode() {
        return mode;
    }

    public String source() {
        return source;
    }

    public String value() {
        return value;
    }

    public String signature() {
        return signature;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof NPCSkin other)) return false;
        return mode == other.mode
                && source.equals(other.source)
                && value.equals(other.value)
                && signature.equals(other.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, source, value, signature);
    }

    @Override
    public String toString() {
        return "NPCSkin[mode=" + mode + ", source=" + source + ", value=" + value + ", signature=" + signature + "]";
    }
}
