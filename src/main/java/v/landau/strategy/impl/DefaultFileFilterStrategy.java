package v.landau.strategy.impl;

import v.landau.strategy.FileFilterStrategy;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Filters files by extension.
 */
public class DefaultFileFilterStrategy implements FileFilterStrategy {
    private final Set<String> allowedExtensions;

    public DefaultFileFilterStrategy(String... extensions) {
        this.allowedExtensions = new HashSet<>(Arrays.asList(extensions));
    }

    @Override
    public boolean accept(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');

        if (lastDot == -1) {
            return allowedExtensions.contains("");
        }

        String extension = fileName.substring(lastDot).toLowerCase();
        return allowedExtensions.contains(extension);
    }
}