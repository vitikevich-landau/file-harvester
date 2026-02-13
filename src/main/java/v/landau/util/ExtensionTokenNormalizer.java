package v.landau.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Normalizes user-provided extension tokens to keep filtering contracts consistent
 * between harvester logic and directory tree preview.
 */
public final class ExtensionTokenNormalizer {

    private ExtensionTokenNormalizer() {
    }

    public static NormalizedRules normalize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return normalize((String[]) null);
        }
        String[] parts = input.split(",");
        return normalize(parts);
    }

    public static NormalizedRules normalize(String... rawTokens) {
        List<String> normalizedTokens = new ArrayList<>();
        List<String> includeExtensions = new ArrayList<>();
        boolean hasExcludeRules = false;
        boolean hasNoExtRule = false;

        if (rawTokens != null) {
            for (String raw : rawTokens) {
                if (raw == null) {
                    continue;
                }

                String token = raw.trim().toLowerCase(Locale.ROOT);
                if (token.isEmpty()) {
                    continue;
                }

                int dotPos = 0;
                while (dotPos < token.length() && token.charAt(dotPos) == '.') {
                    dotPos++;
                }
                token = token.substring(dotPos);
                if (token.isEmpty()) {
                    continue;
                }

                char sign = '+';
                char first = token.charAt(0);
                if (first == '+' || first == '-') {
                    sign = first;
                    token = token.substring(1);
                    if (token.isEmpty()) {
                        continue;
                    }
                }

                if ("noext".equals(token)) {
                    hasNoExtRule = true;
                    normalizedTokens.add(sign + token);
                    continue;
                }

                if (sign == '-') {
                    hasExcludeRules = true;
                } else {
                    includeExtensions.add(token);
                }

                normalizedTokens.add(sign == '+' ? token : sign + token);
            }
        }

        return new NormalizedRules(normalizedTokens, includeExtensions, hasExcludeRules, hasNoExtRule);
    }

    public static final class NormalizedRules {
        private final List<String> normalizedTokens;
        private final List<String> includeExtensions;
        private final boolean hasExcludeRules;
        private final boolean hasNoExtRule;

        private NormalizedRules(List<String> normalizedTokens,
                                List<String> includeExtensions,
                                boolean hasExcludeRules,
                                boolean hasNoExtRule) {
            this.normalizedTokens = Collections.unmodifiableList(new ArrayList<>(normalizedTokens));
            this.includeExtensions = Collections.unmodifiableList(new ArrayList<>(includeExtensions));
            this.hasExcludeRules = hasExcludeRules;
            this.hasNoExtRule = hasNoExtRule;
        }

        public boolean hasAnyRules() {
            return !normalizedTokens.isEmpty();
        }

        public String[] toFilterTokensArray() {
            return normalizedTokens.toArray(new String[0]);
        }

        /**
         * Returns include extensions in the tree-preview contract (clean values without +/- or leading dot).
         *
         * @throws IllegalStateException if rules unsupported by tree preview are present.
         */
        public String[] toTreePreviewIncludesArray() {
            if (hasExcludeRules || hasNoExtRule) {
                throw new IllegalStateException(
                        "Tree preview supports only include extensions (e.g. jpg,png). " +
                                "Exclude rules (-ext) and noext are not supported in preview."
                );
            }
            return includeExtensions.toArray(new String[0]);
        }

        @Override
        public String toString() {
            return "NormalizedRules{" +
                    "normalizedTokens=" + Arrays.toString(toFilterTokensArray()) +
                    ", includeExtensions=" + includeExtensions +
                    ", hasExcludeRules=" + hasExcludeRules +
                    ", hasNoExtRule=" + hasNoExtRule +
                    '}';
        }
    }
}
