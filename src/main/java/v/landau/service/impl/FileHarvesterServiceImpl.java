package v.landau.service.impl;

import v.landau.config.HarvesterConfig;
import v.landau.exception.HarvesterException;
import v.landau.model.FileOperation;
import v.landau.model.HarvestResult;
import v.landau.service.FileHarvesterService;
import v.landau.strategy.ProgressListener;
import v.landau.strategy.impl.ConsoleProgressListener;
import v.landau.util.ConsoleLogger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

import static v.landau.model.FileOperation.OperationStatus.*;

/**
 * Implementation of FileHarvesterService.
 * Handles the recursive file collection and copying logic.
 */
public class FileHarvesterServiceImpl implements FileHarvesterService {
    private final ConsoleLogger logger = ConsoleLogger.getInstance();
    private final ProgressListener progressListener;

    public FileHarvesterServiceImpl() {
        this.progressListener = new ConsoleProgressListener();
    }

    public FileHarvesterServiceImpl(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public HarvestResult harvest(HarvesterConfig config) throws HarvesterException {
        long startTime = System.currentTimeMillis();

        // Validate configuration
        validateConfig(config);

        // Prepare target directory
        prepareTargetDirectory(config);

        // Count files first to provide accurate progress N/M.
        int totalFilesToProcess = countFilesToProcess(config);

        // Start progress tracking
        progressListener.onStart(totalFilesToProcess);

        // Process files while traversing the tree (streaming mode, no intermediate list)
        HarvestResult.Builder resultBuilder = HarvestResult.builder();
        try {
            Files.walkFileTree(config.getSourceDirectory(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && config.getFileFilterStrategy().accept(file)) {
                        processFile(file, config, resultBuilder);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Cannot access file: " + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new HarvesterException("Failed to traverse source directory: " + e.getMessage(), e);
        }

        // Complete progress tracking
        progressListener.onComplete();

        // Set elapsed time
        long elapsedTime = System.currentTimeMillis() - startTime;
        resultBuilder.elapsedTime(elapsedTime);

        return resultBuilder.build();
    }

    private void validateConfig(HarvesterConfig config) throws HarvesterException {
        Path source = config.getSourceDirectory();
        Path target = config.getTargetDirectory();

        if (!Files.exists(source)) {
            throw new HarvesterException("Source directory does not exist: " + source);
        }

        if (!Files.isDirectory(source)) {
            throw new HarvesterException("Source path is not a directory: " + source);
        }

        if (source.equals(target)) {
            throw new HarvesterException("Source and target directories cannot be the same");
        }

        if (target.startsWith(source)) {
            throw new HarvesterException("Target directory cannot be inside source directory");
        }
    }

    private void prepareTargetDirectory(HarvesterConfig config) throws HarvesterException {
        Path target = config.getTargetDirectory();

        if (!Files.exists(target)) {
            if (config.isCreateTargetIfNotExists()) {
                try {
                    Files.createDirectories(target);
                    logger.info("Created target directory: " + target);
                } catch (IOException e) {
                    throw new HarvesterException("Failed to create target directory: " + e.getMessage(), e);
                }
            } else {
                throw new HarvesterException("Target directory does not exist: " + target);
            }
        }

        if (!Files.isDirectory(target)) {
            throw new HarvesterException("Target path is not a directory: " + target);
        }
    }

    private int countFilesToProcess(HarvesterConfig config) throws HarvesterException {
        AtomicLong filesCount = new AtomicLong();

        try {
            Files.walkFileTree(config.getSourceDirectory(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && config.getFileFilterStrategy().accept(file)) {
                        filesCount.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Cannot access file: " + file + " (" + exc.getMessage() + ")");
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new HarvesterException("Failed to traverse source directory: " + e.getMessage(), e);
        }

        long total = filesCount.get();
        if (total > Integer.MAX_VALUE) {
            logger.warn("Too many files for exact progress value, capping at Integer.MAX_VALUE");
            return Integer.MAX_VALUE;
        }

        return (int) total;
    }

    private void processFile(Path sourceFile, HarvesterConfig config, HarvestResult.Builder resultBuilder) {
        resultBuilder.incrementProcessed();

        // Generate target file path (flatten structure)
        String fileName = generateUniqueFileName(sourceFile, config.getTargetDirectory());
        Path targetFile = config.getTargetDirectory().resolve(fileName);

        try {
            FileOperation operation = config.getProcessingStrategy()
                    .processFile(sourceFile, targetFile, config);

            resultBuilder.addOperation(operation);
            progressListener.onFileProcessed(operation);

            // Update statistics
            switch (operation.getStatus()) {
                case SUCCESS:
                    resultBuilder.incrementCopied();
                    resultBuilder.addBytesCopied(operation.getFileSize());
                    break;
                case SKIPPED:
                case ALREADY_EXISTS:
                case FILTERED_OUT:
                    resultBuilder.incrementSkipped();
                    break;
                case FAILED:
                    resultBuilder.incrementFailed();
                    resultBuilder.addError(operation.getMessage());
                    break;
            }

        } catch (IOException e) {
            resultBuilder.incrementFailed();
            String errorMsg = "Failed to process " + sourceFile + ": " + e.getMessage();
            resultBuilder.addError(errorMsg);
            progressListener.onError(e.getMessage(), sourceFile);
        }
    }

    private String generateUniqueFileName(Path sourceFile, Path targetDir) {
        String originalName = sourceFile.getFileName().toString();
        Path potentialTarget = targetDir.resolve(originalName);

        if (!Files.exists(potentialTarget)) {
            return originalName;
        }

        // Generate unique name by adding counter
        String nameWithoutExt = originalName;
        String extension = "";

        int lastDot = originalName.lastIndexOf('.');
        if (lastDot != -1) {
            nameWithoutExt = originalName.substring(0, lastDot);
            extension = originalName.substring(lastDot);
        }

        int counter = 1;
        String newName;
        do {
            newName = nameWithoutExt + "_" + counter + extension;
            potentialTarget = targetDir.resolve(newName);
            counter++;
        } while (Files.exists(potentialTarget));

        return newName;
    }
}
