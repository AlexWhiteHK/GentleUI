package io.github.alexwhitehk.gentleui.fabric;

import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.fabric.screen.GentleScreen;
import net.minecraft.client.Minecraft;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.scene.PauseMenuScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

/** Decides which vanilla screens are replaced by GentleUI screens. */
public final class ScreenRouter {
	private static boolean bypassNextTitle;
	private static boolean bypassNextPause;

	private ScreenRouter() {
	}

	/** The next title screen opens as vanilla (used by the "classic menu" entry). */
	public static void bypassNextTitle() {
		bypassNextTitle = true;
	}

	/** The next pause screen opens as vanilla (used by "More options" in the themed pause menu). */
	public static void bypassNextPause() {
		bypassNextPause = true;
	}

	public static Screen route(Screen screen) {
		if (screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
			if (bypassNextPause) {
				bypassNextPause = false;
				return screen;
			}
			if (ConfigStore.config().themedPauseMenu) {
				return new GentleScreen(new PauseMenuScreen(), null);
			}
		}
		if (screen instanceof TitleScreen && !(screen instanceof GentleScreen)) {
			if (bypassNextTitle) {
				bypassNextTitle = false;
				return screen;
			}
			if (Minecraft.getInstance().isDemo()) {
				return screen;
			}
			try {
				return new GentleScreen(Themes.active().createTitleScreen(), null);
			} catch (RuntimeException e) {
				GentleUIClient.LOG.error("GentleUI: title screen failed, falling back to vanilla", e);
				return screen;
			}
		}
		return screen;
	}
}
