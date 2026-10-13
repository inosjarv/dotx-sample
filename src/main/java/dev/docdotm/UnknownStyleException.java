package dev.docdotm;

import java.util.List;

/** Thrown when content references a style ID the template does not define. */
public class UnknownStyleException extends IllegalArgumentException {

    private final String styleId;
    private final List<String> suggestions;

    UnknownStyleException(String styleId, String message, List<String> suggestions) {
        super(message);
        this.styleId = styleId;
        this.suggestions = List.copyOf(suggestions);
    }

    public String styleId() {
        return styleId;
    }

    /** Close-matching style IDs from the template, best first. */
    public List<String> suggestions() {
        return suggestions;
    }
}
