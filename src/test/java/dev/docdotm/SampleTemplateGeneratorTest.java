package dev.docdotm;

import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Style;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleTemplateGeneratorTest {

    @TempDir
    static Path tmp;
    static Path dotm;
    static WordprocessingMLPackage template;
    static StyleCatalog catalog;

    @BeforeAll
    static void generate() throws Exception {
        dotm = SampleTemplateGenerator.generate(tmp.resolve("sample.dotm"));
        template = WordprocessingMLPackage.load(dotm.toFile());
        catalog = StyleCatalog.of(template);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            Normal       | Normal        | PARAGRAPH
            Title        | Title         | PARAGRAPH
            Subtitle     | Subtitle      | PARAGRAPH
            Heading1     | heading 1     | PARAGRAPH
            Heading2     | heading 2     | PARAGRAPH
            Heading3     | heading 3     | PARAGRAPH
            Header       | header        | PARAGRAPH
            Caption      | caption       | PARAGRAPH
            Emphasis     | Emphasis      | CHARACTER
            TableGrid    | Table Grid    | TABLE
            Quote        | Quote         | PARAGRAPH
            IntenseQuote | Intense Quote | PARAGRAPH
            ListBullet   | List Bullet   | PARAGRAPH
            Footer       | footer        | PARAGRAPH
            Strong       | Strong        | CHARACTER
            """)
    void builtInStylesPresent(String id, String name, StyleType type) {
        StyleInfo style = catalog.find(id).orElseThrow(() -> new AssertionError(id + " missing"));
        assertEquals(name, style.name());
        assertEquals(type, style.type());
    }

    @Test
    void styleIdDiffersFromDisplayName() {
        assertEquals("heading 1", catalog.find("Heading1").orElseThrow().name());
    }

    @ParameterizedTest
    @CsvSource({"Title", "Heading1"})
    void houseStyleDiffersFromDocx4jDefault(String id) throws Exception {
        Style ours = template.getMainDocumentPart().getStyleDefinitionsPart().getStyleById(id);
        Style docx4jDefault = WordprocessingMLPackage.createPackage().getMainDocumentPart().getStyleDefinitionsPart().getStyleById(id);
        assertNotEquals(docx4jDefault.getRPr().getColor().getVal(), ours.getRPr().getColor().getVal());
        assertEquals(SampleTemplateGenerator.HOUSE_FONT, ours.getRPr().getRFonts().getAscii());
        assertEquals(null, ours.getRPr().getRFonts().getAsciiTheme(), "theme font would override the house font");
    }

    @Test
    void calloutNoteIsCustom() {
        StyleInfo callout = catalog.find("CalloutNote").orElseThrow();
        assertEquals("Callout Note", callout.name());
        assertEquals(StyleType.PARAGRAPH, callout.type());
        assertEquals("Normal", callout.basedOn());
        assertTrue(callout.custom());
    }

    @Test
    void contentControlsHavePlaceholders() {
        var controls = ContentControls.findAll(template);
        var customer = controls.stream().filter(c -> "customerName".equals(c.tag())).findFirst().orElseThrow();
        var date = controls.stream().filter(c -> "documentDate".equals(c.tag())).findFirst().orElseThrow();
        assertEquals("[customer name]", customer.text());
        assertEquals("[document date]", date.text());
    }

    @Test
    void documentDateLivesInHeader() {
        var header = template.getDocumentModel().getSections().getFirst().getHeaderFooterPolicy().getDefaultHeader();
        assertTrue(ContentControls.containsContentControl(header.getJaxbElement()));
    }

    @Test
    void savedAsMacroEnabledTemplate() throws Exception {
        assertEquals(ContentTypes.WORDPROCESSINGML_TEMPLATE_MACROENABLED, template.getMainDocumentPart().getContentType());
        try (var zip = new ZipFile(dotm.toFile())) {
            String types = new String(zip.getInputStream(zip.getEntry("[Content_Types].xml")).readAllBytes());
            assertTrue(types.contains("application/vnd.ms-word.template.macroEnabledTemplate.main+xml"));
        }
    }

    @Test
    void createsMissingParentDirectories() throws Exception {
        Path nested = tmp.resolve("a/b/c/t.dotm");
        SampleTemplateGenerator.generate(nested);
        assertTrue(Files.size(nested) > 0);
    }
}
