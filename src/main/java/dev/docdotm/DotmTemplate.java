package dev.docdotm;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A loaded Word template ({@code .dotm}, {@code .dotx} or {@code .docx}) that documents are created from.
 *
 * <pre>{@code
 * var template = DotmTemplate.load(Path.of("house-style.dotm"));
 * template.newDocument()
 *         .paragraph("Title", "Quarterly Report")
 *         .save(Path.of("report.docx"));
 * }</pre>
 *
 * <p>Instances are immutable and reusable: every {@link #newDocument()} starts from a fresh copy
 * of the template, so documents never affect each other or the template.
 */
public final class DotmTemplate {

    private final byte[] bytes;
    private final String source;
    private final StyleCatalog styles;

    private DotmTemplate(byte[] bytes, String source) {
        this.bytes = bytes;
        this.source = source;
        this.styles = StyleCatalog.of(parse());
    }

    /**
     * Loads a template from the file system.
     *
     * @throws TemplateLoadException if the file is missing or not a Word package
     */
    public static DotmTemplate load(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new TemplateLoadException("Template not found: " + path.toAbsolutePath(), null);
        }
        try {
            return new DotmTemplate(Files.readAllBytes(path), path.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read template " + path, e);
        }
    }

    /**
     * Loads a template from the classpath, e.g. {@code "templates/sample-template.dotm"}.
     *
     * @throws TemplateLoadException if the resource is missing or not a Word package
     */
    public static DotmTemplate loadResource(String resource) {
        try (InputStream in = DotmTemplate.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new TemplateLoadException("Template not found on classpath: " + resource, null);
            }
            return new DotmTemplate(in.readAllBytes(), "classpath:" + resource);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read template " + resource, e);
        }
    }

    /** The styles this template defines. */
    public StyleCatalog styles() {
        return styles;
    }

    /** Starts a new document, keeping the template body's content controls so they can be filled. */
    public DocumentBuilder newDocument() {
        return newDocument(ContentControls.KEEP);
    }

    /**
     * Starts a new document that carries the template's styles, theme, numbering, headers, footers
     * and page setup. The template's body content is removed, except (with {@link ContentControls#KEEP})
     * top-level elements that are or contain content controls.
     */
    public DocumentBuilder newDocument(ContentControls contentControls) {
        WordprocessingMLPackage pkg = parse();
        // The final w:sectPr (page size, margins, header/footer references) is held outside
        // getContent(), so clearing the content list keeps the page setup.
        pkg.getMainDocumentPart().getContent().removeIf(
                element -> contentControls == ContentControls.DROP || !ContentControls.containsContentControl(element));
        return new DocumentBuilder(pkg, styles);
    }

    private WordprocessingMLPackage parse() {
        try {
            return WordprocessingMLPackage.load(new ByteArrayInputStream(bytes));
        } catch (Docx4JException | RuntimeException e) {
            throw new TemplateLoadException("Not a valid Word template: " + source, e);
        }
    }
}
