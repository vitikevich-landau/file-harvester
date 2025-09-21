package v.landau.util;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * Console path prompting and validation helpers.
 *
 * Validates:
 * - Source: must exist, be a readable directory.
 * - Target: must NOT be the same as or inside source; if exists -> writable dir;
 *           if not exists -> parent must exist, be a writable dir.
 *
 * All user-facing messages are in English.
 */
public final class PathPrompter {

    private PathPrompter() {}

    /**
     * Prompts until a valid, existing, readable source directory is provided.
     */
    public static Path promptSourceDirectory(Scanner scanner, ConsoleLogger logger) {
        while (true) {
            System.out.print("Enter source directory path: ");
            String raw = scanner.nextLine().trim();

            if (raw.isEmpty()) {
                logger.error("Source path cannot be empty. Please try again.");
                continue;
            }

            final Path path;
            try {
                path = Paths.get(raw).toAbsolutePath().normalize();
            } catch (InvalidPathException ex) {
                logger.error("Invalid source path syntax. Please enter a valid directory path.");
                continue;
            }

            if (!Files.exists(path)) {
                logger.error("Source path does not exist: " + path);
                continue;
            }
            if (!Files.isDirectory(path)) {
                logger.error("Source path is not a directory: " + path);
                continue;
            }
            if (!Files.isReadable(path)) {
                logger.error("Source directory is not readable: " + path);
                continue;
            }

            return toRealPathIfPossible(path);
        }
    }

    /**
     * Prompts until a valid target directory path is provided.
     * Rules:
     * - Must not be the same as source.
     * - Must not be inside source (to avoid self-copy recursion).
     * - If exists: must be a directory and writable.
     * - If not exists: parent must exist and be writable (so we can create it).
     */
    public static Path promptTargetDirectory(Scanner scanner, Path sourceDir, ConsoleLogger logger) {
        final Path sourceReal = toRealPathIfPossible(sourceDir);

        while (true) {
            System.out.print("Enter target directory path: ");
            String raw = scanner.nextLine().trim();

            if (raw.isEmpty()) {
                logger.error("Target path cannot be empty. Please try again.");
                continue;
            }

            final Path targetPath;
            try {
                targetPath = Paths.get(raw).toAbsolutePath().normalize();
            } catch (InvalidPathException ex) {
                logger.error("Invalid target path syntax. Please enter a valid directory path.");
                continue;
            }

            if (isSameOrSubPath(targetPath, sourceReal)) {
                logger.error("Target directory must not be the same as, or inside, the source directory.");
                continue;
            }

            if (Files.exists(targetPath)) {
                if (!Files.isDirectory(targetPath)) {
                    logger.error("Target path exists but is not a directory: " + targetPath);
                    continue;
                }
                if (!Files.isWritable(targetPath)) {
                    logger.error("Target directory is not writable: " + targetPath);
                    continue;
                }
                return toRealPathIfPossible(targetPath);
            } else {
                Path parent = targetPath.getParent();
                if (parent == null) {
                    logger.error("Target path has no parent directory. Please provide a resolvable path.");
                    continue;
                }
                Path parentReal = toRealPathIfPossible(parent);
                if (!Files.exists(parentReal)) {
                    logger.error("Parent directory does not exist: " + parentReal);
                    continue;
                }
                if (!Files.isDirectory(parentReal)) {
                    logger.error("Parent path is not a directory: " + parentReal);
                    continue;
                }
                if (!Files.isWritable(parentReal)) {
                    logger.error("Parent directory is not writable: " + parentReal);
                    continue;
                }

                return targetPath; // creation will be handled later by the app
            }
        }
    }

    /**
     * Returns real path if resolvable (follows symlinks); otherwise normalized absolute path.
     */
    private static Path toRealPathIfPossible(Path p) {
        try {
            return p.toRealPath();
        } catch (Exception ignored) {
            return p.toAbsolutePath().normalize();
        }
    }

    /**
     * Checks whether candidate is the same as base or located inside base.
     * Uses normalized absolute paths.
     */
    private static boolean isSameOrSubPath(Path candidate, Path base) {
        Path candNorm = candidate.toAbsolutePath().normalize();
        Path baseNorm = base.toAbsolutePath().normalize();
        return candNorm.equals(baseNorm) || candNorm.startsWith(baseNorm);
    }
}
