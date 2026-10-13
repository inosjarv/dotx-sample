## Context

The repository is empty apart from OpenSpec. JDK 25 (Temurin 25.0.1) and Maven 3.9.16 are installed; Maven currently defaults to JDK 27, so the build must pin the release level. See proposal.md for motivation and specs/ for required behavior.

Background on the "IDs" in a `.dotm`: a template is a ZIP of XML parts. `word/styles.xml` defines each style with a `w:styleId` (the ID paragraphs reference, e.g. `Heading1`) and a `w:name` (what Word shows, e.g. "heading 1"). Paragraphs point at styles with `<w:pStyle w:val="Heading1"/>`, runs with `<w:rStyle>`, tables with `<w:tblStyle>`. If a referenced ID does not exist, Word silently renders `Normal` — which is why this design validates IDs in code. Content controls (`w:sdt`) carry a `w:tag` that code can use as a stable placeholder key.

## Goals / Non-Goals

**Goals:**
- Using a template takes ~3 lines: load template, build document with style IDs, save.
- Style ID mistakes fail at generation time with a helpful message.
- The same code works for the sample template and the user's real `.dotm`.

**Non-Goals:**
- Running, editing, or preserving VBA macros.
- A web service/REST API (the project name suggests one later; this change is a library + CLI demo).
- Data binding via custom XML parts, mail merge fields, bookmarks, or PDF export.
- Generating Java constants automatically from a template (a later enhancement; the inspector prints the IDs to copy in).

## Decisions

### D1. Maven, Java 25, docx4j 17.2.0 with JAXB RI
- `maven.compiler.release=25`, Maven Enforcer requires JDK ≥ 25. Uses records and pattern matching where natural; no preview features.
- Dependencies: `org.docx4j:docx4j-JAXB-ReferenceImpl:17.2.0` (pulls `docx4j-core`), `org.slf4j:slf4j-simple:2.0.20` (docx4j logs through SLF4J; the 2.1 line is still alpha), `org.junit.jupiter:junit-jupiter:6.1.3`. Plugins at latest stable: compiler, surefire, enforcer, exec.
- *Alternative:* MOXy JAXB provider — rejected; RI is docx4j's default and has fewer classpath surprises. Apache POI — rejected; the user asked for docx4j, and docx4j's JAXB object model makes style/sdt manipulation straightforward.
- Package root: `dev.docdotm`.

### D2. Create documents by loading the template, clearing the body, then converting content type
`DotmTemplate` keeps the template bytes. `newDocument()` loads a fresh `WordprocessingMLPackage` from those bytes (satisfies "reusable, independent documents"), removes all body content except the last `w:sectPr` (page setup + header/footer references) and the body's content controls, then returns a `DocumentBuilder`. Content controls are kept by default because they are the template's fill-in placeholders; `newDocument(ContentControls.DROP)` removes them too. The sample paragraphs the generator puts in the template body (D7) are therefore dropped automatically.

On save, the builder converts template → document:
1. Set the main document part content type to `ContentTypes.WORDPROCESSINGML_DOCUMENT`.
2. Remove the `vbaProject` part and its related parts (`vbaData.xml`, `keyMapCustomizations.xml`) and their relationships.
3. Remove `w:attachedTemplate` from settings so Word does not look for the template path.
4. Save as `.docx`.

- *Alternative A:* Create an empty `WordprocessingMLPackage` and copy the styles, numbering, theme, font table, header/footer parts in. Rejected: every copied part needs its relationships rewired (headers with images, numbering referenced by styles), which is error-prone; loading the template keeps everything consistent for free.
- *Alternative B:* Reference the template via `attachedTemplate` only. Rejected: styles would not be embedded, so output would render without them on other machines.

### D3. `StyleCatalog` wraps `StyleDefinitionsPart`
Built once per loaded template: a `Map<String, StyleInfo>` keyed by style ID, where `record StyleInfo(String id, String name, StyleType type, String basedOn, boolean custom)`. `custom` comes from `w:customStyle`. Operations: `all()`, `find(id)`, `idForName(displayName)` (case-insensitive), `require(id, type)`.

`require` throws `UnknownStyleException` with up to 3 suggestions ranked by Levenshtein distance against IDs and names (if the value exactly matches a display name, the message says so and gives the ID), or `StyleTypeMismatchException`. Every `DocumentBuilder` method that takes a style ID calls `require` — this is the single enforcement point for the spec's "reject unknown or mismatched style IDs".

### D4. Style IDs as constants: `final class SampleStyles` with `String` constants plus `verify(StyleCatalog)`
Constants such as `TITLE = "Title"`, `HEADING_1 = "Heading1"`, `EMPHASIS = "Emphasis"`, `TABLE_GRID = "TableGrid"`, `CALLOUT_NOTE = "CalloutNote"`, each paired with its expected type. Chosen over an enum because docx4j APIs take `String` values and a user's real template will simply get its own constants class copied from the inspector output. `verify` checks each constant's existence and type (drives the "constants match template" scenario and a unit test that catches template drift).

### D5. Small fluent `DocumentBuilder`
```java
var template = DotmTemplate.load(Path.of("templates/sample-template.dotm"));
template.newDocument()
    .fill("customerName", "Acme Ltd")
    .fill("documentDate", "2026-09-23")
    .paragraph(SampleStyles.TITLE, "Quarterly Report")
    .paragraph(SampleStyles.HEADING_1, "Overview")
    .paragraph(SampleStyles.NORMAL, p -> p.text("Revenue grew ").run(SampleStyles.STRONG, "12%").text(" this quarter."))
    .paragraph(SampleStyles.CALLOUT_NOTE, "Figures are unaudited.")
    .table(SampleStyles.TABLE_GRID, List.of(List.of("Region","Q2","Q3"), List.of("EU","1.2M","1.4M")))
    .save(Path.of("target/report.docx"));
```
Built on docx4j's `ObjectFactory` (`P`, `PPr.PStyle`, `R`, `RPr.RStyle`, `Tbl`, `TblPr.TblStyle`). Deliberately small: headings, paragraphs, runs, tables, page break. Callers can reach the underlying `WordprocessingMLPackage` via `unwrap()` for anything else.

### D6. Content controls filled by tag via XPath-free traversal
Use `TraversalUtil` / `SdtFinder` over the main document part and every header/footer part, match `SdtPr.getTag().getVal()`, and replace the text of the first run in `SdtContent` (removing extra runs) so the control's run formatting is kept. Controls are left in place (not unwrapped) so the document stays re-fillable. Unknown tag → `UnknownContentControlException`.

### D7. Sample template built from docx4j's own styles, output committed
`SampleTemplateGenerator` (main class):
1. `WordprocessingMLPackage.createPackage()` — docx4j populates `StyleDefinitionsPart` from its bundled default `styles.xml` (22 styles incl. `Normal`, `Title`, `Subtitle`, `Heading1`–`Heading4`, `Header`, `Caption`, `Emphasis`, `TableGrid`).
2. Activate extra Word built-ins from docx4j's bundled `KnownStyles.xml` (164 styles) — `Quote`, `IntenseQuote`, `ListBullet`, `Footer`, `Strong` — via the property resolver's style activation (`activateStyle`), which copies the definition and its `basedOn`/linked chain into the styles part.
3. House-style override: edit the `Title` and `Heading1` `Style` objects in place (font e.g. Georgia, colour e.g. dark teal `1F4E5F`). Keeps Word's IDs and names, so documents stay "normal" Word docs.
4. Add custom `CalloutNote` (`customStyle=true`, basedOn `Normal`, light shading + left indent).
5. Add a header part with the `documentDate` control and a body with the `customerName` control; add a small sample body (one paragraph per style) so opening the `.dotm` in Word shows the styles in use.
6. Set the main part content type to `application/vnd.ms-word.template.macroEnabledTemplate.main+xml` and save as `.dotm`.

The generated file is committed at `src/main/resources/templates/sample-template.dotm` so the demo works without running the generator.
- *Alternative:* hand-define all styles as custom `Doc*` styles — rejected per user direction; built-ins also demonstrate the ID-vs-display-name split better (`Heading1` / "heading 1"), and one custom style still covers the custom path.
- *Alternative:* exact docx4j defaults with no override — rejected; output would look the same with or without the template, so the demo would not prove anything.
- The sample has no VBA project (we cannot author `vbaProject.bin` meaningfully). A test fixture adds a dummy `vbaProject.bin` part to verify the macro-stripping path (D2).

### D8. CLIs via `exec-maven-plugin`
- `StyleInspector <path>` → `ID  TYPE  NAME  [custom]`, custom first; exit code 1 on missing/invalid file.
- `SampleTemplateGenerator [outPath]`
- `Demo [templatePath] [outPath]` — defaults to the bundled sample.

## Risks / Trade-offs

- [Maven runs on JDK 27 locally] → `release=25` plus enforcer; README documents `JAVA_HOME=$(/usr/libexec/java_home -v 25)`.
- [docx4j auto-activates known styles when using helpers like `addStyledParagraphOfText`, which would hide an unknown-ID error] → `DocumentBuilder` builds `P`/`PStyle` directly via `ObjectFactory` after `StyleCatalog.require`, never through the auto-activating helpers.
- [docx4j 17.x API differences from widely documented 8.x/11.x examples] → use only core classes (`WordprocessingMLPackage`, `ObjectFactory`, `StyleDefinitionsPart`, `TraversalUtil`); verify against 17.2.0 at implementation time and adjust.
- [Real `.dotm` may use content controls with nested/rich content or styles defined via `latentStyles` only] → catalog lists only defined `w:style` entries; latent built-ins (e.g. `Heading5` never used) are reported as unknown with a hint to use the style once in Word so it gets defined. Auto-activating from docx4j's `KnownStyles.xml` at generation time is deliberately not done: it would silently bring in docx4j's formatting instead of the house style. Rich controls: fill replaces text only.
- [docx4j startup (JAXB context) takes ~1–2 s] → acceptable for a demo; `DotmTemplate` is reusable so a service pays it once.
- [Removing template body may drop intended boilerplate text] → put boilerplate that must survive inside content controls (kept by default); full boilerplate retention is a later option.

## Open Questions

- Where the real `.dotm` will live (classpath vs. external path) — both are supported, so this can be decided when integrating it.
