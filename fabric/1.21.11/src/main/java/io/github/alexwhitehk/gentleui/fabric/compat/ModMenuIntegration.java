package io.github.alexwhitehk.gentleui.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.alexwhitehk.gentleui.core.scene.SettingsScreen;
import io.github.alexwhitehk.gentleui.fabric.screen.GentleScreen;

/** Opens GentleUI's settings from Mod Menu's "configure" button. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> new GentleScreen(new SettingsScreen(), parent);
	}
}
