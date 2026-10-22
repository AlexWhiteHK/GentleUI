package io.github.alexwhitehk.gentleui.core.config;

import io.github.alexwhitehk.gentleui.core.host.Actions;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LiquidConfig {
	public static final String TOP = "top";
	public static final String BOTTOM = "bottom";
	public static final String LEFT = "left";
	public static final String RIGHT = "right";

	/** Palette id, see the Liquid theme for the available ones. */
	public String palette = "night";
	/** 0..2 multiplier for cursor-driven ripples and line displacement. */
	public float rippleStrength = 1f;
	/** Show text labels under bubbles once a menu is open. */
	public boolean labels = true;
	/** Show the faint "drag from an edge" hint on the idle lake. */
	public boolean idleHint = true;
	/** Edge id to menu tree; an edge without an entry has no menu. */
	public Map<String, MenuNode> menus = defaultMenus();

	public static Map<String, MenuNode> defaultMenus() {
		Map<String, MenuNode> m = new LinkedHashMap<>();
		m.put(BOTTOM, MenuNode.branch("play", "gentleui.menu.play", "play",
			MenuNode.leaf("continue", "gentleui.menu.continue", "forward", Actions.CONTINUE),
			MenuNode.leaf("singleplayer", "menu.singleplayer", "cube", Actions.SINGLEPLAYER),
			MenuNode.leaf("multiplayer", "menu.multiplayer", "people", Actions.MULTIPLAYER),
			MenuNode.leaf("realms", "menu.online", "cloud", Actions.REALMS)
		));
		m.put(RIGHT, MenuNode.branch("options", "menu.options", "gear",
			MenuNode.leaf("video", "options.video", "monitor", Actions.OPTIONS_VIDEO),
			MenuNode.leaf("sound", "options.sounds", "speaker", Actions.OPTIONS_SOUND),
			MenuNode.leaf("controls", "options.controls", "keyboard", Actions.OPTIONS_CONTROLS),
			MenuNode.leaf("language", "options.language", "globe", Actions.OPTIONS_LANGUAGE),
			MenuNode.leaf("packs", "options.resourcepack", "layers", Actions.OPTIONS_PACKS),
			MenuNode.branch("more", "gentleui.menu.more", "more",
				MenuNode.leaf("accessibility", "options.accessibility", "accessibility", Actions.OPTIONS_ACCESSIBILITY),
				MenuNode.leaf("skin", "options.skinCustomisation", "shirt", Actions.OPTIONS_SKIN),
				MenuNode.leaf("chat", "options.chat", "chat", Actions.OPTIONS_CHAT),
				MenuNode.leaf("online", "options.online", "wifi", Actions.OPTIONS_ONLINE),
				MenuNode.leaf("all", "gentleui.menu.all_options", "sliders", Actions.OPTIONS)
			)
		));
		m.put(LEFT, MenuNode.branch("personalize", "gentleui.menu.personalize", "palette",
			MenuNode.branch("themes", "gentleui.menu.themes", "brush",
				MenuNode.leaf("theme_liquid", "gentleui.theme.liquid", "drop", Actions.THEME + ":liquid"),
				MenuNode.leaf("theme_material", "gentleui.theme.material", "sparkle", Actions.THEME + ":material"),
				MenuNode.leaf("theme_glass", "gentleui.theme.glass", "glass", Actions.THEME + ":glass")
			),
			MenuNode.leaf("editor", "gentleui.menu.editor", "move", Actions.GENTLE_EDITOR),
			MenuNode.leaf("settings", "gentleui.menu.settings", "sliders", Actions.GENTLE_SETTINGS),
			MenuNode.leaf("mods", "gentleui.menu.mods", "puzzle", Actions.MODS)
		));
		m.put(TOP, MenuNode.branch("system", "gentleui.menu.system", "power",
			MenuNode.leaf("quit", "menu.quit", "power", Actions.QUIT),
			MenuNode.leaf("credits", "gentleui.menu.credits", "book", Actions.CREDITS),
			MenuNode.leaf("classic", "gentleui.menu.classic", "list", Actions.VANILLA_TITLE)
		));
		return m;
	}
}
