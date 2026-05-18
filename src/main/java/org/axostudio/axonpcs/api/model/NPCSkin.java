package org.axostudio.axonpcs.api.model;

/**
 * Public skin payload. TEXTURE mode expects Mojang texture value/signature data.
 */
public record NPCSkin(NPCSkinMode mode, String source, String value, String signature) {
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

    public NPCSkin {
        mode = mode == null ? NPCSkinMode.NONE : mode;
        source = source == null ? "" : source;
        value = value == null ? "" : value;
        signature = signature == null ? "" : signature;
    }
}
