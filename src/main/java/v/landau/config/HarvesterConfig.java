package v.landau.config;

import v.landau.strategy.FileFilterStrategy;
import v.landau.strategy.FileProcessingStrategy;
import v.landau.strategy.impl.AcceptAllFilterStrategy;
import v.landau.strategy.impl.CopyFileStrategy;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Configuration for file harvesting operations.
 * Uses Builder pattern for flexible configuration.
 */
public class HarvesterConfig {
    private final Path sourceDirectory;
    private final Path targetDirectory;
    private final boolean createTargetIfNotExists;
    private final boolean overwriteExisting;
    private final boolean preserveFileAttributes;
    private final FileFilterStrategy fileFilterStrategy;
    private final FileProcessingStrategy processingStrategy;

    private HarvesterConfig(Builder builder) {
        this.sourceDirectory = Objects.requireNonNull(builder.sourceDirectory, "Source directory cannot be null");
        this.targetDirectory = Objects.requireNonNull(builder.targetDirectory, "Target directory cannot be null");
        this.createTargetIfNotExists = builder.createTargetIfNotExists;
        this.overwriteExisting = builder.overwriteExisting;
        this.preserveFileAttributes = builder.preserveFileAttributes;
        this.fileFilterStrategy = builder.fileFilterStrategy != null
                ? builder.fileFilterStrategy
                : new AcceptAllFilterStrategy();
        this.processingStrategy = builder.processingStrategy != null
                ? builder.processingStrategy
                : new CopyFileStrategy();
    }

    // Getters
    public Path getSourceDirectory() { return sourceDirectory; }
    public Path getTargetDirectory() { return targetDirectory; }
    public boolean isCreateTargetIfNotExists() { return createTargetIfNotExists; }
    public boolean isOverwriteExisting() { return overwriteExisting; }
    public boolean isPreserveFileAttributes() { return preserveFileAttributes; }
    public FileFilterStrategy getFileFilterStrategy() { return fileFilterStrategy; }
    public FileProcessingStrategy getProcessingStrategy() { return processingStrategy; }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for HarvesterConfig
     */
    public static class Builder {
        private Path sourceDirectory;
        private Path targetDirectory;
        private boolean createTargetIfNotExists = true;
        private boolean overwriteExisting = false;
        private boolean preserveFileAttributes = true;
        private FileFilterStrategy fileFilterStrategy;
        private FileProcessingStrategy processingStrategy;

        public Builder sourceDirectory(Path sourceDirectory) {
            this.sourceDirectory = sourceDirectory;
            return this;
        }

        public Builder targetDirectory(Path targetDirectory) {
            this.targetDirectory = targetDirectory;
            return this;
        }

        public Builder createTargetIfNotExists(boolean create) {
            this.createTargetIfNotExists = create;
            return this;
        }

        public Builder overwriteExisting(boolean overwrite) {
            this.overwriteExisting = overwrite;
            return this;
        }

        public Builder preserveFileAttributes(boolean preserve) {
            this.preserveFileAttributes = preserve;
            return this;
        }

        public Builder fileFilterStrategy(FileFilterStrategy strategy) {
            this.fileFilterStrategy = strategy;
            return this;
        }

        public Builder processingStrategy(FileProcessingStrategy strategy) {
            this.processingStrategy = strategy;
            return this;
        }

        public HarvesterConfig build() {
            return new HarvesterConfig(this);
        }
    }
}