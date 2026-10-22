package io.github.alexwhitehk.gentleui.core.math;

/** Mutable float rectangle in GUI coordinates. */
public final class Rect {
	public float x;
	public float y;
	public float w;
	public float h;

	public Rect() {
	}

	public Rect(float x, float y, float w, float h) {
		set(x, y, w, h);
	}

	public Rect set(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		return this;
	}

	public Rect set(Rect other) {
		return set(other.x, other.y, other.w, other.h);
	}

	public float right() {
		return x + w;
	}

	public float bottom() {
		return y + h;
	}

	public float cx() {
		return x + w * 0.5f;
	}

	public float cy() {
		return y + h * 0.5f;
	}

	public boolean contains(float px, float py) {
		return px >= x && py >= y && px < x + w && py < y + h;
	}

	public Rect copy() {
		return new Rect(x, y, w, h);
	}

	public Rect inset(float d) {
		return new Rect(x + d, y + d, Math.max(0f, w - 2f * d), Math.max(0f, h - 2f * d));
	}

	public Rect expand(float d) {
		return inset(-d);
	}

	@Override
	public String toString() {
		return "Rect[" + x + ", " + y + ", " + w + "x" + h + "]";
	}
}
