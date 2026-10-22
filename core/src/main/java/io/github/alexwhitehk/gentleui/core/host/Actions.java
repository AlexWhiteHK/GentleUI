package io.github.alexwhitehk.gentleui.core.host;

/**
 * Action ids understood by every {@link Host}. Actions with arguments use {@code id:argument}, for example
 * {@code connect:play.example.net} or {@code url:https://example.net}. Menu entries in the user configuration
 * reference these strings, so they form a stable, versioned contract.
 */
public final class Actions {
	public static final String SINGLEPLAYER = "singleplayer";
	public static final String MULTIPLAYER = "multiplayer";
	public static final String REALMS = "realms";
	public static final String OPTIONS = "options";
	public static final String OPTIONS_VIDEO = "options.video";
	public static final String OPTIONS_SOUND = "options.sound";
	public static final String OPTIONS_CONTROLS = "options.controls";
	public static final String OPTIONS_LANGUAGE = "options.language";
	public static final String OPTIONS_PACKS = "options.packs";
	public static final String OPTIONS_ACCESSIBILITY = "options.accessibility";
	public static final String OPTIONS_SKIN = "options.skin";
	public static final String OPTIONS_CHAT = "options.chat";
	public static final String OPTIONS_ONLINE = "options.online";
	public static final String CREDITS = "credits";
	public static final String QUIT = "quit";
	public static final String MODS = "mods";
	public static final String VANILLA_TITLE = "vanilla_title";
	public static final String GENTLE_SETTINGS = "gentleui.settings";
	public static final String GENTLE_EDITOR = "gentleui.editor";
	/** {@code theme:<id>} switches the active theme. */
	public static final String THEME = "theme";
	/** {@code world:<folder>} loads a singleplayer world. */
	public static final String WORLD = "world";
	/** Loads the most recently played world. */
	public static final String CONTINUE = "continue";
	/** {@code connect:<address>} joins a server. */
	public static final String CONNECT = "connect";
	/** {@code url:<https url>} opens a link after the usual confirmation. */
	public static final String URL = "url";
	/** {@code folder:config} or {@code folder:images} opens a GentleUI folder in the file manager. */
	public static final String FOLDER = "folder";

	// In-game (pause menu) actions.
	public static final String RESUME = "resume";
	public static final String ADVANCEMENTS = "advancements";
	public static final String STATISTICS = "statistics";
	public static final String OPEN_TO_LAN = "lan";
	public static final String DISCONNECT = "disconnect";
	public static final String FEEDBACK = "feedback";
	public static final String PLAYER_REPORTING = "player_reporting";
	/** Opens the game's own pause menu (for server-provided extras such as links and dialogs). */
	public static final String VANILLA_PAUSE = "vanilla_pause";

	private Actions() {
	}

	public static String id(String action) {
		int i = action.indexOf(':');
		return i < 0 ? action : action.substring(0, i);
	}

	public static String argument(String action) {
		int i = action.indexOf(':');
		return i < 0 ? "" : action.substring(i + 1);
	}
}
