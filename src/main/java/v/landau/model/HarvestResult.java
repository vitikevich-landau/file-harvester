package v.landau.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of file harvesting operation.
 * Contains statistics and details about processed files.
 */
public class HarvestResult {
    private final int totalFilesProcessed;
    private final int filesCopied;
    private final int filesSkipped;
    private final int filesFailed;
    private final long totalBytesCopied;
    private final long elapsedTime;
    private final List<FileOperation> operations;
    private final List<String> errors;

    private HarvestResult(Builder builder) {
        this.totalFilesProcessed = builder.totalFilesProcessed;
        this.filesCopied = builder.filesCopied;
        this.filesSkipped = builder.filesSkipped;
        this.filesFailed = builder.filesFailed;
        this.totalBytesCopied = builder.totalBytesCopied;
        this.elapsedTime = builder.elapsedTime;
        this.operations = Collections.unmodifiableList(new ArrayList<>(builder.operations));
        this.errors = Collections.unmodifiableList(new ArrayList<>(builder.errors));
    }

    // Getters
    public int getTotalFilesProcessed() { return totalFilesProcessed; }
    public int getFilesCopied() { return filesCopied; }
    public int getFilesSkipped() { return filesSkipped; }
    public int getFilesFailed() { return filesFailed; }
    public long getTotalBytesCopied() { return totalBytesCopied; }
    public long getElapsedTime() { return elapsedTime; }
    public List<FileOperation> getOperations() { return operations; }
    public List<String> getErrors() { return errors; }

    public boolean hasErrors() {
        return !errors.isEmpty() || filesFailed > 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for HarvestResult
     */
    public static class Builder {
        private int totalFilesProcessed = 0;
        private int filesCopied = 0;
        private int filesSkipped = 0;
        private int filesFailed = 0;
        private long totalBytesCopied = 0;
        private long elapsedTime = 0;
        private final List<FileOperation> operations = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public Builder incrementProcessed() {
            this.totalFilesProcessed++;
            return this;
        }

        public Builder incrementCopied() {
            this.filesCopied++;
            return this;
        }

        public Builder incrementSkipped() {
            this.filesSkipped++;
            return this;
        }

        public Builder incrementFailed() {
            this.filesFailed++;
            return this;
        }

        public Builder addBytesCopied(long bytes) {
            this.totalBytesCopied += bytes;
            return this;
        }

        public Builder elapsedTime(long millis) {
            this.elapsedTime = millis;
            return this;
        }

        public Builder addOperation(FileOperation operation) {
            this.operations.add(operation);
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            return this;
        }

        public HarvestResult build() {
            return new HarvestResult(this);
        }
    }
}