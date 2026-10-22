package io.github.alexwhitehk.gentleui.core.math;

/**
 * Compact 2D/3D simplex noise (after Stefan Gustavson's public domain reference) used for organic motion.
 * Output range is roughly [-1, 1].
 */
public final class Noise {
	private static final int[] PERM = new int[512];
	private static final float[][] GRAD3 = {
		{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
		{1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
		{0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}
	};

	static {
		int[] p = new int[256];
		for (int i = 0; i < 256; i++) {
			p[i] = i;
		}
		// Fixed shuffle so that visuals are identical between runs.
		long seed = 0x5DEECE66DL;
		for (int i = 255; i > 0; i--) {
			seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
			int j = (int) ((seed >>> 17) % (i + 1));
			int tmp = p[i];
			p[i] = p[j];
			p[j] = tmp;
		}
		for (int i = 0; i < 512; i++) {
			PERM[i] = p[i & 255];
		}
	}

	private Noise() {
	}

	private static int fastFloor(float x) {
		int xi = (int) x;
		return x < xi ? xi - 1 : xi;
	}

	public static float noise2(float xin, float yin) {
		final float f2 = 0.5f * ((float) Math.sqrt(3.0) - 1f);
		final float g2 = (3f - (float) Math.sqrt(3.0)) / 6f;
		float s = (xin + yin) * f2;
		int i = fastFloor(xin + s);
		int j = fastFloor(yin + s);
		float t = (i + j) * g2;
		float x0 = xin - (i - t);
		float y0 = yin - (j - t);
		int i1 = x0 > y0 ? 1 : 0;
		int j1 = x0 > y0 ? 0 : 1;
		float x1 = x0 - i1 + g2;
		float y1 = y0 - j1 + g2;
		float x2 = x0 - 1f + 2f * g2;
		float y2 = y0 - 1f + 2f * g2;
		int ii = i & 255;
		int jj = j & 255;
		float n0 = corner2(PERM[ii + PERM[jj]] % 12, x0, y0);
		float n1 = corner2(PERM[ii + i1 + PERM[jj + j1]] % 12, x1, y1);
		float n2 = corner2(PERM[ii + 1 + PERM[jj + 1]] % 12, x2, y2);
		return 70f * (n0 + n1 + n2);
	}

	private static float corner2(int gi, float x, float y) {
		float t = 0.5f - x * x - y * y;
		if (t < 0f) {
			return 0f;
		}
		t *= t;
		return t * t * (GRAD3[gi][0] * x + GRAD3[gi][1] * y);
	}

	public static float noise3(float xin, float yin, float zin) {
		final float f3 = 1f / 3f;
		final float g3 = 1f / 6f;
		float s = (xin + yin + zin) * f3;
		int i = fastFloor(xin + s);
		int j = fastFloor(yin + s);
		int k = fastFloor(zin + s);
		float t = (i + j + k) * g3;
		float x0 = xin - (i - t);
		float y0 = yin - (j - t);
		float z0 = zin - (k - t);
		int i1;
		int j1;
		int k1;
		int i2;
		int j2;
		int k2;
		if (x0 >= y0) {
			if (y0 >= z0) {
				i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
			} else if (x0 >= z0) {
				i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1;
			} else {
				i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1;
			}
		} else {
			if (y0 < z0) {
				i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1;
			} else if (x0 < z0) {
				i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1;
			} else {
				i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
			}
		}
		float x1 = x0 - i1 + g3;
		float y1 = y0 - j1 + g3;
		float z1 = z0 - k1 + g3;
		float x2 = x0 - i2 + 2f * g3;
		float y2 = y0 - j2 + 2f * g3;
		float z2 = z0 - k2 + 2f * g3;
		float x3 = x0 - 1f + 3f * g3;
		float y3 = y0 - 1f + 3f * g3;
		float z3 = z0 - 1f + 3f * g3;
		int ii = i & 255;
		int jj = j & 255;
		int kk = k & 255;
		float n0 = corner3(PERM[ii + PERM[jj + PERM[kk]]] % 12, x0, y0, z0);
		float n1 = corner3(PERM[ii + i1 + PERM[jj + j1 + PERM[kk + k1]]] % 12, x1, y1, z1);
		float n2 = corner3(PERM[ii + i2 + PERM[jj + j2 + PERM[kk + k2]]] % 12, x2, y2, z2);
		float n3 = corner3(PERM[ii + 1 + PERM[jj + 1 + PERM[kk + 1]]] % 12, x3, y3, z3);
		return 32f * (n0 + n1 + n2 + n3);
	}

	private static float corner3(int gi, float x, float y, float z) {
		float t = 0.6f - x * x - y * y - z * z;
		if (t < 0f) {
			return 0f;
		}
		t *= t;
		return t * t * (GRAD3[gi][0] * x + GRAD3[gi][1] * y + GRAD3[gi][2] * z);
	}

	/** Fractal sum of two octaves, good enough for gentle drifting motion. */
	public static float fbm2(float x, float y) {
		return noise2(x, y) * 0.66f + noise2(x * 2.07f + 13.1f, y * 2.07f - 7.3f) * 0.34f;
	}
}
