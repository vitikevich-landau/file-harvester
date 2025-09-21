package v.landau.strategy;

import java.nio.file.Path;

/**
 * File filter strategy interface.
 * Implementations decide whether a given file path should be accepted.
 *
 * Provides default combinators to build complex filters.
 */
@FunctionalInterface
public interface FileFilterStrategy {
    /**
     * @param filePath file to check
     * @return true if the file should be accepted (processed), false otherwise
     */
    boolean accept(Path filePath);

    /**
     * Logical AND combinator.
     */
    default FileFilterStrategy and(FileFilterStrategy other) {
        return p -> this.accept(p) && other.accept(p);
    }

    /**
     * Logical OR combinator.
     */
    default FileFilterStrategy or(FileFilterStrategy other) {
        return p -> this.accept(p) || other.accept(p);
    }

    /**
     * Logical NOT combinator.
     */
    default FileFilterStrategy negate() {
        return p -> !this.accept(p);
    }
}