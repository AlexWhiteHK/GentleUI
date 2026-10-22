package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Setting row: label (and optional description) on the left, switch on the right. */
public class Toggle extends Widget {
	private final Supplier<String> label;
	private final Supplier<Boolean> getter;
	private final Consumer<Boolean> setter;
	private final Spring on = new Spring(0f, 260f, 0.78f);
	private final Rect switchRect = new Rect();
	public String description;
	private boolean first = true;

	public Toggle(Supplier<String> label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
		this.label = label;
		this.getter = getter;
		this.setter = setter;
		this.prefHeight = 24f;
	}

	public Toggle description(String d) {
		this.description = d;
		this.prefHeight = 32f;
		return this;
	}

	@Override
	public boolean interactive() {
		return true;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
		if (inside && enabled) {
			activate(ctx);
		}
	}

	@Override
	public void activate(UiContext ctx) {
		setter.accept(!getter.get());
		ctx.host.playSound(UiSound.TOGGLE);
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		boolean value = getter.get();
		if (first) {
			on.snap(value ? 1f : 0f);
			first = false;
		}
		on.target(value ? 1f : 0f).update(ctx.dt);
		if (state.hover > 0.01f) {
			ctx.skin().selection(c, bounds, state.hover * 0.6f, false);
		}
		float sw = 26f;
		float sh = 14f;
		switchRect.set(bounds.right() - sw - 6f, bounds.cy() - sh * 0.5f, sw, sh);
		state.active = MathUtil.clamp(on.value(), -0.2f, 1.2f);
		ctx.skin().toggle(c, switchRect, state);
		int text = enabled ? ctx.palette().text : ctx.palette().textDisabled;
		float maxW = bounds.w - sw - 22f;
		if (description == null) {
			c.text(c.ellipsize(label.get(), maxW, 1f), bounds.x + 8f, bounds.cy() - c.fontHeight() * 0.5f, text, 1f, Align.LEFT, false);
		} else {
			c.text(c.ellipsize(label.get(), maxW, 1f), bounds.x + 8f, bounds.y + 5f, text, 1f, Align.LEFT, false);
			c.text(c.ellipsize(description, maxW / 0.85f, 0.85f), bounds.x + 8f, bounds.y + 17f, ctx.palette().textMuted, 0.85f, Align.LEFT, false);
		}
		if (state.focus > 0f) {
			ctx.skin().focusRing(c, bounds, 6f, state.focus);
		}
	}
}
