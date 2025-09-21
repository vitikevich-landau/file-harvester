package v.landau.strategy;


import v.landau.model.FileOperation;

import java.nio.file.Path;

/**
 * Listener interface for monitoring harvesting progress.
 */
public interface ProgressListener {
    /**
     * Called when a file operation is completed.
     *
     * @param operation the completed operation
     */
    void onFileProcessed(FileOperation operation);

    /**
     * Called when harvesting starts.
     *
     * @param totalFiles estimated total files to process
     */
    void onStart(int totalFiles);

    /**
     * Called when harvesting completes.
     */
    void onComplete();

    /**
     * Called when an error occurs.
     *
     * @param error error message
     * @param path the file path that caused the error
     */
    void onError(String error, Path path);
}