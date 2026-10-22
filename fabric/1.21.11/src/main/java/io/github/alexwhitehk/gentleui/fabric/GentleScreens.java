package io.github.alexwhitehk.gentleui.fabric;

import io.github.alexwhitehk.gentleui.core.scene.LayoutEditorScreen;
import io.github.alexwhitehk.gentleui.core.scene.SettingsScreen;
import io.github.alexwhitehk.gentleui.fabric.screen.GentleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Opens GentleUI's own configuration screens. */
public final class GentleScreens {
	private GentleScreens() {
	}

	public static void openSettings(Screen parent) {
		Minecraft.getInstance().setScreen(new GentleScreen(new SettingsScreen(), parent));
	}

	public static void openEditor(Screen parent) {
		Minecraft.getInstance().setScreen(new GentleScreen(new LayoutEditorScreen(), parent));
	}
}
