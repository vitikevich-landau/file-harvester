package v.landau.strategy.impl;


import v.landau.strategy.FileFilterStrategy;

import java.nio.file.Path;

/**
 * Accepts all files without filtering.
 */
public class AcceptAllFilterStrategy implements FileFilterStrategy {
    @Override
    public boolean accept(Path filePath) {
        return true;
    }
}