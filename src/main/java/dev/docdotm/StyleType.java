package dev.docdotm;

import java.util.Locale;

/** The four kinds of style a WordprocessingML {@code w:style} can be ({@code w:type}). */
public enum StyleType {
    PARAGRAPH, CHARACTER, TABLE, NUMBERING;

    /** Parses the {@code w:type} attribute; Word treats a missing type as paragraph. */
    static StyleType fromXml(String value) {
        return value == null ? PARAGRAPH : valueOf(value.toUpperCase(Locale.ROOT));
    }

    String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
