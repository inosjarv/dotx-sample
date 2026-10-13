package dev.docdotm;

import org.docx4j.TextUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.Tr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static dev.docdotm.TestDocs.SAMPLE;
import static dev.docdotm.TestDocs.body;
import static dev.docdotm.TestDocs.paragraphWithText;
import static dev.docdotm.TestDocs.reload;
import static dev.docdotm.TestDocs.tables;
import static dev.docdotm.TestDocs.zipEntries;
import static dev.docdotm.TestDocs.zipEntry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentBuilderTest {

    @TempDir
    Path tmp;

    @Test
    void styledParagraph() throws Exception {
        Path out = SAMPLE.newDocument().paragraph(SampleStyles.HEADING_1, "Overview").save(tmp.resolve("p.docx"));
        P p = paragraphWithText(reload(out), "Overview");
        assertEquals("Heading1", p.getPPr().getPStyle().getVal());
    }

    @Test
    void styledRunInsideParagraph() throws Exception {
        Path out = SAMPLE.newDocument()
                .paragraph(SampleStyles.NORMAL, p -> p.text("Revenue grew ").run(SampleStyles.EMPHASIS, "12%").text("."))
                .save(tmp.resolve("r.docx"));
        P p = paragraphWithText(reload(out), "Revenue grew 12%.");
        R emphasised = p.getContent().stream().map(XmlUtils::unwrap).filter(R.class::isInstance).map(R.class::cast)
                .filter(r -> "12%".equals(TextUtils.getText(r))).findFirst().orElseThrow();
        assertEquals("Emphasis", emphasised.getRPr().getRStyle().getVal());
        assertEquals("Normal", p.getPPr().getPStyle().getVal());
    }

    @Test
    void styledTable() throws Exception {
        Path out = SAMPLE.newDocument()
                .table(SampleStyles.TABLE_GRID, List.of(List.of("Region", "Q2", "Q3"), List.of("EU", "1.2M", "1.4M")))
                .save(tmp.resolve("t.docx"));
        var table = tables(reload(out)).getFirst();
        assertEquals("TableGrid", table.getTblPr().getTblStyle().getVal());
        assertEquals(2, table.getContent().size());
        assertEquals(3, ((Tr) XmlUtils.unwrap(table.getContent().getFirst())).getContent().size());
        assertEquals(3, table.getTblGrid().getGridCol().size());
    }

    @Test
    void unknownStyleFailsAtGenerationTime() {
        var builder = SAMPLE.newDocument();
        var e = assertThrows(UnknownStyleException.class, () -> builder.paragraph("Headng1", "x"));
        assertEquals("Heading1", e.suggestions().getFirst());
        assertThrows(StyleTypeMismatchException.class, () -> builder.paragraph(SampleStyles.EMPHASIS, "x"));
        assertThrows(StyleTypeMismatchException.class, () -> builder.paragraph(SampleStyles.NORMAL, p -> p.run(SampleStyles.QUOTE, "x")));
        assertThrows(StyleTypeMismatchException.class, () -> builder.table(SampleStyles.NORMAL, List.of(List.of("x"))));
    }

    @Test
    void savedAsDocumentContentType() throws Exception {
        Path out = SAMPLE.newDocument().save(tmp.resolve("ct.docx"));
        assertEquals(ContentTypes.WORDPROCESSINGML_DOCUMENT, reload(out).getMainDocumentPart().getContentType());
        String types = zipEntry(out, "[Content_Types].xml");
        assertTrue(types.contains("wordprocessingml.document.main+xml"));
        assertFalse(types.contains("macroEnabledTemplate"));
    }

    @Test
    void documentsFromOneTemplateAreIndependent() throws Exception {
        Path a = SAMPLE.newDocument().paragraph(SampleStyles.NORMAL, "Only in A").save(tmp.resolve("a.docx"));
        Path b = SAMPLE.newDocument().paragraph(SampleStyles.NORMAL, "Only in B").save(tmp.resolve("b.docx"));
        String textA = TextUtils.getText(reload(a).getMainDocumentPart().getJaxbElement());
        String textB = TextUtils.getText(reload(b).getMainDocumentPart().getJaxbElement());
        assertTrue(textA.contains("Only in A") && !textA.contains("Only in B"));
        assertTrue(textB.contains("Only in B") && !textB.contains("Only in A"));
        // and the template itself is untouched
        Path c = SAMPLE.newDocument().save(tmp.resolve("c.docx"));
        assertFalse(TextUtils.getText(reload(c).getMainDocumentPart().getJaxbElement()).contains("Only in"));
    }

    @Test
    void emptyDocumentKeepsContentControlsHeaderAndPageSetup() throws Exception {
        Path out = SAMPLE.newDocument().save(tmp.resolve("empty.docx"));
        var doc = reload(out);
        assertEquals(1, body(doc).size(), "only the customerName control remains");
        assertTrue(ContentControls.containsContentControl(body(doc).getFirst()));
        assertFalse(TextUtils.getText(doc.getMainDocumentPart().getJaxbElement()).contains("Sample Template"));

        SectPr ours = doc.getMainDocumentPart().getJaxbElement().getBody().getSectPr();
        SectPr template = SampleTemplateGenerator.build().getMainDocumentPart().getJaxbElement().getBody().getSectPr();
        assertEquals(template.getPgSz().getW(), ours.getPgSz().getW());
        assertEquals(template.getPgSz().getH(), ours.getPgSz().getH());
        assertEquals(template.getPgMar().getLeft(), ours.getPgMar().getLeft());
        assertEquals(template.getPgMar().getTop(), ours.getPgMar().getTop());
        var header = doc.getDocumentModel().getSections().getFirst().getHeaderFooterPolicy().getDefaultHeader();
        assertNotNull(header);
        assertTrue(TextUtils.getText(header.getJaxbElement()).contains("Date:"));
    }

    @Test
    void contentControlsCanBeDropped() throws Exception {
        Path out = SAMPLE.newDocument(ContentControls.DROP).save(tmp.resolve("drop.docx"));
        assertTrue(body(reload(out)).isEmpty());
    }

    @Test
    void macrosAndAttachedTemplateAreRemoved() throws Exception {
        WordprocessingMLPackage withMacros = SampleTemplateGenerator.build();
        var vba = new BinaryPart(new PartName("/word/vbaProject.bin"));
        vba.setBinaryData(new byte[] {1, 2, 3, 4});
        vba.setContentType(new ContentType("application/vnd.ms-office.vbaProject"));
        vba.setRelationshipType("http://schemas.microsoft.com/office/2006/relationships/vbaProject");
        withMacros.getMainDocumentPart().addTargetPart(vba);
        withMacros.getMainDocumentPart().getDocumentSettingsPart().attachTemplate("C:\\Templates\\house.dotm");
        Path dotm = tmp.resolve("macros.dotm");
        withMacros.save(dotm.toFile());
        assertTrue(zipEntries(dotm).contains("word/vbaProject.bin"), "fixture must contain a VBA project");

        Path out = DotmTemplate.load(dotm).newDocument().paragraph(SampleStyles.NORMAL, "Hi").save(tmp.resolve("clean.docx"));

        assertFalse(zipEntries(out).contains("word/vbaProject.bin"));
        assertFalse(zipEntry(out, "[Content_Types].xml").contains("vbaProject"));
        assertFalse(zipEntry(out, "word/_rels/document.xml.rels").contains("vbaProject"));
        assertFalse(zipEntry(out, "word/settings.xml").contains("attachedTemplate"));
        String settingsRels = zipEntry(out, "word/_rels/settings.xml.rels");
        assertTrue(settingsRels == null || !settingsRels.contains("house.dotm"));
        assertEquals(ContentTypes.WORDPROCESSINGML_DOCUMENT, reload(out).getMainDocumentPart().getContentType());
    }

    @Test
    void roundTripKeepsTemplateDefinitions() throws Exception {
        Path out = SAMPLE.newDocument().paragraph(SampleStyles.HEADING_1, "Overview").save(tmp.resolve("rt.docx"));
        var doc = reload(out);
        var saved = StyleCatalog.of(doc);
        for (StyleInfo style : SAMPLE.styles().all()) {
            assertEquals(style, saved.find(style.id()).orElse(null), style.id());
        }
        var heading = doc.getMainDocumentPart().getStyleDefinitionsPart().getStyleById("Heading1");
        assertEquals(SampleTemplateGenerator.HOUSE_FONT, heading.getRPr().getRFonts().getAscii());
        assertEquals(SampleTemplateGenerator.HOUSE_COLOR, heading.getRPr().getColor().getVal());
        assertNotNull(doc.getMainDocumentPart().getThemePart(), "theme");
        assertNotNull(doc.getMainDocumentPart().getNumberingDefinitionsPart(), "numbering");
        assertNotNull(doc.getDocumentModel().getSections().getFirst().getHeaderFooterPolicy().getDefaultFooter(), "footer");
    }
}
