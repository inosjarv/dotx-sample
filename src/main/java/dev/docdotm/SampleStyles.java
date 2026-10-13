package dev.docdotm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Style IDs defined by the bundled {@code sample-template.dotm}.
 *
 * <p>For a real template, run {@link StyleInspector} on it and copy the IDs you use into a class
 * like this one, then call {@link #verify} (for example from a unit test) so a template change
 * that drops or retypes a style breaks the build instead of silently producing Normal text.
 */
public final class SampleStyles {

    // docx4j default styles
    public static final String NORMAL = "Normal";
    public static final String TITLE = "Title";
    public static final String SUBTITLE = "Subtitle";
    public static final String HEADING_1 = "Heading1";
    public static final String HEADING_2 = "Heading2";
    public static final String HEADING_3 = "Heading3";
    public static final String HEADER = "Header";
    public static final String CAPTION = "Caption";
    public static final String EMPHASIS = "Emphasis";
    public static final String TABLE_GRID = "TableGrid";

    // Word built-ins activated from docx4j's known styles
    public static final String QUOTE = "Quote";
    public static final String INTENSE_QUOTE = "IntenseQuote";
    public static final String LIST_BULLET = "ListBullet";
    public static final String FOOTER = "Footer";
    public static final String STRONG = "Strong";

    // Custom
    public static final String CALLOUT_NOTE = "CalloutNote";

    /** Every constant above with the style type it must have. */
    public static final Map<String, StyleType> EXPECTED = expected();

    private SampleStyles() {
    }

    /**
     * Checks that the catalog defines every constant with the expected type.
     *
     * @throws IllegalStateException naming each missing or mistyped style
     */
    public static void verify(StyleCatalog catalog) {
        StyleExpectations.verify(EXPECTED, catalog, "SampleStyles");
    }

    private static Map<String, StyleType> expected() {
        var m = new LinkedHashMap<String, StyleType>();
        for (String id : new String[] {NORMAL, TITLE, SUBTITLE, HEADING_1, HEADING_2, HEADING_3, HEADER, CAPTION,
                QUOTE, INTENSE_QUOTE, LIST_BULLET, FOOTER, CALLOUT_NOTE}) {
            m.put(id, StyleType.PARAGRAPH);
        }
        m.put(EMPHASIS, StyleType.CHARACTER);
        m.put(STRONG, StyleType.CHARACTER);
        m.put(TABLE_GRID, StyleType.TABLE);
        return Collections.unmodifiableMap(m);
    }
}
