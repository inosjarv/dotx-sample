package dev.docdotm;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.docdotm.DotxStyles.BODY_TEXT;
import static dev.docdotm.DotxStyles.HEADING_1;
import static dev.docdotm.DotxStyles.HEADING_2;
import static dev.docdotm.DotxStyles.NORMAL;
import static dev.docdotm.DotxStyles.SHADED_PANEL;
import static dev.docdotm.DotxStyles.SHADED_TEXT;
import static dev.docdotm.DotxStyles.TABLE_BLUE;
import static dev.docdotm.DotxStyles.TABLE_HEADER_RULE;
import static dev.docdotm.DotxStyles.TABLE_RED;
import static dev.docdotm.DotxStyles.TABLE_TWO_TONE;
import static dev.docdotm.DotxStyles.TITLE;

/**
 * Generates {@code target/report-dotx.docx} from the hand-written {@code sample-template.dotx}:
 * spacing, shading, hyperlinks and every table style, with tables sized to the template's margins.
 *
 * <p>Usage: {@code DotxDemo [outPath]}.
 */
public final class DotxDemo {

    private DotxDemo() {
    }

    public static void main(String[] args) throws Exception {
        DotmTemplate template = DotmTemplate.loadResource("templates/sample-template.dotx");
        Path out = Path.of(args.length > 0 ? args[0] : "target/report-dotx.docx");

        DotxStyles.verify(template.styles());

        var sales = List.of(
                List.of("Region", "Q2", "Q3"),
                List.of("EU", "1.2M", "1.4M"),
                List.of("US", "2.0M", "2.2M"));

        template.newDocument()
                .paragraph(TITLE, "Regional Sales Report")
                .paragraph(HEADING_1, "Summary")
                .paragraph(BODY_TEXT, p -> p
                        .text("Revenue grew in every region, with ")
                        .run(SHADED_TEXT, "Europe up 17%")
                        .text(" quarter on quarter. Body Text uses 1.5 line spacing with space before and after."))
                .paragraph(BODY_TEXT, p -> p
                        .text("Full figures are on ")
                        .link("our website", "https://example.com/reports/q3")
                        .text(", and questions can go to ")
                        .link("the sales team", "mailto:sales@example.com")
                        .text("."))
                .paragraph(SHADED_PANEL, "Note: figures are unaudited and may be revised before year end.")
                .paragraph(HEADING_2, "Border colours")
                .paragraph(NORMAL, "Blue borders:")
                .table(TABLE_BLUE, sales)
                .paragraph(NORMAL, "Red borders:")
                .table(TABLE_RED, sales)
                .paragraph(NORMAL, "Two-tone: dark outer frame, light inner lines:")
                .table(TABLE_TWO_TONE, sales)
                .paragraph(HEADING_2, "Header rule only")
                .paragraph(NORMAL, "Only a line under the header row; the header repeats on the next page.")
                .table(TABLE_HEADER_RULE, monthlyRows())
                .save(out);

        System.out.println("Wrote " + out.toAbsolutePath());
    }

    /** Enough rows to run onto a second page, so the repeating header row is visible. */
    private static List<List<String>> monthlyRows() {
        var rows = new ArrayList<List<String>>();
        rows.add(List.of("Week", "Region", "Orders", "Revenue"));
        for (int week = 1; week <= 40; week++) {
            rows.add(List.of("Week " + week, week % 2 == 0 ? "EU" : "US",
                    String.valueOf(100 + week * 7), "%,d".formatted(20_000 + week * 1_250)));
        }
        return rows;
    }
}
