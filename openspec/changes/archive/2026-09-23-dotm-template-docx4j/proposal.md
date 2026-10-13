## Why

We have a Word macro-enabled template (`.dotm`) that holds our house styles, and we want generated documents to use those styles instead of re-creating formatting in code. The project is empty, so this change sets up a small Java 25 + docx4j project that shows, end-to-end, how to load a `.dotm`, reference its styles by their IDs, and produce a `.docx` — with a sample `.dotm` so the approach can be tried before plugging in the real template.

## What Changes

- New Maven project on Java 25 using the latest docx4j (17.2.0, JAXB Reference Implementation) and JUnit 6.
- A **sample template generator** that creates `sample-template.dotm` from docx4j's own style definitions: the default styles docx4j puts in every new package (`Title`, `Heading1`, `Emphasis`, `TableGrid`, …) plus a few Word built-ins activated from docx4j's known-styles catalog (`Quote`, `Strong`, `ListBullet`, …). A light house-style override on `Title` and `Heading1` makes the template's effect visible, one custom style (`CalloutNote`) shows how custom styles behave, and a header and content controls (placeholders identified by tag) complete it.
- A **template-backed document generator**: load any `.dotm`, start a new document that inherits its styles, theme, numbering, headers/footers, and page setup, add content, and save as a clean `.docx` (macros removed).
- **Style ID integration**: read the template's style catalog (style ID ↔ display name ↔ type), look up IDs by display name, fail fast when code references a style ID the template does not define, and expose the sample template's IDs as Java constants.
- **Content control filling**: set text in the template's content controls by their `tag`.
- A **style inspector** command that lists every style ID in a given `.dotm`, so the real template's IDs can be discovered in one step.
- A runnable demo and README showing the full flow in a few lines of code.

Assumption: "IDs in the dotm" primarily means Word style IDs (`w:styleId`, e.g. `Heading1`, which differ from display names like "heading 1"). Content control tags are also covered, since they are the other common kind of ID used to bind data into templates. Bookmarks and VBA macros are out of scope.

## Capabilities

### New Capabilities
- `sample-dotm-template`: Producing a small, valid `.dotm` built from docx4j's default and known built-in styles, with a visible house-style override, one custom style, and tagged content controls, for demos and tests.
- `dotm-document-generation`: Creating a `.docx` from a `.dotm` so that the output inherits the template's styles and layout and contains no macros.
- `template-style-ids`: Discovering, resolving, and validating the template's style IDs, and filling content controls by tag.

### Modified Capabilities
<!-- none: no existing specs -->

## Impact

- New code: Maven `pom.xml`, `src/main/java/...` (generator, template loader, style catalog, document builder, inspector, demo), `src/test/java/...`, `src/main/resources/templates/sample-template.dotm`, `README.md`.
- New dependencies: `org.docx4j:docx4j-JAXB-ReferenceImpl:17.2.0`, `org.slf4j:slf4j-simple:2.0.20`, `org.junit.jupiter:junit-jupiter:6.1.3` (test).
- Build requires JDK 25 (local Maven currently runs on JDK 27 by default; the build will enforce release 25).
