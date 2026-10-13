package dev.docdotm;

import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.Style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The styles a template defines, keyed by style ID.
 *
 * <p>Word stores two identifiers per style: the style ID ({@code Heading1}) that paragraphs, runs and
 * tables reference, and the display name ({@code heading 1}) shown in Word's UI. Word silently falls
 * back to {@code Normal} when content references an ID that does not exist, so {@link #require} is
 * used to catch such mistakes at generation time.
 */
public final class StyleCatalog {

    private final Map<String, StyleInfo> byId;
    private final Map<String, String> idByLowerName;

    private StyleCatalog(Collection<StyleInfo> styles) {
        var ids = new LinkedHashMap<String, StyleInfo>();
        var names = new LinkedHashMap<String, String>();
        for (StyleInfo style : styles) {
            ids.put(style.id(), style);
            if (style.name() != null) {
                names.putIfAbsent(style.name().toLowerCase(Locale.ROOT), style.id());
            }
        }
        this.byId = Map.copyOf(ids);
        this.idByLowerName = Map.copyOf(names);
    }

    /** Reads the style definitions part of a loaded package. */
    public static StyleCatalog of(WordprocessingMLPackage pkg) {
        var part = pkg.getMainDocumentPart().getStyleDefinitionsPart();
        if (part == null) {
            return new StyleCatalog(List.of());
        }
        var styles = new ArrayList<StyleInfo>();
        for (Style s : part.getJaxbElement().getStyle()) {
            styles.add(new StyleInfo(
                    s.getStyleId(),
                    s.getName() == null ? null : s.getName().getVal(),
                    StyleType.fromXml(s.getType()),
                    s.getBasedOn() == null ? null : s.getBasedOn().getVal(),
                    Boolean.TRUE.equals(s.isCustomStyle())));
        }
        return new StyleCatalog(styles);
    }

    /** All styles, custom first, then by type and ID. */
    public List<StyleInfo> all() {
        return byId.values().stream()
                .sorted(Comparator.comparing((StyleInfo s) -> !s.custom())
                        .thenComparing(StyleInfo::type)
                        .thenComparing(StyleInfo::id))
                .toList();
    }

    public Optional<StyleInfo> find(String styleId) {
        return Optional.ofNullable(byId.get(styleId));
    }

    /** Resolves a display name as shown in Word (case-insensitive) to its style ID. */
    public Optional<String> idForName(String displayName) {
        return Optional.ofNullable(idByLowerName.get(displayName.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns the style with this ID, checking it has the expected type.
     *
     * @throws UnknownStyleException      if the template does not define the ID
     * @throws StyleTypeMismatchException if the style is of a different type
     */
    public StyleInfo require(String styleId, StyleType expected) {
        StyleInfo style = byId.get(styleId);
        if (style == null) {
            throw unknown(styleId);
        }
        if (style.type() != expected) {
            throw new StyleTypeMismatchException(style, expected);
        }
        return style;
    }

    private UnknownStyleException unknown(String styleId) {
        Optional<String> byName = idForName(styleId);
        if (byName.isPresent()) {
            return new UnknownStyleException(styleId,
                    "'%s' is a display name, not a style ID; the style ID is '%s'".formatted(styleId, byName.get()),
                    List.of(byName.get()));
        }
        List<String> suggestions = suggest(styleId);
        String hint = suggestions.isEmpty() ? "" : "; did you mean " + String.join(", ", suggestions.stream().map(s -> "'" + s + "'").toList()) + "?";
        return new UnknownStyleException(styleId,
                "Style ID '%s' is not defined in the template%s".formatted(styleId, hint), suggestions);
    }

    private List<String> suggest(String styleId) {
        String wanted = styleId.toLowerCase(Locale.ROOT);
        int maxDistance = Math.max(2, wanted.length() / 3);
        record Candidate(String id, int distance) {}
        var best = new LinkedHashMap<String, Integer>();
        for (StyleInfo s : byId.values()) {
            int d = levenshtein(wanted, s.id().toLowerCase(Locale.ROOT));
            if (s.name() != null) {
                d = Math.min(d, levenshtein(wanted, s.name().toLowerCase(Locale.ROOT)));
            }
            if (d <= maxDistance) {
                best.merge(s.id(), d, Math::min);
            }
        }
        return best.entrySet().stream()
                .map(e -> new Candidate(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt(Candidate::distance).thenComparing(Candidate::id))
                .limit(3)
                .map(Candidate::id)
                .toList();
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}
