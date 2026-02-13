package v.landau.config;

/**
 * Policies for handling filename collisions in target directory.
 */
public enum CollisionResolutionMode {
    OVERWRITE,
    SUFFIX_COUNTER,
    PRESERVE_RELATIVE_PATH,
    HASH_SUFFIX
}
