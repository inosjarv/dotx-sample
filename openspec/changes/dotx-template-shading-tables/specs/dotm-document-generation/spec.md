## ADDED Requirements

### Requirement: Macro-free templates are supported
The system SHALL load `.dotx` templates the same way as `.dotm` templates. Documents generated from a `.dotx` SHALL inherit its styles and page setup and be saved as `.docx` with the standard document content type.

#### Scenario: Generate from a dotx
- **WHEN** a document is created from the sample `.dotx` and saved
- **THEN** the saved file's main part declares the document (not template) content type
- **AND** it contains every style defined in the `.dotx`

#### Scenario: Page margins carried over
- **WHEN** a document is created from the sample `.dotx` and saved
- **THEN** its page size and margins equal the template's

### Requirement: Tables fit within the page margins
Generated tables SHALL span the document's text width: page width minus left and right margins, taken from the template's final section properties. The table width, the sum of grid column widths, and the sum of each row's cell widths MUST each equal that text width, so no table extends into a margin.

#### Scenario: Table width follows template margins
- **WHEN** a 3-column table is added to a document from the sample `.dotx` (text width 8504 twips)
- **THEN** the table width and the sum of its grid columns are 8504 twips
- **AND** every row's cell widths sum to 8504 twips

#### Scenario: Different template, different width
- **WHEN** a table is added to a document from the sample `.dotm`
- **THEN** its width equals that template's page width minus its left and right margins (11907 − 1440 − 1440 = 9027 twips)

#### Scenario: Rendered table stays inside margins
- **WHEN** a generated document containing tables is rendered
- **THEN** no table border or cell content extends beyond the left or right page margin

### Requirement: First table row is a header row
The first row of a generated table SHALL be marked as the header row, so that header-row formatting defined by the table style applies. It SHALL repeat at the top of each page when the table spans pages.

#### Scenario: Header formatting applies
- **WHEN** a table with style `TableHeaderRule` is generated
- **THEN** the table enables first-row conditional formatting
- **AND** its first row is marked to repeat as a header row

#### Scenario: Styles without header formatting are unaffected
- **WHEN** a table with style `TableGrid` is generated from the `.dotm`
- **THEN** all its rows render with the same borders as before this change

### Requirement: Add hyperlinks
The system SHALL let callers add a hyperlink inside a paragraph with display text and a URL given separately. The display text SHALL be what the reader sees; the URL SHALL be stored as an external link target and MUST NOT appear in the text. The link text SHALL use the template's `Hyperlink` character style. Only absolute `http`, `https`, and `mailto` URLs SHALL be accepted.

#### Scenario: Display text differs from URL
- **WHEN** a paragraph is added containing a link with text "our website" and URL `https://example.com`
- **THEN** the saved document shows "our website" as a clickable link
- **AND** the link's external target is `https://example.com`
- **AND** the text `https://example.com` does not appear in the document body

#### Scenario: Link uses the template's style
- **WHEN** a link is added to a document from the sample `.dotx`
- **THEN** the link text references character style `Hyperlink`
- **AND** renders with that style's color and underline

#### Scenario: Invalid link input
- **WHEN** a link is added with a relative URL (`/about`), an unsupported scheme (`javascript:alert(1)`), a malformed URL, or blank display text
- **THEN** generation fails with an error naming the problem

#### Scenario: Template without a Hyperlink style
- **WHEN** a link is added to a document whose template does not define `Hyperlink`
- **THEN** generation fails with an unknown-style error naming `Hyperlink`
