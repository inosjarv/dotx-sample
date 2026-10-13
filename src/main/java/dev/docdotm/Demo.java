package dev.docdotm;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static dev.docdotm.SampleStyles.CALLOUT_NOTE;
import static dev.docdotm.SampleStyles.HEADING_1;
import static dev.docdotm.SampleStyles.HEADING_2;
import static dev.docdotm.SampleStyles.LIST_BULLET;
import static dev.docdotm.SampleStyles.NORMAL;
import static dev.docdotm.SampleStyles.QUOTE;
import static dev.docdotm.SampleStyles.STRONG;
import static dev.docdotm.SampleStyles.TABLE_GRID;
import static dev.docdotm.SampleStyles.TITLE;

/**
 * Generates {@code target/report.docx} from the bundled sample template.
 *
 * <p>Usage: {@code Demo [templatePath] [outPath]}; without a template path the classpath sample is used.
 */
public final class Demo {

    private Demo() {
    }

    public static void main(String[] args) throws Exception {
        DotmTemplate template = args.length > 0
                ? DotmTemplate.load(Path.of(args[0]))
                : DotmTemplate.loadResource("templates/sample-template.dotm");
        Path out = Path.of(args.length > 1 ? args[1] : "target/report.docx");

        SampleStyles.verify(template.styles());

        template.newDocument()
                .fill("customerName", "Acme Ltd")
                .fill("documentDate", LocalDate.now().toString())
                .paragraph(TITLE, "Quarterly Report")
                .paragraph(HEADING_1, "Overview")
                .paragraph(NORMAL, p -> p.text("Revenue grew ").run(STRONG, "12%").text(" this quarter."))
                .paragraph(QUOTE, "Best quarter since launch. — CFO")
                .paragraph(HEADING_2, "Highlights")
                .paragraph(LIST_BULLET, "Two new regions opened")
                .paragraph(LIST_BULLET, "Churn down to 3%")
                .paragraph(CALLOUT_NOTE, "Figures are unaudited.")
                .table(TABLE_GRID, List.of(
                        List.of("Region", "Q2", "Q3"),
                        List.of("EU", "1.2M", "1.4M"),
                        List.of("US", "2.0M", "2.2M")))
                .save(out);

        System.out.println("Wrote " + out.toAbsolutePath());
    }
}
