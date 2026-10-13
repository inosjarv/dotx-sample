package dev.docdotm;

import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.CTBorder;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.STTblStyleOverrideType;
import org.docx4j.wml.Style;
import org.docx4j.wml.UnderlineEnumeration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the packaged, hand-written sample-template.dotx against the sample-dotx-template spec. */
class SampleDotxTemplateTest {

    static Path dotx;
    static WordprocessingMLPackage template;

    @BeforeAll
    static void load() throws Exception {
        dotx = Path.of(SampleDotxTemplateTest.class.getClassLoader().getResource("templates/sample-template.dotx").toURI());
        template = WordprocessingMLPackage.load(dotx.toFile());
    }

    private static Style style(String id) {
        Style s = template.getMainDocumentPart().getStyleDefinitionsPart().getStyleById(id);
        assertNotNull(s, id + " missing");
        return s;
    }

    @Test
    void isMacroFreeTemplate() throws Exception {
        assertEquals(ContentTypes.WORDPROCESSINGML_TEMPLATE, template.getMainDocumentPart().getContentType());
        assertFalse(TestDocs.zipEntries(dotx).stream().anyMatch(n -> n.contains("vbaProject")));
    }

    @Test
    void pageSizeAndMargins() {
        var sectPr = template.getMainDocumentPart().getJaxbElement().getBody().getSectPr();
        assertEquals(BigInteger.valueOf(11906), sectPr.getPgSz().getW());
        assertEquals(BigInteger.valueOf(16838), sectPr.getPgSz().getH());
        assertEquals(BigInteger.valueOf(1418), sectPr.getPgMar().getTop());
        assertEquals(BigInteger.valueOf(1134), sectPr.getPgMar().getBottom());
        assertEquals(BigInteger.valueOf(2268), sectPr.getPgMar().getLeft());
        assertEquals(BigInteger.valueOf(1134), sectPr.getPgMar().getRight());
    }

    @Test
    void normalSpacingComesFromDocDefaults() {
        var defaults = template.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement()
                .getDocDefaults().getPPrDefault().getPPr().getSpacing();
        assertEquals(BigInteger.ZERO, defaults.getBefore());
        assertEquals(BigInteger.valueOf(160), defaults.getAfter());
        assertEquals(BigInteger.valueOf(276), defaults.getLine());
        assertEquals("auto", defaults.getLineRule().value());
        var normal = style("Normal").getPPr();
        assertTrue(normal == null || normal.getSpacing() == null, "Normal must not override table-style cell spacing");
    }

    @ParameterizedTest
    @CsvSource({"TableBlue", "TableRed", "TableTwoTone", "TableHeaderRule"})
    void tableCellsAreCompact(String id) {
        var spacing = style(id).getPPr().getSpacing();
        assertEquals(BigInteger.ZERO, spacing.getBefore());
        assertEquals(BigInteger.ZERO, spacing.getAfter());
        var cellMar = style(id).getTblPr().getTblCellMar();
        assertEquals(BigInteger.valueOf(60), cellMar.getTop().getW());
        assertEquals(BigInteger.valueOf(60), cellMar.getBottom().getW());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(nullValues = "-", textBlock = """
            BodyText, 120, 240, 360
            Heading1, 480, 240, -
            Heading2, 360, 120, -
            """)
    void paragraphSpacing(String id, int before, int after, Integer line) {
        var spacing = style(id).getPPr().getSpacing();
        assertEquals(BigInteger.valueOf(before), spacing.getBefore(), "before");
        assertEquals(BigInteger.valueOf(after), spacing.getAfter(), "after");
        if (line == null) {
            assertNull(spacing.getLine(), "line spacing is inherited");
        } else {
            assertEquals(BigInteger.valueOf(line), spacing.getLine(), "line");
            assertEquals("auto", spacing.getLineRule().value());
        }
    }

    @Test
    void shading() {
        StyleCatalog catalog = StyleCatalog.of(template);
        assertEquals(StyleType.PARAGRAPH, catalog.find("ShadedPanel").orElseThrow().type());
        assertEquals("DEEAF6", style("ShadedPanel").getPPr().getShd().getFill());
        assertEquals(StyleType.CHARACTER, catalog.find("ShadedText").orElseThrow().type());
        assertEquals("FFF2CC", style("ShadedText").getRPr().getShd().getFill());
    }

    private static List<CTBorder> outer(String id) {
        var b = style(id).getTblPr().getTblBorders();
        return List.of(b.getTop(), b.getLeft(), b.getBottom(), b.getRight());
    }

    private static List<CTBorder> inner(String id) {
        var b = style(id).getTblPr().getTblBorders();
        return List.of(b.getInsideH(), b.getInsideV());
    }

    @ParameterizedTest
    @CsvSource({"TableBlue, 2E75B6", "TableRed, C00000"})
    void singleColourTables(String id, String colour) {
        for (CTBorder border : concat(outer(id), inner(id))) {
            assertEquals(STBorder.SINGLE, border.getVal());
            assertEquals(colour, border.getColor());
        }
    }

    @Test
    void twoToneTable() {
        outer("TableTwoTone").forEach(b -> assertEquals("1F3864", b.getColor()));
        inner("TableTwoTone").forEach(b -> assertEquals("BFBFBF", b.getColor()));
        assertTrue(outer("TableTwoTone").getFirst().getSz().intValue() > inner("TableTwoTone").getFirst().getSz().intValue(),
                "outer frame is thicker than inner lines");
    }

    @Test
    void headerRuleTable() {
        concat(outer("TableHeaderRule"), inner("TableHeaderRule")).forEach(b -> assertEquals(STBorder.NONE, b.getVal()));
        var firstRow = style("TableHeaderRule").getTblStylePr().stream()
                .filter(p -> p.getType() == STTblStyleOverrideType.FIRST_ROW).findFirst().orElseThrow();
        var bottom = firstRow.getTcPr().getTcBorders().getBottom();
        assertEquals(STBorder.SINGLE, bottom.getVal());
        assertEquals("1F3864", bottom.getColor());
        assertNull(firstRow.getTcPr().getTcBorders().getTop(), "no other header borders");
        assertNotNull(firstRow.getRPr().getB(), "header text is bold");
    }

    @ParameterizedTest
    @CsvSource({"Hyperlink, 0563C1", "FollowedHyperlink, 954F72"})
    void hyperlinkStyles(String id, String colour) {
        assertEquals(StyleType.CHARACTER, StyleCatalog.of(template).find(id).orElseThrow().type());
        assertEquals(colour, style(id).getRPr().getColor().getVal());
        assertEquals(UnderlineEnumeration.SINGLE, style(id).getRPr().getU().getVal());
    }

    private static List<CTBorder> concat(List<CTBorder> a, List<CTBorder> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
