package v.landau.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for file operations.
 */
public class FileUtils {

    private FileUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Calculate total size of all files in a directory (recursive).
     */
    public static long calculateDirectorySize(Path directory) throws IOException {
        AtomicLong size = new AtomicLong(0);

        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                size.addAndGet(attrs.size());
                return FileVisitResult.CONTINUE;
            }
        });

        return size.get();
    }

    /**
     * Count total files in a directory (recursive).
     */
    public static long countFiles(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile).count();
        }
    }

    /**
     * Safely create a directory if it doesn't exist.
     */
    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        } else if (!Files.isDirectory(directory)) {
            throw new IOException("Path exists but is not a directory: " + directory);
        }
    }

    /**
     * Check if path is safe (not a symbolic link that could escape boundaries).
     */
    public static boolean isSafePath(Path path) {
        try {
            Path realPath = path.toRealPath();
            return realPath.startsWith(path.getParent());
        } catch (IOException e) {
            return false;
        }
    }
}