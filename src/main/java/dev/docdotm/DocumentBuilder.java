package dev.docdotm;

import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.sharedtypes.STOnOff;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTTblLayoutType;
import org.docx4j.wml.CTTblLook;
import org.docx4j.wml.Br;
import org.docx4j.wml.CTTblPrBase;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.RStyle;
import org.docx4j.wml.STBrType;
import org.docx4j.wml.STTblLayoutType;
import org.docx4j.wml.SectPr;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblGrid;
import org.docx4j.wml.TblGridCol;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.Text;
import org.docx4j.wml.Tr;
import org.docx4j.wml.TrPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Adds content to a document created by {@link DotmTemplate#newDocument()} and saves it as .docx.
 *
 * <p>Every style ID is checked against the template's {@link StyleCatalog}: an unknown ID or a style
 * of the wrong type fails immediately instead of rendering as Normal in Word.
 */
public final class DocumentBuilder {

    /** Relationship types from the main document part to macro-related parts. */
    private static final List<String> VBA_RELATIONSHIP_TYPES = List.of(
            "http://schemas.microsoft.com/office/2006/relationships/vbaProject",
            "http://schemas.microsoft.com/office/2006/relationships/keyMapCustomizations",
            "http://schemas.microsoft.com/office/2006/relationships/attachedToolbars");

    private static final Logger LOG = LoggerFactory.getLogger(DocumentBuilder.class);

    /** Used only when the template has no page size or margins: US Letter with 1" margins, in twips. */
    static final int FALLBACK_TEXT_WIDTH_TWIPS = 9360;

    static final String HYPERLINK_STYLE = "Hyperlink";
    private static final Set<String> LINK_SCHEMES = Set.of("http", "https", "mailto");

    private final WordprocessingMLPackage pkg;
    private final StyleCatalog styles;
    private final ObjectFactory wml = Context.getWmlObjectFactory();

    DocumentBuilder(WordprocessingMLPackage pkg, StyleCatalog styles) {
        this.pkg = pkg;
        this.styles = styles;
    }

    /** Appends a paragraph with the given paragraph style ID. */
    public DocumentBuilder paragraph(String styleId, String text) {
        return paragraph(styleId, p -> p.text(text));
    }

    /** Appends a paragraph whose runs are built by {@code content}, e.g. to mix character styles. */
    public DocumentBuilder paragraph(String styleId, Consumer<ParagraphBuilder> content) {
        P p = newParagraph(styles.require(styleId, StyleType.PARAGRAPH).id());
        content.accept(new ParagraphBuilder(p));
        pkg.getMainDocumentPart().getContent().add(p);
        return this;
    }

    /**
     * Appends a table with the given table style ID, spanning the text width between the page margins.
     * The first row is the header row: table-style header formatting applies to it and it repeats
     * at the top of each page.
     */
    public DocumentBuilder table(String styleId, List<List<String>> rows) {
        styles.require(styleId, StyleType.TABLE);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("A table needs at least one row");
        }
        int columns = rows.stream().mapToInt(List::size).max().orElse(1);
        int textWidth = textWidthTwips();
        int[] widths = new int[columns];
        Arrays.fill(widths, textWidth / columns);
        widths[columns - 1] += textWidth - (textWidth / columns) * columns;

        Tbl tbl = wml.createTbl();
        TblPr tblPr = wml.createTblPr();
        CTTblPrBase.TblStyle tblStyle = wml.createCTTblPrBaseTblStyle();
        tblStyle.setVal(styleId);
        tblPr.setTblStyle(tblStyle);
        tblPr.setTblW(width("dxa", textWidth));
        tblPr.setTblInd(width("dxa", 0));
        CTTblLayoutType layout = wml.createCTTblLayoutType();
        layout.setType(STTblLayoutType.FIXED);
        tblPr.setTblLayout(layout);
        tblPr.setTblLook(headerRowLook());
        tbl.setTblPr(tblPr);

        TblGrid grid = wml.createTblGrid();
        for (int w : widths) {
            TblGridCol col = wml.createTblGridCol();
            col.setW(BigInteger.valueOf(w));
            grid.getGridCol().add(col);
        }
        tbl.setTblGrid(grid);

        for (int r = 0; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            Tr tr = wml.createTr();
            if (r == 0) {
                TrPr trPr = wml.createTrPr();
                trPr.getCnfStyleOrDivIdOrGridBefore().add(wml.createCTTrPrBaseTblHeader(new BooleanDefaultTrue()));
                tr.setTrPr(trPr);
            }
            for (int c = 0; c < columns; c++) {
                Tc tc = wml.createTc();
                TcPr tcPr = wml.createTcPr();
                tcPr.setTcW(width("dxa", widths[c]));
                tc.setTcPr(tcPr);
                P p = newParagraph(null);
                p.getContent().add(newRun(null, c < row.size() ? row.get(c) : ""));
                tc.getContent().add(p);
                tr.getContent().add(tc);
            }
            tbl.getContent().add(tr);
        }
        pkg.getMainDocumentPart().getContent().add(tbl);
        return this;
    }

    public DocumentBuilder pageBreak() {
        Br br = wml.createBr();
        br.setType(STBrType.PAGE);
        R r = wml.createR();
        r.getContent().add(br);
        P p = newParagraph(null);
        p.getContent().add(r);
        pkg.getMainDocumentPart().getContent().add(p);
        return this;
    }

    /**
     * Sets the text of the content control(s) tagged {@code tag}, in the body, headers or footers.
     *
     * @throws UnknownContentControlException if no control has that tag
     */
    public DocumentBuilder fill(String tag, String value) {
        ContentControls.fill(pkg, tag, value);
        return this;
    }

    /** The underlying docx4j package, for anything this builder does not cover. */
    public WordprocessingMLPackage unwrap() {
        return pkg;
    }

    /**
     * Saves as a macro-free .docx: switches the main part to the document content type, removes VBA
     * parts and the {@code attachedTemplate} link, and creates parent directories as needed.
     */
    public Path save(Path out) throws IOException {
        convertToDocument();
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try {
            pkg.save(out.toFile());
        } catch (Docx4JException e) {
            throw new IOException("Cannot save " + out, e);
        }
        return out;
    }

    private void convertToDocument() {
        var mdp = pkg.getMainDocumentPart();
        RelationshipsPart rels = mdp.getRelationshipsPart(false);
        if (rels != null) {
            for (String type : VBA_RELATIONSHIP_TYPES) {
                for (var rel : List.copyOf(rels.getRelationshipsByType(type))) {
                    Part part = rels.getPart(rel);
                    if (part != null) {
                        // removePart drops the part, its relationship and descendants, but leaves
                        // their [Content_Types].xml overrides behind.
                        for (var removed : rels.removePart(part.getPartName())) {
                            pkg.getContentTypeManager().removeContentType(removed);
                        }
                    } else {
                        rels.removeRelationship(rel);
                    }
                }
            }
        }

        var settingsPart = mdp.getDocumentSettingsPart();
        if (settingsPart != null && settingsPart.getJaxbElement().getAttachedTemplate() != null) {
            String relId = settingsPart.getJaxbElement().getAttachedTemplate().getId();
            settingsPart.getJaxbElement().setAttachedTemplate(null);
            RelationshipsPart settingsRels = settingsPart.getRelationshipsPart(false);
            if (settingsRels != null) {
                settingsRels.getRelationships().getRelationship().removeIf(r -> r.getId().equals(relId));
            }
        }

        Wml.setMainContentType(pkg, ContentTypes.WORDPROCESSINGML_DOCUMENT);
    }

    /**
     * The text width of the document's (final) section: page width minus left and right margins.
     */
    int textWidthTwips() {
        SectPr sectPr = pkg.getMainDocumentPart().getJaxbElement().getBody().getSectPr();
        if (sectPr == null || sectPr.getPgSz() == null || sectPr.getPgSz().getW() == null || sectPr.getPgMar() == null) {
            LOG.warn("Template has no page size/margins; assuming a text width of {} twips", FALLBACK_TEXT_WIDTH_TWIPS);
            return FALLBACK_TEXT_WIDTH_TWIPS;
        }
        var margins = sectPr.getPgMar();
        return sectPr.getPgSz().getW().intValue() - intOrZero(margins.getLeft()) - intOrZero(margins.getRight());
    }

    private static int intOrZero(BigInteger value) {
        return value == null ? 0 : value.intValue();
    }

    /** Enables the table style's first-row (header) formatting; banding and first/last column stay off. */
    private CTTblLook headerRowLook() {
        CTTblLook look = wml.createCTTblLook();
        look.setVal("0420");
        look.setFirstRow(STOnOff.ONE);
        look.setLastRow(STOnOff.ZERO);
        look.setFirstColumn(STOnOff.ZERO);
        look.setLastColumn(STOnOff.ZERO);
        look.setNoHBand(STOnOff.ONE);
        look.setNoVBand(STOnOff.ONE);
        return look;
    }

    /** Adds an external hyperlink relationship to the main document part and returns its ID. */
    private String addHyperlinkRelationship(String url) {
        RelationshipsPart rels = pkg.getMainDocumentPart().getRelationshipsPart();
        var rel = new org.docx4j.relationships.ObjectFactory().createRelationship();
        rel.setId(rels.getNextId());
        rel.setType(Namespaces.HYPERLINK);
        rel.setTarget(url);
        rel.setTargetMode("External");
        rels.addRelationship(rel);
        return rel.getId();
    }

    private static void checkLink(String text, String url) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Link text must not be blank (URL: " + url + ")");
        }
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Link URL must not be blank (text: " + text + ")");
        }
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Malformed link URL '%s': %s".formatted(url, e.getReason()), e);
        }
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("Link URL '%s' must be absolute (http, https or mailto)".formatted(url));
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!LINK_SCHEMES.contains(scheme)) {
            throw new IllegalArgumentException("Link URL '%s' uses unsupported scheme '%s'; allowed: http, https, mailto".formatted(url, scheme));
        }
        if (!scheme.equals("mailto") && (uri.getHost() == null || uri.getHost().isBlank())) {
            throw new IllegalArgumentException("Malformed link URL '%s': no host".formatted(url));
        }
    }

    private P newParagraph(String styleId) {
        P p = wml.createP();
        if (styleId != null) {
            PPr pPr = wml.createPPr();
            PPrBase.PStyle pStyle = wml.createPPrBasePStyle();
            pStyle.setVal(styleId);
            pPr.setPStyle(pStyle);
            p.setPPr(pPr);
        }
        return p;
    }

    private R newRun(String characterStyleId, String value) {
        R r = wml.createR();
        if (characterStyleId != null) {
            RPr rPr = wml.createRPr();
            RStyle rStyle = wml.createRStyle();
            rStyle.setVal(characterStyleId);
            rPr.setRStyle(rStyle);
            r.setRPr(rPr);
        }
        Text t = wml.createText();
        t.setValue(value);
        t.setSpace("preserve");
        r.getContent().add(wml.createRT(t));
        return r;
    }

    private TblWidth width(String type, int value) {
        TblWidth w = wml.createTblWidth();
        w.setType(type);
        w.setW(BigInteger.valueOf(value));
        return w;
    }

    /** Builds the runs of one paragraph. */
    public final class ParagraphBuilder {

        private final P p;

        private ParagraphBuilder(P p) {
            this.p = p;
        }

        /** Appends plain text (inherits the paragraph style). */
        public ParagraphBuilder text(String value) {
            p.getContent().add(newRun(null, value));
            return this;
        }

        /**
         * Appends a hyperlink: {@code text} is what the reader sees, {@code url} is where it goes.
         * Styled with the template's {@code Hyperlink} character style. Only absolute http, https and
         * mailto URLs are accepted.
         *
         * @throws IllegalArgumentException if the text is blank or the URL is not an accepted absolute URL
         * @throws UnknownStyleException    if the template has no {@code Hyperlink} style
         */
        public ParagraphBuilder link(String text, String url) {
            checkLink(text, url);
            styles.require(HYPERLINK_STYLE, StyleType.CHARACTER);
            var hyperlink = wml.createPHyperlink();
            hyperlink.setId(addHyperlinkRelationship(url));
            hyperlink.setHistory(true);
            hyperlink.getContent().add(newRun(HYPERLINK_STYLE, text));
            p.getContent().add(wml.createPHyperlink(hyperlink));
            return this;
        }

        /** Appends text with a character style ID, e.g. {@code Strong}. */
        public ParagraphBuilder run(String characterStyleId, String value) {
            styles.require(characterStyleId, StyleType.CHARACTER);
            p.getContent().add(newRun(characterStyleId, value));
            return this;
        }
    }
}
