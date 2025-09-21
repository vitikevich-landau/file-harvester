package v.landau;

import v.landau.config.HarvesterConfig;
import v.landau.service.FileHarvesterService;
import v.landau.service.impl.FileHarvesterServiceImpl;
import v.landau.strategy.impl.DefaultFileFilterStrategy;
import v.landau.strategy.impl.FlexibleExtensionFilterStrategy;
import v.landau.util.ConsoleLogger;
import v.landau.util.DirectoryTreePrinter;

import java.nio.file.Paths;
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
            // Get source directory
            System.out.print("Enter source directory path: ");
            String sourcePath = scanner.nextLine().trim();

            // Get target directory
            System.out.print("Enter target directory path: ");
            String targetPath = scanner.nextLine().trim();

            // Optional: ask for file extensions filter
            System.out.print("Enter rules (e.g., +jpg,+png,-tmp,-bak; press Enter for no filter): ");
            String extensionsInput = scanner.nextLine().trim();

            // Parse extensions for filter
            String[] extensions = null;
            if (!extensionsInput.isEmpty()) {
                extensions = extensionsInput.split(",");
                for (int i = 0; i < extensions.length; i++) {
                    extensions[i] = extensions[i].trim().toLowerCase();
                    if (!extensions[i].startsWith(".")) {
                        extensions[i] = "." + extensions[i];
                    }
                }
            }

            // Build configuration
            HarvesterConfig.Builder configBuilder = HarvesterConfig.builder()
                    .sourceDirectory(Paths.get(sourcePath))
                    .targetDirectory(Paths.get(targetPath))
                    .createTargetIfNotExists(true)
                    .overwriteExisting(askYesNo(scanner, "Overwrite existing files? (y/n): "))
                    .preserveFileAttributes(askYesNo(scanner, "Preserve file attributes? (y/n): "));

            // Add file filter if extensions specified
            if (extensions != null) {
                configBuilder.fileFilterStrategy(new FlexibleExtensionFilterStrategy(extensions));
            }

            HarvesterConfig config = configBuilder.build();

            // Show source directory structure before processing
            boolean showTree = askYesNo(scanner, "Show directory structure? (y/n): ");
            if (showTree) {
                DirectoryTreePrinter.Builder treePrinterBuilder = DirectoryTreePrinter.builder()
                        .showSize(true)
                        .showHidden(false);
//                        .maxDepth(5);  // Limit depth for large structures

                if (extensions != null) {
                    treePrinterBuilder.filterExtensions(extensions);
                }

                DirectoryTreePrinter treePrinter = treePrinterBuilder.build();
                treePrinter.printTree(config.getSourceDirectory(), "Source Directory Structure");
            }

            // Execute harvesting
            logger.info("\nStarting file harvesting process...");
            logger.info("Source: " + config.getSourceDirectory());
            logger.info("Target: " + config.getTargetDirectory());

            FileHarvesterService harvester = new FileHarvesterServiceImpl();
            var result = harvester.harvest(config);

            // Display results
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

            // Show target directory structure after processing
            if (showTree && result.getFilesCopied() > 0) {
                System.out.print("\nShow target directory structure? (y/n): ");
                if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                    DirectoryTreePrinter.Builder targetTreeBuilder = DirectoryTreePrinter.builder()
                            .showSize(true)
                            .showHidden(false);
//                            .maxDepth(2);  // Shallow depth since files are flattened

                    if (extensions != null) {
                        targetTreeBuilder.filterExtensions(extensions);
                    }

                    DirectoryTreePrinter targetTreePrinter = targetTreeBuilder.build();
                    targetTreePrinter.printTree(config.getTargetDirectory(), "Target Directory Structure (After Harvest)");
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