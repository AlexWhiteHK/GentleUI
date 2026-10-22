package io.github.alexwhitehk.gentleui.core.math;

/** Small scalar helpers shared by animation, layout and tessellation code. */
public final class MathUtil {
	public static final float PI = (float) Math.PI;
	public static final float TAU = (float) (Math.PI * 2.0);

	private MathUtil() {
	}

	public static float clamp(float v, float min, float max) {
		return v < min ? min : (v > max ? max : v);
	}

	public static int clamp(int v, int min, int max) {
		return v < min ? min : (v > max ? max : v);
	}

	public static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	public static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	/** Inverse of {@link #lerp}: where {@code v} lies between {@code a} and {@code b}, unclamped. */
	public static float unlerp(float a, float b, float v) {
		return a == b ? 0f : (v - a) / (b - a);
	}

	public static float remap(float v, float inMin, float inMax, float outMin, float outMax) {
		return lerp(outMin, outMax, unlerp(inMin, inMax, v));
	}

	public static float remapClamped(float v, float inMin, float inMax, float outMin, float outMax) {
		return lerp(outMin, outMax, clamp01(unlerp(inMin, inMax, v)));
	}

	public static float smoothstep(float edge0, float edge1, float x) {
		float t = clamp01((x - edge0) / (edge1 - edge0));
		return t * t * (3f - 2f * t);
	}

	public static float length(float x, float y) {
		return (float) Math.sqrt(x * x + y * y);
	}

	public static float dist(float x0, float y0, float x1, float y1) {
		return length(x1 - x0, y1 - y0);
	}

	public static float sin(float a) {
		return (float) Math.sin(a);
	}

	public static float cos(float a) {
		return (float) Math.cos(a);
	}

	public static float atan2(float y, float x) {
		return (float) Math.atan2(y, x);
	}

	public static float sqrt(float v) {
		return (float) Math.sqrt(v);
	}

	/** Wraps an angle into (-PI, PI]. */
	public static float wrapAngle(float a) {
		a = a % TAU;
		if (a <= -PI) {
			a += TAU;
		} else if (a > PI) {
			a -= TAU;
		}
		return a;
	}

	/** Frame-rate independent exponential approach: the fraction to move this frame for a given half-life. */
	public static float damp(float halfLife, float dt) {
		if (halfLife <= 0f) {
			return 1f;
		}
		return 1f - (float) Math.pow(2.0, -dt / halfLife);
	}

	public static float approach(float current, float target, float halfLife, float dt) {
		return lerp(current, target, damp(halfLife, dt));
	}

	/** Deterministic hash in [0, 1) used to give stable per-item variation without storing random state. */
	public static float hash01(int seed) {
		int h = seed * 0x9E3779B1;
		h ^= h >>> 15;
		h *= 0x85EBCA77;
		h ^= h >>> 13;
		h *= 0xC2B2AE3D;
		h ^= h >>> 16;
		return (h >>> 8) / (float) (1 << 24);
	}

	public static int ceilToInt(float v) {
		return (int) Math.ceil(v);
	}

	public static int floorToInt(float v) {
		return (int) Math.floor(v);
	}
}
