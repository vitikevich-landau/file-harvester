package v.landau.strategy.impl;

import v.landau.model.FileOperation;
import v.landau.strategy.ProgressListener;
import v.landau.util.ConsoleLogger;

import java.nio.file.Path;

/**
 * Progress listener that outputs to console.
 */
public class ConsoleProgressListener implements ProgressListener {
    private final ConsoleLogger logger = ConsoleLogger.getInstance();
    private int processedCount = 0;
    private int totalFiles = 0;

    @Override
    public void onFileProcessed(FileOperation operation) {
        processedCount++;

        switch (operation.getStatus()) {
            case SUCCESS:
                logger.success(String.format("[%d/%d] ✓ %s",
                        processedCount, totalFiles, operation.getSourcePath().getFileName()));
                break;
            case SKIPPED:
            case ALREADY_EXISTS:
                logger.warn(String.format("[%d/%d] ⊘ %s - %s",
                        processedCount, totalFiles, operation.getSourcePath().getFileName(),
                        operation.getMessage()));
                break;
            case FAILED:
                logger.error(String.format("[%d/%d] ✗ %s - %s",
                        processedCount, totalFiles, operation.getSourcePath().getFileName(),
                        operation.getMessage()));
                break;
            case FILTERED_OUT:
                // Silent skip for filtered files
                processedCount--;
                break;
        }
    }

    @Override
    public void onStart(int totalFiles) {
        this.totalFiles = totalFiles;
        this.processedCount = 0;
        logger.info("Found " + totalFiles + " files to process");
    }

    @Override
    public void onComplete() {
        logger.info("Processing complete!");
    }

    @Override
    public void onError(String error, Path path) {
        logger.error("Error processing " + path + ": " + error);
    }
}