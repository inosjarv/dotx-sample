package dev.docdotm;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleCatalogTest {

    static WordprocessingMLPackage template;
    static StyleCatalog catalog;

    @BeforeAll
    static void load() throws Exception {
        template = SampleTemplateGenerator.build();
        catalog = StyleCatalog.of(template);
    }

    @Test
    void listsSampleCatalog() {
        StyleInfo heading = catalog.find("Heading1").orElseThrow();
        assertEquals("heading 1", heading.name());
        assertEquals(StyleType.PARAGRAPH, heading.type());
        assertFalse(heading.custom());
        assertEquals(StyleType.CHARACTER, catalog.find("Emphasis").orElseThrow().type());
        assertTrue(catalog.find("CalloutNote").orElseThrow().custom());
        assertEquals("CalloutNote", catalog.all().getFirst().id(), "custom styles are listed first");
    }

    @Test
    void resolvesDisplayNamesCaseInsensitively() {
        assertEquals(Optional.of("Heading1"), catalog.idForName("Heading 1"));
        assertEquals(Optional.of("CalloutNote"), catalog.idForName("Callout Note"));
        assertEquals(Optional.of("TableGrid"), catalog.idForName("table grid"));
    }

    @Test
    void unknownDisplayName() {
        assertEquals(Optional.empty(), catalog.idForName("Fancy Banner"));
    }

    @Test
    void requireReturnsMatchingStyle() {
        assertEquals("Heading1", catalog.require("Heading1", StyleType.PARAGRAPH).id());
    }

    @Test
    void unknownIdSuggestsCloseMatch() {
        var e = assertThrows(UnknownStyleException.class, () -> catalog.require("Headng1", StyleType.PARAGRAPH));
        assertEquals("Headng1", e.styleId());
        assertEquals("Heading1", e.suggestions().getFirst());
        assertTrue(e.getMessage().contains("'Headng1'"), e.getMessage());
        assertTrue(e.getMessage().contains("'Heading1'"), e.getMessage());
    }

    @Test
    void displayNameUsedAsIdIsExplained() {
        var e = assertThrows(UnknownStyleException.class, () -> catalog.require("heading 1", StyleType.PARAGRAPH));
        assertTrue(e.getMessage().contains("display name"), e.getMessage());
        assertTrue(e.getMessage().contains("'Heading1'"), e.getMessage());
    }

    @Test
    void wrongTypeIsRejected() {
        var e = assertThrows(StyleTypeMismatchException.class, () -> catalog.require("Emphasis", StyleType.PARAGRAPH));
        assertEquals("Style 'Emphasis' is a character style, not a paragraph style", e.getMessage());
    }
}
