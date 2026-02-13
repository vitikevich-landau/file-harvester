package v.landau.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import v.landau.config.HarvesterConfig;
import v.landau.exception.HarvesterException;
import v.landau.model.FileOperation;
import v.landau.strategy.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void harvestWithoutExactProgressStartsWithUnknownTotal() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source3"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target3"));
        Files.writeString(sourceDir.resolve("a.txt"), "A");
        Files.writeString(sourceDir.resolve("b.txt"), "B");

        RecordingProgressListener progressListener = new RecordingProgressListener();
        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .exactProgress(false)
                .build();

        new FileHarvesterServiceImpl(progressListener).harvest(config);

        assertFalse(progressListener.startTotal.isPresent());
        assertEquals(2, progressListener.processedCount);
        assertTrue(progressListener.completed);
    }

    @Test
    void harvestWithExactProgressStartsWithTotalCount() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source4"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target4"));
        Files.writeString(sourceDir.resolve("a.txt"), "A");
        Files.writeString(sourceDir.resolve("b.txt"), "B");

        RecordingProgressListener progressListener = new RecordingProgressListener();
        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .exactProgress(true)
                .build();

        new FileHarvesterServiceImpl(progressListener).harvest(config);

        assertTrue(progressListener.startTotal.isPresent());
        assertEquals(2, progressListener.startTotal.getAsInt());
        assertEquals(2, progressListener.processedCount);
    }

    private static class RecordingProgressListener implements ProgressListener {
        private OptionalInt startTotal = OptionalInt.empty();
        private int processedCount = 0;
        private boolean completed = false;

        @Override
        public void onFileProcessed(FileOperation operation) {
            processedCount++;
        }

        @Override
        public void onStart(OptionalInt totalFiles) {
            this.startTotal = totalFiles;
        }

        @Override
        public void onComplete() {
            this.completed = true;
        }

        @Override
        public void onError(String error, Path path) {
            // no-op for test
        }
    }
}
