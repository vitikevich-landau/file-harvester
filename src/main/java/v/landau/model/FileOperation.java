package v.landau.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Represents a single file operation during harvesting.
 */
public class FileOperation {
    private final Path sourcePath;
    private final Path targetPath;
    private final OperationType type;
    private final OperationStatus status;
    private final long fileSize;
    private final LocalDateTime timestamp;
    private final String message;

    public FileOperation(Path sourcePath, Path targetPath, OperationType type,
                         OperationStatus status, long fileSize, String message) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.type = type;
        this.status = status;
        this.fileSize = fileSize;
        this.timestamp = LocalDateTime.now();
        this.message = message;
    }

    // Getters
    public Path getSourcePath() { return sourcePath; }
    public Path getTargetPath() { return targetPath; }
    public OperationType getType() { return type; }
    public OperationStatus getStatus() { return status; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }

    /**
     * Type of file operation
     */
    public enum OperationType {
        COPY("Copied"),
        MOVE("Moved"),
        SKIP("Skipped"),
        ERROR("Error");

        private final String description;

        OperationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Status of operation
     */
    public enum OperationStatus {
        SUCCESS,
        SKIPPED,
        FAILED,
        ALREADY_EXISTS,
        FILTERED_OUT
    }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s): %s",
                type.getDescription(),
                sourcePath.getFileName(),
                targetPath != null ? targetPath.getFileName() : "N/A",
                status,
                message != null ? message : "");
    }
}