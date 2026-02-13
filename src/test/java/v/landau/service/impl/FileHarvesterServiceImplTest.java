package v.landau.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import v.landau.config.HarvesterConfig;
import v.landau.exception.HarvesterException;
import v.landau.model.HarvestResult;
import v.landau.strategy.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHarvesterServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void overwriteModeUsesOriginalNameAndReplacesExistingFile() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target"));

        Path sourceFile = sourceDir.resolve("report.txt");
        Files.writeString(sourceFile, "new-content");
        Files.writeString(targetDir.resolve("report.txt"), "old-content");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .overwriteExisting(true)
                .build();

        new FileHarvesterServiceImpl().harvest(config);

        assertEquals("new-content", Files.readString(targetDir.resolve("report.txt")));
        assertTrue(Files.notExists(targetDir.resolve("report_1.txt")));
    }

    @Test
    void nonOverwriteModeGeneratesUniqueSuffixWhenFileExists() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source2"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target2"));

        Path sourceFile = sourceDir.resolve("report.txt");
        Files.writeString(sourceFile, "new-content");
        Files.writeString(targetDir.resolve("report.txt"), "old-content");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .overwriteExisting(false)
                .build();

        new FileHarvesterServiceImpl().harvest(config);

        assertEquals("old-content", Files.readString(targetDir.resolve("report.txt")));
        assertEquals("new-content", Files.readString(targetDir.resolve("report_1.txt")));
    }

    @Test
    void copyFailureUsesUnifiedErrorFormatAndNotifiesListener() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source3"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target3"));

        Path sourceFile = sourceDir.resolve("broken.txt");
        Files.writeString(sourceFile, "content");

        RecordingProgressListener listener = new RecordingProgressListener();

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .processingStrategy((source, target, cfg) -> {
                    throw new IOException("disk full");
                })
                .build();

        HarvestResult result = new FileHarvesterServiceImpl(listener).harvest(config);

        String expectedError = "Failed to process " + sourceFile + ": disk full";
        assertEquals(1, result.getFilesFailed());
        assertEquals(1, result.getErrors().size());
        assertEquals(expectedError, result.getErrors().get(0));
        assertEquals(expectedError, listener.lastError);
        assertEquals(sourceFile, listener.lastErrorPath);
    }

    private static class RecordingProgressListener implements ProgressListener {
        private String lastError;
        private Path lastErrorPath;

        @Override
        public void onFileProcessed(v.landau.model.FileOperation operation) {
        }

        @Override
        public void onStart(int totalFiles) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onError(String error, Path path) {
            this.lastError = error;
            this.lastErrorPath = path;
        }
    }
}
