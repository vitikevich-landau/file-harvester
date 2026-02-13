package v.landau.strategy.impl;

import v.landau.strategy.FileFilterStrategy;
import v.landau.util.ExtensionTokenNormalizer;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Flexible extension-based file filter that works with pre-parsed extension tokens.
 *
 * Designed to be a drop-in replacement for strategies that accept String... extensions,
 * where upstream code already split user input and (optionally) added a leading dot.
 *
 * Features:
 * - Include/Exclude rules via optional '+' (include) and '-' (exclude) prefixes.
 * - Accepts tokens with or without leading dots (".jpg" or "jpg"); this class strips them.
 * - Supports compound extensions like "tar.gz".
 * - Special token "noext" controls files without extension:
 *      +noext -> allow no-extension files
 *      -noext -> disallow no-extension files
 *
 * Behavior:
 * - If include set is EMPTY -> accept everything EXCEPT what matches the exclude set.
 * - If include set is NOT EMPTY -> accept ONLY what matches the include set, then remove excludes.
 */
public class FlexibleExtensionFilterStrategy implements FileFilterStrategy {

    private final Set<String> include; // normalized: lower-case, no leading dot
    private final Set<String> exclude; // normalized: lower-case, no leading dot
    private final boolean acceptFilesWithoutExtension;

    /**
     * Drop-in constructor: accepts extension tokens as provided by upstream code.
     *
     * Tokens may look like ".jpg", ".+png", ".-tmp", ".tar.gz", "jpg", "+png", "-tmp".
     * This constructor:
     *  - trims, lower-cases,
     *  - strips ALL leading dots,
     *  - detects '+' (include) and '-' (exclude) prefixes (if present),
     *  - treats bare tokens (no sign) as include.
     * Special token:
     *  - "noext" -> controls behavior for files without extension
     *      +noext -> allow, -noext -> disallow. Default is allow when not specified.
     */
    public FlexibleExtensionFilterStrategy(String... extensions) {
        Set<String> inc = new HashSet<>();
        Set<String> exc = new HashSet<>();
        Boolean noExtFlag = null; // null -> not specified (defaults to true)

        String[] normalizedTokens = ExtensionTokenNormalizer.normalize(extensions).toFilterTokensArray();
        for (String normalized : normalizedTokens) {
            boolean isInclude = true;
            String token = normalized;
            if (token.charAt(0) == '-') {
                isInclude = false;
                token = token.substring(1);
            }

            if ("noext".equals(token) || "+noext".equals(token)) {
                noExtFlag = isInclude;
                continue;
            }

            if (isInclude) {
                inc.add(token);
            } else {
                exc.add(token);
            }
        }

        this.include = Set.copyOf(inc);
        this.exclude = Set.copyOf(exc);
        this.acceptFilesWithoutExtension = noExtFlag == null || noExtFlag;
    }

    /**
     * Advanced constructor for manual wiring or tests.
     *
     * @param include normalized include set (no leading dots, lower-case). May be empty.
     * @param exclude normalized exclude set (no leading dots, lower-case). May be empty.
     * @param acceptFilesWithoutExtension whether files without extension should be accepted
     */
    public FlexibleExtensionFilterStrategy(Set<String> include,
                                           Set<String> exclude,
                                           boolean acceptFilesWithoutExtension) {
        this.include = include == null ? Set.of() : Set.copyOf(include);
        this.exclude = exclude == null ? Set.of() : Set.copyOf(exclude);
        this.acceptFilesWithoutExtension = acceptFilesWithoutExtension;
    }

    @Override
    public boolean accept(Path filePath) {
        String fileName = filePath.getFileName().toString();

        // ".gitignore" (single leading dot, no others) -> treat as no-extension
        if (fileName.startsWith(".") && fileName.indexOf('.', 1) < 0) {
            return acceptFilesWithoutExtension && allowByIncludeExclude("");
        }

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            // No dot at all, or trailing dot -> treat as no-extension
            return acceptFilesWithoutExtension && allowByIncludeExclude("");
        }

        // Compute simple and compound extensions
        String simpleExt = fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT); // e.g., "gz"
        int firstDot = fileName.indexOf('.');
        String compoundExt = (firstDot != -1 && firstDot < lastDot)
                ? fileName.substring(firstDot + 1).toLowerCase(Locale.ROOT)         // e.g., "tar.gz"
                : simpleExt;

        boolean inInclude = include.isEmpty() || include.contains(simpleExt) || include.contains(compoundExt);
        boolean inExclude = exclude.contains(simpleExt) || exclude.contains(compoundExt);

        if (!include.isEmpty()) {
            // Whitelist mode: must be in include and not in exclude
            return inInclude && !inExclude;
        } else {
            // Blacklist mode: accept everything except what is excluded
            return !inExclude;
        }
    }

    /**
     * Apply include/exclude semantics for the special "no extension" key ("").
     */
    private boolean allowByIncludeExclude(String extKey) {
        boolean inInc = include.isEmpty() || include.contains(extKey);
        boolean inExc = exclude.contains(extKey);
        if (!include.isEmpty()) return inInc && !inExc;
        return !inExc;
    }

    @Override
    public String toString() {
        return "FlexibleExtensionFilterStrategy{" +
                "include=" + sorted(include) +
                ", exclude=" + sorted(exclude) +
                ", acceptNoExt=" + acceptFilesWithoutExtension +
                '}';
    }

    private static List<String> sorted(Collection<String> c) {
        return c.stream().sorted().collect(Collectors.toList());
    }
}