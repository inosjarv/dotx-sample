package dev.docdotm;

import jakarta.xml.bind.JAXBException;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/** Small package-private helpers shared by the generator and the document builder. */
final class Wml {

    static final String NS = "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"";

    private Wml() {
    }

    /** Unmarshals a WordprocessingML fragment; the root element must declare {@link #NS}. */
    @SuppressWarnings("unchecked")
    static <T> T parse(String xml) {
        try {
            return (T) XmlUtils.unwrap(XmlUtils.unmarshalString(xml));
        } catch (JAXBException e) {
            throw new IllegalStateException("Invalid WordprocessingML fragment", e);
        }
    }

    /**
     * Sets the main document part's content type. This alone decides whether Word treats the
     * package as a document (.docx), template (.dotx) or macro-enabled template (.dotm).
     */
    static void setMainContentType(WordprocessingMLPackage pkg, String contentType) {
        var mdp = pkg.getMainDocumentPart();
        mdp.setContentType(new ContentType(contentType));
        pkg.getContentTypeManager().addOverrideContentType(mdp.getPartName().getURI(), contentType);
    }
}
