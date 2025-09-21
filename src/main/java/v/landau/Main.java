package v.landau;

import v.landau.config.HarvesterConfig;
import v.landau.service.FileHarvesterService;
import v.landau.service.impl.FileHarvesterServiceImpl;
import v.landau.strategy.impl.FlexibleExtensionFilterStrategy;
import v.landau.util.ConsoleLogger;
import v.landau.util.DirectoryTreePrinter;
import v.landau.util.PathPrompter;

import java.nio.file.Path;
import java.util.Scanner;

/**
 * Main entry point for FileHarvester application.
 * Recursively collects files from source directory and copies them to target directory.
 */
public class Main {
    private static final ConsoleLogger logger = ConsoleLogger.getInstance();

    public static void main(String[] args) {
        logger.info("=== FileHarvester v1.0 ===");
        logger.info("Recursive file collection and copying utility\n");

        try (Scanner scanner = new Scanner(System.in)) {
            // Prompt & validate paths
            Path sourceDir = PathPrompter.promptSourceDirectory(scanner, logger);
            Path targetDir = PathPrompter.promptTargetDirectory(scanner, sourceDir, logger);

            // Ask for extension rules (English)
            System.out.print("Enter rules (e.g., +jpg,+png,-tmp,-bak; press Enter for no filter): ");
            String extensionsInput = scanner.nextLine().trim();

            // Parse extensions for filter (kept compatible with existing logic)
            String[] extensions = null;
            if (!extensionsInput.isEmpty()) {
                String[] parts = extensionsInput.split(",");
                extensions = new String[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String t = parts[i].trim().toLowerCase();
                    extensions[i] = t.startsWith(".") ? t : "." + t;
                }
            }

            // Build configuration
            HarvesterConfig.Builder configBuilder = HarvesterConfig.builder()
                    .sourceDirectory(sourceDir)
                    .targetDirectory(targetDir)
                    .createTargetIfNotExists(true)
                    .overwriteExisting(askYesNo(scanner, "Overwrite existing files? (y/n): "))
                    .preserveFileAttributes(askYesNo(scanner, "Preserve file attributes? (y/n): "));

            if (extensions != null) {
                configBuilder.fileFilterStrategy(new FlexibleExtensionFilterStrategy(extensions));
            }

            HarvesterConfig config = configBuilder.build();

            // Optional tree preview
            boolean showTree = askYesNo(scanner, "Show directory structure? (y/n): ");
            if (showTree) {
                DirectoryTreePrinter.Builder treePrinterBuilder = DirectoryTreePrinter.builder()
                        .showSize(true)
                        .showHidden(false);
                if (extensions != null) {
                    treePrinterBuilder.filterExtensions(extensions);
                }
                DirectoryTreePrinter treePrinter = treePrinterBuilder.build();
                treePrinter.printTree(config.getSourceDirectory(), "Source Directory Structure");
            }

            // Run harvesting
            logger.info("\nStarting file harvesting process...");
            logger.info("Source: " + config.getSourceDirectory());
            logger.info("Target: " + config.getTargetDirectory());

            FileHarvesterService harvester = new FileHarvesterServiceImpl();
            var result = harvester.harvest(config);

            // Results
            logger.success("\n=== Harvesting Complete ===");
            logger.info("Files processed: " + result.getTotalFilesProcessed());
            logger.success("Files copied: " + result.getFilesCopied());
            if (result.getFilesSkipped() > 0) {
                logger.warn("Files skipped: " + result.getFilesSkipped());
            }
            if (result.getFilesFailed() > 0) {
                logger.error("Files failed: " + result.getFilesFailed());
                logger.error("Check the log for details about failed files");
            }
            logger.info("Total size copied: " + formatFileSize(result.getTotalBytesCopied()));
            logger.info("Time elapsed: " + result.getElapsedTime() + " ms");

            // Target tree after
            if (showTree && result.getFilesCopied() > 0) {
                System.out.print("\nShow target directory structure? (y/n): ");
                if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                    DirectoryTreePrinter.Builder targetTreeBuilder = DirectoryTreePrinter.builder()
                            .showSize(true)
                            .showHidden(false);
                    if (extensions != null) {
                        targetTreeBuilder.filterExtensions(extensions);
                    }
                    DirectoryTreePrinter targetTreePrinter = targetTreeBuilder.build();
                    targetTreePrinter.printTree(config.getTargetDirectory(),
                            "Target Directory Structure (After Harvest)");
                }
            }
        } catch (Exception e) {
            logger.error("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static boolean askYesNo(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String answer = scanner.nextLine().trim().toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }
}
