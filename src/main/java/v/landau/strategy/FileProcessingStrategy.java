package v.landau.strategy;


import v.landau.config.HarvesterConfig;
import v.landau.model.FileOperation;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy interface for processing files during harvesting.
 */
public interface FileProcessingStrategy {
    /**
     * Process a single file according to the strategy.
     *
     * @param sourcePath the source file path
     * @param targetPath the target file path
     * @param config the harvester configuration
     * @return FileOperation describing the result
     * @throws IOException if processing fails
     */
    FileOperation processFile(Path sourcePath, Path targetPath, HarvesterConfig config) throws IOException;
}