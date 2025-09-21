package v.landau.util;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for printing directory tree structure to console.
 * Provides ASCII-art style tree visualization.
 */
public class DirectoryTreePrinter {

    // Tree drawing characters
    private static final String BRANCH = "├── ";
    private static final String LAST_BRANCH = "└── ";
    private static final String VERTICAL = "│   ";
    private static final String SPACE = "    ";

    // Icons for different file types
    private static final String DIR_ICON = "📁 ";
    private static final String FILE_ICON = "📄 ";
    private static final String IMAGE_ICON = "🖼️  ";
    private static final String CODE_ICON = "💻 ";
    private static final String ARCHIVE_ICON = "📦 ";
    private static final String DOCUMENT_ICON = "📝 ";
    private static final String AUDIO_ICON = "🎵 ";
    private static final String VIDEO_ICON = "🎬 ";

    private final ConsoleLogger logger = ConsoleLogger.getInstance();
    private final boolean useIcons;
    private final boolean showHidden;
    private final boolean showSize;
    private final int maxDepth;
    private final Set<String> filterExtensions;

    /**
     * Builder for DirectoryTreePrinter configuration
     */
    public static class Builder {
        private boolean useIcons = true;
        private boolean showHidden = false;
        private boolean showSize = true;
        private int maxDepth = Integer.MAX_VALUE;
        private Set<String> filterExtensions = new HashSet<>();

        public Builder useIcons(boolean useIcons) {
            this.useIcons = useIcons;
            return this;
        }

        public Builder showHidden(boolean showHidden) {
            this.showHidden = showHidden;
            return this;
        }

        public Builder showSize(boolean showSize) {
            this.showSize = showSize;
            return this;
        }

        public Builder maxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder filterExtensions(String... extensions) {
            this.filterExtensions = Arrays.stream(extensions)
                    .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                    .collect(Collectors.toSet());
            return this;
        }

        public DirectoryTreePrinter build() {
            return new DirectoryTreePrinter(this);
        }
    }

    private DirectoryTreePrinter(Builder builder) {
        this.useIcons = builder.useIcons;
        this.showHidden = builder.showHidden;
        this.showSize = builder.showSize;
        this.maxDepth = builder.maxDepth;
        this.filterExtensions = builder.filterExtensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Print directory tree to console
     */
    public void printTree(Path directory, String header) {
        if (!Files.exists(directory)) {
            logger.error("Directory does not exist: " + directory);
            return;
        }

        if (!Files.isDirectory(directory)) {
            logger.error("Path is not a directory: " + directory);
            return;
        }

        // Print header
        System.out.println("\n" + ConsoleColors.BOLD + ConsoleColors.CYAN +
                "=== " + header + " ===" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.YELLOW + directory.toAbsolutePath() + ConsoleColors.RESET);

        try {
            TreeNode root = buildTree(directory);
            printNode(root, "", true, 0);

            // Print summary
            TreeStats stats = calculateStats(root);
            printSummary(stats);

        } catch (IOException e) {
            logger.error("Error reading directory structure: " + e.getMessage());
        }
    }

    /**
     * Build tree structure from directory
     */
    private TreeNode buildTree(Path path) throws IOException {
        TreeNode node = new TreeNode(path);

        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                List<Path> children = new ArrayList<>();
                for (Path child : stream) {
                    if (!showHidden && Files.isHidden(child)) {
                        continue;
                    }

                    if (Files.isRegularFile(child) && !filterExtensions.isEmpty()) {
                        String fileName = child.getFileName().toString();
                        int lastDot = fileName.lastIndexOf('.');
                        if (lastDot != -1) {
                            String ext = fileName.substring(lastDot);
                            if (!filterExtensions.contains(ext.toLowerCase())) {
                                continue;
                            }
                        }
                    }

                    children.add(child);
                }

                // Sort: directories first, then files, alphabetically
                children.sort((p1, p2) -> {
                    boolean isDir1 = Files.isDirectory(p1);
                    boolean isDir2 = Files.isDirectory(p2);

                    if (isDir1 && !isDir2) return -1;
                    if (!isDir1 && isDir2) return 1;

                    return p1.getFileName().toString()
                            .compareToIgnoreCase(p2.getFileName().toString());
                });

                for (Path child : children) {
                    node.children.add(buildTree(child));
                }
            }
        }

        return node;
    }

    /**
     * Print a tree node recursively
     */
    private void printNode(TreeNode node, String prefix, boolean isLast, int depth) throws IOException {
        if (depth > maxDepth) {
            return;
        }

        Path path = node.path;
        String fileName = depth == 0 ? "" : path.getFileName().toString();

        if (depth > 0) {
            // Print branch
            System.out.print(prefix);
            System.out.print(isLast ? LAST_BRANCH : BRANCH);

            // Print icon if enabled
            if (useIcons) {
                System.out.print(getIcon(path));
            }

            // Print name
            if (Files.isDirectory(path)) {
                System.out.print(ConsoleColors.BOLD + ConsoleColors.BLUE + fileName + ConsoleColors.RESET);
            } else {
                System.out.print(fileName);
            }

            // Print size if enabled and it's a file
            if (showSize && Files.isRegularFile(path)) {
                long size = Files.size(path);
                System.out.print(ConsoleColors.GRAY + " (" + formatFileSize(size) + ")" + ConsoleColors.RESET);
            }

            // Print file count for directories
            if (Files.isDirectory(path) && !node.children.isEmpty()) {
                long fileCount = node.children.stream()
                        .filter(n -> Files.isRegularFile(n.path))
                        .count();
                long dirCount = node.children.stream()
                        .filter(n -> Files.isDirectory(n.path))
                        .count();

                System.out.print(ConsoleColors.GRAY + " [");
                if (dirCount > 0) System.out.print(dirCount + "d");
                if (dirCount > 0 && fileCount > 0) System.out.print(", ");
                if (fileCount > 0) System.out.print(fileCount + "f");
                System.out.print("]" + ConsoleColors.RESET);
            }

            System.out.println();
        }

        // Print children
        String newPrefix = prefix + (depth == 0 ? "" : (isLast ? SPACE : VERTICAL));
        for (int i = 0; i < node.children.size(); i++) {
            printNode(node.children.get(i), newPrefix, i == node.children.size() - 1, depth + 1);
        }
    }

    /**
     * Get appropriate icon for file type
     */
    private String getIcon(Path path) {
        if (Files.isDirectory(path)) {
            return DIR_ICON;
        }

        String fileName = path.getFileName().toString().toLowerCase();
        String extension = "";

        int lastDot = fileName.lastIndexOf('.');
        if (lastDot != -1) {
            extension = fileName.substring(lastDot + 1);
        }

        // Code files
        if (Arrays.asList("java", "py", "js", "ts", "cpp", "c", "cs", "php", "rb", "go", "rs", "kt", "swift")
                .contains(extension)) {
            return CODE_ICON;
        }

        // Image files
        if (Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "ico")
                .contains(extension)) {
            return IMAGE_ICON;
        }

        // Document files
        if (Arrays.asList("doc", "docx", "odt", "pdf", "tex", "md", "txt", "rtf")
                .contains(extension)) {
            return DOCUMENT_ICON;
        }

        // Archive files
        if (Arrays.asList("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
                .contains(extension)) {
            return ARCHIVE_ICON;
        }

        // Audio files
        if (Arrays.asList("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a")
                .contains(extension)) {
            return AUDIO_ICON;
        }

        // Video files
        if (Arrays.asList("mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg")
                .contains(extension)) {
            return VIDEO_ICON;
        }

        return FILE_ICON;
    }

    /**
     * Calculate statistics for the tree
     */
    private TreeStats calculateStats(TreeNode root) {
        TreeStats stats = new TreeStats();
        calculateStatsRecursive(root, stats, 0);
        return stats;
    }

    private void calculateStatsRecursive(TreeNode node, TreeStats stats, int depth) {
        try {
            if (Files.isDirectory(node.path)) {
                stats.directoryCount++;
                stats.maxDepth = Math.max(stats.maxDepth, depth);
            } else if (Files.isRegularFile(node.path)) {
                stats.fileCount++;
                stats.totalSize += Files.size(node.path);
            }

            for (TreeNode child : node.children) {
                calculateStatsRecursive(child, stats, depth + 1);
            }
        } catch (IOException e) {
            // Ignore errors for individual files
        }
    }

    /**
     * Print summary statistics
     */
    private void printSummary(TreeStats stats) {
        System.out.println("\n" + ConsoleColors.GRAY + "──────────────────────────" + ConsoleColors.RESET);
        System.out.println(ConsoleColors.CYAN + "Summary:" + ConsoleColors.RESET);
        System.out.println("  Directories: " + stats.directoryCount);
        System.out.println("  Files: " + stats.fileCount);
        System.out.println("  Total size: " + formatFileSize(stats.totalSize));
        System.out.println("  Max depth: " + stats.maxDepth);

        if (!filterExtensions.isEmpty()) {
            System.out.println("  Filter: " + String.join(", ", filterExtensions));
        }
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Internal node representation
     */
    private static class TreeNode {
        Path path;
        List<TreeNode> children = new ArrayList<>();

        TreeNode(Path path) {
            this.path = path;
        }
    }

    /**
     * Tree statistics
     */
    private static class TreeStats {
        int fileCount = 0;
        int directoryCount = 0;
        long totalSize = 0;
        int maxDepth = 0;
    }

    /**
     * Console color codes
     */
    private static class ConsoleColors {
        static final String RESET = "\u001B[0m";
        static final String BLACK = "\u001B[30m";
        static final String RED = "\u001B[31m";
        static final String GREEN = "\u001B[32m";
        static final String YELLOW = "\u001B[33m";
        static final String BLUE = "\u001B[34m";
        static final String PURPLE = "\u001B[35m";
        static final String CYAN = "\u001B[36m";
        static final String WHITE = "\u001B[37m";
        static final String GRAY = "\u001B[90m";
        static final String BOLD = "\u001B[1m";
    }
}