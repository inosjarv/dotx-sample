# dotm-document-generation Specification

## Purpose

Generates Word documents (`.docx`) from a `.dotm` template so that the output uses the template's styles, theme, numbering, headers/footers, and page setup, without carrying the template's macros.

## Requirements

### Requirement: Load a dotm template
The system SHALL load a `.dotm` template from a file path or classpath resource. Loading MUST fail with a descriptive error when the file is missing or is not a WordprocessingML package.

#### Scenario: Load from file path
- **WHEN** a valid `.dotm` path is supplied
- **THEN** the template is loaded and its styles are available

#### Scenario: Missing template
- **WHEN** a path that does not exist is supplied
- **THEN** loading fails with an error that names the path

#### Scenario: Not a Word package
- **WHEN** a file that is not a WordprocessingML package is supplied
- **THEN** loading fails with an error stating the file is not a valid Word template

### Requirement: New documents inherit template definitions
A document created from a loaded template SHALL contain the template's style definitions, theme, numbering definitions, headers, footers, and final section page setup. The template's original body content MUST NOT appear in the new document, except that body content controls SHALL be kept by default so they can be filled; the caller MUST be able to opt out and drop them.

#### Scenario: Styles carried over
- **WHEN** a document is created from the sample template and saved
- **THEN** the saved document contains every style defined in the template with the same style IDs

#### Scenario: Template formatting wins over library defaults
- **WHEN** a document is generated from the sample template with a `Heading1` paragraph and saved
- **THEN** the saved document's `Heading1` style has the template's overridden font and color, not docx4j's default `Heading1` formatting

#### Scenario: Header and page setup carried over
- **WHEN** a document is created from the sample template and saved
- **THEN** the saved document has the template's header and the same page size and margins

#### Scenario: Template body starts empty
- **WHEN** a document is created from a template and no content is added
- **THEN** the saved document body contains no paragraphs from the template other than its content controls

#### Scenario: Content controls dropped on request
- **WHEN** a document is created from the sample template with content controls dropped and no content is added
- **THEN** the saved document body contains no content from the template

### Requirement: Template loading is reusable
A loaded template SHALL be usable to create multiple independent documents; content added to one document MUST NOT appear in another or alter the loaded template.

#### Scenario: Two documents from one template
- **WHEN** two documents are created from the same loaded template and different text is added to each
- **THEN** each saved document contains only its own text

### Requirement: Output is a macro-free docx
Saving a generated document SHALL produce a `.docx` whose main part content type is `application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml` and which contains no VBA project part or VBA-related relationships.

#### Scenario: Template with macros
- **WHEN** a document is generated from a `.dotm` that contains a VBA project
- **THEN** the saved `.docx` contains no `vbaProject.bin` part
- **AND** the saved file opens in Word without a macro or file-type warning

#### Scenario: Correct content type
- **WHEN** a generated document is saved
- **THEN** its main part declares the standard document (not template) content type

### Requirement: Add styled content
The system SHALL let callers append paragraphs with a paragraph style ID, runs with a character style ID, and tables with a table style ID, using text supplied by the caller.

#### Scenario: Styled paragraph
- **WHEN** a caller adds a paragraph with style ID `Heading1` and text "Overview"
- **THEN** the saved document contains a paragraph with text "Overview" whose paragraph style is `Heading1`

#### Scenario: Styled run inside a paragraph
- **WHEN** a caller adds a `Normal` paragraph containing a run styled `Emphasis`
- **THEN** that run references character style `Emphasis`

#### Scenario: Styled table
- **WHEN** a caller adds a 2x3 table with style ID `TableGrid`
- **THEN** the saved document contains a table with 2 rows, 3 columns, and table style `TableGrid`
