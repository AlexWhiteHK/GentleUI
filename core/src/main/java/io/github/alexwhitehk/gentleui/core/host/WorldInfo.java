package io.github.alexwhitehk.gentleui.core.host;

import io.github.alexwhitehk.gentleui.core.render.ImageRef;

/** Summary of a singleplayer world used for quick-resume entries. */
public record WorldInfo(String folder, String name, long lastPlayed, String detail, ImageRef icon) {
}
