package io.github.alexwhitehk.gentleui.core.theme.material;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.color.Oklab;
import io.github.alexwhitehk.gentleui.core.theme.Palette;

/** Tonal color scheme derived from a single seed color, in the spirit of Material You. */
public final class MaterialColors {
	public static final int BLUE = 0xFF4285F4;
	public static final int RED = 0xFFEA4335;
	public static final int YELLOW = 0xFFFBBC04;
	public static final int GREEN = 0xFF34A853;
	public static final int[] BRAND = {BLUE, RED, YELLOW, GREEN};

	/** Preset seeds offered in the settings screen. */
	public static final String[] SEEDS = {"#1A73E8", "#0B8043", "#E37400", "#D93025", "#9334E6", "#007B83"};

	public final Palette palette = new Palette();
	public int containerHigh;
	public int outlineVariant;
	public int shapeTint;
	public boolean dark;

	public MaterialColors(int seed, boolean dark) {
		this.dark = dark;
		float[] lab = Oklab.fromArgb(seed);
		float chroma = (float) Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
		float hue = (float) Math.toDegrees(Math.atan2(lab[2], lab[1]));
		Palette p = palette;
		p.light = !dark;
		if (!dark) {
			p.background = Oklab.lch(0.975f, 0.006f, hue, 1f);
			p.surface = 0xFFFFFFFF;
			p.surfaceVariant = Oklab.lch(0.935f, 0.014f, hue, 1f);
			containerHigh = Oklab.lch(0.91f, 0.016f, hue, 1f);
			p.outline = Oklab.lch(0.6f, 0.01f, hue, 1f);
			outlineVariant = Oklab.lch(0.88f, 0.008f, hue, 1f);
			p.text = Oklab.lch(0.22f, 0.006f, hue, 1f);
			p.textMuted = Oklab.lch(0.46f, 0.012f, hue, 1f);
			p.textDisabled = Argb.withAlpha(p.text, 0.38f);
			p.accent = Oklab.lch(0.55f, Math.min(chroma, 0.18f), hue, 1f);
			p.onAccent = 0xFFFFFFFF;
			p.accentContainer = Oklab.lch(0.92f, 0.045f, hue, 1f);
			p.onAccentContainer = Oklab.lch(0.28f, 0.08f, hue, 1f);
			p.danger = 0xFFD93025;
			p.shadow = 0xFF000000;
			p.highlight = 0xFFFFFFFF;
			shapeTint = 0xFFFFFFFF;
		} else {
			p.background = Oklab.lch(0.19f, 0.006f, hue, 1f);
			p.surface = Oklab.lch(0.235f, 0.008f, hue, 1f);
			p.surfaceVariant = Oklab.lch(0.30f, 0.012f, hue, 1f);
			containerHigh = Oklab.lch(0.34f, 0.014f, hue, 1f);
			p.outline = Oklab.lch(0.58f, 0.01f, hue, 1f);
			outlineVariant = Oklab.lch(0.38f, 0.01f, hue, 1f);
			p.text = Oklab.lch(0.93f, 0.005f, hue, 1f);
			p.textMuted = Oklab.lch(0.74f, 0.01f, hue, 1f);
			p.textDisabled = Argb.withAlpha(p.text, 0.38f);
			p.accent = Oklab.lch(0.79f, Math.min(chroma, 0.12f), hue, 1f);
			p.onAccent = Oklab.lch(0.25f, 0.08f, hue, 1f);
			p.accentContainer = Oklab.lch(0.36f, 0.07f, hue, 1f);
			p.onAccentContainer = Oklab.lch(0.92f, 0.03f, hue, 1f);
			p.danger = 0xFFF2B8B5;
			p.shadow = 0xFF000000;
			p.highlight = 0xFFFFFFFF;
			shapeTint = p.background;
		}
	}

	/** Brand color softened for large background shapes. */
	public int pastel(int brand) {
		return dark ? Argb.lerp(brand, palette.background, 0.55f) : Argb.lerp(brand, 0xFFFFFFFF, 0.72f);
	}
}
