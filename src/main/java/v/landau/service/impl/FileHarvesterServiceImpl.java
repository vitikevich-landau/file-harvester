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
import java.util.ArrayList;
import java.util.List;

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

        // Collect all files to process
        List<Path> filesToProcess = collectFiles(config);

        // Start progress tracking
        progressListener.onStart(filesToProcess.size());

        // Process files
        HarvestResult.Builder resultBuilder = HarvestResult.builder();

        for (Path sourceFile : filesToProcess) {
            processFile(sourceFile, config, resultBuilder);
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

    private List<Path> collectFiles(HarvesterConfig config) throws HarvesterException {
        List<Path> files = new ArrayList<>();

        try {
            Files.walkFileTree(config.getSourceDirectory(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile() && config.getFileFilterStrategy().accept(file)) {
                        files.add(file);
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

        return files;
    }

    private void processFile(Path sourceFile, HarvesterConfig config, HarvestResult.Builder resultBuilder) {
        resultBuilder.incrementProcessed();

        // Generate target file path (flatten structure)
        String fileName = generateUniqueFileName(sourceFile, config.getTargetDirectory(), config.isOverwriteExisting());
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

    private String generateUniqueFileName(Path sourceFile, Path targetDir, boolean overwriteExisting) {
        String originalName = sourceFile.getFileName().toString();

        if (overwriteExisting) {
            return originalName;
        }

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
