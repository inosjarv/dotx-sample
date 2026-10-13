package dev.docdotm;

import org.docx4j.TextUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static dev.docdotm.TestDocs.DOTX;
import static dev.docdotm.TestDocs.SAMPLE;
import static dev.docdotm.TestDocs.reload;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HyperlinkTest {

    @TempDir
    Path tmp;

    private static P.Hyperlink onlyLink(WordprocessingMLPackage doc) {
        return doc.getMainDocumentPart().getContent().stream()
                .map(XmlUtils::unwrap).filter(P.class::isInstance).map(P.class::cast)
                .flatMap(p -> p.getContent().stream()).map(XmlUtils::unwrap)
                .filter(P.Hyperlink.class::isInstance).map(P.Hyperlink.class::cast)
                .findFirst().orElseThrow(() -> new AssertionError("no hyperlink"));
    }

    @Test
    void displayTextDiffersFromUrl() throws Exception {
        Path out = DOTX.newDocument()
                .paragraph(DotxStyles.BODY_TEXT, p -> p.text("Visit ").link("our website", "https://example.com").text(" today."))
                .save(tmp.resolve("link.docx"));
        var doc = reload(out);
        P.Hyperlink link = onlyLink(doc);

        assertEquals("our website", TextUtils.getText(link));
        var rel = doc.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(link.getId());
        assertEquals(Namespaces.HYPERLINK, rel.getType());
        assertEquals("https://example.com", rel.getTarget());
        assertEquals("External", rel.getTargetMode());

        String body = TextUtils.getText(doc.getMainDocumentPart().getJaxbElement());
        assertTrue(body.contains("Visit our website today."), body);
        assertFalse(body.contains("example.com"), "URL must not appear in the text");
    }

    @Test
    void linkUsesHyperlinkStyle() throws Exception {
        Path out = DOTX.newDocument().paragraph(DotxStyles.NORMAL, p -> p.link("mail us", "mailto:hello@example.com"))
                .save(tmp.resolve("style.docx"));
        R run = (R) XmlUtils.unwrap(onlyLink(reload(out)).getContent().getFirst());
        assertEquals("Hyperlink", run.getRPr().getRStyle().getVal());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/about", "javascript:alert(1)", "https://exa mple.com", "https://", "ftp://example.com/file"})
    void invalidUrlsAreRejected(String url) {
        var builder = DOTX.newDocument();
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.paragraph(DotxStyles.NORMAL, p -> p.link("text", url)));
        assertTrue(e.getMessage().contains(url), e.getMessage());
    }

    @Test
    void blankTextIsRejected() {
        var builder = DOTX.newDocument();
        var e = assertThrows(IllegalArgumentException.class,
                () -> builder.paragraph(DotxStyles.NORMAL, p -> p.link("  ", "https://example.com")));
        assertTrue(e.getMessage().contains("text"), e.getMessage());
    }

    @Test
    void templateWithoutHyperlinkStyleFails() throws Exception {
        var pkg = SampleTemplateGenerator.build();
        pkg.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement().getStyle()
                .removeIf(s -> "Hyperlink".equals(s.getStyleId()));
        Path dotm = tmp.resolve("no-link-style.dotm");
        pkg.save(dotm.toFile());

        var builder = DotmTemplate.load(dotm).newDocument();
        var e = assertThrows(UnknownStyleException.class,
                () -> builder.paragraph(SampleStyles.NORMAL, p -> p.link("x", "https://example.com")));
        assertEquals("Hyperlink", e.styleId());
    }

    @Test
    void worksWithTheDotmToo() throws Exception {
        Path out = SAMPLE.newDocument().paragraph(SampleStyles.NORMAL, p -> p.link("docs", "https://docx4j.org"))
                .save(tmp.resolve("dotm-link.docx"));
        var doc = reload(out);
        var rel = doc.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(onlyLink(doc).getId());
        assertEquals("https://docx4j.org", rel.getTarget());
    }
}
