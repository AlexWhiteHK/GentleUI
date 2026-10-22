package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;

/** Vertical scroll container with spring-smoothed scrolling and an auto-hiding scrollbar. */
public class ScrollPanel extends Widget {
	private final Spring scroll = new Spring(0f, 240f, 1f);
	private float target;
	private float contentHeight;
	private float barVisibility;
	private boolean draggingBar;
	private final Rect track = new Rect();
	private final Rect thumb = new Rect();
	public float padding = 4f;

	@Override
	protected boolean clipsChildren() {
		return true;
	}

	@Override
	public float contentOffsetY() {
		return scroll.value();
	}

	public float maxScroll() {
		return Math.max(0f, contentHeight - bounds.h);
	}

	public void setContentHeight(float h) {
		this.contentHeight = h;
		target = MathUtil.clamp(target, 0f, maxScroll());
		scroll.target(target);
	}

	public void scrollTo(float y) {
		target = MathUtil.clamp(y, 0f, maxScroll());
		scroll.target(target);
	}

	/** Scrolls so that {@code w} (a descendant) is visible. */
	public void reveal(Widget w) {
		float top = w.bounds.y - bounds.y;
		float bottom = top + w.bounds.h;
		if (top < target) {
			scrollTo(top - 8f);
		} else if (bottom > target + bounds.h) {
			scrollTo(bottom - bounds.h + 8f);
		}
	}

	/** Stacks children vertically with the given gap, using their preferred heights. */
	public void stack(float gap) {
		float y = bounds.y + padding;
		for (Widget c : children) {
			if (!c.visible) {
				continue;
			}
			float h = c.prefHeight > 0f ? c.prefHeight : c.bounds.h;
			c.bounds.set(bounds.x + padding, y, bounds.w - padding * 2f - 6f, h);
			c.layout();
			y += h + gap;
		}
		setContentHeight(y - bounds.y + padding - gap);
	}

	@Override
	public boolean interactive() {
		return false;
	}

	@Override
	public Widget hit(float x, float y) {
		Widget h = super.hit(x, y);
		if (h == null && maxScroll() > 0f && bounds.contains(x, y) && x > bounds.right() - 8f) {
			return this;
		}
		return h;
	}

	@Override
	public boolean mouseDown(UiContext ctx, float x, float y, int button) {
		draggingBar = true;
		jumpTo(y);
		return true;
	}

	@Override
	public void mouseDragged(UiContext ctx, float x, float y, float dx, float dy) {
		if (draggingBar) {
			jumpTo(y);
		}
	}

	@Override
	public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
		draggingBar = false;
	}

	private void jumpTo(float y) {
		float t = MathUtil.clamp01((y - screenY()) / bounds.h);
		scrollTo(t * maxScroll());
		scroll.snap(target);
	}

	@Override
	public boolean mouseScrolled(UiContext ctx, float x, float y, float amount) {
		if (maxScroll() <= 0f) {
			return false;
		}
		scrollTo(target - amount * 28f);
		barVisibility = 1f;
		return true;
	}

	@Override
	protected void update(UiContext ctx) {
		super.update(ctx);
		scroll.update(ctx.dt);
		boolean near = bounds.contains(ctx.mouseX - (screenX() - bounds.x), ctx.mouseY - (screenY() - bounds.y));
		float targetVis = maxScroll() > 0f && (near || Math.abs(scroll.velocity()) > 1f) ? 1f : 0f;
		barVisibility = MathUtil.approach(barVisibility, targetVis, targetVis > barVisibility ? 0.05f : 0.35f, ctx.dt);
	}

	@Override
	protected void paintOverlay(UiContext ctx) {
		if (maxScroll() <= 0f || barVisibility < 0.01f) {
			return;
		}
		float th = Math.max(18f, bounds.h * bounds.h / contentHeight);
		float t = MathUtil.clamp01(scroll.value() / maxScroll());
		track.set(bounds.right() - 5f, bounds.y + 3f, 3f, bounds.h - 6f);
		thumb.set(track.x, track.y + (track.h - th) * t, 3f, th);
		ctx.canvas.pushAlpha(barVisibility);
		state.hover = draggingBar ? 1f : state.hover;
		ctx.skin().scrollbar(ctx.canvas, track, thumb, state);
		ctx.canvas.popAlpha();
	}
}
