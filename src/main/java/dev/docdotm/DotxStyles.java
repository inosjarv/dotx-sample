package dev.docdotm;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Style IDs defined by the hand-written {@code sample-template.dotx}. */
public final class DotxStyles {

    // Paragraph styles
    public static final String NORMAL = "Normal";
    public static final String BODY_TEXT = "BodyText";
    public static final String TITLE = "Title";
    public static final String HEADING_1 = "Heading1";
    public static final String HEADING_2 = "Heading2";
    public static final String SHADED_PANEL = "ShadedPanel";

    // Character styles
    public static final String SHADED_TEXT = "ShadedText";
    public static final String HYPERLINK = "Hyperlink";
    public static final String FOLLOWED_HYPERLINK = "FollowedHyperlink";

    // Table styles
    public static final String TABLE_BLUE = "TableBlue";
    public static final String TABLE_RED = "TableRed";
    public static final String TABLE_TWO_TONE = "TableTwoTone";
    public static final String TABLE_HEADER_RULE = "TableHeaderRule";

    /** Every constant above with the style type it must have. */
    public static final Map<String, StyleType> EXPECTED = expected();

    private DotxStyles() {
    }

    /**
     * Checks that the catalog defines every constant with the expected type.
     *
     * @throws IllegalStateException naming each missing or mistyped style
     */
    public static void verify(StyleCatalog catalog) {
        StyleExpectations.verify(EXPECTED, catalog, "DotxStyles");
    }

    private static Map<String, StyleType> expected() {
        var m = new LinkedHashMap<String, StyleType>();
        for (String id : new String[] {NORMAL, BODY_TEXT, TITLE, HEADING_1, HEADING_2, SHADED_PANEL}) {
            m.put(id, StyleType.PARAGRAPH);
        }
        for (String id : new String[] {SHADED_TEXT, HYPERLINK, FOLLOWED_HYPERLINK}) {
            m.put(id, StyleType.CHARACTER);
        }
        for (String id : new String[] {TABLE_BLUE, TABLE_RED, TABLE_TWO_TONE, TABLE_HEADER_RULE}) {
            m.put(id, StyleType.TABLE);
        }
        return Collections.unmodifiableMap(m);
    }
}
