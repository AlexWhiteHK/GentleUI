package io.github.alexwhitehk.gentleui.core.anim;

/**
 * Damped harmonic spring integrated analytically per step, so it stays stable at any frame rate.
 * Springs are the default motion primitive: interrupted animations keep their velocity instead of jumping.
 */
public final class Spring {
	private float value;
	private float velocity;
	private float target;
	private float stiffness;
	private float damping;

	/**
	 * @param stiffness angular frequency squared (higher = faster); ~170 is snappy, ~60 is soft
	 * @param dampingRatio 1 = critically damped, below 1 overshoots
	 */
	public Spring(float initial, float stiffness, float dampingRatio) {
		this.value = initial;
		this.target = initial;
		this.stiffness = stiffness;
		this.damping = dampingRatio;
	}

	public static Spring snappy(float initial) {
		return new Spring(initial, 260f, 0.9f);
	}

	public static Spring gentle(float initial) {
		return new Spring(initial, 90f, 1f);
	}

	public static Spring bouncy(float initial) {
		return new Spring(initial, 180f, 0.62f);
	}

	public Spring configure(float stiffness, float dampingRatio) {
		this.stiffness = stiffness;
		this.damping = dampingRatio;
		return this;
	}

	public float value() {
		return value;
	}

	public float velocity() {
		return velocity;
	}

	public float target() {
		return target;
	}

	public Spring target(float target) {
		this.target = target;
		return this;
	}

	public Spring snap(float v) {
		this.value = v;
		this.target = v;
		this.velocity = 0f;
		return this;
	}

	public Spring impulse(float dv) {
		this.velocity += dv;
		return this;
	}

	public boolean settled() {
		return Math.abs(value - target) < 1e-3f && Math.abs(velocity) < 1e-3f;
	}

	public float update(float dt) {
		if (dt <= 0f) {
			return value;
		}
		// Sub-step large frames (e.g. after a hitch) to keep the integration accurate.
		float remaining = Math.min(dt, 0.25f);
		while (remaining > 0f) {
			float step = Math.min(remaining, 1f / 120f);
			integrate(step);
			remaining -= step;
		}
		if (settled()) {
			value = target;
			velocity = 0f;
		}
		return value;
	}

	private void integrate(float dt) {
		float omega = (float) Math.sqrt(stiffness);
		float zeta = damping;
		float x = value - target;
		float v = velocity;
		if (zeta < 1f) {
			float wd = omega * (float) Math.sqrt(1f - zeta * zeta);
			float e = (float) Math.exp(-zeta * omega * dt);
			float c = (float) Math.cos(wd * dt);
			float s = (float) Math.sin(wd * dt);
			float a = x;
			float b = (v + zeta * omega * x) / wd;
			float nx = e * (a * c + b * s);
			float nv = e * ((b * wd - zeta * omega * a) * c - (a * wd + zeta * omega * b) * s);
			value = target + nx;
			velocity = nv;
		} else {
			// Critically damped (over-damped ratios are treated as critical for simplicity).
			float e = (float) Math.exp(-omega * dt);
			float a = x;
			float b = v + omega * x;
			float nx = (a + b * dt) * e;
			float nv = (b - omega * (a + b * dt)) * e;
			value = target + nx;
			velocity = nv;
		}
	}
}
