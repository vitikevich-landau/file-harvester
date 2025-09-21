package v.landau.strategy.impl;


import v.landau.config.HarvesterConfig;
import v.landau.model.FileOperation;
import v.landau.strategy.FileProcessingStrategy;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Strategy for copying files to target directory.
 */
public class CopyFileStrategy implements FileProcessingStrategy {

    @Override
    public FileOperation processFile(Path sourcePath, Path targetPath, HarvesterConfig config) throws IOException {
        // Check if target file already exists
        if (Files.exists(targetPath) && !config.isOverwriteExisting()) {
            return new FileOperation(
                    sourcePath,
                    targetPath,
                    FileOperation.OperationType.SKIP,
                    FileOperation.OperationStatus.ALREADY_EXISTS,
                    0,
                    "File already exists in target directory"
            );
        }

        try {
            // Ensure parent directory exists
            Files.createDirectories(targetPath.getParent());

            // Prepare copy options
            CopyOption[] options = buildCopyOptions(config);

            // Copy file
            Files.copy(sourcePath, targetPath, options);

            // Get file size
            long fileSize = Files.size(sourcePath);

            return new FileOperation(
                    sourcePath,
                    targetPath,
                    FileOperation.OperationType.COPY,
                    FileOperation.OperationStatus.SUCCESS,
                    fileSize,
                    "Successfully copied"
            );

        } catch (IOException e) {
            return new FileOperation(
                    sourcePath,
                    targetPath,
                    FileOperation.OperationType.ERROR,
                    FileOperation.OperationStatus.FAILED,
                    0,
                    "Failed to copy: " + e.getMessage()
            );
        }
    }

    private CopyOption[] buildCopyOptions(HarvesterConfig config) {
        int optionCount = 0;
        if (config.isOverwriteExisting()) optionCount++;
        if (config.isPreserveFileAttributes()) optionCount++;

        CopyOption[] options = new CopyOption[optionCount];
        int index = 0;

        if (config.isOverwriteExisting()) {
            options[index++] = StandardCopyOption.REPLACE_EXISTING;
        }
        if (config.isPreserveFileAttributes()) {
            options[index] = StandardCopyOption.COPY_ATTRIBUTES;
        }

        return options;
    }
}