package dev.docdotm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleStylesTest {

    @Test
    void constantsMatchBundledTemplate() {
        var catalog = DotmTemplate.loadResource("templates/sample-template.dotm").styles();
        assertDoesNotThrow(() -> SampleStyles.verify(catalog));
    }

    @Test
    void missingStyleIsNamed() throws Exception {
        var pkg = SampleTemplateGenerator.build();
        pkg.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement().getStyle()
                .removeIf(s -> "CalloutNote".equals(s.getStyleId()));
        var e = assertThrows(IllegalStateException.class, () -> SampleStyles.verify(StyleCatalog.of(pkg)));
        assertTrue(e.getMessage().contains("CalloutNote"), e.getMessage());
    }
}
