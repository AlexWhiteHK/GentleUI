package io.github.alexwhitehk.gentleui.core.ui;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.host.UiSound;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.render.Align;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** 3x3 grid of reference points; the chosen one is highlighted. Indices run row by row from the top left. */
public class AnchorPicker extends Widget {
	private final Supplier<String> label;
	private final Supplier<Integer> getter;
	private final Consumer<Integer> setter;
	private final float[] hover = new float[9];

	public AnchorPicker(Supplier<String> label, Supplier<Integer> getter, Consumer<Integer> setter) {
		this.label = label;
		this.getter = getter;
		this.setter = setter;
		this.prefHeight = 46f;
	}

	@Override
	public boolean interactive() {
		return true;
	}

	private float gridX() {
		return bounds.right() - 8f - 36f;
	}

	private int indexAt(float sx, float sy) {
		float lx = sx - (screenX() + (gridX() - bounds.x));
		float ly = sy - (screenY() + 5f);
		int col = (int) Math.floor(lx / 12f);
		int row = (int) Math.floor(ly / 12f);
		return col >= 0 && col < 3 && row >= 0 && row < 3 ? row * 3 + col : -1;
	}

	@Override
	public void mouseUp(UiContext ctx, float x, float y, int button, boolean inside) {
		int i = indexAt(x, y);
		if (inside && i >= 0 && i != getter.get()) {
			setter.accept(i);
			ctx.host.playSound(UiSound.TOGGLE);
		}
	}

	@Override
	protected void paint(UiContext ctx) {
		Canvas c = ctx.canvas;
		c.text(label.get(), bounds.x + 8f, bounds.y + 5f, ctx.palette().text, 1f, Align.LEFT, false);
		float gx = gridX();
		float gy = bounds.y + 5f;
		c.fillRoundRect(gx - 2f, gy - 2f, 40f, 40f, 6f, Argb.withAlpha(ctx.palette().text, 0.06f));
		int hi = isHovered(ctx) ? indexAt(ctx.mouseX, ctx.mouseY) : -1;
		int sel = getter.get();
		for (int i = 0; i < 9; i++) {
			hover[i] = MathUtil.approach(hover[i], i == hi ? 1f : 0f, 0.05f, ctx.dt);
			float cx = gx + (i % 3) * 12f + 6f;
			float cy = gy + (i / 3) * 12f + 6f;
			if (i == sel) {
				c.fillCircle(cx, cy, 3.6f, ctx.palette().accent);
			} else {
				c.fillCircle(cx, cy, 1.8f + 0.8f * hover[i], Argb.withAlpha(ctx.palette().textMuted, 0.6f + 0.4f * hover[i]));
			}
		}
	}
}
