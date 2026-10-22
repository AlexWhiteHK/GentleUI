package io.github.alexwhitehk.gentleui.core.theme;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.color.Oklab;

/**
 * Adapts text colors chosen for vanilla's dark menus to a theme's surfaces, so that light themes stay legible
 * without touching any individual screen.
 */
public final class TextColors {
	private TextColors() {
	}

	/** Remaps a 0xAARRGGBB (or 0xRRGGBB with alpha 0 meaning opaque) text color for {@code palette}. */
	public static int remap(int color, Palette palette) {
		int alpha = color >>> 24;
		int rgb = color & 0xFFFFFF;
		if (!palette.light) {
			return color;
		}
		int opaque = 0xFF000000 | rgb;
		float[] lab = Oklab.fromArgb(opaque);
		float l = lab[0];
		float chroma = (float) Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
		int mapped;
		if (chroma < 0.03f) {
			// Neutral grays: white becomes the main text color, light grays become muted text.
			if (l > 0.86f) {
				mapped = palette.text;
			} else if (l > 0.45f) {
				mapped = palette.textMuted;
			} else {
				return color;
			}
		} else if (l > 0.55f) {
			// Bright accents (yellow warnings, green links...) keep their hue but get dark enough to read.
			mapped = Oklab.toArgb(0.48f, lab[1] * 0.95f, lab[2] * 0.95f, 1f);
		} else {
			return color;
		}
		return (alpha << 24) | (mapped & 0xFFFFFF);
	}

	public static int remapOpaque(int rgb, Palette palette) {
		return remap(0xFF000000 | rgb, palette) & 0xFFFFFF;
	}

	/** Multiplies alpha, treating a zero alpha channel as fully opaque the way Minecraft's font does. */
	public static int fade(int color, float factor) {
		if (factor >= 1f) {
			return color;
		}
		int a = color >>> 24;
		if (a == 0 && (color & 0xFFFFFF) != 0) {
			a = 255;
		}
		return Argb.withAlpha(color, a / 255f * factor);
	}
}
