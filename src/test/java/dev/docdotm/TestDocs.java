package dev.docdotm;

import org.docx4j.TextUtils;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.P;
import org.docx4j.wml.Tbl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

/** Test helpers for inspecting saved documents. */
final class TestDocs {

    static final DotmTemplate SAMPLE = DotmTemplate.loadResource("templates/sample-template.dotm");
    static final DotmTemplate DOTX = DotmTemplate.loadResource("templates/sample-template.dotx");

    private TestDocs() {
    }

    static WordprocessingMLPackage reload(Path docx) throws Exception {
        return WordprocessingMLPackage.load(docx.toFile());
    }

    static List<Object> body(WordprocessingMLPackage pkg) {
        return pkg.getMainDocumentPart().getContent();
    }

    static P paragraphWithText(WordprocessingMLPackage pkg, String text) {
        return body(pkg).stream().map(XmlUtils::unwrap)
                .filter(o -> o instanceof P p && text.equals(TextUtils.getText(p)))
                .map(P.class::cast).findFirst()
                .orElseThrow(() -> new AssertionError("No paragraph with text: " + text));
    }

    static List<Tbl> tables(WordprocessingMLPackage pkg) {
        return body(pkg).stream().map(XmlUtils::unwrap).filter(Tbl.class::isInstance).map(Tbl.class::cast).toList();
    }

    static String zipEntry(Path file, String name) throws IOException {
        try (var zip = new ZipFile(file.toFile())) {
            var entry = zip.getEntry(name);
            return entry == null ? null : new String(zip.getInputStream(entry).readAllBytes());
        }
    }

    static List<String> zipEntries(Path file) throws IOException {
        try (var zip = new ZipFile(file.toFile())) {
            return zip.stream().map(java.util.zip.ZipEntry::getName).toList();
        }
    }
}
