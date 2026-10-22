package io.github.alexwhitehk.gentleui.core.host;

/** A sound event id with playback settings; themes choose their own interface sounds. */
public record SoundSpec(String id, float pitch, float volume) {
	public static final SoundSpec CLICK = new SoundSpec("minecraft:ui.button.click", 1f, 0.25f);
}
