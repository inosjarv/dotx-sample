package dev.docdotm;

/**
 * One style defined in a template.
 *
 * @param id      the {@code w:styleId} that document content references, e.g. {@code Heading1}
 * @param name    the display name Word shows, e.g. {@code heading 1}
 * @param type    paragraph, character, table or numbering
 * @param basedOn the parent style ID, or {@code null}
 * @param custom  {@code true} when the template marks it {@code w:customStyle}
 */
public record StyleInfo(String id, String name, StyleType type, String basedOn, boolean custom) {
}
