package dev.docdotm;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyleInspectorTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return StyleInspector.run(args, new PrintStream(out, true), new PrintStream(err, true));
    }

    @Test
    void listsBuiltInAndCustomStyles() {
        assertEquals(0, run("src/main/resources/templates/sample-template.dotm"));
        var lines = out.toString().lines().toList();
        assertTrue(lines.get(1).matches("CalloutNote\\s+paragraph\\s+Callout Note  \\[custom]"), lines.get(1));
        assertTrue(lines.stream().anyMatch(l -> l.matches("Heading1\\s+paragraph\\s+heading 1")), out.toString());
    }

    @Test
    void missingFileExitsNonZero() {
        assertEquals(1, run("does/not/exist.dotm"));
        assertTrue(err.toString().contains("exist.dotm"), err.toString());
    }
}
