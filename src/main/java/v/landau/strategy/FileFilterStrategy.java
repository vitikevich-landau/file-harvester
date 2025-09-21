package v.landau.strategy;

import java.nio.file.Path;

/**
 * Strategy interface for filtering files during harvesting.
 */
@FunctionalInterface
public interface FileFilterStrategy {
    /**
     * Determines if a file should be processed.
     *
     * @param filePath the path to the file
     * @return true if file should be processed, false otherwise
     */
    boolean accept(Path filePath);
}