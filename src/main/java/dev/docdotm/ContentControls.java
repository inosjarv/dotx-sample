package dev.docdotm;

import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.SdtElement;
import org.docx4j.wml.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What {@link DotmTemplate#newDocument(ContentControls)} does with the template body's content
 * controls, plus helpers to find and fill controls by their {@code w:tag}.
 */
public enum ContentControls {
    /** Keep the template body's content controls so they can be {@linkplain DocumentBuilder#fill filled}. */
    KEEP,
    /** Remove them too, leaving an empty body. */
    DROP;

    /** A content control found in a document part. */
    record Found(SdtElement sdt, String tag, String text) {
    }

    /** All content controls in the main document part, headers and footers. */
    static List<Found> findAll(WordprocessingMLPackage pkg) {
        var sdts = new ArrayList<SdtElement>();
        collect(pkg.getMainDocumentPart().getContent(), sdts);
        for (Part part : pkg.getParts().getParts().values()) {
            if (part instanceof HeaderPart || part instanceof FooterPart) {
                collect(((ContentAccessor) ((JaxbXmlPart<?>) part).getJaxbElement()).getContent(), sdts);
            }
        }
        return sdts.stream().map(s -> new Found(s, tagOf(s), textOf(s))).toList();
    }

    /**
     * Replaces the text of every control tagged {@code tag}. The first run is kept (with its
     * formatting) and given the new text; other runs and extra paragraphs inside the control are removed.
     *
     * @throws UnknownContentControlException if no control has that tag
     */
    static void fill(WordprocessingMLPackage pkg, String tag, String value) {
        List<Found> matches = findAll(pkg).stream().filter(f -> tag.equals(f.tag())).toList();
        if (matches.isEmpty()) {
            throw new UnknownContentControlException(tag);
        }
        for (Found found : matches) {
            replaceText(found.sdt(), value);
        }
    }

    static boolean containsContentControl(Object element) {
        var sdts = new ArrayList<SdtElement>();
        collect(List.of(element), sdts);
        return !sdts.isEmpty();
    }

    private static void replaceText(SdtElement sdt, String value) {
        List<Object> content = sdt.getSdtContent().getContent();
        R firstRun = firstRun(content);
        if (firstRun == null) {
            throw new IllegalStateException("Content control '%s' has no text run to fill".formatted(tagOf(sdt)));
        }
        // Keep only the container holding the first run (the run itself for inline controls,
        // its paragraph for block controls), then keep only that run.
        Object holder = content.stream().filter(o -> contains(o, firstRun)).findFirst().orElseThrow();
        content.removeIf(o -> o != holder);
        if (XmlUtils.unwrap(holder) instanceof P p) {
            p.getContent().removeIf(o -> XmlUtils.unwrap(o) instanceof R && XmlUtils.unwrap(o) != firstRun);
        }
        if (firstRun.getRPr() != null && firstRun.getRPr().getRStyle() != null
                && "PlaceholderText".equals(firstRun.getRPr().getRStyle().getVal())) {
            firstRun.getRPr().setRStyle(null);
        }
        var text = new Text();
        text.setValue(value);
        text.setSpace("preserve");
        firstRun.getContent().clear();
        firstRun.getContent().add(text);
        sdt.getSdtPr().setShowingPlcHdr(false);
    }

    private static void collect(List<Object> content, List<SdtElement> out) {
        for (Object o : content) {
            Object u = XmlUtils.unwrap(o);
            if (u instanceof SdtElement sdt) {
                out.add(sdt);
                if (sdt.getSdtContent() != null) {
                    collect(sdt.getSdtContent().getContent(), out);
                }
            } else if (u instanceof ContentAccessor ca) {
                collect(ca.getContent(), out);
            }
        }
    }

    private static R firstRun(List<Object> content) {
        for (Object o : content) {
            Object u = XmlUtils.unwrap(o);
            if (u instanceof R r) {
                return r;
            }
            if (u instanceof ContentAccessor ca) {
                R nested = firstRun(ca.getContent());
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean contains(Object container, R run) {
        Object u = XmlUtils.unwrap(container);
        if (u == run) {
            return true;
        }
        return u instanceof ContentAccessor ca && ca.getContent().stream().anyMatch(c -> contains(c, run));
    }

    private static String tagOf(SdtElement sdt) {
        var tag = sdt.getSdtPr() == null ? null : sdt.getSdtPr().getTag();
        return tag == null ? null : tag.getVal();
    }

    private static String textOf(SdtElement sdt) {
        var sb = new StringBuilder();
        appendText(sdt.getSdtContent() == null ? List.of() : sdt.getSdtContent().getContent(), sb);
        return sb.toString();
    }

    private static void appendText(List<Object> content, StringBuilder sb) {
        for (Object o : content) {
            Object u = XmlUtils.unwrap(o);
            if (u instanceof Text t) {
                sb.append(Objects.requireNonNullElse(t.getValue(), ""));
            } else if (u instanceof ContentAccessor ca) {
                appendText(ca.getContent(), sb);
            }
        }
    }
}
