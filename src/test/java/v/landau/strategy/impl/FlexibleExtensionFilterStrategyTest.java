package v.landau.strategy.impl;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlexibleExtensionFilterStrategyTest {

    @Test
    void includeRulesAcceptOnlyRequestedExtensions() {
        FlexibleExtensionFilterStrategy strategy = new FlexibleExtensionFilterStrategy("+jpg", "+png");

        assertTrue(strategy.accept(Path.of("photo.jpg")));
        assertTrue(strategy.accept(Path.of("image.png")));
        assertFalse(strategy.accept(Path.of("doc.txt")));
    }

    @Test
    void excludeRulesRejectSpecifiedExtensionsInBlacklistMode() {
        FlexibleExtensionFilterStrategy strategy = new FlexibleExtensionFilterStrategy("-tmp");

        assertFalse(strategy.accept(Path.of("cache.tmp")));
        assertTrue(strategy.accept(Path.of("keep.log")));
    }

    @Test
    void mixedRulesApplyIncludeThenExclude() {
        FlexibleExtensionFilterStrategy strategy = new FlexibleExtensionFilterStrategy("+jpg", "+png", "-png");

        assertTrue(strategy.accept(Path.of("photo.jpg")));
        assertFalse(strategy.accept(Path.of("icon.png")));
        assertFalse(strategy.accept(Path.of("note.txt")));
    }

    @Test
    void noextRulesControlFilesWithoutExtensionAndDotfiles() {
        FlexibleExtensionFilterStrategy allowNoExt = new FlexibleExtensionFilterStrategy("+noext");
        FlexibleExtensionFilterStrategy denyNoExt = new FlexibleExtensionFilterStrategy("-noext");

        assertTrue(allowNoExt.accept(Path.of("README")));
        assertTrue(allowNoExt.accept(Path.of(".gitignore")));

        assertFalse(denyNoExt.accept(Path.of("README")));
        assertFalse(denyNoExt.accept(Path.of(".gitignore")));
    }

    @Test
    void compoundExtensionTarGzIsMatched() {
        FlexibleExtensionFilterStrategy strategy = new FlexibleExtensionFilterStrategy("+tar.gz");

        assertTrue(strategy.accept(Path.of("archive.tar.gz")));
        assertFalse(strategy.accept(Path.of("archive.gz")));
    }
}
