package v.landau.strategy.impl;

import v.landau.model.FileOperation;
import v.landau.strategy.ProgressListener;
import v.landau.util.ConsoleLogger;

import java.nio.file.Path;
import java.util.OptionalInt;

/**
 * Progress listener that outputs to console.
 */
public class ConsoleProgressListener implements ProgressListener {
    private final ConsoleLogger logger = ConsoleLogger.getInstance();
    private int processedCount = 0;
    private OptionalInt totalFiles = OptionalInt.empty();

    @Override
    public void onFileProcessed(FileOperation operation) {
        processedCount++;

        switch (operation.getStatus()) {
            case SUCCESS:
                logger.success(String.format(progressPrefix() + " ✓ %s",
                        operation.getSourcePath().getFileName()));
                break;
            case SKIPPED:
            case ALREADY_EXISTS:
                logger.warn(String.format(progressPrefix() + " ⊘ %s - %s",
                        operation.getSourcePath().getFileName(), operation.getMessage()));
                break;
            case FAILED:
                logger.error(String.format(progressPrefix() + " ✗ %s - %s",
                        operation.getSourcePath().getFileName(), operation.getMessage()));
                break;
            case FILTERED_OUT:
                // Silent skip for filtered files
                processedCount--;
                break;
        }
    }

    @Override
    public void onStart(OptionalInt totalFiles) {
        this.totalFiles = totalFiles;
        this.processedCount = 0;
        if (totalFiles.isPresent()) {
            logger.info("Found " + totalFiles.getAsInt() + " files to process");
        } else {
            logger.info("Starting processing (exact file count is not precomputed)");
        }
    }

    @Override
    public void onComplete() {
        logger.info("Processing complete!");
    }

    @Override
    public void onError(String error, Path path) {
        logger.error("Error processing " + path + ": " + error);
    }

    private String progressPrefix() {
        if (totalFiles.isPresent()) {
            return String.format("[%d/%d]", processedCount, totalFiles.getAsInt());
        }
        return String.format("[processed %d]", processedCount);
    }
}
