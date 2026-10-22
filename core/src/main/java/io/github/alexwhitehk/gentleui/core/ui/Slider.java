package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** Labelled slider row. The value springs toward the pointer so dragging feels fluid. */
public class Slider extends Widget {
	private final Supplier<String> label;
	private final float min;
	private final float max;
	private final float step;
	private final Supplier<Float> getter;
	private final Consumer<Float> setter;
	private final Function<Float, String> format;
	private final Spring shown = new Spring(0f, 400f, 1f);
	private final Rect track = new Rect();
	private boolean first = true;

	public Slider(Supplier<String> label, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter, Function<Float, String> format) {
		this.label = label;
		this.min = min;
		this.max = max;
		this.step = step;
		this.getter = getter;
		this.setter = setter;
		this.format = format;
		this.prefHeight = 34f;
	}

	@Override
	public boolean interactive() {
		return true;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	private float norm(float v) {
		return MathUtil.clamp01((v - min) / (max - min));
	}

	private void setFromPointer(float x) {
		float sx = screenX() + (track.x - bounds.x);
		float t = MathUtil.clamp01((x - sx) / track.w);
		float v = min + t * (max - min);
		if (step > 0f) {
			v = Math.round(v / step) * step;
		}
		setter.accept(MathUtil.clamp(v, min, max));
	}

	@Override
	public boolean mouseDown(UiContext ctx, float x, float y, int button) {
		setFromPointer(x);
		return true;
	}

	@Override
	public void mouseDragged(UiContext ctx, float x, float y, float dx, float dy) {
		setFromPointer(x);
	}

	@Override
	public boolean keyDown(UiContext ctx, int key, int mods) {
		float delta = step > 0f ? step : (max - min) / 20f;
		if (key == Keys.LEFT) {
			setter.accept(MathUtil.clamp(getter.get() - delta, min, max));
			return true;
		}
		if (key == Keys.RIGHT) {
			setter.accept(MathUtil.clamp(getter.get() + delta, min, max));
			return true;
		}
		return false;
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		float v = norm(getter.get());
		if (first) {
			shown.snap(v);
			first = false;
		}
		shown.target(v).update(ctx.dt);
		int text = enabled ? ctx.palette().text : ctx.palette().textDisabled;
		c.text(label.get(), bounds.x + 8f, bounds.y + 4f, text, 1f, Align.LEFT, false);
		c.text(format.apply(getter.get()), bounds.right() - 8f, bounds.y + 4f, ctx.palette().textMuted, 1f, Align.RIGHT, false);
		track.set(bounds.x + 8f, bounds.y + 18f, bounds.w - 16f, 12f);
		state.active = isPressed(ctx) ? 1f : 0f;
		ctx.skin().slider(c, track, MathUtil.clamp01(shown.value()), state);
		if (state.focus > 0f) {
			ctx.skin().focusRing(c, bounds, 6f, state.focus);
		}
	}
}
