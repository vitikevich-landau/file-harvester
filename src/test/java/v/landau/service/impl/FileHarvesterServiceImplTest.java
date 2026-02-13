package v.landau.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import v.landau.config.HarvesterConfig;
import v.landau.exception.HarvesterException;

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
    void collectOperationsDisabledKeepsStatisticsButNotOperations() throws IOException, HarvesterException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("source3"));
        Path targetDir = Files.createDirectory(tempDir.resolve("target3"));

        Files.writeString(sourceDir.resolve("a.txt"), "abc");
        Files.writeString(sourceDir.resolve("b.txt"), "12345");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(sourceDir)
                .targetDirectory(targetDir)
                .collectOperations(false)
                .build();

        var result = new FileHarvesterServiceImpl().harvest(config);

        assertTrue(result.getOperations().isEmpty());
        assertEquals(2, result.getTotalFilesProcessed());
        assertEquals(2, result.getFilesCopied());
        assertEquals(0, result.getFilesSkipped());
        assertEquals(0, result.getFilesFailed());
        assertEquals(8, result.getTotalBytesCopied());
    }

}
