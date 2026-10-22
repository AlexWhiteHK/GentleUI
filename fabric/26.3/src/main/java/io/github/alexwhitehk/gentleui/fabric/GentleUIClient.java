package io.github.alexwhitehk.gentleui.fabric;

import io.github.alexwhitehk.gentleui.core.anim.Motion;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.core.theme.glass.GlassTheme;
import io.github.alexwhitehk.gentleui.core.theme.liquid.LiquidTheme;
import io.github.alexwhitehk.gentleui.core.theme.material.MaterialTheme;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint: loads configuration and registers the built-in themes. */
public final class GentleUIClient implements ClientModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("GentleUI");

	@Override
	public void onInitializeClient() {
		ConfigStore store = ConfigStore.init(FabricLoader.getInstance().getConfigDir().resolve("gentleui"));
		Themes.register(LiquidTheme.ID, LiquidTheme::new);
		Themes.register(MaterialTheme.ID, MaterialTheme::new);
		Themes.register(GlassTheme.ID, GlassTheme::new);
		applyMotion(ConfigStore.config());
		store.addListener(cfg -> {
			applyMotion(cfg);
			Themes.configChanged();
		});
		LOG.info("GentleUI ready (theme: {})", ConfigStore.config().theme);
	}

	private static void applyMotion(GentleConfig cfg) {
		Motion.configure(cfg.animationSpeed, cfg.reduceMotion);
	}
}
