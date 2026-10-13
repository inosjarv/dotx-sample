## Purpose

Provides a hand-authored Word template (`.dotx`), kept as readable OOXML source and packaged by the build, that defines page margins, paragraph spacing, shading, and table border styles. It demonstrates and tests that generated documents respect a designer-controlled template.

## ADDED Requirements

### Requirement: Template is built from readable source
The `.dotx` SHALL be produced by the build from OOXML XML source files kept in the repository, and SHALL be available on the classpath as `templates/sample-template.dotx`. No binary copy of the template SHALL be committed. The packaged file MUST be a Word template (main part content type `application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml`), MUST contain no VBA project, and MUST open in Microsoft Word and LibreOffice.

#### Scenario: Build packages the template
- **WHEN** the project is built
- **THEN** `templates/sample-template.dotx` is on the classpath
- **AND** its main part declares the template content type
- **AND** it contains no `vbaProject.bin`

#### Scenario: Source edit is picked up
- **WHEN** a style in the template's XML source is changed and the project is rebuilt
- **THEN** the packaged `.dotx` contains the changed style

### Requirement: Template defines page size and margins
The template SHALL use A4 page size (11906 × 16838 twips) with unequal margins: top 1418, bottom 1134, left 2268, right 1134 twips (2.5 cm, 2 cm, 4 cm, 2 cm). This gives a text width of 8504 twips.

#### Scenario: Page setup is present
- **WHEN** the template's final section properties are inspected
- **THEN** page size and all four margins have the values above

### Requirement: Template defines paragraph spacing
The template SHALL give these paragraph styles the following effective spacing before, spacing after, and line spacing. `Normal`'s values SHALL come from the document defaults rather than the `Normal` style itself, so that table styles can set compact spacing for text in table cells (a paragraph style's own spacing would override the table style's):

| Style ID | Before | After | Line spacing |
|---|---|---|---|
| `Normal` | 0 | 160 | 276 (auto, 1.15) |
| `BodyText` | 120 | 240 | 360 (auto, 1.5) |
| `Heading1` | 480 | 240 | inherited |
| `Heading2` | 360 | 120 | inherited |

Values are in twips (1/20 pt); for "auto" line spacing, 240 means single spacing.

#### Scenario: Spacing values are defined
- **WHEN** the styles `BodyText`, `Heading1`, and `Heading2` and the document defaults are inspected
- **THEN** each style has the before/after/line values listed
- **AND** the document defaults carry `Normal`'s values, and `Normal` defines no spacing of its own

#### Scenario: Table cells stay compact
- **WHEN** a table in one of this template's table styles is rendered
- **THEN** cell text has no 8 pt gap below it; rows are padded only by the table style's cell margins

### Requirement: Template defines shading styles
The template SHALL define a custom paragraph style `ShadedPanel` ("Shaded Panel") with a solid background fill `DEEAF6`, and a custom character style `ShadedText` ("Shaded Text") with a solid background fill `FFF2CC` for shading words within a sentence.

#### Scenario: Paragraph shading
- **WHEN** the `ShadedPanel` style is inspected
- **THEN** it is a paragraph style whose paragraph shading fill is `DEEAF6`

#### Scenario: Text shading
- **WHEN** the `ShadedText` style is inspected
- **THEN** it is a character style whose run shading fill is `FFF2CC`

### Requirement: Template defines table styles with different border colors
The template SHALL define these table styles:

| Style ID | Display name | Borders |
|---|---|---|
| `TableBlue` | Table Blue | all outer and inner borders single, color `2E75B6` |
| `TableRed` | Table Red | all outer and inner borders single, color `C00000` |
| `TableTwoTone` | Table Two Tone | outer borders color `1F3864` (thicker), inner borders color `BFBFBF` (thinner) |

#### Scenario: Border colors per style
- **WHEN** the table styles are inspected
- **THEN** `TableBlue` borders are `2E75B6`, `TableRed` borders are `C00000`
- **AND** `TableTwoTone` outer borders are `1F3864` and inner borders are `BFBFBF`

### Requirement: Template defines a header-rule table style
The template SHALL define a table style `TableHeaderRule` ("Table Header Rule") that has no table or cell borders, except a single bottom border (color `1F3864`) under the header row. Header row text SHALL be bold.

#### Scenario: Only a rule under the header
- **WHEN** a document with a `TableHeaderRule` table is rendered
- **THEN** the only visible line in the table is below the header row
- **AND** the header row text is bold

#### Scenario: Style definition
- **WHEN** the `TableHeaderRule` style is inspected
- **THEN** its table-level borders are all none
- **AND** its header-row conditional formatting defines a single bottom border with color `1F3864`

### Requirement: Template defines hyperlink styles
The template SHALL define the character styles `Hyperlink` ("Hyperlink"), color `0563C1` with single underline, and `FollowedHyperlink` ("FollowedHyperlink"), color `954F72` with single underline.

#### Scenario: Hyperlink styles are defined
- **WHEN** the styles `Hyperlink` and `FollowedHyperlink` are inspected
- **THEN** both are character styles with single underline
- **AND** their colors are `0563C1` and `954F72` respectively

### Requirement: Typed constants for the dotx style IDs
The system SHALL provide compile-time constants for every style ID named in this capability's requirements (including `Hyperlink` and `FollowedHyperlink`), with a check that confirms each exists in a given template with the expected type.

#### Scenario: Constants match template
- **WHEN** the constants check runs against the packaged `.dotx`
- **THEN** every constant is found with the expected type

#### Scenario: Constant missing
- **WHEN** the constants check runs against a template lacking `TableHeaderRule`
- **THEN** the check fails and names `TableHeaderRule`
