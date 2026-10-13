package dev.docdotm;

import org.docx4j.openpackaging.contenttype.ContentTypes;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.FooterPart;
import org.docx4j.openpackaging.parts.WordprocessingML.HeaderPart;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.WordprocessingML.NumberingDefinitionsPart;
import org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart;
import org.docx4j.wml.Color;
import org.docx4j.wml.Document;
import org.docx4j.wml.FooterReference;
import org.docx4j.wml.Ftr;
import org.docx4j.wml.HdrFtrRef;
import org.docx4j.wml.Hdr;
import org.docx4j.wml.HeaderReference;
import org.docx4j.wml.Numbering;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.Style;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Builds {@code sample-template.dotm} from docx4j's own style definitions.
 *
 * <ol>
 *   <li>{@link WordprocessingMLPackage#createPackage()} brings docx4j's default styles
 *       ({@code Normal}, {@code Title}, {@code Heading1}, {@code Emphasis}, {@code TableGrid}, ...).</li>
 *   <li>Further Word built-ins are activated from docx4j's known-styles catalog.</li>
 *   <li>{@code Title} and {@code Heading1} get a house font and colour so generated documents
 *       visibly use the template rather than docx4j's defaults.</li>
 *   <li>One custom style, {@code CalloutNote}, plus a header, footer and two content controls.</li>
 * </ol>
 *
 * <p>Usage: {@code SampleTemplateGenerator [outPath]}.
 */
public final class SampleTemplateGenerator {

    public static final Path DEFAULT_OUTPUT = Path.of("src/main/resources/templates/sample-template.dotm");

    static final String HOUSE_FONT = "Georgia";
    static final String HOUSE_COLOR = "1F4E5F";

    /** Built-ins not in docx4j's default styles.xml, copied in from its KnownStyles.xml. */
    static final List<String> KNOWN_STYLES_TO_ACTIVATE = List.of(
            SampleStyles.QUOTE, SampleStyles.INTENSE_QUOTE, SampleStyles.LIST_BULLET, SampleStyles.FOOTER, SampleStyles.STRONG);

    private SampleTemplateGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path out = args.length > 0 ? Path.of(args[0]) : DEFAULT_OUTPUT;
        generate(out);
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    /** Writes the sample template to {@code out}, creating parent directories as needed. */
    public static Path generate(Path out) throws Docx4JException, IOException {
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        build().save(out.toFile());
        return out;
    }

    static WordprocessingMLPackage build() throws Docx4JException {
        WordprocessingMLPackage pkg = WordprocessingMLPackage.createPackage();
        MainDocumentPart mdp = pkg.getMainDocumentPart();

        activateKnownStyles(mdp);
        applyHouseStyle(mdp.getStyleDefinitionsPart());
        addCalloutNote(mdp.getStyleDefinitionsPart());
        addBulletNumbering(mdp);
        addHeaderAndFooter(pkg);
        addSampleBody(mdp);

        Wml.setMainContentType(pkg, ContentTypes.WORDPROCESSINGML_TEMPLATE_MACROENABLED);
        return pkg;
    }

    private static void activateKnownStyles(MainDocumentPart mdp) throws Docx4JException {
        var resolver = mdp.getPropertyResolver();
        for (String id : KNOWN_STYLES_TO_ACTIVATE) {
            if (!resolver.activateStyle(id)) {
                throw new IllegalStateException("docx4j does not know built-in style " + id);
            }
        }
    }

    /** Replaces the theme font/colour on Title and Heading1 (and their linked character styles). */
    private static void applyHouseStyle(StyleDefinitionsPart styles) {
        for (String id : List.of("Title", "TitleChar", "Heading1", "Heading1Char")) {
            Style style = styles.getStyleById(id);
            if (style == null) {
                continue;
            }
            RPr rPr = style.getRPr() != null ? style.getRPr() : new RPr();
            var fonts = new RFonts();
            fonts.setAscii(HOUSE_FONT);
            fonts.setHAnsi(HOUSE_FONT);
            fonts.setCs(HOUSE_FONT);
            rPr.setRFonts(fonts);
            var color = new Color();
            color.setVal(HOUSE_COLOR);
            rPr.setColor(color);
            style.setRPr(rPr);
        }
        Style title = styles.getStyleById("Title");
        if (title.getPPr() != null && title.getPPr().getPBdr() != null && title.getPPr().getPBdr().getBottom() != null) {
            var bottom = title.getPPr().getPBdr().getBottom();
            bottom.setColor(HOUSE_COLOR);
            bottom.setThemeColor(null);
        }
    }

    private static void addCalloutNote(StyleDefinitionsPart styles) {
        Style callout = Wml.parse("""
                <w:style %s w:type="paragraph" w:customStyle="1" w:styleId="CalloutNote">
                  <w:name w:val="Callout Note"/>
                  <w:basedOn w:val="Normal"/>
                  <w:qFormat/>
                  <w:pPr>
                    <w:pBdr><w:left w:val="single" w:sz="24" w:space="8" w:color="%s"/></w:pBdr>
                    <w:shd w:val="clear" w:color="auto" w:fill="E6F0F2"/>
                    <w:spacing w:before="120" w:after="120"/>
                    <w:ind w:left="284" w:right="284"/>
                  </w:pPr>
                  <w:rPr><w:i/><w:color w:val="%s"/></w:rPr>
                </w:style>
                """.formatted(Wml.NS, HOUSE_COLOR, HOUSE_COLOR));
        styles.getJaxbElement().getStyle().add(callout);
    }

    /** docx4j's ListBullet style points at numId 4, which must exist for bullets to render. */
    private static void addBulletNumbering(MainDocumentPart mdp) throws Docx4JException {
        Numbering numbering = Wml.parse("""
                <w:numbering %s>
                  <w:abstractNum w:abstractNumId="0">
                    <w:multiLevelType w:val="singleLevel"/>
                    <w:lvl w:ilvl="0">
                      <w:start w:val="1"/>
                      <w:numFmt w:val="bullet"/>
                      <w:pStyle w:val="ListBullet"/>
                      <w:lvlText w:val="•"/>
                      <w:lvlJc w:val="left"/>
                      <w:pPr><w:ind w:left="360" w:hanging="360"/></w:pPr>
                    </w:lvl>
                  </w:abstractNum>
                  <w:num w:numId="4"><w:abstractNumId w:val="0"/></w:num>
                </w:numbering>
                """.formatted(Wml.NS));
        var part = new NumberingDefinitionsPart();
        part.setJaxbElement(numbering);
        mdp.addTargetPart(part);
    }

    private static void addHeaderAndFooter(WordprocessingMLPackage pkg) throws Docx4JException {
        MainDocumentPart mdp = pkg.getMainDocumentPart();
        var sectPr = mdp.getJaxbElement().getBody().getSectPr();

        Hdr hdr = Wml.parse("""
                <w:hdr %s>
                  <w:p>
                    <w:pPr><w:pStyle w:val="Header"/><w:jc w:val="right"/></w:pPr>
                    <w:r><w:t xml:space="preserve">Date: </w:t></w:r>
                    <w:sdt>
                      <w:sdtPr><w:alias w:val="Document date"/><w:tag w:val="documentDate"/><w:id w:val="2"/><w:text/></w:sdtPr>
                      <w:sdtContent><w:r><w:t>[document date]</w:t></w:r></w:sdtContent>
                    </w:sdt>
                  </w:p>
                </w:hdr>
                """.formatted(Wml.NS));
        var headerPart = new HeaderPart();
        headerPart.setPackage(pkg);
        headerPart.setJaxbElement(hdr);
        var headerRef = new HeaderReference();
        headerRef.setId(mdp.addTargetPart(headerPart).getId());
        headerRef.setType(HdrFtrRef.DEFAULT);
        sectPr.getEGHdrFtrReferences().add(headerRef);

        Ftr ftr = Wml.parse("""
                <w:ftr %s>
                  <w:p>
                    <w:pPr><w:pStyle w:val="Footer"/><w:jc w:val="center"/></w:pPr>
                    <w:r><w:t>Generated from sample-template.dotm</w:t></w:r>
                  </w:p>
                </w:ftr>
                """.formatted(Wml.NS));
        var footerPart = new FooterPart();
        footerPart.setPackage(pkg);
        footerPart.setJaxbElement(ftr);
        var footerRef = new FooterReference();
        footerRef.setId(mdp.addTargetPart(footerPart).getId());
        footerRef.setType(HdrFtrRef.DEFAULT);
        sectPr.getEGHdrFtrReferences().add(footerRef);
    }

    /** One paragraph per style so opening the .dotm in Word shows the styles in use. */
    private static void addSampleBody(MainDocumentPart mdp) {
        Document sample = Wml.parse("""
                <w:document %s><w:body>
                  <w:p><w:pPr><w:pStyle w:val="Title"/></w:pPr><w:r><w:t>Sample Template (Title)</w:t></w:r></w:p>
                  <w:sdt>
                    <w:sdtPr><w:alias w:val="Customer name"/><w:tag w:val="customerName"/><w:id w:val="1"/><w:text/></w:sdtPr>
                    <w:sdtContent>
                      <w:p><w:pPr><w:pStyle w:val="Subtitle"/></w:pPr><w:r><w:t>[customer name]</w:t></w:r></w:p>
                    </w:sdtContent>
                  </w:sdt>
                  <w:p><w:pPr><w:pStyle w:val="Heading1"/></w:pPr><w:r><w:t>Heading 1 (Heading1)</w:t></w:r></w:p>
                  <w:p><w:r><w:t xml:space="preserve">Normal text with </w:t></w:r><w:r><w:rPr><w:rStyle w:val="Strong"/></w:rPr><w:t>Strong</w:t></w:r><w:r><w:t xml:space="preserve"> and </w:t></w:r><w:r><w:rPr><w:rStyle w:val="Emphasis"/></w:rPr><w:t>Emphasis</w:t></w:r><w:r><w:t xml:space="preserve"> runs.</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="Heading2"/></w:pPr><w:r><w:t>Heading 2 (Heading2)</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="Quote"/></w:pPr><w:r><w:t>A quotation (Quote)</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="IntenseQuote"/></w:pPr><w:r><w:t>An intense quotation (IntenseQuote)</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="ListBullet"/></w:pPr><w:r><w:t>First bullet (ListBullet)</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="ListBullet"/></w:pPr><w:r><w:t>Second bullet (ListBullet)</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="Heading3"/></w:pPr><w:r><w:t>Heading 3 (Heading3)</w:t></w:r></w:p>
                  <w:p><w:pPr><w:pStyle w:val="CalloutNote"/></w:pPr><w:r><w:t>A custom callout (CalloutNote)</w:t></w:r></w:p>
                  <w:tbl>
                    <w:tblPr><w:tblStyle w:val="TableGrid"/><w:tblW w:w="5000" w:type="pct"/></w:tblPr>
                    <w:tblGrid><w:gridCol w:w="4680"/><w:gridCol w:w="4680"/></w:tblGrid>
                    <w:tr><w:tc><w:p><w:r><w:t>TableGrid</w:t></w:r></w:p></w:tc><w:tc><w:p><w:r><w:t>table style</w:t></w:r></w:p></w:tc></w:tr>
                  </w:tbl>
                  <w:p><w:pPr><w:pStyle w:val="Caption"/></w:pPr><w:r><w:t>Table 1: a caption (Caption)</w:t></w:r></w:p>
                </w:body></w:document>
                """.formatted(Wml.NS));
        mdp.getContent().addAll(sample.getBody().getContent());
    }
}
