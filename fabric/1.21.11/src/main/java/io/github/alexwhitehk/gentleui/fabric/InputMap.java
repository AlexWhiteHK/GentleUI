package io.github.alexwhitehk.gentleui.fabric;

/**
 * Converts this Minecraft version's input codes to GentleUI's key space (GLFW-compatible values, see
 * {@link io.github.alexwhitehk.gentleui.core.host.Keys}). On 1.21.x the game itself uses GLFW, so this is the
 * identity mapping.
 */
public final class InputMap {
	private InputMap() {
	}

	public static int key(int key) {
		return key;
	}

	public static int mods(int mods) {
		return mods;
	}

	public static int button(int button) {
		return button;
	}
}
