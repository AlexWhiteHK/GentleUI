package io.github.alexwhitehk.gentleui.core.color;

/** sRGB to OKLab / OKLCH conversions (Björn Ottosson's reference formulas). */
public final class Oklab {
	private Oklab() {
	}

	private static float toLinear(float v) {
		return v <= 0.04045f ? v / 12.92f : (float) Math.pow((v + 0.055f) / 1.055f, 2.4f);
	}

	private static float toGamma(float v) {
		v = Math.max(0f, Math.min(1f, v));
		return v <= 0.0031308f ? v * 12.92f : 1.055f * (float) Math.pow(v, 1f / 2.4f) - 0.055f;
	}

	/** @return {L, a, b} */
	public static float[] fromArgb(int c) {
		float r = toLinear(Argb.r(c) / 255f);
		float g = toLinear(Argb.g(c) / 255f);
		float b = toLinear(Argb.b(c) / 255f);
		float l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
		float m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
		float s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);
		return new float[] {
			0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
			1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
			0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
		};
	}

	public static int toArgb(float lightness, float a, float b, float alpha) {
		float l = lightness + 0.3963377774f * a + 0.2158037573f * b;
		float m = lightness - 0.1055613458f * a - 0.0638541728f * b;
		float s = lightness - 0.0894841775f * a - 1.2914855480f * b;
		l = l * l * l;
		m = m * m * m;
		s = s * s * s;
		float r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
		float g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
		float bb = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
		return Argb.ofFloat(alpha, toGamma(r), toGamma(g), toGamma(bb));
	}

	/** Builds a color from OKLCH: lightness 0..1, chroma ~0..0.37, hue in degrees. */
	public static int lch(float lightness, float chroma, float hueDeg, float alpha) {
		double h = Math.toRadians(hueDeg);
		return toArgb(lightness, chroma * (float) Math.cos(h), chroma * (float) Math.sin(h), alpha);
	}

	/** Returns a copy of {@code c} with its OKLab lightness replaced, keeping hue and chroma. */
	public static int withLightness(int c, float lightness) {
		float[] lab = fromArgb(c);
		return toArgb(lightness, lab[1], lab[2], Argb.a(c) / 255f);
	}
}
