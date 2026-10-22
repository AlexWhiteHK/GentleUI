package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.host.Keys;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.theme.ButtonStyle;
import io.github.alexwhitehk.gentleui.core.theme.WidgetState;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Segmented control: a labelled row of mutually exclusive options with a sliding indicator. */
public class Choice extends Widget {
	private final Supplier<String> label;
	private final List<String> optionLabels;
	private final Supplier<Integer> getter;
	private final Consumer<Integer> setter;
	private final Spring indicator = new Spring(0f, 300f, 0.86f);
	private final Rect seg = new Rect();
	private final Rect ind = new Rect();
	private final WidgetState segState = new WidgetState();
	private boolean first = true;
	private int hoverIndex = -1;

	public Choice(Supplier<String> label, List<String> optionLabels, Supplier<Integer> getter, Consumer<Integer> setter) {
		this.label = label;
		this.optionLabels = optionLabels;
		this.getter = getter;
		this.setter = setter;
		this.prefHeight = label == null ? 22f : 38f;
	}

	@Override
	public boolean interactive() {
		return true;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	private float segTop() {
		return label == null ? 0f : 16f;
	}

	private int indexAt(float x) {
		float sx = screenX() + 8f;
		float w = (bounds.w - 16f) / optionLabels.size();
		int i = (int) ((x - sx) / w);
		return i < 0 || i >= optionLabels.size() ? -1 : i;
	}

	@Override
	public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
		int i = indexAt(x);
		if (inside && i >= 0 && i != getter.get()) {
			setter.accept(i);
			ctx.host.playSound(UiSound.TOGGLE);
		}
	}

	@Override
	public boolean keyDown(UiContext ctx, int key, int mods) {
		int n = optionLabels.size();
		if (key == Keys.LEFT || key == Keys.RIGHT) {
			int i = Math.floorMod(getter.get() + (key == Keys.LEFT ? -1 : 1), n);
			setter.accept(i);
			ctx.host.playSound(UiSound.TOGGLE);
			return true;
		}
		return false;
	}

	@Override
	public void activate(UiContext ctx) {
		setter.accept((getter.get() + 1) % optionLabels.size());
		ctx.host.playSound(UiSound.TOGGLE);
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		int n = optionLabels.size();
		int sel = Math.max(0, Math.min(n - 1, getter.get()));
		if (first) {
			indicator.snap(sel);
			first = false;
		}
		indicator.target(sel).update(ctx.dt);
		if (label != null) {
			c.text(label.get(), bounds.x + 8f, bounds.y + 3f, enabled ? ctx.palette().text : ctx.palette().textDisabled, 1f, Align.LEFT, false);
		}
		seg.set(bounds.x + 8f, bounds.y + segTop(), bounds.w - 16f, 20f);
		segState.reset();
		ctx.skin().textField(c, seg, segState);
		float w = seg.w / n;
		ind.set(seg.x + indicator.value() * w + 2f, seg.y + 2f, w - 4f, seg.h - 4f);
		segState.set(0f, 0f, 0f, 1f, true);
		ctx.skin().button(c, ind, ButtonStyle.PRIMARY, segState);
		hoverIndex = isHovered(ctx) ? indexAt(ctx.mouseX) : -1;
		for (int i = 0; i < n; i++) {
			float cx = seg.x + w * (i + 0.5f);
			boolean on = i == sel;
			segState.set(i == hoverIndex ? 1f : 0f, 0f, 0f, on ? 1f : 0f, true);
			int color = on ? ctx.skin().buttonText(ButtonStyle.PRIMARY, segState) : (i == hoverIndex ? ctx.palette().text : ctx.palette().textMuted);
			c.text(c.ellipsize(optionLabels.get(i), w - 6f, 1f), cx, seg.cy() - c.fontHeight() * 0.5f + 0.5f, color, 1f, Align.CENTER, false);
		}
		if (state.focus > 0f) {
			ctx.skin().focusRing(c, seg, 6f, state.focus);
		}
	}
}
