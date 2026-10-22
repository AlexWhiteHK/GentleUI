package io.github.alexwhitehk.gentleui.fabric.skin;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.config.GentleConfig;
import io.github.alexwhitehk.gentleui.core.math.Easing;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.theme.Backdrop;
import io.github.alexwhitehk.gentleui.core.theme.Theme;
import io.github.alexwhitehk.gentleui.core.theme.Themes;
import io.github.alexwhitehk.gentleui.core.anim.Motion;
import io.github.alexwhitehk.gentleui.fabric.mixin.GuiGraphicsAccessor;
import io.github.alexwhitehk.gentleui.fabric.render.McCanvas;
import io.github.alexwhitehk.gentleui.fabric.screen.GentleScreen;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.debug.GameModeSwitcherScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;

/**
 * Per-frame state of the vanilla skin: which screen is being drawn, whether it is restyled, and the progress of
 * its entrance transition.
 */
public final class SkinContext {
	private static final float TRANSITION = 0.24f;
	private static final Map<Screen, Long> OPENED = new WeakHashMap<>();

	private static Screen screen;
	private static boolean active;
	private static boolean content;
	private static float contentAlpha = 1f;
	private static float slide;
	private static GuiGraphicsExtractor frameGraphics;
	private static long frameCounter;
	private static long lastNanos = -1L;
	private static float dt;
	private static float time;
	private static boolean pointerWasDown;
	private static final Backdrop BACKDROP = new Backdrop();

	private SkinContext() {
	}

	/** Gameplay screens keep their vanilla look. */
	public static boolean isSkinnable(Screen s) {
		if (s == null || s instanceof GentleScreen) {
			return false;
		}
		if (s instanceof AbstractContainerScreen<?> || s instanceof ChatScreen || s instanceof BookEditScreen || s instanceof BookViewScreen
			|| s instanceof AbstractSignEditScreen || s instanceof WinScreen || s instanceof GameModeSwitcherScreen) {
			return false;
		}
		if (!ConfigStore.config().themedLoading && (s instanceof net.minecraft.client.gui.screens.LevelLoadingScreen
			|| s instanceof net.minecraft.client.gui.screens.GenericWaitingScreen || s instanceof net.minecraft.client.gui.screens.ConnectScreen
			|| s instanceof net.minecraft.client.gui.screens.ProgressScreen || s instanceof net.minecraft.client.gui.screens.GenericMessageScreen)) {
			return false;
		}
		for (String name : ConfigStore.config().skinExclusions) {
			if (s.getClass().getName().equals(name)) {
				return false;
			}
		}
		return true;
	}

	public static void screenOpened(Screen s) {
		if (s != null) {
			OPENED.put(s, System.nanoTime());
		}
	}

	private static void tick(GuiGraphicsExtractor g) {
		if (g == frameGraphics) {
			return;
		}
		frameGraphics = g;
		frameCounter++;
		long now = System.nanoTime();
		dt = lastNanos < 0L ? 0f : Math.min(0.1f, (now - lastNanos) / 1e9f);
		lastNanos = now;
		time += dt;
	}

	public static void beginScreen(Screen s, GuiGraphicsExtractor g) {
		tick(g);
		screen = s;
		GentleConfig cfg = ConfigStore.config();
		active = cfg.skinMenus && isSkinnable(s);
		content = false;
		if (active && cfg.screenTransitions && !Motion.reduced()) {
			Long opened = OPENED.get(s);
			float t = opened == null ? 1f : (System.nanoTime() - opened) / 1e9f * Motion.speed() / TRANSITION;
			float e = Easing.OUT_CUBIC.apply(MathUtil.clamp01(t));
			contentAlpha = e;
			slide = (1f - e) * 7f;
		} else {
			contentAlpha = 1f;
			slide = 0f;
		}
		if (active) {
			boolean down = Minecraft.getInstance().mouseHandler.isLeftPressed();
			if (down && !pointerWasDown) {
				theme().pointerPressed(mouseX(), mouseY());
			}
			pointerWasDown = down;
		}
	}

	public static void endScreen() {
		active = false;
		content = false;
		screen = null;
	}

	public static void beginContent() {
		content = true;
	}

	public static void endContent() {
		content = false;
	}

	public static boolean active() {
		return active;
	}

	public static Screen screen() {
		return screen;
	}

	/** Opacity applied to restyled content during the entrance transition (1 outside of it). */
	public static float contentAlpha() {
		return active && content ? contentAlpha : 1f;
	}

	public static float slide() {
		return slide;
	}

	public static long frame() {
		return frameCounter;
	}

	public static float dt() {
		return dt;
	}

	public static float time() {
		return time;
	}

	public static Theme theme() {
		return Themes.active();
	}

	public static float mouseX() {
		Minecraft mc = Minecraft.getInstance();
		return (float) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / Math.max(1, mc.getWindow().getScreenWidth()));
	}

	public static float mouseY() {
		Minecraft mc = Minecraft.getInstance();
		return (float) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / Math.max(1, mc.getWindow().getScreenHeight()));
	}

	public static int graphicsMouseX(GuiGraphicsExtractor g) {
		return ((GuiGraphicsAccessor) g).gentleui$mouseX();
	}

	public static int graphicsMouseY(GuiGraphicsExtractor g) {
		return ((GuiGraphicsAccessor) g).gentleui$mouseY();
	}

	/** Draws the active theme's background for the restyled screen. */
	public static void drawBackground(GuiGraphicsExtractor g, Screen s) {
		drawBackground(g, s, Minecraft.getInstance().level != null);
	}

	public static void drawBackground(GuiGraphicsExtractor g, Screen s, boolean inWorld) {
		Minecraft mc = Minecraft.getInstance();
		BACKDROP.width = s.width;
		BACKDROP.height = s.height;
		BACKDROP.mouseX = mouseX();
		BACKDROP.mouseY = mouseY();
		BACKDROP.dt = dt;
		BACKDROP.time = time;
		BACKDROP.inWorld = inWorld;
		BACKDROP.calm = 1f;
		BACKDROP.pointerActive = ConfigStore.config().pointerEffects;
		McCanvas c = McCanvas.begin(g);
		theme().background(c, BACKDROP);
		c.end();
	}
}
