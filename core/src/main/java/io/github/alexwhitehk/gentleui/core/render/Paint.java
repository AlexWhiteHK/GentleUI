package io.github.alexwhitehk.gentleui.core.render;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;

/**
 * Fill description evaluated per vertex in local (untransformed) coordinates. Gradients are exact for linear
 * paints and radially exact along spokes for radial paints, which is what fan-triangulated shapes need.
 */
public final class Paint {
	static final int SOLID = 0;
	static final int LINEAR = 1;
	static final int RADIAL = 2;

	int type = SOLID;
	int c0 = Argb.WHITE;
	int c1 = Argb.WHITE;
	float x0;
	float y0;
	float x1;
	float y1;
	float radius = 1f;

	public static Paint solid(int color) {
		return new Paint().setSolid(color);
	}

	public static Paint linear(float x0, float y0, int c0, float x1, float y1, int c1) {
		return new Paint().setLinear(x0, y0, c0, x1, y1, c1);
	}

	public static Paint radial(float cx, float cy, float radius, int inner, int outer) {
		return new Paint().setRadial(cx, cy, radius, inner, outer);
	}

	public Paint setSolid(int color) {
		type = SOLID;
		c0 = color;
		c1 = color;
		return this;
	}

	public Paint setLinear(float x0, float y0, int c0, float x1, float y1, int c1) {
		type = LINEAR;
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.c0 = c0;
		this.c1 = c1;
		return this;
	}

	public Paint setRadial(float cx, float cy, float radius, int inner, int outer) {
		type = RADIAL;
		this.x0 = cx;
		this.y0 = cy;
		this.radius = Math.max(1e-3f, radius);
		this.c0 = inner;
		this.c1 = outer;
		return this;
	}

	public Paint set(Paint other) {
		type = other.type;
		c0 = other.c0;
		c1 = other.c1;
		x0 = other.x0;
		y0 = other.y0;
		x1 = other.x1;
		y1 = other.y1;
		radius = other.radius;
		return this;
	}

	public boolean isSolid() {
		return type == SOLID;
	}

	public int eval(float x, float y) {
		switch (type) {
			case LINEAR: {
				float dx = x1 - x0;
				float dy = y1 - y0;
				float len2 = dx * dx + dy * dy;
				float t = len2 <= 0f ? 0f : ((x - x0) * dx + (y - y0) * dy) / len2;
				return Argb.lerp(c0, c1, MathUtil.clamp01(t));
			}
			case RADIAL: {
				float d = MathUtil.dist(x0, y0, x, y) / radius;
				return Argb.lerp(c0, c1, MathUtil.clamp01(d));
			}
			default:
				return c0;
		}
	}
}
