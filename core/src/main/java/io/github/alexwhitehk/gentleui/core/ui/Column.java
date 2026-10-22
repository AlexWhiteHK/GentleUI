package io.github.alexwhitehk.gentleui.core.ui;

/** Lays children out top to bottom inside its bounds. */
public class Column extends Widget {
	public float gap = 4f;
	public float padding;

	public Column gap(float g) {
		this.gap = g;
		return this;
	}

	public float contentHeight() {
		float h = padding * 2f;
		int n = 0;
		for (Widget c : children) {
			if (c.visible) {
				h += (c.prefHeight > 0f ? c.prefHeight : c.bounds.h);
				n++;
			}
		}
		return h + Math.max(0, n - 1) * gap;
	}

	@Override
	public void layout() {
		float y = bounds.y + padding;
		for (Widget c : children) {
			if (!c.visible) {
				continue;
			}
			float h = c.prefHeight > 0f ? c.prefHeight : c.bounds.h;
			c.bounds.set(bounds.x + padding, y, bounds.w - padding * 2f, h);
			c.layout();
			y += h + gap;
		}
	}
}
