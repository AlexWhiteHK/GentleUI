package io.github.alexwhitehk.gentleui.core.ui;

/** Lays children out left to right; children with prefWidth 0 share the remaining space equally. */
public class Row extends Widget {
	public float gap = 4f;

	public Row gap(float g) {
		this.gap = g;
		return this;
	}

	@Override
	public void layout() {
		float fixed = 0f;
		int flexible = 0;
		int visibleCount = 0;
		for (Widget c : children) {
			if (!c.visible) {
				continue;
			}
			visibleCount++;
			if (c.prefWidth > 0f) {
				fixed += c.prefWidth;
			} else {
				flexible++;
			}
		}
		float free = bounds.w - fixed - gap * Math.max(0, visibleCount - 1);
		float flexW = flexible > 0 ? Math.max(0f, free / flexible) : 0f;
		float x = bounds.x;
		for (Widget c : children) {
			if (!c.visible) {
				continue;
			}
			float w = c.prefWidth > 0f ? c.prefWidth : flexW;
			c.bounds.set(x, bounds.y, w, bounds.h);
			c.layout();
			x += w + gap;
		}
	}
}
