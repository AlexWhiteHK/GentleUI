package io.github.alexwhitehk.gentleui.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.alexwhitehk.gentleui.core.host.Keys;

/**
 * Converts this Minecraft version's input codes to GentleUI's key space (GLFW-compatible values, see
 * {@link Keys}). From 26.1 the game uses SDL scancodes and modifier masks, so the keys GentleUI reacts to are
 * translated here.
 */
public final class InputMap {
	private InputMap() {
	}

	public static int key(int key) {
		return switch (key) {
			case InputConstants.KEY_ESCAPE -> Keys.ESCAPE;
			case InputConstants.KEY_RETURN -> Keys.ENTER;
			case InputConstants.KEY_NUMPADENTER -> Keys.KP_ENTER;
			case InputConstants.KEY_TAB -> Keys.TAB;
			case InputConstants.KEY_BACKSPACE -> Keys.BACKSPACE;
			case InputConstants.KEY_DELETE -> Keys.DELETE;
			case InputConstants.KEY_RIGHT -> Keys.RIGHT;
			case InputConstants.KEY_LEFT -> Keys.LEFT;
			case InputConstants.KEY_DOWN -> Keys.DOWN;
			case InputConstants.KEY_UP -> Keys.UP;
			case InputConstants.KEY_PAGEUP -> Keys.PAGE_UP;
			case InputConstants.KEY_PAGEDOWN -> Keys.PAGE_DOWN;
			case InputConstants.KEY_HOME -> Keys.HOME;
			case InputConstants.KEY_END -> Keys.END;
			case InputConstants.KEY_SPACE -> Keys.SPACE;
			case InputConstants.KEY_A -> Keys.A;
			case InputConstants.KEY_C -> Keys.C;
			case InputConstants.KEY_V -> Keys.V;
			case InputConstants.KEY_X -> Keys.X;
			case InputConstants.KEY_Y -> Keys.Y;
			case InputConstants.KEY_Z -> Keys.Z;
			default -> -1;
		};
	}

	public static int mods(int mods) {
		int out = 0;
		if ((mods & InputConstants.MOD_SHIFT) != 0) {
			out |= Keys.MOD_SHIFT;
		}
		if ((mods & InputConstants.MOD_CONTROL) != 0) {
			out |= Keys.MOD_CONTROL;
		}
		if ((mods & InputConstants.MOD_ALT) != 0) {
			out |= Keys.MOD_ALT;
		}
		if ((mods & InputConstants.MOD_SUPER) != 0) {
			out |= Keys.MOD_SUPER;
		}
		return out;
	}

	public static int button(int button) {
		return switch (button) {
			case InputConstants.MOUSE_BUTTON_LEFT -> Keys.MOUSE_LEFT;
			case InputConstants.MOUSE_BUTTON_RIGHT -> Keys.MOUSE_RIGHT;
			case InputConstants.MOUSE_BUTTON_MIDDLE -> Keys.MOUSE_MIDDLE;
			default -> button + 10;
		};
	}
}
