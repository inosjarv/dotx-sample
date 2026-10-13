package dev.docdotm;

import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.docdotm.TestDocs.DOTX;
import static dev.docdotm.TestDocs.reload;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DotxGenerationTest {

    @TempDir
    Path tmp;

    @Test
    void savedAsDocumentWithTemplateStylesAndPageSetup() throws Exception {
        Path out = DOTX.newDocument().paragraph(DotxStyles.BODY_TEXT, "Hello").save(tmp.resolve("from-dotx.docx"));
        var doc = reload(out);

        assertEquals(ContentTypes.WORDPROCESSINGML_DOCUMENT, doc.getMainDocumentPart().getContentType());

        var saved = StyleCatalog.of(doc);
        for (StyleInfo style : DOTX.styles().all()) {
            assertEquals(style, saved.find(style.id()).orElse(null), style.id());
        }

        var sectPr = doc.getMainDocumentPart().getJaxbElement().getBody().getSectPr();
        assertEquals(11906, sectPr.getPgSz().getW().intValue());
        assertEquals(16838, sectPr.getPgSz().getH().intValue());
        assertEquals(1418, sectPr.getPgMar().getTop().intValue());
        assertEquals(1134, sectPr.getPgMar().getBottom().intValue());
        assertEquals(2268, sectPr.getPgMar().getLeft().intValue());
        assertEquals(1134, sectPr.getPgMar().getRight().intValue());
    }

    @Test
    void templateBodyIsDropped() throws Exception {
        Path out = DOTX.newDocument().save(tmp.resolve("empty.docx"));
        assertEquals(0, reload(out).getMainDocumentPart().getContent().size());
    }
}
