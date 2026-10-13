## 1. Project setup

- [x] 1.1 Create `pom.xml` (groupId `dev.docdotm`, `maven.compiler.release=25`, docx4j-JAXB-ReferenceImpl 17.2.0, slf4j-simple 2.0.20, junit-jupiter 6.1.3, latest compiler/surefire/enforcer/exec plugins, enforcer requiring JDK ≥ 25) and verify `JAVA_HOME=$(/usr/libexec/java_home -v 25) mvn -q verify` succeeds on the empty project
- [x] 1.2 Add `.gitignore` (target/, IDE files) and package skeleton `dev.docdotm` under `src/main/java` and `src/test/java`; verify `mvn -q compile` passes

## 2. Sample template generator

- [x] 2.1 Implement `SampleTemplateGenerator` starting from `WordprocessingMLPackage.createPackage()` (docx4j default styles) and activating `Quote`, `IntenseQuote`, `ListBullet`, `Footer`, `Strong` from docx4j's known styles; verify a test reads back all 15 built-in styles from the spec table with the expected ID, display name, and type
- [x] 2.2 Apply the house-style override to `Title` and `Heading1` and add custom `CalloutNote` (basedOn `Normal`, custom flag, shading/indent); verify tests that both overridden styles differ in font or color from a fresh docx4j package and that `CalloutNote` is flagged custom
- [x] 2.3 Add the `customerName` body content control and a header part with the `documentDate` content control, each with placeholder text; verify a test finds both tags and their placeholder text
- [x] 2.4 Save with the macro-enabled template main content type, creating parent directories as needed; verify a test asserts the content type and that output into a non-existent directory works
- [x] 2.5 Add a sample body paragraph per style, run the generator, commit `src/main/resources/templates/sample-template.dotm`, and verify it opens in Word or LibreOffice with the teal/Georgia `Title` and `Heading1` and `Callout Note` in the style gallery

## 3. Style catalog and IDs

- [x] 3.1 Implement `StyleInfo` record and `StyleCatalog` (`all`, `find`, `idForName` case-insensitive); verify tests for listing the sample catalog and resolving "Heading 1" → `Heading1`, "Callout Note" → `CalloutNote`, and unknown names
- [x] 3.2 Implement `require(id, type)` with `UnknownStyleException` (Levenshtein suggestions) and `StyleTypeMismatchException`; verify tests for `Headng1` suggesting `Heading1`, `heading 1` reported as a display name for `Heading1`, and `Emphasis` rejected as a paragraph style
- [x] 3.3 Implement `SampleStyles` constants and `verify(StyleCatalog)`; verify a test passes against the sample template and fails naming `CalloutNote` against a template with that style removed

## 4. Template loading and document generation

- [x] 4.1 Implement `DotmTemplate.load(Path)` and `loadResource(String)` with descriptive errors for missing and non-Word files; verify tests for all three load scenarios
- [x] 4.2 Implement `newDocument()` (fresh package per call, body cleared except final `sectPr` and content controls, which are kept by default) and `newDocument(ContentControls.DROP)`; verify tests that two documents are independent, an empty document has no template paragraphs but keeps its content controls, header and page setup, and the drop option leaves an empty body
- [x] 4.3 Implement `DocumentBuilder` methods `paragraph`, run builder (`text`, `run`), `table`, `pageBreak`, `unwrap`, all validating style IDs via the catalog; build `P`/`PStyle` directly via `ObjectFactory` (never docx4j's auto-activating helpers); verify tests assert `pStyle`, `rStyle`, and `tblStyle` values and table dimensions in the saved output
- [x] 4.4 Implement `save(Path)` conversion: document content type, remove VBA parts and relationships, remove `attachedTemplate`; verify a test using a fixture with a dummy `vbaProject.bin` shows no VBA part and the standard document content type after save
- [x] 4.5 Verify saved output retains all template style IDs, theme, numbering, and header via a round-trip test (reload saved `.docx` and compare catalogs), including that `Heading1` keeps the template's overridden font and color rather than docx4j's default

## 5. Content controls

- [x] 5.1 Implement `fill(tag, value)` across body, headers, and footers preserving run formatting, with `UnknownContentControlException`; verify tests for `customerName`, `documentDate` (header), and unknown `invoiceNumber`

## 6. CLIs, demo, docs

- [x] 6.1 Implement `StyleInspector` main (custom styles first, `[custom]` marker, exit code 1 on bad path) and exec config; verify `mvn -q exec:java -Dexec.mainClass=dev.docdotm.StyleInspector -Dexec.args=src/main/resources/templates/sample-template.dotm` lists `Heading1` as built-in and `CalloutNote` as custom, and a missing path exits non-zero
- [x] 6.2 Implement `Demo` main producing `target/report.docx` from the bundled template (`Title`, `Heading1`, `Normal` with a `Strong` run, `Quote`, `ListBullet`, `CalloutNote`, `TableGrid` table, both content controls filled); verify the file is produced and opens in Word/LibreOffice with template styling applied
- [x] 6.3 Write `README.md`: JDK 25 setup, generating/inspecting templates, the 5-line usage example, how style IDs differ from display names, how to plug in the real `.dotm` (inspect → copy IDs into a constants class → `verify`); verify commands in the README run as written
- [x] 6.4 Run `mvn verify` on JDK 25 and confirm all tests pass
