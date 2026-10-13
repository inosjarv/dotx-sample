package dev.docdotm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Keeps the snippet at the top of README.md compiling and working. */
class ReadmeExampleTest {

    @TempDir
    Path tmp;

    @Test
    void readmeSnippet() throws Exception {
        var template = DotmTemplate.loadResource("templates/sample-template.dotm");
        template.newDocument()
                .fill("customerName", "Acme Ltd")
                .paragraph("Title", "Quarterly Report")
                .paragraph("Normal", p -> p.text("Revenue grew ").run("Strong", "12%"))
                .table("TableGrid", List.of(List.of("Region", "Q3"), List.of("EU", "1.4M")))
                .save(tmp.resolve("report.docx"));
        assertTrue(Files.size(tmp.resolve("report.docx")) > 0);
    }

    @Test
    void readmeDotxSnippet() throws Exception {
        var template = DotmTemplate.loadResource("templates/sample-template.dotx");
        template.newDocument()
                .paragraph(DotxStyles.BODY_TEXT, p -> p
                        .text("Revenue grew, with ").run(DotxStyles.SHADED_TEXT, "Europe up 17%")
                        .text(". See ").link("our website", "https://example.com").text("."))
                .paragraph(DotxStyles.SHADED_PANEL, "Figures are unaudited.")
                .table(DotxStyles.TABLE_HEADER_RULE, List.of(List.of("Region", "Q3"), List.of("EU", "1.4M")))
                .save(tmp.resolve("report-dotx.docx"));
        assertTrue(Files.size(tmp.resolve("report-dotx.docx")) > 0);
    }
}
