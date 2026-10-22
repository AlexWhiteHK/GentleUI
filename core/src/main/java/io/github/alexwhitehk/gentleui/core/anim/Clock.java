package io.github.alexwhitehk.gentleui.core.anim;

/** Monotonic frame clock. One instance per screen; {@link #tick()} once per rendered frame. */
public final class Clock {
	private long last = -1L;
	private float delta;
	private double time;

	public float tick() {
		long now = System.nanoTime();
		if (last < 0L) {
			delta = 0f;
		} else {
			delta = Math.min((now - last) / 1_000_000_000f, 0.1f);
		}
		last = now;
		time += delta;
		return delta;
	}

	/** Real seconds since the previous frame, capped to avoid huge jumps after a hitch. */
	public float delta() {
		return delta;
	}

	/** Animation-scaled delta (see {@link Motion}). */
	public float animDelta() {
		return Motion.scale(delta);
	}

	public float time() {
		return (float) time;
	}
}
