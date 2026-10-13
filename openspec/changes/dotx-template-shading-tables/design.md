## Context

`DotmTemplate` already loads any WordprocessingML package, and `DocumentBuilder.save` already switches template content types to the document type. So a `.dotx` works with no loader changes. See proposal.md for motivation and specs/ for the required values.

Two gaps in `DocumentBuilder.table`:
- It uses a hard-coded `TABLE_WIDTH_TWIPS = 9360` (US Letter with 1" margins). That is already wrong for the existing `.dotm` (A4, text width 9027), whose tables overflow the right margin by 333 twips. The sample `.dotx` has a text width of 8504, so its tables would overflow further.
- It writes no `w:tblLook`. Without `firstRow="1"`, Word ignores a table style's `firstRow` conditional formatting, so the header-rule style would render with no rule at all.

## Goals / Non-Goals

**Goals:**
- A `.dotx` whose every style is readable and reviewable as XML in the repo.
- Tables that are always exactly as wide as the text area of whichever template is used.

**Non-Goals:**
- Per-table border color overrides in code (colors live in template table styles).
- Multi-section documents with different margins per section: the final section's page setup is used.
- Header/footer, content controls, or macros in the `.dotx`.
- Column width control (columns stay equal).
- Hyperlinks inside table cells (`table(...)` takes plain strings).

## Decisions

### D1. Hand-written OOXML source, zipped by `maven-antrun-plugin`
```
src/main/template/sample-dotx/
  [Content_Types].xml            Default xml/rels + Overrides; document.xml → template.main+xml
  _rels/.rels                    officeDocument → word/document.xml
  docProps/core.xml, app.xml     minimal
  word/document.xml              sample body (one paragraph/table per style) + final sectPr
  word/styles.xml                docDefaults, latentStyles-free, all styles in the spec
  word/settings.xml              minimal (compat mode 15)
  word/_rels/document.xml.rels   styles, settings
```
`maven-antrun-plugin` 3.2.0 runs `<zip destfile="${project.build.outputDirectory}/templates/sample-template.dotx" basedir="src/main/template/sample-dotx" excludes="**/.DS_Store"/>` in `generate-resources`. The file lands on the classpath for tests, the demo and the jar. For opening it in Word, the path is `target/classes/templates/sample-template.dotx`.
- *Alternative:* a Java generator (like the `.dotm`). Rejected per user choice; XML source is what a designer's template really is, and diffs show exact style changes.
- *Alternative:* commit the binary. Rejected: unreviewable, and it drifts from any source.
- *Alternative:* a Java zip step via `exec-maven-plugin`. Rejected: needs compiled classes before `process-resources`; antrun's `<zip>` is one line.
- No theme part: fonts are named explicitly (`Calibri` body, `Georgia` headings), so there's no theme dependency and less XML.

### D2. Style definitions (values from the spec)
- `docDefaults`: Calibri 11 pt, `spacing before=0 after=160 line=276 lineRule=auto`. `Normal` has no spacing of its own. It inherits these, so a table style's `pPr` (applied after doc defaults, before paragraph styles) can make cell text compact. Found during implementation: with the spacing on `Normal`, every table row carried an 8 pt gap.
- `BodyText` (built-in ID, name "Body Text"): `before=120 after=240 line=360 auto`, basedOn Normal.
- `Title`, `Heading1` (`before=480 after=240`, `outlineLvl 0`), `Heading2` (`before=360 after=120`, `outlineLvl 1`), all Georgia `1F3864`, `keepNext`.
- `ShadedPanel` (custom paragraph): `w:shd w:val="clear" w:fill="DEEAF6"`, a small left/right indent, and `before/after=240`. The indent is inside the text area, so shading never reaches the margin.
- `ShadedText` (custom character): `w:shd w:val="clear" w:fill="FFF2CC"` in `rPr`.
- `Hyperlink` (character): `color 0563C1`, `u single`. `FollowedHyperlink` (character): `color 954F72`, `u single`. Both carry `w:semiHidden`/`w:unhideWhenUsed` as Word writes them.
- `DefaultParagraphFont`, `TableNormal` (with default cell margins 108 twips left/right), required by Word.
- Table styles all basedOn `TableNormal`, with `tblBorders` in `tblPr`:
  - `TableBlue` / `TableRed`: `top/left/bottom/right/insideH/insideV` single sz 8, colors `2E75B6` / `C00000`.
  - `TableTwoTone`: outer sz 12 `1F3864`, `insideH/insideV` sz 4 `BFBFBF`.
  - `TableHeaderRule`: all six `tblBorders` `none`; `<w:tblStylePr w:type="firstRow">` with `tcPr/tcBorders/bottom` single sz 12 `1F3864` and `rPr/b`.
- The table styles set `tblStyleRowBandSize`/`ColBandSize` to 1 but have no banding (kept simple).
- Table styles pad cells with `tblCellMar` (top/bottom 60, left/right 108) and set `pPr spacing 0/0 single`. Using paragraph spacing for the padding was rejected: LibreOffice drops space-before in the first row after a page break, which pushed text into the repeated header's rule.

### D3. Table width from the section's page setup
`DocumentBuilder` reads `body.getSectPr()`: `textWidth = pgSz.w − pgMar.left − pgMar.right`. `tblW` becomes `dxa textWidth` (was `pct 5000`). Columns are `textWidth / n`, with the remainder added to the last column so the sums are exact. Every `tcW` uses the same widths. If there's no `sectPr`/`pgSz`/`pgMar` (malformed template), it falls back to 9360 with a logged warning.
- `tblInd` stays 0. The `TableNormal` cell margins (108) mean the border sits 108 twips left of the text in Word's default layout. To keep borders inside the margin, set `tblInd w=0` explicitly and use `tblLayout fixed`, so widths are honoured exactly. LibreOffice rendering (task check) confirms borders stay within margins; if Word's cell-margin offset pushes the left border out, `tblInd` becomes the left cell margin (108) and the width is reduced by it. That's decided in the implementation task by rendering, and both options satisfy the spec.
- *Alternative:* keep `pct 5000`. Rejected: grid/tcW values would still be wrong, and LibreOffice/Word disagree on pct handling with fixed grids.

### D4. Header row markup
Every generated table gets `<w:tblLook w:firstRow="1" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="1" w:noVBand="1" w:val="0420"/>`, and its first `w:tr` gets `trPr/tblHeader`. Styles without `firstRow` formatting (e.g. `TableGrid`) are unaffected, as the spec requires. No new API; the README already documents the first row as the header.

### D5. `DotxStyles` and `DotxDemo`
- `DotxStyles`: same shape as `SampleStyles` (constants + `EXPECTED` + `verify`). To avoid duplication, extract the verify loop into a package-private `StyleExpectations.verify(Map<String, StyleType>, StyleCatalog, String label)` used by both.
- `DotxDemo` (main): loads `templates/sample-template.dotx`, verifies constants, writes `target/report-dotx.docx` with:
  - title and headings;
  - `BodyText` paragraphs with a `ShadedText` run;
  - a `ShadedPanel`;
  - a sentence with a link ("our website" → `https://example.com`);
  - one table each in `TableBlue`, `TableRed`, `TableTwoTone` and `TableHeaderRule`;
  - enough rows in one table to cross a page, showing the header repeating.

### D6. Hyperlinks via `ParagraphBuilder.link(text, url)`
```java
p.text("Visit ").link("our website", "https://example.com").text(" for details.")
```
1. Check the input: blank `text` → `IllegalArgumentException`. `URI.create(url)` must parse and be absolute with scheme `http`, `https` or `mailto` (case-insensitive); otherwise `IllegalArgumentException` naming the URL and the reason.
2. `styles.require("Hyperlink", CHARACTER)`, so a template without the style gets the existing `UnknownStyleException`.
3. Add a relationship on the main document part: type `Namespaces.HYPERLINK` (`…/relationships/hyperlink`), `Target=url`, `TargetMode="External"`, via `mdp.getRelationshipsPart().getRelationships().getRelationship().add(rel)` with an ID from `ObjectFactory`/`RelationshipsPart` (docx4j assigns `rIdN`).
4. Append `P.Hyperlink` (`w:hyperlink r:id=… w:history="1"`) containing one run with `rStyle Hyperlink` and the display text.

- `FollowedHyperlink` is never applied by the builder: Word switches to it itself after a click. It's defined so the template controls that color too.
- Links only in paragraphs built by the builder (body and table cells use the same `newRun` path, but `table(...)` takes plain strings, so no links in cells for now).
- *Alternative:* a `HYPERLINK` field code (`w:fldSimple`). Rejected: relationship-based `w:hyperlink` is what Word writes, renders without field updates, and docx4j reads it back directly.
- *Alternative:* accept any absolute URI. Rejected by user choice: `javascript:`/`file:` links in generated documents are a safety and portability risk.

## Risks / Trade-offs

- [IDE runs skip the antrun zip, so the `.dotx` is missing from the classpath] → `loadResource` error message already names the resource. The README says to run `mvn process-resources` (or any Maven build) once.
- [Hand-written XML typo produces an invalid package Word refuses to open] → a test loads the packaged `.dotx` through docx4j. Implementation renders it with LibreOffice. The README asks the user to open it in Word, since Word is the stricter reader and isn't available in this environment.
- [Changing table width alters existing `.dotm` output] → intended: the `.dotm` is A4 (11907) with 1" margins, so its text width is 9027. Today's fixed 9360 overflows its right margin by 333 twips (≈0.23"), and this change fixes that existing bug. A test pins 9027 for the `.dotm`.
- [Word vs LibreOffice cell-margin/indent differences] → see D3; verified by rendering.
