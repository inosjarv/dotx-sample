package dev.docdotm;

import org.docx4j.TextUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.docdotm.TestDocs.SAMPLE;
import static dev.docdotm.TestDocs.reload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentControlsTest {

    @TempDir
    Path tmp;

    @Test
    void fillsBodyControlKeepingItsStyle() throws Exception {
        Path out = SAMPLE.newDocument().fill("customerName", "Acme Ltd").save(tmp.resolve("body.docx"));
        var doc = reload(out);
        var control = ContentControls.findAll(doc).stream().filter(c -> "customerName".equals(c.tag())).findFirst().orElseThrow();
        assertEquals("Acme Ltd", control.text());
        String xml = org.docx4j.XmlUtils.marshaltoString(doc.getMainDocumentPart().getJaxbElement());
        assertTrue(xml.contains("w:val=\"Subtitle\""), "control keeps its paragraph style");
        assertFalse(TextUtils.getText(doc.getMainDocumentPart().getJaxbElement()).contains("[customer name]"));
    }

    @Test
    void fillsHeaderControl() throws Exception {
        Path out = SAMPLE.newDocument().fill("documentDate", "2026-09-23").save(tmp.resolve("header.docx"));
        var header = reload(out).getDocumentModel().getSections().getFirst().getHeaderFooterPolicy().getDefaultHeader();
        assertEquals("Date: 2026-09-23", TextUtils.getText(header.getJaxbElement()).trim());
    }

    @Test
    void unknownTagIsNamed() {
        var builder = SAMPLE.newDocument();
        var e = assertThrows(UnknownContentControlException.class, () -> builder.fill("invoiceNumber", "42"));
        assertTrue(e.getMessage().contains("invoiceNumber"), e.getMessage());
    }
}
