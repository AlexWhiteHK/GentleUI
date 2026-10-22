package io.github.alexwhitehk.gentleui.core.math;

/**
 * Easing curves. Every curve maps [0, 1] to [0, 1] (overshooting curves may leave the range in between).
 */
@FunctionalInterface
public interface Easing {
	float apply(float t);

	Easing LINEAR = t -> t;
	Easing IN_QUAD = t -> t * t;
	Easing OUT_QUAD = t -> 1f - (1f - t) * (1f - t);
	Easing IN_OUT_QUAD = t -> t < 0.5f ? 2f * t * t : 1f - (float) Math.pow(-2f * t + 2f, 2) / 2f;
	Easing IN_CUBIC = t -> t * t * t;
	Easing OUT_CUBIC = t -> 1f - (float) Math.pow(1f - t, 3);
	Easing IN_OUT_CUBIC = t -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
	Easing OUT_QUART = t -> 1f - (float) Math.pow(1f - t, 4);
	Easing OUT_QUINT = t -> 1f - (float) Math.pow(1f - t, 5);
	Easing IN_OUT_QUINT = t -> t < 0.5f ? 16f * t * t * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 5) / 2f;
	Easing OUT_EXPO = t -> t >= 1f ? 1f : 1f - (float) Math.pow(2, -10f * t);
	Easing IN_OUT_EXPO = t -> {
		if (t <= 0f) {
			return 0f;
		}
		if (t >= 1f) {
			return 1f;
		}
		return t < 0.5f ? (float) Math.pow(2, 20f * t - 10f) / 2f : (2f - (float) Math.pow(2, -20f * t + 10f)) / 2f;
	};
	Easing OUT_SINE = t -> (float) Math.sin(t * Math.PI / 2.0);
	Easing IN_OUT_SINE = t -> -((float) Math.cos(Math.PI * t) - 1f) / 2f;
	Easing OUT_BACK = t -> {
		float c1 = 1.70158f;
		float c3 = c1 + 1f;
		float u = t - 1f;
		return 1f + c3 * u * u * u + c1 * u * u;
	};
	Easing OUT_BACK_SOFT = t -> {
		float c1 = 0.9f;
		float c3 = c1 + 1f;
		float u = t - 1f;
		return 1f + c3 * u * u * u + c1 * u * u;
	};
	/** Material "emphasized decelerate" approximation. */
	Easing EMPHASIZED = t -> {
		float u = 1f - t;
		return 1f - u * u * u * u * (1f - 0.2f * t);
	};

	/** Cubic bezier easing defined like CSS {@code cubic-bezier(x1, y1, x2, y2)}. */
	static Easing bezier(float x1, float y1, float x2, float y2) {
		return t -> {
			if (t <= 0f) {
				return 0f;
			}
			if (t >= 1f) {
				return 1f;
			}
			// Solve x(s) = t with Newton iterations, falling back to bisection.
			float s = t;
			for (int i = 0; i < 8; i++) {
				float x = bezierComponent(s, x1, x2) - t;
				float dx = bezierDerivative(s, x1, x2);
				if (Math.abs(x) < 1e-5f) {
					return bezierComponent(s, y1, y2);
				}
				if (Math.abs(dx) < 1e-6f) {
					break;
				}
				s -= x / dx;
			}
			float lo = 0f;
			float hi = 1f;
			s = t;
			for (int i = 0; i < 24; i++) {
				float x = bezierComponent(s, x1, x2);
				if (Math.abs(x - t) < 1e-5f) {
					break;
				}
				if (x < t) {
					lo = s;
				} else {
					hi = s;
				}
				s = (lo + hi) * 0.5f;
			}
			return bezierComponent(s, y1, y2);
		};
	}

	private static float bezierComponent(float s, float p1, float p2) {
		float u = 1f - s;
		return 3f * u * u * s * p1 + 3f * u * s * s * p2 + s * s * s;
	}

	private static float bezierDerivative(float s, float p1, float p2) {
		float u = 1f - s;
		return 3f * u * u * p1 + 6f * u * s * (p2 - p1) + 3f * s * s * (1f - p2);
	}
}
