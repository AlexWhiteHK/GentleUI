package io.github.alexwhitehk.gentleui.core.host;

import io.github.alexwhitehk.gentleui.core.render.ImageRef;
import io.github.alexwhitehk.gentleui.core.scene.UiScreen;
import java.nio.file.Path;
import java.util.List;

/**
 * Everything the core needs from the game, expressed without any Minecraft types. Each supported Minecraft
 * version provides one implementation; the rest of GentleUI is written against this interface only.
 */
public interface Host {
	String minecraftVersion();

	String modVersion();

	boolean isModLoaded(String modId);

	/** True while a world or server is loaded (menus are then drawn over the running game). */
	boolean inWorld();

	/** Translates a key with the active game language, falling back to the key itself. */
	String tr(String key, Object... args);

	boolean hasTranslation(String key);

	String clipboard();

	void setClipboard(String text);

	void playSound(UiSound sound);

	Path configDir();

	/**
	 * Resolves an image. {@code source} is either a resource location ("namespace:path/to.png") or a path to a PNG
	 * file inside the GentleUI config directory prefixed with {@code file:}. Loading may complete later.
	 */
	ImageRef image(String source);

	/** Runs a navigation or system action. See {@link Actions} for the supported ids. */
	void run(String action);

	/** Whether {@code action} can currently run (for example multiplayer may be disabled for this account). */
	boolean available(String action);

	/** A human readable reason shown when {@link #available} is false, or null. */
	String unavailableReason(String action);

	/** Opens a core screen on top of the current one. */
	void open(UiScreen screen);

	/** Closes the current screen, returning to whatever opened it. */
	void close();

	/** Most recently played singleplayer worlds, newest first. May be empty while still loading. */
	List<WorldInfo> recentWorlds();

	/** Platform-specific label for an action (e.g. "Save and Quit to Title" vs "Disconnect"), or null. */
	default String actionLabel(String action) {
		return null;
	}

	/** Player name of the signed-in account. */
	String playerName();

	/** Called by the core when configuration changed and platform-side caches should refresh. */
	void configChanged();
}
