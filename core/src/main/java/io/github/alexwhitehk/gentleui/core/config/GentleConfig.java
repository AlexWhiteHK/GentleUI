package io.github.alexwhitehk.gentleui.core.config;

import java.util.LinkedHashMap;
import java.util.Map;

/** Root of the persisted configuration ({@code config/gentleui/gentleui.json}). */
public final class GentleConfig {
	public static final int SCHEMA = 1;

	public int schema = SCHEMA;
	public String theme = "liquid";
	public float animationSpeed = 1f;
	public boolean reduceMotion = false;
	/** Restyle vanilla and modded menu screens with the active theme. */
	public boolean skinMenus = true;
	/** Recolor menu text for light themes so it stays readable. */
	public boolean adaptText = true;
	public boolean screenTransitions = true;
	/** Background reacts to the cursor. */
	public boolean pointerEffects = true;
	/** 0.25..1.5 multiplier for particles, lines and other background detail. */
	public float detail = 1f;
	public boolean showVersion = true;
	public boolean uiSounds = true;
	/** Replace the pause menu with the themed one. */
	public boolean themedPauseMenu = true;
	/** Replace world loading and connection screens with themed progress screens. */
	public boolean themedLoading = true;
	/** Fully qualified screen class names that keep their vanilla look (for mods that do not fit the skin). */
	public java.util.List<String> skinExclusions = new java.util.ArrayList<>();

	public LiquidConfig liquid = new LiquidConfig();
	public MaterialConfig material = new MaterialConfig();
	public GlassConfig glass = new GlassConfig();
	/** Theme id to layout. */
	public Map<String, LayoutConfig> layouts = new LinkedHashMap<>();

	public LayoutConfig layout(String themeId) {
		return layouts.computeIfAbsent(themeId, k -> new LayoutConfig());
	}

	/** Fills in sections missing from older or hand-edited files. */
	public GentleConfig sanitize() {
		if (theme == null || theme.isBlank()) {
			theme = "liquid";
		}
		if (liquid == null) {
			liquid = new LiquidConfig();
		}
		if (liquid.menus == null) {
			liquid.menus = LiquidConfig.defaultMenus();
		}
		if (material == null) {
			material = new MaterialConfig();
		}
		if (glass == null) {
			glass = new GlassConfig();
		}
		if (layouts == null) {
			layouts = new LinkedHashMap<>();
		}
		if (skinExclusions == null) {
			skinExclusions = new java.util.ArrayList<>();
		}
		for (LayoutConfig l : layouts.values()) {
			if (l.overrides == null) {
				l.overrides = new LinkedHashMap<>();
			}
			if (l.custom == null) {
				l.custom = new java.util.ArrayList<>();
			}
		}
		animationSpeed = clamp(animationSpeed, 0.25f, 3f);
		detail = clamp(detail, 0.25f, 1.5f);
		liquid.rippleStrength = clamp(liquid.rippleStrength, 0f, 2f);
		glass.frost = clamp(glass.frost, 0f, 1f);
		schema = SCHEMA;
		return this;
	}

	private static float clamp(float v, float min, float max) {
		if (Float.isNaN(v)) {
			return min;
		}
		return Math.max(min, Math.min(max, v));
	}
}
