package dev.docdotm;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Prints every style a .dotm/.dotx/.docx defines, so its style IDs can be copied into code.
 *
 * <pre>
 * mvn -q exec:java -Dexec.mainClass=dev.docdotm.StyleInspector -Dexec.args=path/to/template.dotm
 * </pre>
 */
public final class StyleInspector {

    private StyleInspector() {
    }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) {
            System.exit(status);
        }
    }

    static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length != 1) {
            err.println("Usage: StyleInspector <template.dotm|.dotx|.docx>");
            return 2;
        }
        StyleCatalog catalog;
        try {
            catalog = DotmTemplate.load(Path.of(args[0])).styles();
        } catch (TemplateLoadException e) {
            err.println("Error: " + e.getMessage());
            return 1;
        }
        int idWidth = Math.max("STYLE ID".length(), catalog.all().stream().mapToInt(s -> s.id().length()).max().orElse(0));
        String row = "%-" + idWidth + "s  %-9s  %s%n";
        out.printf(row, "STYLE ID", "TYPE", "DISPLAY NAME");
        for (StyleInfo style : catalog.all()) {
            String name = style.name() == null ? "" : style.name();
            out.printf(row, style.id(), style.type().label(), style.custom() ? name + "  [custom]" : name);
        }
        return 0;
    }
}
