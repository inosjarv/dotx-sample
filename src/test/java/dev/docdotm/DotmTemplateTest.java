package dev.docdotm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotmTemplateTest {

    @TempDir
    Path tmp;

    @Test
    void loadsFromFilePath() throws Exception {
        Path dotm = SampleTemplateGenerator.generate(tmp.resolve("t.dotm"));
        var template = DotmTemplate.load(dotm);
        assertTrue(template.styles().find("Heading1").isPresent());
    }

    @Test
    void loadsFromClasspath() {
        assertTrue(DotmTemplate.loadResource("templates/sample-template.dotm").styles().find("CalloutNote").isPresent());
    }

    @Test
    void missingFileNamesPath() {
        Path missing = tmp.resolve("nope.dotm");
        var e = assertThrows(TemplateLoadException.class, () -> DotmTemplate.load(missing));
        assertTrue(e.getMessage().contains("nope.dotm"), e.getMessage());
        assertTrue(e.getMessage().startsWith("Template not found"), e.getMessage());
    }

    @Test
    void missingResource() {
        var e = assertThrows(TemplateLoadException.class, () -> DotmTemplate.loadResource("templates/nope.dotm"));
        assertTrue(e.getMessage().contains("templates/nope.dotm"), e.getMessage());
    }

    @Test
    void nonWordFileIsRejected() throws Exception {
        Path text = Files.writeString(tmp.resolve("fake.dotm"), "this is not a zip");
        var e = assertThrows(TemplateLoadException.class, () -> DotmTemplate.load(text));
        assertTrue(e.getMessage().startsWith("Not a valid Word template"), e.getMessage());
        assertTrue(e.getMessage().contains("fake.dotm"), e.getMessage());
    }
}
