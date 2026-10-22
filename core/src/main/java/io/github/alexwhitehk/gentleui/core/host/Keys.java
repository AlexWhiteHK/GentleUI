package io.github.alexwhitehk.gentleui.core.host;

/** Key and modifier codes. Values follow GLFW, which every current Minecraft version uses. */
public final class Keys {
	public static final int SPACE = 32;
	public static final int A = 65;
	public static final int C = 67;
	public static final int V = 86;
	public static final int X = 88;
	public static final int Y = 89;
	public static final int Z = 90;
	public static final int ESCAPE = 256;
	public static final int ENTER = 257;
	public static final int TAB = 258;
	public static final int BACKSPACE = 259;
	public static final int DELETE = 261;
	public static final int RIGHT = 262;
	public static final int LEFT = 263;
	public static final int DOWN = 264;
	public static final int UP = 265;
	public static final int PAGE_UP = 266;
	public static final int PAGE_DOWN = 267;
	public static final int HOME = 268;
	public static final int END = 269;
	public static final int KP_ENTER = 335;

	public static final int MOD_SHIFT = 1;
	public static final int MOD_CONTROL = 2;
	public static final int MOD_ALT = 4;
	public static final int MOD_SUPER = 8;

	public static final int MOUSE_LEFT = 0;
	public static final int MOUSE_RIGHT = 1;
	public static final int MOUSE_MIDDLE = 2;

	private Keys() {
	}

	public static boolean ctrl(int mods) {
		return (mods & (MOD_CONTROL | MOD_SUPER)) != 0;
	}

	public static boolean shift(int mods) {
		return (mods & MOD_SHIFT) != 0;
	}
}
