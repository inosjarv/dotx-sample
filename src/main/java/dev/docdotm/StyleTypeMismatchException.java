package dev.docdotm;

/** Thrown when a style is used as the wrong kind, e.g. a character style as a paragraph style. */
public class StyleTypeMismatchException extends IllegalArgumentException {

    StyleTypeMismatchException(StyleInfo style, StyleType expected) {
        super("Style '%s' is a %s style, not a %s style".formatted(style.id(), style.type().label(), expected.label()));
    }
}
