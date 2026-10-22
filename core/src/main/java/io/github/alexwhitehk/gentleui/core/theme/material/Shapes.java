package io.github.alexwhitehk.gentleui.core.theme.material;

import io.github.alexwhitehk.gentleui.core.math.MathUtil;

/**
 * Soft geometric outlines inspired by expressive design systems: scalloped "cookies", petals, squircles and
 * rounded polygons. Every outline is star-shaped around its center so it can be filled as a fan.
 */
public final class Shapes {
	public enum Kind {
		CIRCLE,
		COOKIE,
		FLOWER,
		SQUIRCLE,
		TRIANGLE,
		CLOVER,
		PILL,
		BURST
	}

	private Shapes() {
	}

	/**
	 * Writes {@code segments} outline points into {@code out} (interleaved x, y) for a shape of the given kind,
	 * centered at (cx, cy) with outer radius r and rotation in radians.
	 */
	public static void outline(Kind kind, float cx, float cy, float r, float rotation, int segments, float[] out) {
		for (int i = 0; i < segments; i++) {
			float a = MathUtil.TAU * i / segments;
			float rr = r * radius(kind, a);
			float x;
			float y;
			if (kind == Kind.PILL) {
				// Stadium: two half circles joined by straight edges, aspect 2:1.
				float hw = r;
				float hh = r * 0.5f;
				float ca = MathUtil.cos(a);
				float sa = MathUtil.sin(a);
				float t = stadium(ca, sa, hw, hh);
				x = ca * t;
				y = sa * t;
			} else {
				x = MathUtil.cos(a) * rr;
				y = MathUtil.sin(a) * rr;
			}
			float c = MathUtil.cos(rotation);
			float s = MathUtil.sin(rotation);
			out[i * 2] = cx + x * c - y * s;
			out[i * 2 + 1] = cy + x * s + y * c;
		}
	}

	private static float radius(Kind kind, float a) {
		switch (kind) {
			case COOKIE:
				return 0.92f + 0.08f * MathUtil.cos(9f * a);
			case FLOWER: {
				float k = Math.abs(MathUtil.cos(4f * a));
				return 0.72f + 0.28f * (float) Math.pow(k, 0.6);
			}
			case SQUIRCLE: {
				float n = 4.5f;
				float ca = Math.abs(MathUtil.cos(a));
				float sa = Math.abs(MathUtil.sin(a));
				return (float) Math.pow(Math.pow(ca, n) + Math.pow(sa, n), -1.0 / n) * 0.82f;
			}
			case TRIANGLE: {
				// Rounded triangle via a low-order Fourier approximation.
				return 0.78f + 0.2f * MathUtil.cos(3f * a) + 0.03f * MathUtil.cos(6f * a);
			}
			case CLOVER:
				return 0.68f + 0.32f * Math.abs(MathUtil.cos(2f * a));
			case BURST:
				return 0.8f + 0.2f * (float) Math.pow(Math.abs(MathUtil.cos(6f * a)), 2.5);
			default:
				return 1f;
		}
	}

	private static float stadium(float ca, float sa, float hw, float hh) {
		// Distance from center to the stadium boundary along direction (ca, sa), found by bisection.
		float lo = 0f;
		float hi = hw + hh;
		for (int i = 0; i < 18; i++) {
			float mid = (lo + hi) * 0.5f;
			float x = ca * mid;
			float y = sa * mid;
			float dx = Math.max(Math.abs(x) - (hw - hh), 0f);
			boolean inside = dx * dx + y * y <= hh * hh;
			if (inside) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		return lo;
	}
}
