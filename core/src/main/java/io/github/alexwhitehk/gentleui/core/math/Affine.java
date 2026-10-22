package io.github.alexwhitehk.gentleui.core.math;

/**
 * 2D affine transform {@code [m00 m01 m02; m10 m11 m12]} mapping (x, y) to
 * (m00*x + m01*y + m02, m10*x + m11*y + m12).
 */
public final class Affine {
	public float m00 = 1f;
	public float m01;
	public float m02;
	public float m10;
	public float m11 = 1f;
	public float m12;

	public Affine identity() {
		m00 = 1f;
		m01 = 0f;
		m02 = 0f;
		m10 = 0f;
		m11 = 1f;
		m12 = 0f;
		return this;
	}

	public Affine set(Affine o) {
		m00 = o.m00;
		m01 = o.m01;
		m02 = o.m02;
		m10 = o.m10;
		m11 = o.m11;
		m12 = o.m12;
		return this;
	}

	public boolean isIdentity() {
		return m00 == 1f && m01 == 0f && m02 == 0f && m10 == 0f && m11 == 1f && m12 == 0f;
	}

	public Affine translate(float x, float y) {
		m02 += m00 * x + m01 * y;
		m12 += m10 * x + m11 * y;
		return this;
	}

	public Affine scale(float sx, float sy) {
		m00 *= sx;
		m10 *= sx;
		m01 *= sy;
		m11 *= sy;
		return this;
	}

	public Affine rotate(float radians) {
		float c = MathUtil.cos(radians);
		float s = MathUtil.sin(radians);
		float a00 = m00 * c + m01 * s;
		float a01 = -m00 * s + m01 * c;
		float a10 = m10 * c + m11 * s;
		float a11 = -m10 * s + m11 * c;
		m00 = a00;
		m01 = a01;
		m10 = a10;
		m11 = a11;
		return this;
	}

	public float tx(float x, float y) {
		return m00 * x + m01 * y + m02;
	}

	public float ty(float x, float y) {
		return m10 * x + m11 * y + m12;
	}

	/** Uniform scale factor of the linear part (geometric mean), used to size anti-aliasing fringes. */
	public float scaleFactor() {
		float det = Math.abs(m00 * m11 - m01 * m10);
		return det <= 0f ? 1f : (float) Math.sqrt(det);
	}

	/** Maps a point from transformed space back into local space. */
	public float inverseX(float x, float y) {
		float det = m00 * m11 - m01 * m10;
		if (det == 0f) {
			return x;
		}
		float dx = x - m02;
		float dy = y - m12;
		return (m11 * dx - m01 * dy) / det;
	}

	public float inverseY(float x, float y) {
		float det = m00 * m11 - m01 * m10;
		if (det == 0f) {
			return y;
		}
		float dx = x - m02;
		float dy = y - m12;
		return (-m10 * dx + m00 * dy) / det;
	}
}
