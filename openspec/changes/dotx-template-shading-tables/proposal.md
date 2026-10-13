## Why

The existing sample is a generated `.dotm`, but a typical house template is a plain `.dotx` a designer controls: page margins, paragraph spacing, shading and table looks. We want a hand-authored `.dotx` that shows these features and proves the generator respects them. In particular, tables must stay inside the page margins. Today the builder assumes a fixed 6.5" text width, which already overflows the existing A4 `.dotm` by about 0.23" and would overflow templates with wider margins even more.

## What Changes

- New **hand-authored `.dotx` template**: its OOXML parts (`styles.xml`, `document.xml`, …) live as readable source files in the repo. The Maven build zips them into `sample-template.dotx` on the classpath, so no binary is committed and every style change is a readable diff.
- The `.dotx` defines:
  - **Page setup**: A4 with deliberately unequal margins, so margin handling is visible.
  - **Spacing**: explicit before/after and line spacing on body text and headings.
  - **Shading**: a shaded paragraph style (panel) and a shaded character style (highlighted text within a sentence).
  - **Table styles with different border colors**: blue and red, plus a two-tone style (dark outer borders, light inner lines).
  - **A "header rule" table style**: no borders except a single bottom border under the header row.
- **Table generation respects the template**:
  - Table and column widths are computed from the document's page width minus its left/right margins, instead of a fixed width.
  - The first row is marked as the header row, so table styles with header-row formatting (like the header rule) apply. The header row also repeats on page breaks.
- **Hyperlinks**: the builder can add a link whose display text differs from its URL (e.g. "our website" → `https://example.com`). It is formatted with the template's `Hyperlink` character style. The `.dotx` defines `Hyperlink` and `FollowedHyperlink`; the `.dotm` already has `Hyperlink`. Today there is no link support: a URL can only be written as plain text.
- **`DotxStyles` constants** with `verify()`, and a **`DotxDemo`** that generates a report from the `.dotx` showing every feature.
- The `.dotm` sample, its generator and its tests stay as they are.

Assumptions: "shed" means shading (background fill). Tables fill the full text width between the margins. Header/footer and content controls are not part of the `.dotx`, since they weren't asked for.

## Capabilities

### New Capabilities
- `sample-dotx-template`: A hand-authored, build-packaged `.dotx` defining page margins, paragraph spacing, shading styles, table styles (colored borders, header-rule-only), and hyperlink styles, with typed style constants.

### Modified Capabilities
- `dotm-document-generation`: Adds requirements that macro-free `.dotx` templates are supported, tables fit within the document's page margins, the first table row is a header row that table styles can format, and hyperlinks can be added with display text separate from the URL.

## Impact

- New source tree `src/main/template/sample-dotx/` (OOXML XML parts) and a `maven-antrun-plugin` (3.2.0) zip step producing `templates/sample-template.dotx` in the build output.
- `DocumentBuilder.table(...)`: column widths now depend on the template's page setup, and generated tables gain header-row markup (`tblLook` firstRow, `tblHeader`). Tables generated from the `.dotm` keep their look, since `TableGrid` has no header-row formatting. Their width shrinks from 9360 to 9027 twips, fixing an existing overflow into the right margin.
- New `ParagraphBuilder.link(text, url)`, which adds an external hyperlink relationship to the main document part.
- New classes `DotxStyles` and `DotxDemo`, new tests, and a README section.
