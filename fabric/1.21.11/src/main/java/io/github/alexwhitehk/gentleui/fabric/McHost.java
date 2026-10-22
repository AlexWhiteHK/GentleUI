package io.github.alexwhitehk.gentleui.fabric;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.host.Host;
import io.github.alexwhitehk.gentleui.core.host.SoundSpec;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.host.WorldInfo;
import io.github.alexwhitehk.gentleui.core.render.ImageRef;
import io.github.alexwhitehk.gentleui.core.scene.TitleScreenBase;
import io.github.alexwhitehk.gentleui.core.scene.UiScreen;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.fabric.render.Images;
import io.github.alexwhitehk.gentleui.fabric.screen.GentleScreen;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** {@link Host} implementation for Minecraft 1.21.11. */
public final class McHost implements Host {
	private static final McHost INSTANCE = new McHost();
	private final Minecraft mc = Minecraft.getInstance();
	private final String modVersion = FabricLoader.getInstance().getModContainer("gentleui")
		.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");

	private McHost() {
	}

	public static McHost get() {
		return INSTANCE;
	}

	@Override
	public String minecraftVersion() {
		return SharedConstants.getCurrentVersion().name();
	}

	@Override
	public String modVersion() {
		return modVersion;
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public boolean inWorld() {
		return mc.level != null;
	}

	@Override
	public String tr(String key, Object... args) {
		return I18n.get(key, args);
	}

	@Override
	public boolean hasTranslation(String key) {
		return I18n.exists(key);
	}

	@Override
	public String clipboard() {
		return mc.keyboardHandler.getClipboard();
	}

	@Override
	public void setClipboard(String text) {
		mc.keyboardHandler.setClipboard(text);
	}

	@Override
	public void playSound(UiSound sound) {
		if (!ConfigStore.config().uiSounds) {
			return;
		}
		SoundSpec spec = Themes.active().sound(sound);
		if (spec == null) {
			return;
		}
		Identifier id = Identifier.tryParse(spec.id());
		if (id == null) {
			return;
		}
		SoundEvent event = BuiltInRegistries.SOUND_EVENT.getValue(id);
		if (event != null) {
			mc.getSoundManager().play(SimpleSoundInstance.forUI(event, spec.pitch(), spec.volume()));
		}
	}

	@Override
	public Path configDir() {
		return ConfigStore.get().directory();
	}

	@Override
	public ImageRef image(String source) {
		return Images.get(source);
	}

	@Override
	public void run(String action) {
		ActionRunner.run(action);
	}

	@Override
	public boolean available(String action) {
		return ActionRunner.available(action);
	}

	@Override
	public String actionLabel(String action) {
		return ActionRunner.label(action);
	}

	@Override
	public String unavailableReason(String action) {
		return ActionRunner.unavailableReason(action);
	}

	@Override
	public void open(UiScreen screen) {
		mc.setScreen(new GentleScreen(screen, mc.screen));
	}

	@Override
	public void close() {
		if (mc.screen instanceof GentleScreen gs) {
			if (gs.parent() instanceof GentleScreen parent && parent.ui() instanceof TitleScreenBase title
				&& (!title.theme().id().equals(Themes.active().id()) || title.editedLayoutStale())) {
				// The theme or layout changed while a child screen was open: rebuild the title screen.
				mc.setScreen(new TitleScreen());
				return;
			}
			gs.closeToParent();
		} else {
			mc.setScreen(null);
		}
	}

	@Override
	public List<WorldInfo> recentWorlds() {
		return WorldCache.recent();
	}

	@Override
	public String playerName() {
		return mc.getUser().getName();
	}

	@Override
	public void configChanged() {
		Themes.configChanged();
	}
}
