package dev.docdotm;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotxStylesTest {

    @Test
    void constantsMatchPackagedDotx() {
        assertDoesNotThrow(() -> DotxStyles.verify(TestDocs.DOTX.styles()));
    }

    @Test
    void missingStyleIsNamed() throws Exception {
        WordprocessingMLPackage pkg;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("templates/sample-template.dotx")) {
            pkg = WordprocessingMLPackage.load(in);
        }
        pkg.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement().getStyle()
                .removeIf(s -> "TableHeaderRule".equals(s.getStyleId()));
        var e = assertThrows(IllegalStateException.class, () -> DotxStyles.verify(StyleCatalog.of(pkg)));
        assertTrue(e.getMessage().contains("TableHeaderRule"), e.getMessage());
    }
}
