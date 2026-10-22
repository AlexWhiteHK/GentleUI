package io.github.alexwhitehk.gentleui.core.render;

import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import java.util.Arrays;

/**
 * Flattened vector path made of sub-paths. Curves are subdivided when added, so a path is cheap to draw
 * repeatedly; build paths once and reuse them (icons do exactly that).
 */
public final class Path {
	private float[] xs = new float[64];
	private float[] ys = new float[64];
	private int size;
	private int[] starts = new int[8];
	private boolean[] closed = new boolean[8];
	private int subpaths;
	private float curX;
	private float curY;
	private int curveSegments = 12;

	public Path curveSegments(int n) {
		this.curveSegments = Math.max(2, n);
		return this;
	}

	private void add(float x, float y) {
		if (size == xs.length) {
			xs = Arrays.copyOf(xs, size * 2);
			ys = Arrays.copyOf(ys, size * 2);
		}
		xs[size] = x;
		ys[size] = y;
		size++;
		curX = x;
		curY = y;
	}

	public Path moveTo(float x, float y) {
		if (subpaths == starts.length) {
			starts = Arrays.copyOf(starts, subpaths * 2);
			closed = Arrays.copyOf(closed, subpaths * 2);
		}
		starts[subpaths] = size;
		closed[subpaths] = false;
		subpaths++;
		add(x, y);
		return this;
	}

	public Path lineTo(float x, float y) {
		if (subpaths == 0) {
			return moveTo(x, y);
		}
		add(x, y);
		return this;
	}

	public Path quadTo(float cx, float cy, float x, float y) {
		float x0 = curX;
		float y0 = curY;
		for (int i = 1; i <= curveSegments; i++) {
			float t = (float) i / curveSegments;
			float u = 1f - t;
			lineTo(u * u * x0 + 2f * u * t * cx + t * t * x, u * u * y0 + 2f * u * t * cy + t * t * y);
		}
		return this;
	}

	public Path cubicTo(float c1x, float c1y, float c2x, float c2y, float x, float y) {
		float x0 = curX;
		float y0 = curY;
		for (int i = 1; i <= curveSegments; i++) {
			float t = (float) i / curveSegments;
			float u = 1f - t;
			float a = u * u * u;
			float b = 3f * u * u * t;
			float c = 3f * u * t * t;
			float d = t * t * t;
			lineTo(a * x0 + b * c1x + c * c2x + d * x, a * y0 + b * c1y + c * c2y + d * y);
		}
		return this;
	}

	/** Appends an arc; starts a new sub-path when none is open. */
	public Path arc(float cx, float cy, float r, float a0, float a1) {
		int n = Math.max(4, MathUtil.ceilToInt(Math.abs(a1 - a0) / MathUtil.TAU * 40f));
		for (int i = 0; i <= n; i++) {
			float a = a0 + (a1 - a0) * i / n;
			float x = cx + MathUtil.cos(a) * r;
			float y = cy + MathUtil.sin(a) * r;
			if (i == 0 && subpaths == 0) {
				moveTo(x, y);
			} else {
				lineTo(x, y);
			}
		}
		return this;
	}

	public Path close() {
		if (subpaths > 0) {
			closed[subpaths - 1] = true;
		}
		return this;
	}

	public Path circle(float cx, float cy, float r) {
		moveTo(cx + r, cy);
		int n = 40;
		for (int i = 1; i < n; i++) {
			float a = MathUtil.TAU * i / n;
			lineTo(cx + MathUtil.cos(a) * r, cy + MathUtil.sin(a) * r);
		}
		return close();
	}

	public Path rect(float x, float y, float w, float h) {
		return moveTo(x, y).lineTo(x + w, y).lineTo(x + w, y + h).lineTo(x, y + h).close();
	}

	public Path roundRect(float x, float y, float w, float h, float r) {
		r = Math.min(r, Math.min(w, h) * 0.5f);
		moveTo(x + r, y);
		lineTo(x + w - r, y);
		arcNoMove(x + w - r, y + r, r, -MathUtil.PI * 0.5f, 0f);
		lineTo(x + w, y + h - r);
		arcNoMove(x + w - r, y + h - r, r, 0f, MathUtil.PI * 0.5f);
		lineTo(x + r, y + h);
		arcNoMove(x + r, y + h - r, r, MathUtil.PI * 0.5f, MathUtil.PI);
		lineTo(x, y + r);
		arcNoMove(x + r, y + r, r, MathUtil.PI, MathUtil.PI * 1.5f);
		return close();
	}

	private void arcNoMove(float cx, float cy, float r, float a0, float a1) {
		int n = 8;
		for (int i = 1; i <= n; i++) {
			float a = a0 + (a1 - a0) * i / n;
			lineTo(cx + MathUtil.cos(a) * r, cy + MathUtil.sin(a) * r);
		}
	}

	public int subpathCount() {
		return subpaths;
	}

	public int subpathStart(int i) {
		return starts[i];
	}

	public int subpathEnd(int i) {
		return i + 1 < subpaths ? starts[i + 1] : size;
	}

	public boolean subpathClosed(int i) {
		return closed[i];
	}

	public float x(int i) {
		return xs[i];
	}

	public float y(int i) {
		return ys[i];
	}

	public boolean isEmpty() {
		return size == 0;
	}
}
