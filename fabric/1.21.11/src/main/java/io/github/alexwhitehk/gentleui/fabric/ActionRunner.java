package io.github.alexwhitehk.gentleui.fabric;

import com.mojang.realmsclient.RealmsMainScreen;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.host.Actions;
import io.github.alexwhitehk.gentleui.core.host.WorldInfo;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.ChatOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executes {@link Actions} ids against the 1.21.11 client. */
public final class ActionRunner {
	private static final Logger LOG = LoggerFactory.getLogger("GentleUI");

	private ActionRunner() {
	}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	private static void open(Supplier<Screen> screen) {
		mc().setScreen(screen.get());
	}

	public static void run(String action) {
		if (action == null || action.isEmpty()) {
			return;
		}
		try {
			dispatch(Actions.id(action), Actions.argument(action));
		} catch (RuntimeException e) {
			LOG.error("GentleUI: action '{}' failed", action, e);
		}
	}

	private static void dispatch(String id, String arg) {
		Minecraft mc = mc();
		Screen cur = mc.screen;
		Options o = mc.options;
		switch (id) {
			case Actions.SINGLEPLAYER -> open(() -> new SelectWorldScreen(cur));
			case Actions.MULTIPLAYER -> {
				if (available(id)) {
					open(() -> o.skipMultiplayerWarning ? new JoinMultiplayerScreen(cur) : new SafetyScreen(cur));
				}
			}
			case Actions.REALMS -> {
				if (available(id)) {
					open(() -> new RealmsMainScreen(cur));
				}
			}
			case Actions.OPTIONS -> open(() -> new OptionsScreen(cur, o));
			case Actions.OPTIONS_VIDEO -> open(() -> new VideoSettingsScreen(cur, mc, o));
			case Actions.OPTIONS_SOUND -> open(() -> new SoundOptionsScreen(cur, o));
			case Actions.OPTIONS_CONTROLS -> open(() -> new ControlsScreen(cur, o));
			case Actions.OPTIONS_LANGUAGE -> open(() -> new LanguageSelectScreen(cur, o, mc.getLanguageManager()));
			case Actions.OPTIONS_PACKS -> open(() -> new PackSelectionScreen(mc.getResourcePackRepository(), repo -> {
				o.updateResourcePacks(repo);
				mc.setScreen(cur);
			}, mc.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
			case Actions.OPTIONS_ACCESSIBILITY -> open(() -> new AccessibilityOptionsScreen(cur, o));
			case Actions.OPTIONS_SKIN -> open(() -> new SkinCustomizationScreen(cur, o));
			case Actions.OPTIONS_CHAT -> open(() -> new ChatOptionsScreen(cur, o));
			case Actions.OPTIONS_ONLINE -> open(() -> new OnlineOptionsScreen(cur, o));
			case Actions.CREDITS -> open(() -> new CreditsAndAttributionScreen(cur));
			case Actions.QUIT -> mc.stop();
			case Actions.MODS -> openModMenu(cur);
			case Actions.VANILLA_TITLE -> {
				ScreenRouter.bypassNextTitle();
				mc.setScreen(new TitleScreen());
			}
			case Actions.GENTLE_SETTINGS -> GentleScreens.openSettings(cur);
			case Actions.GENTLE_EDITOR -> GentleScreens.openEditor(cur);
			case Actions.THEME -> switchTheme(arg);
			case Actions.WORLD -> openWorld(arg, cur);
			case Actions.CONTINUE -> {
				List<WorldInfo> worlds = WorldCache.recent();
				if (worlds.isEmpty()) {
					open(() -> new SelectWorldScreen(cur));
				} else {
					openWorld(worlds.get(0).folder(), cur);
				}
			}
			case Actions.CONNECT -> {
				if (available(Actions.MULTIPLAYER) && !arg.isBlank()) {
					ServerData data = new ServerData(arg, arg, ServerData.Type.OTHER);
					ConnectScreen.startConnecting(cur, mc, ServerAddress.parseString(arg), data, false, null);
				}
			}
			case Actions.URL -> {
				if (arg.startsWith("https://") || arg.startsWith("http://")) {
					ConfirmLinkScreen.confirmLinkNow(cur, arg);
				}
			}
			case Actions.FOLDER -> {
				java.nio.file.Path dir = "images".equals(arg) ? io.github.alexwhitehk.gentleui.core.config.ConfigStore.get().imagesDirectory()
					: io.github.alexwhitehk.gentleui.core.config.ConfigStore.get().directory();
				try {
					java.nio.file.Files.createDirectories(dir);
				} catch (java.io.IOException e) {
					LOG.warn("GentleUI: could not create {}", dir, e);
				}
				net.minecraft.util.Util.getPlatform().openPath(dir);
			}
			case Actions.RESUME -> {
				mc.setScreen(null);
				mc.mouseHandler.grabMouse();
			}
			case Actions.VANILLA_PAUSE -> {
				ScreenRouter.bypassNextPause();
				mc.setScreen(new net.minecraft.client.gui.screens.PauseScreen(true));
			}
			case Actions.ADVANCEMENTS -> {
				if (mc.player != null) {
					open(() -> new AdvancementsScreen(mc.player.connection.getAdvancements(), cur));
				}
			}
			case Actions.STATISTICS -> {
				if (mc.player != null) {
					open(() -> new StatsScreen(cur, mc.player.getStats()));
				}
			}
			case Actions.OPEN_TO_LAN -> {
				if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null && !mc.getSingleplayerServer().isPublished()) {
					open(() -> new ShareToLanScreen(cur));
				}
			}
			case Actions.PLAYER_REPORTING -> open(() -> new SocialInteractionsScreen(cur));
			case Actions.DISCONNECT -> {
				if (mc.level != null) {
					mc.getReportingContext().draftReportHandled(mc, cur, () -> mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
				}
			}
			default -> LOG.warn("GentleUI: unknown action '{}'", id);
		}
	}

	private static void switchTheme(String themeId) {
		if (!Themes.ids().contains(themeId)) {
			return;
		}
		ConfigStore.get().update(c -> c.theme = themeId);
		Themes.configChanged();
		Minecraft mc = mc();
		if (mc.level == null) {
			mc.setScreen(new TitleScreen());
		}
	}

	private static void openWorld(String folder, Screen cur) {
		Minecraft mc = mc();
		var summary = WorldCache.summary(folder);
		if (summary == null || summary.isLocked() || summary.requiresManualConversion() || !summary.isCompatible() || !summary.primaryActionActive()) {
			// Anything unusual goes through the vanilla world list, which knows how to ask the right questions.
			mc.setScreen(new SelectWorldScreen(cur));
			return;
		}
		mc.createWorldOpenFlows().openWorld(folder, () -> mc.setScreen(cur));
	}

	private static void openModMenu(Screen cur) {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu")) {
			try {
				Class<?> cls = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
				Screen screen = (Screen) cls.getConstructor(Screen.class).newInstance(cur);
				mc().setScreen(screen);
			} catch (ReflectiveOperationException | ClassCastException e) {
				LOG.warn("GentleUI: could not open Mod Menu", e);
			}
		}
	}

	public static boolean available(String action) {
		String id = Actions.id(action);
		Minecraft mc = mc();
		return switch (id) {
			case Actions.MULTIPLAYER, Actions.REALMS, Actions.CONNECT -> mc.allowsMultiplayer();
			case Actions.MODS -> net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("modmenu");
			case Actions.CONTINUE -> !WorldCache.recent().isEmpty();
			case Actions.ADVANCEMENTS, Actions.STATISTICS -> mc.player != null;
			case Actions.OPEN_TO_LAN -> mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null && !mc.getSingleplayerServer().isPublished();
			default -> true;
		};
	}

	public static String label(String action) {
		Minecraft mc = mc();
		return switch (Actions.id(action)) {
			case Actions.DISCONNECT -> net.minecraft.network.chat.CommonComponents.disconnectButtonLabel(mc.isLocalServer()).getString();
			default -> null;
		};
	}

	public static String unavailableReason(String action) {
		String id = Actions.id(action);
		return switch (id) {
			case Actions.MULTIPLAYER, Actions.REALMS, Actions.CONNECT -> Component.translatable("title.multiplayer.disabled").getString();
			case Actions.MODS -> Component.translatable("gentleui.reason.modmenu").getString();
			case Actions.CONTINUE -> Component.translatable("gentleui.reason.no_worlds").getString();
			default -> null;
		};
	}
}
