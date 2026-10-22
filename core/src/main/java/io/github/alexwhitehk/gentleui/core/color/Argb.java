package io.github.alexwhitehk.gentleui.core.color;

import io.github.alexwhitehk.gentleui.core.math.MathUtil;

/** Packed 0xAARRGGBB color helpers. */
public final class Argb {
	public static final int TRANSPARENT = 0x00000000;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int BLACK = 0xFF000000;

	private Argb() {
	}

	public static int a(int c) {
		return c >>> 24;
	}

	public static int r(int c) {
		return (c >> 16) & 0xFF;
	}

	public static int g(int c) {
		return (c >> 8) & 0xFF;
	}

	public static int b(int c) {
		return c & 0xFF;
	}

	public static int of(int a, int r, int g, int b) {
		return (MathUtil.clamp(a, 0, 255) << 24) | (MathUtil.clamp(r, 0, 255) << 16) | (MathUtil.clamp(g, 0, 255) << 8) | MathUtil.clamp(b, 0, 255);
	}

	public static int rgb(int rgb) {
		return 0xFF000000 | (rgb & 0xFFFFFF);
	}

	public static int ofFloat(float a, float r, float g, float b) {
		return of(Math.round(a * 255f), Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f));
	}

	/** Replaces the alpha channel. */
	public static int withAlpha(int c, float alpha) {
		return (MathUtil.clamp(Math.round(alpha * 255f), 0, 255) << 24) | (c & 0xFFFFFF);
	}

	/** Multiplies the existing alpha channel. */
	public static int mulAlpha(int c, float factor) {
		if (factor >= 1f) {
			return c;
		}
		int a = Math.round(a(c) * MathUtil.clamp01(factor));
		return (a << 24) | (c & 0xFFFFFF);
	}

	public static int lerp(int c0, int c1, float t) {
		if (t <= 0f) {
			return c0;
		}
		if (t >= 1f) {
			return c1;
		}
		int a = Math.round(MathUtil.lerp(a(c0), a(c1), t));
		int r = Math.round(MathUtil.lerp(r(c0), r(c1), t));
		int g = Math.round(MathUtil.lerp(g(c0), g(c1), t));
		int b = Math.round(MathUtil.lerp(b(c0), b(c1), t));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** Interpolates in OKLab, which keeps mid-points vivid instead of muddy. Alpha is interpolated linearly. */
	public static int lerpOklab(int c0, int c1, float t) {
		if (t <= 0f) {
			return c0;
		}
		if (t >= 1f) {
			return c1;
		}
		float[] l0 = Oklab.fromArgb(c0);
		float[] l1 = Oklab.fromArgb(c1);
		float l = MathUtil.lerp(l0[0], l1[0], t);
		float aa = MathUtil.lerp(l0[1], l1[1], t);
		float bb = MathUtil.lerp(l0[2], l1[2], t);
		float alpha = MathUtil.lerp(a(c0), a(c1), t) / 255f;
		return Oklab.toArgb(l, aa, bb, alpha);
	}

	/** Mixes toward white (amount > 0) or black (amount < 0). */
	public static int shade(int c, float amount) {
		int target = amount >= 0f ? WHITE : BLACK;
		int mixed = lerp(c | 0xFF000000, target, Math.abs(amount));
		return (c & 0xFF000000) | (mixed & 0xFFFFFF);
	}

	/** Relative luminance (sRGB, 0..1). */
	public static float luminance(int c) {
		return 0.2126f * lin(r(c)) + 0.7152f * lin(g(c)) + 0.0722f * lin(b(c));
	}

	private static float lin(int channel) {
		float v = channel / 255f;
		return v <= 0.04045f ? v / 12.92f : (float) Math.pow((v + 0.055f) / 1.055f, 2.4f);
	}

	/** Parses "#RGB", "#RRGGBB" or "#AARRGGBB"; returns {@code fallback} when malformed. */
	public static int parse(String s, int fallback) {
		if (s == null) {
			return fallback;
		}
		String t = s.trim();
		if (t.startsWith("#")) {
			t = t.substring(1);
		} else if (t.startsWith("0x") || t.startsWith("0X")) {
			t = t.substring(2);
		}
		try {
			if (t.length() == 3) {
				int r = Integer.parseInt(t.substring(0, 1), 16) * 17;
				int g = Integer.parseInt(t.substring(1, 2), 16) * 17;
				int b = Integer.parseInt(t.substring(2, 3), 16) * 17;
				return of(255, r, g, b);
			}
			if (t.length() == 6) {
				return 0xFF000000 | Integer.parseInt(t, 16);
			}
			if (t.length() == 8) {
				return (int) Long.parseLong(t, 16);
			}
		} catch (NumberFormatException ignored) {
			// fall through
		}
		return fallback;
	}

	public static String format(int c) {
		if (a(c) == 255) {
			return String.format("#%06X", c & 0xFFFFFF);
		}
		return String.format("#%08X", c);
	}
}
