package dev.docdotm;

import org.docx4j.XmlUtils;
import org.docx4j.sharedtypes.STOnOff;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tc;
import org.docx4j.wml.Tr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigInteger;
import java.nio.file.Path;
import java.util.List;

import static dev.docdotm.TestDocs.DOTX;
import static dev.docdotm.TestDocs.SAMPLE;
import static dev.docdotm.TestDocs.reload;
import static dev.docdotm.TestDocs.tables;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableLayoutTest {

    private static final List<List<String>> ROWS = List.of(
            List.of("Region", "Q2", "Q3"), List.of("EU", "1.2M", "1.4M"), List.of("US", "2.0M", "2.2M"));

    @TempDir
    Path tmp;

    private Tbl generate(DotmTemplate template, String style) throws Exception {
        Path out = template.newDocument().table(style, ROWS).save(tmp.resolve(style + ".docx"));
        return tables(reload(out)).getFirst();
    }

    private static void assertSpans(Tbl table, int textWidth) {
        assertEquals(BigInteger.valueOf(textWidth), table.getTblPr().getTblW().getW(), "tblW");
        assertEquals("dxa", table.getTblPr().getTblW().getType());
        assertEquals(textWidth, table.getTblGrid().getGridCol().stream().mapToInt(c -> c.getW().intValue()).sum(), "grid");
        for (Object row : table.getContent()) {
            int cells = ((Tr) XmlUtils.unwrap(row)).getContent().stream()
                    .map(XmlUtils::unwrap).map(Tc.class::cast)
                    .mapToInt(tc -> tc.getTcPr().getTcW().getW().intValue()).sum();
            assertEquals(textWidth, cells, "row cell widths");
        }
    }

    @Test
    void dotxTableSpansTextWidthBetweenMargins() throws Exception {
        // A4 11906 - left 2268 - right 1134
        assertSpans(generate(DOTX, DotxStyles.TABLE_BLUE), 8504);
    }

    @Test
    void dotmTableFollowsItsOwnMargins() throws Exception {
        // A4 11907 - 1440 - 1440 (previously a fixed 9360, overflowing the right margin)
        assertSpans(generate(SAMPLE, SampleStyles.TABLE_GRID), 9027);
    }

    @Test
    void remainderGoesToLastColumn() throws Exception {
        var cols = generate(DOTX, DotxStyles.TABLE_RED).getTblGrid().getGridCol();
        assertEquals(List.of(2834, 2834, 2836), cols.stream().map(c -> c.getW().intValue()).toList());
    }

    @Test
    void firstRowIsHeaderRow() throws Exception {
        Tbl table = generate(DOTX, DotxStyles.TABLE_HEADER_RULE);
        assertEquals(STOnOff.ONE, table.getTblPr().getTblLook().getFirstRow());
        assertEquals(STOnOff.ONE, table.getTblPr().getTblLook().getNoHBand());
        Tr header = (Tr) XmlUtils.unwrap(table.getContent().getFirst());
        assertTrue(header.getTrPr().getCnfStyleOrDivIdOrGridBefore().stream()
                .anyMatch(e -> e.getName().getLocalPart().equals("tblHeader")), "header row repeats");
        assertNull(((Tr) XmlUtils.unwrap(table.getContent().get(1))).getTrPr(), "only the first row is a header");
    }

    @Test
    void layoutIsFixedWithNoIndent() throws Exception {
        Tbl table = generate(DOTX, DotxStyles.TABLE_TWO_TONE);
        assertEquals("fixed", table.getTblPr().getTblLayout().getType().value());
        assertEquals(BigInteger.ZERO, table.getTblPr().getTblInd().getW());
    }
}
