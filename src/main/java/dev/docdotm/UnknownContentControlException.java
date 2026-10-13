package dev.docdotm;

/** Thrown when no content control in the document has the requested tag. */
public class UnknownContentControlException extends IllegalArgumentException {

    UnknownContentControlException(String tag) {
        super("No content control with tag '%s' found in the document body, headers or footers".formatted(tag));
    }
}
