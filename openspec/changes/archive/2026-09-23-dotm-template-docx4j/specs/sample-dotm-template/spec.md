## Purpose

Provides a small, known-good macro-enabled Word template (`.dotm`) built from docx4j's default and built-in style definitions, with a visible house-style override, one custom style, and tagged content controls, so the template-driven generation flow can be demonstrated and tested without the real corporate template.

## ADDED Requirements

### Requirement: Generate a sample dotm template
The system SHALL provide a command that writes a sample template file to a caller-specified path. The file MUST be a Word macro-enabled template package (main part content type `application/vnd.ms-word.template.macroEnabledTemplate.main+xml`) that Microsoft Word and LibreOffice can open.

#### Scenario: Template file is created
- **WHEN** the generator command is run with an output path ending in `.dotm`
- **THEN** a file is written at that path
- **AND** its main document part declares the macro-enabled template content type

#### Scenario: Output directory does not exist
- **WHEN** the generator is given a path whose parent directory does not exist
- **THEN** the parent directories are created and the file is written

### Requirement: Sample template uses docx4j's built-in styles
The sample template SHALL contain the styles docx4j provides by default in a new package, and SHALL additionally define the listed Word built-in styles taken from docx4j's known-styles catalog. At minimum these styles MUST be defined:

| Style ID | Display name | Type | Origin |
|---|---|---|---|
| `Normal` | Normal | paragraph | docx4j default |
| `Title` | Title | paragraph | docx4j default |
| `Subtitle` | Subtitle | paragraph | docx4j default |
| `Heading1` | heading 1 | paragraph | docx4j default |
| `Heading2` | heading 2 | paragraph | docx4j default |
| `Heading3` | heading 3 | paragraph | docx4j default |
| `Header` | header | paragraph | docx4j default |
| `Caption` | caption | paragraph | docx4j default |
| `Emphasis` | Emphasis | character | docx4j default |
| `TableGrid` | Table Grid | table | docx4j default |
| `Quote` | Quote | paragraph | docx4j known styles |
| `IntenseQuote` | Intense Quote | paragraph | docx4j known styles |
| `ListBullet` | List Bullet | paragraph | docx4j known styles |
| `Footer` | footer | paragraph | docx4j known styles |
| `Strong` | Strong | character | docx4j known styles |

Built-in styles MUST keep Word's standard style IDs and display names so that documents generated from the template behave like ordinary Word documents.

#### Scenario: Built-in styles are present with IDs and names
- **WHEN** the generated sample template is inspected
- **THEN** each style in the table exists with the listed style ID, display name, and type

#### Scenario: Style ID differs from display name
- **WHEN** the style with ID `Heading1` is inspected
- **THEN** its display name is "heading 1"

### Requirement: Sample template overrides some built-in formatting
The sample template SHALL override the formatting of `Title` and `Heading1` (at least font and color) so that they differ from docx4j's shipped definitions, making the template's influence visible in generated documents. Other built-in styles MAY keep docx4j's formatting.

#### Scenario: Title differs from docx4j default
- **WHEN** the `Title` style of the sample template is compared with `Title` in a package newly created by docx4j
- **THEN** their font or color differs

#### Scenario: Heading1 differs from docx4j default
- **WHEN** the `Heading1` style of the sample template is compared with `Heading1` in a package newly created by docx4j
- **THEN** their font or color differs

### Requirement: Sample template defines one custom style
The sample template SHALL define a custom paragraph style with ID `CalloutNote`, display name "Callout Note", based on `Normal`, marked as custom, and with formatting (e.g. shading, indent, or color) visibly different from `Normal`.

#### Scenario: Custom style is present and marked custom
- **WHEN** the generated sample template is inspected
- **THEN** `CalloutNote` exists as a paragraph style named "Callout Note", based on `Normal`, and flagged as custom

### Requirement: Sample template contains tagged content controls
The sample template SHALL contain plain-text content controls identified by the tags `customerName` (in the body) and `documentDate` (in the header), each with placeholder text.

#### Scenario: Content controls are discoverable by tag
- **WHEN** the sample template is inspected for content controls
- **THEN** controls with tags `customerName` and `documentDate` are found
- **AND** each shows placeholder text
