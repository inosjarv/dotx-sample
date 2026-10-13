# template-style-ids Specification

## Purpose

Makes a template's style IDs and content control tags easy to discover and safe to use from code, so documents reference template styles by ID and errors surface at generation time instead of as silent fallback to Normal formatting in Word.

## Requirements

### Requirement: List template styles
The system SHALL expose the full list of styles defined in a loaded template, each with its style ID, display name, type (paragraph, character, table, numbering), and based-on style ID if any.

#### Scenario: Catalog of the sample template
- **WHEN** the style list of the sample template is requested
- **THEN** it includes `Heading1` with display name "heading 1" and type paragraph
- **AND** it includes `Emphasis` with type character
- **AND** it includes `CalloutNote` flagged as custom

### Requirement: Inspect styles from the command line
The system SHALL provide a command that, given a path to a `.dotm`, `.dotx`, or `.docx`, prints one line per style showing style ID, type, and display name, with custom styles distinguishable from built-in ones.

#### Scenario: Inspect a template
- **WHEN** the inspector command is run against the sample template
- **THEN** output lists `Heading1` with type paragraph and name "heading 1", not marked custom
- **AND** lists `CalloutNote` with type paragraph and name "Callout Note", marked custom

#### Scenario: Inspect missing file
- **WHEN** the inspector is run with a path that does not exist
- **THEN** it prints an error naming the path and exits with a non-zero status

### Requirement: Resolve style ID by display name
The system SHALL return the style ID for a given display name, matching case-insensitively, because display names shown in Word differ from the IDs stored in the file.

#### Scenario: Display name lookup
- **WHEN** the ID for display name "Heading 1" is requested from the sample template
- **THEN** the result is `Heading1`

#### Scenario: Display name with different spelling from ID
- **WHEN** the ID for display name "Callout Note" is requested from the sample template
- **THEN** the result is `CalloutNote`

#### Scenario: Unknown display name
- **WHEN** the ID for a display name not defined in the template is requested
- **THEN** the lookup reports that no such style exists

### Requirement: Reject unknown or mismatched style IDs
When content is added with a style ID, the system SHALL verify the ID exists in the template and is of the matching type. It MUST fail with an error naming the missing ID and suggesting close matches when the ID is unknown, and MUST fail when the type does not match (for example, a character style used as a paragraph style).

#### Scenario: Unknown style ID
- **WHEN** a paragraph is added with style ID `Headng1`
- **THEN** generation fails with an error that names `Headng1` and suggests `Heading1`

#### Scenario: Display name used instead of ID
- **WHEN** a paragraph is added with style ID `heading 1`
- **THEN** generation fails with an error stating that "heading 1" is a display name and the style ID is `Heading1`

#### Scenario: Wrong style type
- **WHEN** a paragraph is added with style ID `Emphasis`
- **THEN** generation fails with an error stating `Emphasis` is a character style, not a paragraph style

### Requirement: Typed constants for sample style IDs
The system SHALL provide compile-time constants for the sample template's style IDs, and a check that confirms every constant exists in a given template.

#### Scenario: Constants match template
- **WHEN** the constants check runs against the sample template
- **THEN** every constant is found with the expected type

#### Scenario: Constant missing from template
- **WHEN** the constants check runs against a template lacking `CalloutNote`
- **THEN** the check fails and names `CalloutNote`

### Requirement: Fill content controls by tag
The system SHALL replace the text of a content control identified by its tag, keeping the control's formatting and style, in both the document body and headers/footers. It MUST fail with an error naming the tag when no control has that tag.

#### Scenario: Fill body control
- **WHEN** the control tagged `customerName` is filled with "Acme Ltd"
- **THEN** the saved document shows "Acme Ltd" in place of the placeholder

#### Scenario: Fill header control
- **WHEN** the control tagged `documentDate` is filled with "2026-09-23"
- **THEN** the saved document's header shows "2026-09-23"

#### Scenario: Unknown tag
- **WHEN** a control tagged `invoiceNumber` is filled but no such control exists
- **THEN** generation fails with an error naming `invoiceNumber`
