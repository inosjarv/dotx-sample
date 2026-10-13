## 1. Hand-authored .dotx source and packaging

- [x] 1.1 Create `src/main/template/sample-dotx/` package skeleton (`[Content_Types].xml` with template main content type, `_rels/.rels`, `docProps/core.xml`, `docProps/app.xml`, `word/settings.xml`, `word/_rels/document.xml.rels`) and verify every file is well-formed with `xmllint --noout`
- [x] 1.2 Write `word/styles.xml`: docDefaults, `Normal`, `BodyText`, `Title`, `Heading1`, `Heading2`, `ShadedPanel`, `ShadedText`, `Hyperlink`, `FollowedHyperlink`, `DefaultParagraphFont`, `TableNormal`, `TableBlue`, `TableRed`, `TableTwoTone`, `TableHeaderRule` with the spec's spacing, shading, border, and hyperlink values; verify with `xmllint --noout`
- [x] 1.3 Write `word/document.xml`: one sample paragraph per style, one small table per table style, and the final `sectPr` with A4 size and margins top 1418 / bottom 1134 / left 2268 / right 1134; verify with `xmllint --noout`
- [x] 1.4 Add `maven-antrun-plugin` 3.2.0 `<zip>` in `generate-resources` producing `target/classes/templates/sample-template.dotx` (excluding `.DS_Store`); verify `mvn -q process-resources` creates it and `unzip -l` lists all parts
- [x] 1.5 Render the packaged `.dotx` with LibreOffice to PNG and verify visually: margins, spacing, shaded panel and text, three colored tables, and a header-rule table showing only the rule under the header

## 2. Table generation respects the template

- [x] 2.1 Replace `TABLE_WIDTH_TWIPS` with the text width from the final `sectPr` (page width − left − right margin; remainder to last column; `tblW`/`gridCol`/`tcW` in dxa; `tblLayout fixed`; 9360 fallback with warning); verify tests that `.dotx` tables are 8504 and `.dotm` tables are 9027, with grid and per-row cell sums equal to the width
- [x] 2.2 Add `tblLook` (firstRow on, banding off) to every generated table and `tblHeader` to its first row; verify a test asserts both on a `TableHeaderRule` table, and the existing `.dotm` tests still pass
- [x] 2.3 Render a generated `.dotx` document with LibreOffice and confirm table borders sit within the left/right margins; if the left border falls outside, apply the D3 `tblInd` adjustment and re-verify both the render and task 2.1's width tests
- [x] 2.4 Implement `ParagraphBuilder.link(text, url)` (URL check: absolute http/https/mailto; blank text rejected; `Hyperlink` style required; external hyperlink relationship + `w:hyperlink` with styled run); verify tests: "our website" → `https://example.com` round-trips with the correct relationship target and `TargetMode=External`, the URL is absent from the body text, `rStyle` is `Hyperlink`, `/about`, `javascript:alert(1)`, a malformed URL and blank text are rejected, a template without `Hyperlink` fails naming it, and a link works from the `.dotm`

## 3. Dotx style constants and template tests

- [x] 3.1 Extract the shared verify loop into package-private `StyleExpectations` and make `SampleStyles.verify` use it; verify existing `SampleStylesTest` passes unchanged
- [x] 3.2 Add `DotxStyles` (constants for every style ID in the `sample-dotx-template` spec + `EXPECTED` + `verify`); verify a test passes against the packaged `.dotx` and fails naming `TableHeaderRule` when that style is removed
- [x] 3.3 Add `SampleDotxTemplateTest` covering the spec: template content type and no VBA part, page size/margins, spacing values per style, shading fills, table border colors (including two-tone outer vs inner), header-rule borders none + firstRow bottom border + bold, hyperlink styles' colors and underline; verify `mvn -q test` passes
- [x] 3.4 Add generation tests from the `.dotx`: saved output has the document content type, all template styles, and the template's page size/margins; verify they pass

## 4. Demo and docs

- [x] 4.1 Implement `DotxDemo` writing `target/report-dotx.docx` (title, headings, `BodyText` with a `ShadedText` run, `ShadedPanel`, a sentence with a link "our website" → `https://example.com`, one table per table style, a `TableHeaderRule` table long enough to cross a page); verify it runs via `mvn -q exec:java -Dexec.mainClass=dev.docdotm.DotxDemo`
- [x] 4.2 Render `report-dotx.docx` with LibreOffice and verify: content within margins on every page, visible spacing, shaded panel and text, different border colors, header-rule table with only the header bottom border, the header row repeating on page 2, and the link showing "our website" in blue underline (not the URL)
- [x] 4.3 Update `README.md` with a `.dotx` section (where the XML source lives, how it is packaged, where to find the built `.dotx`, run `mvn process-resources` for IDE use, table width and header-row behavior, `link(text, url)` usage and accepted schemes); verify the commands run as written
- [x] 4.4 Run `mvn verify` on JDK 25 and confirm all tests pass
