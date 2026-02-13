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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHarvesterServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void overwriteModeUsesOriginalNameAndReplacesExistingFile() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target"));

        Files.writeString(sourceDir.resolve("report.txt"), "new-content");
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

        Files.writeString(sourceDir.resolve("report.txt"), "new-content");
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
    void shouldFailWhenSourceAndTargetAreSame() throws IOException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("same"));

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(sourceDir)
                .build();

        HarvesterException ex = assertThrows(HarvesterException.class,
                () -> new FileHarvesterServiceImpl().harvest(config));

        assertTrue(ex.getMessage().contains("cannot be the same"));
    }

    @Test
    void shouldFailWhenTargetIsInsideSource() throws IOException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-inside"));
        Path targetDir = sourceDir.resolve("nested-target");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .build();

        HarvesterException ex = assertThrows(HarvesterException.class,
                () -> new FileHarvesterServiceImpl().harvest(config));

        assertTrue(ex.getMessage().contains("inside source"));
    }

    @Test
    void shouldCreateTargetWhenTargetDoesNotExistButParentIsValid() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-valid-parent"));
        Files.writeString(sourceDir.resolve("a.txt"), "ok");

        Path validParent = Files.createDirectory(tempDir.resolve("valid-parent"));
        Path targetDir = validParent.resolve("new-target");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .createTargetIfNotExists(true)
                .build();

        new FileHarvesterServiceImpl().harvest(config);

        assertTrue(Files.isDirectory(targetDir));
        assertTrue(Files.exists(targetDir.resolve("a.txt")));
    }

    @Test
    void shouldFailWhenTargetDoesNotExistAndParentIsNotDirectory() throws IOException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("src-invalid-parent"));
        Files.writeString(sourceDir.resolve("a.txt"), "ok");

        Path invalidParent = tempDir.resolve("parent-file");
        Files.writeString(invalidParent, "not-a-directory");
        Path targetDir = invalidParent.resolve("child");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .createTargetIfNotExists(true)
                .build();

        HarvesterException ex = assertThrows(HarvesterException.class,
                () -> new FileHarvesterServiceImpl().harvest(config));

        assertTrue(ex.getMessage().contains("Failed to create target directory"));
    }

    @Test
    void flattenModeShouldResolveNameCollisionsAcrossDifferentSubfolders() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-collision"));
        Path first = Files.createDirectories(sourceDir.resolve("a/b"));
        Path second = Files.createDirectories(sourceDir.resolve("x/y"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target-collision"));

        Files.writeString(first.resolve("report.txt"), "from-a-b");
        Files.writeString(second.resolve("report.txt"), "from-x-y");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .overwriteExisting(false)
                .build();

        HarvestResult result = new FileHarvesterServiceImpl().harvest(config);

        assertEquals(2, result.getFilesCopied());
        assertEquals(2, result.getTotalFilesProcessed());
        assertTrue(Files.exists(targetDir.resolve("report.txt")));
        assertTrue(Files.exists(targetDir.resolve("report_1.txt")));
    }

    @Test
    void shouldNotifyListenerWhenProcessingStrategyThrowsIOException() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source-errors"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target-errors"));
        Path sourceFile = sourceDir.resolve("problem.txt");
        Files.writeString(sourceFile, "boom");

        AtomicInteger onErrorCalls = new AtomicInteger();
        ProgressListener listener = new ProgressListener() {
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
                if (path.equals(sourceFile) && error.contains("forced I/O failure")) {
                    onErrorCalls.incrementAndGet();
                }
            }
        };

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .processingStrategy((src, tgt, cfg) -> {
                    throw new IOException("forced I/O failure");
                })
                .build();

        HarvestResult result = new FileHarvesterServiceImpl(listener).harvest(config);

        assertEquals(1, result.getFilesFailed());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, onErrorCalls.get());
    }
}
