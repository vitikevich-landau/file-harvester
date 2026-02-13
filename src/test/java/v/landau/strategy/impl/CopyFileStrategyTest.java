package v.landau.strategy.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import v.landau.config.HarvesterConfig;
import v.landau.model.FileOperation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopyFileStrategyTest {

    @TempDir
    Path tempDir;

    @Test
    void overwriteModeReplacesExistingFile() throws IOException {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        Files.writeString(source, "fresh-data");
        Files.writeString(target, "stale-data");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(tempDir)
                .targetDirectory(tempDir)
                .overwriteExisting(true)
                .build();

        FileOperation result = new CopyFileStrategy().processFile(source, target, config);

        assertEquals(FileOperation.OperationStatus.SUCCESS, result.getStatus());
        assertEquals("fresh-data", Files.readString(target));
    }

    @Test
    void nonOverwriteModeReturnsAlreadyExistsWithoutReplacingFile() throws IOException {
        Path source = tempDir.resolve("source2.txt");
        Path target = tempDir.resolve("target2.txt");
        Files.writeString(source, "fresh-data");
        Files.writeString(target, "stale-data");

        HarvesterConfig config = HarvesterConfig.builder()
                .sourceDirectory(tempDir)
                .targetDirectory(tempDir)
                .overwriteExisting(false)
                .build();

        FileOperation result = new CopyFileStrategy().processFile(source, target, config);

        assertEquals(FileOperation.OperationStatus.ALREADY_EXISTS, result.getStatus());
        assertEquals("stale-data", Files.readString(target));
    }
}
