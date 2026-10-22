package io.github.alexwhitehk.gentleui.core.anim;

import io.github.alexwhitehk.gentleui.core.math.Easing;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;

/** Time based 0 to 1 progress with an easing curve. Useful for one-shot, non-interruptible effects. */
public final class Tween {
	private float elapsed;
	private float duration;
	private float delay;
	private Easing easing;
	private boolean running;

	public Tween(float duration, Easing easing) {
		this.duration = duration;
		this.easing = easing;
	}

	public Tween start() {
		return start(0f);
	}

	public Tween start(float delay) {
		this.elapsed = 0f;
		this.delay = delay;
		this.running = true;
		return this;
	}

	public Tween finish() {
		this.elapsed = duration + delay;
		this.running = false;
		return this;
	}

	public Tween duration(float duration) {
		this.duration = duration;
		return this;
	}

	public Tween easing(Easing easing) {
		this.easing = easing;
		return this;
	}

	public void update(float dt) {
		if (!running) {
			return;
		}
		elapsed += dt;
		if (elapsed >= duration + delay) {
			running = false;
		}
	}

	public boolean running() {
		return running;
	}

	public boolean done() {
		return !running && elapsed >= duration + delay;
	}

	public float raw() {
		if (duration <= 0f) {
			return elapsed >= delay ? 1f : 0f;
		}
		return MathUtil.clamp01((elapsed - delay) / duration);
	}

	public float value() {
		return easing.apply(raw());
	}
}
