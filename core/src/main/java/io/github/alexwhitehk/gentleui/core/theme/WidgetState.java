package io.github.alexwhitehk.gentleui.core.theme;

/** Animated interaction state handed to painters. All values are already eased into 0..1. */
public final class WidgetState {
	public float hover;
	public float press;
	public float focus;
	/** Selection/checked/on progress for toggles, tabs and checkboxes. */
	public float active;
	public boolean enabled = true;
	/** Seconds, for subtle idle motion. */
	public float time;
	/** Stable per-widget seed in [0, 1) for staggered or varied effects. */
	public float seed;
	/** Press ripple origin relative to the widget's top-left corner, and its progress (0 = none, 0..1 running). */
	public float rippleX;
	public float rippleY;
	public float ripple;

	public WidgetState set(float hover, float press, float focus, float active, boolean enabled) {
		this.hover = hover;
		this.press = press;
		this.focus = focus;
		this.active = active;
		this.enabled = enabled;
		return this;
	}

	public WidgetState reset() {
		ripple = 0f;
		return set(0f, 0f, 0f, 0f, true);
	}
}
