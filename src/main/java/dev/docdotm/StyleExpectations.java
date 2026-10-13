package dev.docdotm;

import java.util.ArrayList;
import java.util.Map;

/** Shared check behind {@link SampleStyles#verify} and {@link DotxStyles#verify}. */
final class StyleExpectations {

    private StyleExpectations() {
    }

    /**
     * @throws IllegalStateException naming each style that is missing or has the wrong type
     */
    static void verify(Map<String, StyleType> expected, StyleCatalog catalog, String label) {
        var problems = new ArrayList<String>();
        expected.forEach((id, type) -> catalog.find(id).ifPresentOrElse(
                style -> {
                    if (style.type() != type) {
                        problems.add("%s is a %s style, expected %s".formatted(id, style.type().label(), type.label()));
                    }
                },
                () -> problems.add(id + " is missing")));
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Template does not match %s: %s".formatted(label, String.join("; ", problems)));
        }
    }
}
