package io.github.alexwhitehk.gentleui.core.render;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.math.Affine;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import io.github.alexwhitehk.gentleui.core.math.Rect;

/**
 * Immediate-mode vector canvas.
 *
 * <p>All geometry is tessellated here into colored quads with a one-physical-pixel feathered fringe, which gives
 * smooth anti-aliased edges using nothing but the platform's plain position+color GUI pipeline. Platforms only
 * implement the small abstract contract at the top of this class (quads, text, images, clipping), so the visual
 * design never depends on a particular Minecraft rendering API.
 */
public abstract class Canvas {
	public enum Cap {
		BUTT,
		ROUND
	}

	// ------------------------------------------------------------------------------------------------------------
	// Platform contract
	// ------------------------------------------------------------------------------------------------------------

	/** Emits one quad in final GUI coordinates. Vertices wind around the quad; a triangle repeats its last vertex. */
	protected abstract void emitQuad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3);

	/**
	 * Forwards a quad to the platform with the winding Minecraft's GUI pipeline treats as front-facing
	 * (counter-clockwise on screen, i.e. a negative shoelace sum in y-down coordinates), since that pipeline culls.
	 */
	protected final void quad(float x0, float y0, int c0, float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3) {
		float area = (x0 * y1 - x1 * y0) + (x1 * y2 - x2 * y1) + (x2 * y3 - x3 * y2) + (x3 * y0 - x0 * y3);
		if (area > 0f) {
			emitQuad(x0, y0, c0, x3, y3, c3, x2, y2, c2, x1, y1, c1);
		} else if (area < 0f) {
			emitQuad(x0, y0, c0, x1, y1, c1, x2, y2, c2, x3, y3, c3);
		}
	}

	/** Size of one physical pixel in GUI units (1 / gui scale). */
	public abstract float pixel();

	/** GUI-scaled viewport width. */
	public abstract float width();

	/** GUI-scaled viewport height. */
	public abstract float height();

	public abstract float textWidth(String text);

	/** Height of a line of text at scale 1, excluding the descender gap. */
	public abstract float fontHeight();

	/** Draws text whose top-left corner sits at the origin of {@code transform}. */
	protected abstract void emitText(String text, Affine transform, int argb, boolean shadow);

	/** Draws an image filling the {@code w} by {@code h} box at the origin of {@code transform}. */
	protected abstract void emitImage(ImageRef image, Affine transform, float w, float h, float u0, float v0, float u1, float v1, int tint);

	/** Applies a clip rectangle in final GUI coordinates, or removes clipping when {@code null}. */
	protected abstract void applyClip(Rect clip);

	/**
	 * Asks the platform to blur everything drawn so far this frame. Returns whether the request was honoured;
	 * platforms may only support this once per frame.
	 */
	public boolean backdropBlur() {
		return false;
	}

	/** Ends the current geometry batch. Called automatically around text, images and clip changes. */
	public void flush() {
	}

	// ------------------------------------------------------------------------------------------------------------
	// State: transform, opacity and clip stacks
	// ------------------------------------------------------------------------------------------------------------

	private final Affine[] xfStack = new Affine[64];
	private int xfDepth;
	protected Affine xf = new Affine();

	private final float[] alphaStack = new float[64];
	private int alphaDepth;
	protected float alpha = 1f;

	private final Rect[] clipStack = new Rect[32];
	private int clipDepth;

	private final Paint paint = new Paint();
	private final Affine scratchXf = new Affine();

	protected Canvas() {
		for (int i = 0; i < xfStack.length; i++) {
			xfStack[i] = new Affine();
		}
		for (int i = 0; i < clipStack.length; i++) {
			clipStack[i] = new Rect();
		}
	}

	/** Resets all stacks. Platforms call this at the start of every frame. */
	public void reset() {
		xf.identity();
		xfDepth = 0;
		alpha = 1f;
		alphaDepth = 0;
		if (clipDepth > 0) {
			clipDepth = 0;
			flush();
			applyClip(null);
		}
	}

	public Canvas push() {
		if (xfDepth >= xfStack.length) {
			throw new IllegalStateException("Canvas transform stack overflow");
		}
		xfStack[xfDepth++].set(xf);
		return this;
	}

	public Canvas pop() {
		if (xfDepth <= 0) {
			throw new IllegalStateException("Canvas transform stack underflow");
		}
		xf.set(xfStack[--xfDepth]);
		return this;
	}

	public Canvas translate(float x, float y) {
		xf.translate(x, y);
		return this;
	}

	public Canvas scale(float s) {
		xf.scale(s, s);
		return this;
	}

	public Canvas scale(float sx, float sy) {
		xf.scale(sx, sy);
		return this;
	}

	public Canvas rotate(float radians) {
		xf.rotate(radians);
		return this;
	}

	/** Scales around a pivot point. */
	public Canvas scaleAround(float px, float py, float s) {
		xf.translate(px, py);
		xf.scale(s, s);
		xf.translate(-px, -py);
		return this;
	}

	public Affine transform() {
		return xf;
	}

	/** Multiplies the opacity of everything drawn until the matching {@link #popAlpha()}. */
	public Canvas pushAlpha(float a) {
		if (alphaDepth >= alphaStack.length) {
			throw new IllegalStateException("Canvas alpha stack overflow");
		}
		alphaStack[alphaDepth++] = alpha;
		alpha *= MathUtil.clamp01(a);
		return this;
	}

	public Canvas popAlpha() {
		if (alphaDepth <= 0) {
			throw new IllegalStateException("Canvas alpha stack underflow");
		}
		alpha = alphaStack[--alphaDepth];
		return this;
	}

	public float alpha() {
		return alpha;
	}

	/** Intersects the clip with a rectangle given in local coordinates (its transformed bounding box is used). */
	public Canvas pushClip(float x, float y, float w, float h) {
		if (clipDepth >= clipStack.length) {
			throw new IllegalStateException("Canvas clip stack overflow");
		}
		float ax = xf.tx(x, y);
		float ay = xf.ty(x, y);
		float bx = xf.tx(x + w, y + h);
		float by = xf.ty(x + w, y + h);
		float cx0 = xf.tx(x + w, y);
		float cy0 = xf.ty(x + w, y);
		float dx0 = xf.tx(x, y + h);
		float dy0 = xf.ty(x, y + h);
		float minX = Math.min(Math.min(ax, bx), Math.min(cx0, dx0));
		float minY = Math.min(Math.min(ay, by), Math.min(cy0, dy0));
		float maxX = Math.max(Math.max(ax, bx), Math.max(cx0, dx0));
		float maxY = Math.max(Math.max(ay, by), Math.max(cy0, dy0));
		if (clipDepth > 0) {
			Rect parent = clipStack[clipDepth - 1];
			minX = Math.max(minX, parent.x);
			minY = Math.max(minY, parent.y);
			maxX = Math.min(maxX, parent.right());
			maxY = Math.min(maxY, parent.bottom());
		}
		Rect r = clipStack[clipDepth++].set(minX, minY, Math.max(0f, maxX - minX), Math.max(0f, maxY - minY));
		flush();
		applyClip(r);
		return this;
	}

	public Canvas popClip() {
		if (clipDepth <= 0) {
			throw new IllegalStateException("Canvas clip stack underflow");
		}
		clipDepth--;
		flush();
		applyClip(clipDepth > 0 ? clipStack[clipDepth - 1] : null);
		return this;
	}

	/** Converts a local point to final coordinates. */
	public float toScreenX(float x, float y) {
		return xf.tx(x, y);
	}

	public float toScreenY(float x, float y) {
		return xf.ty(x, y);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Contour scratch buffers
	// ------------------------------------------------------------------------------------------------------------

	private float[] px = new float[512];
	private float[] py = new float[512];
	private float[] lx = new float[512];
	private float[] ly = new float[512];
	private int[] pc = new int[512];
	private float[] nx = new float[512];
	private float[] ny = new float[512];
	private float[] ns = new float[512];
	private float[] ix = new float[512];
	private float[] iy = new float[512];
	private float[] maxIn = new float[512];
	private int count;

	private void ensure(int n) {
		if (n <= px.length) {
			return;
		}
		int size = Math.max(n, px.length * 2);
		px = java.util.Arrays.copyOf(px, size);
		py = java.util.Arrays.copyOf(py, size);
		lx = java.util.Arrays.copyOf(lx, size);
		ly = java.util.Arrays.copyOf(ly, size);
		pc = java.util.Arrays.copyOf(pc, size);
		nx = java.util.Arrays.copyOf(nx, size);
		ny = java.util.Arrays.copyOf(ny, size);
		ns = java.util.Arrays.copyOf(ns, size);
		ix = java.util.Arrays.copyOf(ix, size);
		iy = java.util.Arrays.copyOf(iy, size);
		maxIn = java.util.Arrays.copyOf(maxIn, size);
	}

	private void begin() {
		count = 0;
	}

	/** Appends a local-space vertex, evaluating the current paint for its color. Near-duplicate points are dropped. */
	private void vertex(float x, float y) {
		float fx = xf.tx(x, y);
		float fy = xf.ty(x, y);
		if (count > 0) {
			float dx = fx - px[count - 1];
			float dy = fy - py[count - 1];
			if (dx * dx + dy * dy < 1e-8f) {
				return;
			}
		}
		ensure(count + 1);
		px[count] = fx;
		py[count] = fy;
		lx[count] = x;
		ly[count] = y;
		pc[count] = Argb.mulAlpha(paint.eval(x, y), alpha);
		count++;
	}

	private void vertexColored(float x, float y, int color) {
		float fx = xf.tx(x, y);
		float fy = xf.ty(x, y);
		if (count > 0) {
			float dx = fx - px[count - 1];
			float dy = fy - py[count - 1];
			if (dx * dx + dy * dy < 1e-8f) {
				return;
			}
		}
		ensure(count + 1);
		px[count] = fx;
		py[count] = fy;
		lx[count] = x;
		ly[count] = y;
		pc[count] = Argb.mulAlpha(color, alpha);
		count++;
	}

	private void dropClosingDuplicate() {
		if (count > 2) {
			float dx = px[0] - px[count - 1];
			float dy = py[0] - py[count - 1];
			if (dx * dx + dy * dy < 1e-8f) {
				count--;
			}
		}
	}

	private static int transparent(int c) {
		return c & 0x00FFFFFF;
	}

	private boolean allTransparent() {
		for (int i = 0; i < count; i++) {
			if ((pc[i] >>> 24) != 0) {
				return false;
			}
		}
		return true;
	}

	/** Segment count for an arc of {@code radius} (local units) sweeping {@code sweep} radians. */
	protected int segmentsFor(float radius, float sweep) {
		float rPx = radius * xf.scaleFactor() / pixel();
		int n = MathUtil.ceilToInt(Math.abs(sweep) * rPx / 2.6f);
		return MathUtil.clamp(n, Math.max(2, MathUtil.ceilToInt(Math.abs(sweep) / (MathUtil.TAU / 10f))), 512);
	}

	// ------------------------------------------------------------------------------------------------------------
	// Filled contours
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Fills the current closed contour as a fan from its centroid with a feathered edge.
	 *
	 * @param feather width of the soft edge in final GUI units; {@code pixel()} gives a crisp anti-aliased edge
	 * @param bands   number of rings used for the soft edge (1 for AA, more for smooth shadows)
	 */
	private void fillCurrent(float feather, int bands) {
		dropClosingDuplicate();
		if (count < 3 || allTransparent()) {
			return;
		}
		// Orientation from the signed area decides which side is "outside".
		float area = 0f;
		float cxSum = 0f;
		float cySum = 0f;
		float lcx = 0f;
		float lcy = 0f;
		for (int i = 0; i < count; i++) {
			int j = i + 1 == count ? 0 : i + 1;
			area += px[i] * py[j] - px[j] * py[i];
			cxSum += px[i];
			cySum += py[i];
			lcx += lx[i];
			lcy += ly[i];
		}
		if (Math.abs(area) < 1e-6f) {
			return;
		}
		float sign = area > 0f ? 1f : -1f;
		float ccx = cxSum / count;
		float ccy = cySum / count;
		int centerColor = Argb.mulAlpha(paint.eval(lcx / count, lcy / count), alpha);
		computeNormals(true, sign);

		float half = feather * 0.5f;
		// Inner ring: pulled inside by half the feather, but never past the centroid for tiny shapes.
		for (int i = 0; i < count; i++) {
			float dx = px[i] - ccx;
			float dy = py[i] - ccy;
			float d = MathUtil.length(dx, dy);
			maxIn[i] = d * 0.85f;
			float inset = Math.min(half * ns[i], maxIn[i]);
			ix[i] = px[i] - nx[i] * inset;
			iy[i] = py[i] - ny[i] * inset;
		}
		// Solid interior as a fan, two triangles per quad.
		for (int i = 0; i < count; i += 2) {
			int a = i;
			int b = (i + 1) % count;
			int c = (i + 2) % count;
			if (i + 1 >= count) {
				break;
			}
			quad(ccx, ccy, centerColor, ix[a], iy[a], pc[a], ix[b], iy[b], pc[b], ix[c], iy[c], pc[c]);
		}
		if (count % 2 == 1) {
			// The pairing above leaves the closing triangle (last -> first) when the count is odd.
			int a = count - 1;
			quad(ccx, ccy, centerColor, ix[a], iy[a], pc[a], ix[0], iy[0], pc[0], ix[0], iy[0], pc[0]);
		}
		// Feathered fringe.
		if (bands <= 1) {
			for (int i = 0; i < count; i++) {
				int j = i + 1 == count ? 0 : i + 1;
				float oxi = px[i] + nx[i] * half * ns[i];
				float oyi = py[i] + ny[i] * half * ns[i];
				float oxj = px[j] + nx[j] * half * ns[j];
				float oyj = py[j] + ny[j] * half * ns[j];
				quad(ix[i], iy[i], pc[i], oxi, oyi, transparent(pc[i]), oxj, oyj, transparent(pc[j]), ix[j], iy[j], pc[j]);
			}
		} else {
			for (int band = 0; band < bands; band++) {
				float t0 = (float) band / bands;
				float t1 = (float) (band + 1) / bands;
				float a0 = 1f - MathUtil.smoothstep(0f, 1f, t0);
				float a1 = 1f - MathUtil.smoothstep(0f, 1f, t1);
				for (int i = 0; i < count; i++) {
					int j = i + 1 == count ? 0 : i + 1;
					float r0 = -half + feather * t0;
					float r1 = -half + feather * t1;
					// Inward offsets are clamped per vertex so rings never cross the centroid on small shapes.
					float o0i = Math.max(r0 * ns[i], -maxIn[i]);
					float o0j = Math.max(r0 * ns[j], -maxIn[j]);
					float o1i = Math.max(r1 * ns[i], -maxIn[i]);
					float o1j = Math.max(r1 * ns[j], -maxIn[j]);
					float x0i = px[i] + nx[i] * o0i;
					float y0i = py[i] + ny[i] * o0i;
					float x0j = px[j] + nx[j] * o0j;
					float y0j = py[j] + ny[j] * o0j;
					float x1i = px[i] + nx[i] * o1i;
					float y1i = py[i] + ny[i] * o1i;
					float x1j = px[j] + nx[j] * o1j;
					float y1j = py[j] + ny[j] * o1j;
					quad(
						x0i, y0i, Argb.mulAlpha(pc[i], a0),
						x1i, y1i, Argb.mulAlpha(pc[i], a1),
						x1j, y1j, Argb.mulAlpha(pc[j], a1),
						x0j, y0j, Argb.mulAlpha(pc[j], a0)
					);
				}
			}
		}
	}

	/**
	 * Computes per-vertex outward unit normals (nx, ny) and miter scale (ns) for the current point list.
	 *
	 * @param closed whether the list wraps around
	 * @param sign   +1 or -1 to select which side counts as the outward/left side
	 */
	private void computeNormals(boolean closed, float sign) {
		for (int i = 0; i < count; i++) {
			int prev = i - 1;
			int next = i + 1;
			if (closed) {
				prev = prev < 0 ? count - 1 : prev;
				next = next >= count ? 0 : next;
			}
			float n0x = 0f;
			float n0y = 0f;
			float n1x = 0f;
			float n1y = 0f;
			boolean has0 = prev >= 0;
			boolean has1 = next < count;
			if (has0) {
				float dx = px[i] - px[prev];
				float dy = py[i] - py[prev];
				float len = MathUtil.length(dx, dy);
				if (len > 0f) {
					n0x = sign * dy / len;
					n0y = -sign * dx / len;
				} else {
					has0 = false;
				}
			}
			if (has1) {
				float dx = px[next] - px[i];
				float dy = py[next] - py[i];
				float len = MathUtil.length(dx, dy);
				if (len > 0f) {
					n1x = sign * dy / len;
					n1y = -sign * dx / len;
				} else {
					has1 = false;
				}
			}
			if (!has0) {
				n0x = n1x;
				n0y = n1y;
			}
			if (!has1) {
				n1x = n0x;
				n1y = n0y;
			}
			float mx = n0x + n1x;
			float my = n0y + n1y;
			float ml = MathUtil.length(mx, my);
			if (ml < 1e-5f) {
				// 180 degree turn: fall back to the incoming normal.
				nx[i] = n0x;
				ny[i] = n0y;
				ns[i] = 1f;
				continue;
			}
			mx /= ml;
			my /= ml;
			float dot = mx * n1x + my * n1y;
			nx[i] = mx;
			ny[i] = my;
			ns[i] = 1f / Math.max(dot, 0.3f);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Strokes
	// ------------------------------------------------------------------------------------------------------------

	/**
	 * Strokes the current point list. Colors come from the per-vertex colors captured while building it.
	 *
	 * @param width stroke width in local units
	 */
	private void strokeCurrent(float width, boolean closed, Cap cap) {
		if (closed) {
			dropClosingDuplicate();
		}
		if (count < 2 || allTransparent()) {
			return;
		}
		float f = pixel();
		float half = width * 0.5f * xf.scaleFactor();
		float alphaScale = 1f;
		if (half * 2f < f) {
			// Hairline: keep one pixel of coverage and fade instead of thinning, which avoids shimmering.
			alphaScale = Math.max(0.05f, half * 2f / f);
			half = f * 0.5f;
		}
		if (alphaScale < 1f) {
			for (int i = 0; i < count; i++) {
				pc[i] = Argb.mulAlpha(pc[i], alphaScale);
			}
		}
		float core = Math.max(0f, half - f * 0.5f);
		float outer = half + f * 0.5f;
		computeNormals(closed, 1f);

		int segs = closed ? count : count - 1;
		for (int s = 0; s < segs; s++) {
			int i = s;
			int j = s + 1 == count ? 0 : s + 1;
			float si = Math.min(ns[i], 3f);
			float sj = Math.min(ns[j], 3f);
			float lix = px[i] + nx[i] * core * si;
			float liy = py[i] + ny[i] * core * si;
			float ljx = px[j] + nx[j] * core * sj;
			float ljy = py[j] + ny[j] * core * sj;
			float rix = px[i] - nx[i] * core * si;
			float riy = py[i] - ny[i] * core * si;
			float rjx = px[j] - nx[j] * core * sj;
			float rjy = py[j] - ny[j] * core * sj;
			float loix = px[i] + nx[i] * outer * si;
			float loiy = py[i] + ny[i] * outer * si;
			float lojx = px[j] + nx[j] * outer * sj;
			float lojy = py[j] + ny[j] * outer * sj;
			float roix = px[i] - nx[i] * outer * si;
			float roiy = py[i] - ny[i] * outer * si;
			float rojx = px[j] - nx[j] * outer * sj;
			float rojy = py[j] - ny[j] * outer * sj;
			int ci = pc[i];
			int cj = pc[j];
			if (core > 0f) {
				quad(lix, liy, ci, ljx, ljy, cj, rjx, rjy, cj, rix, riy, ci);
			}
			quad(loix, loiy, transparent(ci), lojx, lojy, transparent(cj), ljx, ljy, cj, lix, liy, ci);
			quad(rix, riy, ci, rjx, rjy, cj, rojx, rojy, transparent(cj), roix, roiy, transparent(ci));
		}
		if (!closed) {
			capEnd(0, 1, core, outer, cap);
			capEnd(count - 1, count - 2, core, outer, cap);
		}
	}

	/** Draws a cap at point {@code end}, facing away from {@code toward}. */
	private void capEnd(int end, int toward, float core, float outer, Cap cap) {
		float ex = px[end];
		float ey = py[end];
		float tx = ex - px[toward];
		float ty = ey - py[toward];
		float tl = MathUtil.length(tx, ty);
		if (tl <= 0f) {
			return;
		}
		tx /= tl;
		ty /= tl;
		float s = Math.min(ns[end], 3f);
		float nxe = nx[end] * s;
		float nye = ny[end] * s;
		int c = pc[end];
		int ct = transparent(c);
		float f = outer - core;
		if (cap == Cap.ROUND) {
			// Half disc around the end, from the left side over the tip to the right side.
			float base = MathUtil.atan2(nye, nxe);
			float dir = (nxe * ty - nye * tx) > 0f ? -1f : 1f;
			int n = Math.max(4, segmentsFor(outer / Math.max(xf.scaleFactor(), 1e-3f), MathUtil.PI));
			float prevCx = ex + nxe * core;
			float prevCy = ey + nye * core;
			float prevOx = ex + nxe * outer;
			float prevOy = ey + nye * outer;
			for (int k = 1; k <= n; k++) {
				float a = base + dir * MathUtil.PI * k / n;
				float ca = MathUtil.cos(a);
				float sa = MathUtil.sin(a);
				float cxk = ex + ca * core;
				float cyk = ey + sa * core;
				float oxk = ex + ca * outer;
				float oyk = ey + sa * outer;
				if (core > 0f) {
					quad(ex, ey, c, prevCx, prevCy, c, cxk, cyk, c, cxk, cyk, c);
				}
				quad(prevCx, prevCy, c, prevOx, prevOy, ct, oxk, oyk, ct, cxk, cyk, c);
				prevCx = cxk;
				prevCy = cyk;
				prevOx = oxk;
				prevOy = oyk;
			}
		} else {
			// Butt cap: a thin feathered strip across the end.
			float lcx = ex + nxe * core;
			float lcy = ey + nye * core;
			float rcx = ex - nxe * core;
			float rcy = ey - nye * core;
			float lox = ex + nxe * outer;
			float loy = ey + nye * outer;
			float rox = ex - nxe * outer;
			float roy = ey - nye * outer;
			float ox = tx * f;
			float oy = ty * f;
			quad(lcx, lcy, c, lcx + ox, lcy + oy, ct, rcx + ox, rcy + oy, ct, rcx, rcy, c);
			quad(lox, loy, ct, lox + ox, loy + oy, ct, lcx + ox, lcy + oy, ct, lcx, lcy, c);
			quad(rcx, rcy, c, rcx + ox, rcy + oy, ct, rox + ox, roy + oy, ct, rox, roy, ct);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Public shape API
	// ------------------------------------------------------------------------------------------------------------

	/** Axis-aligned rectangle without edge feathering; intended for full-screen backgrounds. */
	public void fillRectSharp(float x, float y, float w, float h, int color) {
		int c = Argb.mulAlpha(color, alpha);
		if ((c >>> 24) == 0) {
			return;
		}
		quad(xf.tx(x, y), xf.ty(x, y), c, xf.tx(x, y + h), xf.ty(x, y + h), c, xf.tx(x + w, y + h), xf.ty(x + w, y + h), c, xf.tx(x + w, y), xf.ty(x + w, y), c);
	}

	/** Sharp vertical gradient with arbitrary stops ({@code stops} ascending in [0, 1]). */
	public void fillRectGradientV(float x, float y, float w, float h, int[] colors, float[] stops) {
		for (int i = 0; i + 1 < colors.length; i++) {
			float y0 = y + h * stops[i];
			float y1 = y + h * stops[i + 1];
			int c0 = Argb.mulAlpha(colors[i], alpha);
			int c1 = Argb.mulAlpha(colors[i + 1], alpha);
			quad(xf.tx(x, y0), xf.ty(x, y0), c0, xf.tx(x, y1), xf.ty(x, y1), c1, xf.tx(x + w, y1), xf.ty(x + w, y1), c1, xf.tx(x + w, y0), xf.ty(x + w, y0), c0);
		}
	}

	public void fillRect(float x, float y, float w, float h, int color) {
		fillRect(x, y, w, h, paint.setSolid(color));
	}

	public void fillRect(float x, float y, float w, float h, Paint p) {
		if (w <= 0f || h <= 0f) {
			return;
		}
		paint.set(p);
		begin();
		vertex(x, y);
		vertex(x + w, y);
		vertex(x + w, y + h);
		vertex(x, y + h);
		fillCurrent(pixel(), 1);
	}

	public void fillRoundRect(float x, float y, float w, float h, float r, int color) {
		fillRoundRect(x, y, w, h, r, r, r, r, paint.setSolid(color));
	}

	public void fillRoundRect(float x, float y, float w, float h, float r, Paint p) {
		fillRoundRect(x, y, w, h, r, r, r, r, p);
	}

	/** Rounded rectangle with individual corner radii (top-left, top-right, bottom-right, bottom-left). */
	public void fillRoundRect(float x, float y, float w, float h, float tl, float tr, float br, float bl, Paint p) {
		if (w <= 0f || h <= 0f) {
			return;
		}
		paint.set(p);
		roundRectContour(x, y, w, h, tl, tr, br, bl, 0f);
		fillCurrent(pixel(), 1);
	}

	/**
	 * Soft drop shadow: the rounded rect grown by {@code spread} with a smooth edge {@code blur} wide. Rings are
	 * generated as concentric rounded rectangles, so the falloff stays clean on small shapes and tight corners.
	 */
	public void shadow(float x, float y, float w, float h, float r, float blur, float spread, int color) {
		int c = Argb.mulAlpha(color, alpha);
		if ((c >>> 24) == 0 || w <= 0f || h <= 0f) {
			return;
		}
		float feather = Math.max(pixel() / Math.max(1e-3f, xf.scaleFactor()), blur);
		int bands = feather * xf.scaleFactor() > pixel() * 3f ? 5 : 1;
		x -= spread;
		y -= spread;
		w += spread * 2f;
		h += spread * 2f;
		r = Math.min(Math.max(0f, r + spread), Math.min(w, h) * 0.5f);
		float half = feather * 0.5f;
		float minInset = -Math.min(w, h) * 0.5f + 0.25f;
		int perCorner = Math.max(3, Math.min(16, segmentsFor(r + half, MathUtil.PI * 0.5f)));
		int n = perCorner * 4 + 4;
		ensure(n * (bands + 1));
		float cx = x + w * 0.5f;
		float cy = y + h * 0.5f;
		for (int k = 0; k <= bands; k++) {
			float o = Math.max(-half + feather * k / bands, minInset);
			roundRectRing(x - o, y - o, w + o * 2f, h + o * 2f, Math.max(0f, r + o), perCorner, k * n);
		}
		float fcx = xf.tx(cx, cy);
		float fcy = xf.ty(cx, cy);
		// Interior fan over the innermost ring.
		for (int i = 0; i < n; i++) {
			int j = i + 1 == n ? 0 : i + 1;
			quad(fcx, fcy, c, px[i], py[i], c, px[j], py[j], c, px[j], py[j], c);
		}
		for (int k = 0; k < bands; k++) {
			float a0 = bands == 1 ? 1f : 1f - MathUtil.smoothstep(0f, 1f, (float) k / bands);
			float a1 = bands == 1 ? 0f : 1f - MathUtil.smoothstep(0f, 1f, (float) (k + 1) / bands);
			int c0 = Argb.mulAlpha(c, a0);
			int c1 = Argb.mulAlpha(c, a1);
			int base0 = k * n;
			int base1 = (k + 1) * n;
			for (int i = 0; i < n; i++) {
				int j = i + 1 == n ? 0 : i + 1;
				quad(px[base0 + i], py[base0 + i], c0, px[base1 + i], py[base1 + i], c1, px[base1 + j], py[base1 + j], c1, px[base0 + j], py[base0 + j], c0);
			}
		}
	}

	/** Writes a rounded rectangle ring with a fixed vertex count into the scratch buffers at {@code offset}. */
	private void roundRectRing(float x, float y, float w, float h, float r, int perCorner, int offset) {
		r = Math.min(r, Math.min(w, h) * 0.5f);
		int idx = offset;
		float[][] centers = {{x + w - r, y + r}, {x + w - r, y + h - r}, {x + r, y + h - r}, {x + r, y + r}};
		float[] starts = {-MathUtil.PI * 0.5f, 0f, MathUtil.PI * 0.5f, MathUtil.PI};
		for (int k = 0; k < 4; k++) {
			for (int i = 0; i <= perCorner; i++) {
				float a = starts[k] + MathUtil.PI * 0.5f * i / perCorner;
				float lxp = centers[k][0] + MathUtil.cos(a) * r;
				float lyp = centers[k][1] + MathUtil.sin(a) * r;
				px[idx] = xf.tx(lxp, lyp);
				py[idx] = xf.ty(lxp, lyp);
				idx++;
			}
		}
	}

	public void strokeRoundRect(float x, float y, float w, float h, float r, float width, int color) {
		strokeRoundRect(x, y, w, h, r, width, paint.setSolid(color));
	}

	/** Border centered on the inset edge so that the whole stroke stays inside the rectangle. */
	public void strokeRoundRect(float x, float y, float w, float h, float r, float width, Paint p) {
		if (w <= 0f || h <= 0f) {
			return;
		}
		paint.set(p);
		float in = width * 0.5f;
		roundRectContour(x + in, y + in, w - width, h - width, r - in, r - in, r - in, r - in, 0f);
		strokeCurrent(width, true, Cap.BUTT);
	}

	private void roundRectContour(float x, float y, float w, float h, float tl, float tr, float br, float bl, float unused) {
		float maxR = Math.min(w, h) * 0.5f;
		tl = MathUtil.clamp(tl, 0f, maxR);
		tr = MathUtil.clamp(tr, 0f, maxR);
		br = MathUtil.clamp(br, 0f, maxR);
		bl = MathUtil.clamp(bl, 0f, maxR);
		begin();
		corner(x + tl, y + tl, tl, MathUtil.PI, MathUtil.PI * 1.5f);
		corner(x + w - tr, y + tr, tr, MathUtil.PI * 1.5f, MathUtil.TAU);
		corner(x + w - br, y + h - br, br, 0f, MathUtil.PI * 0.5f);
		corner(x + bl, y + h - bl, bl, MathUtil.PI * 0.5f, MathUtil.PI);
	}

	private void corner(float cx, float cy, float r, float a0, float a1) {
		if (r <= 0f) {
			vertex(cx, cy);
			return;
		}
		int n = segmentsFor(r, a1 - a0);
		for (int i = 0; i <= n; i++) {
			float a = a0 + (a1 - a0) * i / n;
			vertex(cx + MathUtil.cos(a) * r, cy + MathUtil.sin(a) * r);
		}
	}

	public void fillCircle(float cx, float cy, float r, int color) {
		fillCircle(cx, cy, r, paint.setSolid(color));
	}

	public void fillCircle(float cx, float cy, float r, Paint p) {
		if (r <= 0f) {
			return;
		}
		paint.set(p);
		begin();
		int n = Math.max(12, segmentsFor(r, MathUtil.TAU));
		for (int i = 0; i < n; i++) {
			float a = MathUtil.TAU * i / n;
			vertex(cx + MathUtil.cos(a) * r, cy + MathUtil.sin(a) * r);
		}
		fillCurrent(pixel(), 1);
	}

	public void fillEllipse(float cx, float cy, float rx, float ry, int color) {
		fillEllipse(cx, cy, rx, ry, paint.setSolid(color));
	}

	public void fillEllipse(float cx, float cy, float rx, float ry, Paint p) {
		if (rx <= 0f || ry <= 0f) {
			return;
		}
		paint.set(p);
		begin();
		int n = Math.max(12, segmentsFor(Math.max(rx, ry), MathUtil.TAU));
		for (int i = 0; i < n; i++) {
			float a = MathUtil.TAU * i / n;
			vertex(cx + MathUtil.cos(a) * rx, cy + MathUtil.sin(a) * ry);
		}
		fillCurrent(pixel(), 1);
	}

	public void strokeCircle(float cx, float cy, float r, float width, int color) {
		strokeArc(cx, cy, r, 0f, MathUtil.TAU, width, color, Cap.BUTT);
	}

	/** Arc from angle {@code a0} to {@code a1} (radians, clockwise on screen because y points down). */
	public void strokeArc(float cx, float cy, float r, float a0, float a1, float width, int color, Cap cap) {
		if (r <= 0f || a0 == a1) {
			return;
		}
		paint.setSolid(color);
		boolean full = Math.abs(a1 - a0) >= MathUtil.TAU - 1e-4f;
		begin();
		int n = Math.max(full ? 12 : 3, segmentsFor(r, a1 - a0));
		int last = full ? n - 1 : n;
		for (int i = 0; i <= last; i++) {
			float a = a0 + (a1 - a0) * i / n;
			vertex(cx + MathUtil.cos(a) * r, cy + MathUtil.sin(a) * r);
		}
		strokeCurrent(width, full, cap);
	}

	/** Radial glow: smooth falloff from {@code color} at the center to transparent at {@code r}. */
	public void glow(float cx, float cy, float r, int color) {
		int c = Argb.mulAlpha(color, alpha);
		if (r <= 0f || (c >>> 24) == 0) {
			return;
		}
		float rPx = r * xf.scaleFactor() / pixel();
		// Small glows (motes, sparkles) need far fewer rings and segments to look identical.
		int rings = rPx < 24f ? 3 : (rPx < 80f ? 5 : 7);
		int n = Math.max(rPx < 24f ? 10 : 16, Math.min(96, segmentsFor(r, MathUtil.TAU) / 2));
		float[] cosT = trigCos(n);
		float[] sinT = trigSin(n);
		float fcx = xf.tx(cx, cy);
		float fcy = xf.ty(cx, cy);
		for (int ring = 0; ring < rings; ring++) {
			float t0 = (float) ring / rings;
			float t1 = (float) (ring + 1) / rings;
			int c0 = Argb.mulAlpha(c, glowFalloff(t0));
			int c1 = Argb.mulAlpha(c, glowFalloff(t1));
			for (int i = 0; i < n; i++) {
				int j = i + 1 == n ? 0 : i + 1;
				float x0i = cx + cosT[i] * r * t0;
				float y0i = cy + sinT[i] * r * t0;
				float x0j = cx + cosT[j] * r * t0;
				float y0j = cy + sinT[j] * r * t0;
				float x1i = cx + cosT[i] * r * t1;
				float y1i = cy + sinT[i] * r * t1;
				float x1j = cx + cosT[j] * r * t1;
				float y1j = cy + sinT[j] * r * t1;
				if (ring == 0) {
					quad(fcx, fcy, c0, xf.tx(x1i, y1i), xf.ty(x1i, y1i), c1, xf.tx(x1j, y1j), xf.ty(x1j, y1j), c1, xf.tx(x1j, y1j), xf.ty(x1j, y1j), c1);
				} else {
					quad(
						xf.tx(x0i, y0i), xf.ty(x0i, y0i), c0,
						xf.tx(x1i, y1i), xf.ty(x1i, y1i), c1,
						xf.tx(x1j, y1j), xf.ty(x1j, y1j), c1,
						xf.tx(x0j, y0j), xf.ty(x0j, y0j), c0
					);
				}
			}
		}
	}

	private float[] trigCosCache = new float[0];
	private float[] trigSinCache = new float[0];

	private float[] trigCos(int n) {
		fillTrig(n);
		return trigCosCache;
	}

	private float[] trigSin(int n) {
		fillTrig(n);
		return trigSinCache;
	}

	private void fillTrig(int n) {
		if (trigCosCache.length == n) {
			return;
		}
		trigCosCache = new float[n];
		trigSinCache = new float[n];
		for (int i = 0; i < n; i++) {
			float a = MathUtil.TAU * i / n;
			trigCosCache[i] = MathUtil.cos(a);
			trigSinCache[i] = MathUtil.sin(a);
		}
	}

	private static float glowFalloff(float t) {
		float u = 1f - t * t;
		return u * u;
	}

	/** Polyline stroke with a single color. Points are interleaved {x0, y0, x1, y1, ...}. */
	public void strokePolyline(float[] pts, int pointCount, boolean closed, float width, int color, Cap cap) {
		paint.setSolid(color);
		begin();
		for (int i = 0; i < pointCount; i++) {
			vertex(pts[i * 2], pts[i * 2 + 1]);
		}
		strokeCurrent(width, closed, cap);
	}

	/** Polyline stroke with one color per point. */
	public void strokePolyline(float[] pts, int[] colors, int pointCount, boolean closed, float width, Cap cap) {
		begin();
		for (int i = 0; i < pointCount; i++) {
			vertexColored(pts[i * 2], pts[i * 2 + 1], colors[i]);
		}
		strokeCurrent(width, closed, cap);
	}

	public void line(float x0, float y0, float x1, float y1, float width, int color, Cap cap) {
		paint.setSolid(color);
		begin();
		vertex(x0, y0);
		vertex(x1, y1);
		strokeCurrent(width, false, cap);
	}

	/** Fills a polygon that is star-shaped around its centroid (convex shapes always are). */
	public void fillPolygon(float[] pts, int pointCount, Paint p) {
		paint.set(p);
		begin();
		for (int i = 0; i < pointCount; i++) {
			vertex(pts[i * 2], pts[i * 2 + 1]);
		}
		fillCurrent(pixel(), 1);
	}

	public void fillPolygon(float[] pts, int pointCount, int color) {
		fillPolygon(pts, pointCount, paint.setSolid(color));
	}

	/** Same as {@link #fillPolygon} but with a wide smooth edge, for blurred blobs and soft light. */
	public void fillPolygonSoft(float[] pts, int pointCount, Paint p, float feather) {
		paint.set(p);
		begin();
		for (int i = 0; i < pointCount; i++) {
			vertex(pts[i * 2], pts[i * 2 + 1]);
		}
		float f = Math.max(pixel(), feather * xf.scaleFactor());
		fillCurrent(f, f > pixel() * 3f ? 5 : 1);
	}

	/**
	 * Fills the strip between two matching polylines (interleaved local coordinates) with anti-aliased long edges.
	 * The ends are left open, so callers taper them to zero width or run them off-screen. Used for liquid shapes
	 * whose silhouette is not star-shaped (stretched drops, threads between bubbles).
	 *
	 * @param colors one color per point pair, or null to use {@code color}
	 */
	public void fillStrip(float[] left, float[] right, int pointCount, int[] colors, int color) {
		if (pointCount < 2) {
			return;
		}
		float f = pixel();
		float half = f * 0.5f;
		float prevNx = 0f;
		float prevNy = 0f;
		float pLix = 0f;
		float pLiy = 0f;
		float pRix = 0f;
		float pRiy = 0f;
		float pLox = 0f;
		float pLoy = 0f;
		float pRox = 0f;
		float pRoy = 0f;
		int pc0 = 0;
		for (int i = 0; i < pointCount; i++) {
			float lxs = xf.tx(left[i * 2], left[i * 2 + 1]);
			float lys = xf.ty(left[i * 2], left[i * 2 + 1]);
			float rxs = xf.tx(right[i * 2], right[i * 2 + 1]);
			float rys = xf.ty(right[i * 2], right[i * 2 + 1]);
			int c = Argb.mulAlpha(colors != null ? colors[i] : color, alpha);
			float dx = lxs - rxs;
			float dy = lys - rys;
			float len = MathUtil.length(dx, dy);
			float nxv;
			float nyv;
			if (len > 1e-4f) {
				nxv = dx / len;
				nyv = dy / len;
			} else {
				nxv = prevNx;
				nyv = prevNy;
			}
			float inset = Math.min(half, len * 0.5f);
			float lix = lxs - nxv * inset;
			float liy = lys - nyv * inset;
			float rix = rxs + nxv * inset;
			float riy = rys + nyv * inset;
			float lox = lxs + nxv * half;
			float loy = lys + nyv * half;
			float rox = rxs - nxv * half;
			float roy = rys - nyv * half;
			if (i > 0) {
				int ct = transparent(c);
				int pct = transparent(pc0);
				quad(pLix, pLiy, pc0, lix, liy, c, rix, riy, c, pRix, pRiy, pc0);
				quad(pLox, pLoy, pct, lox, loy, ct, lix, liy, c, pLix, pLiy, pc0);
				quad(pRix, pRiy, pc0, rix, riy, c, rox, roy, ct, pRox, pRoy, pct);
			}
			prevNx = nxv;
			prevNy = nyv;
			pLix = lix;
			pLiy = liy;
			pRix = rix;
			pRiy = riy;
			pLox = lox;
			pLoy = loy;
			pRox = rox;
			pRoy = roy;
			pc0 = c;
		}
	}

	public void fillTriangle(float x0, float y0, float x1, float y1, float x2, float y2, int color) {
		paint.setSolid(color);
		begin();
		vertex(x0, y0);
		vertex(x1, y1);
		vertex(x2, y2);
		fillCurrent(pixel(), 1);
	}

	/** Fills every closed sub-path of {@code path} (each must be star-shaped around its centroid). */
	public void fillPath(Path path, Paint p) {
		paint.set(p);
		for (int s = 0; s < path.subpathCount(); s++) {
			begin();
			int start = path.subpathStart(s);
			int end = path.subpathEnd(s);
			for (int i = start; i < end; i++) {
				vertex(path.x(i), path.y(i));
			}
			fillCurrent(pixel(), 1);
		}
	}

	public void fillPath(Path path, int color) {
		fillPath(path, paint.setSolid(color));
	}

	public void strokePath(Path path, float width, int color, Cap cap) {
		paint.setSolid(color);
		for (int s = 0; s < path.subpathCount(); s++) {
			begin();
			int start = path.subpathStart(s);
			int end = path.subpathEnd(s);
			for (int i = start; i < end; i++) {
				vertex(path.x(i), path.y(i));
			}
			strokeCurrent(width, path.subpathClosed(s), cap);
		}
	}

	// ------------------------------------------------------------------------------------------------------------
	// Text and images
	// ------------------------------------------------------------------------------------------------------------

	public void text(String text, float x, float y, int color) {
		text(text, x, y, color, 1f, Align.LEFT, false);
	}

	/**
	 * Draws a single line of text.
	 *
	 * @param x     anchor x; the text is placed according to {@code align}
	 * @param y     top of the line
	 * @param scale text scale relative to the platform font
	 */
	public void text(String text, float x, float y, int color, float scale, Align align, boolean shadow) {
		if (text == null || text.isEmpty()) {
			return;
		}
		int c = Argb.mulAlpha(color, alpha);
		if ((c >>> 24) < 4) {
			return;
		}
		scale = crispScale(scale);
		float w = textWidth(text) * scale;
		scratchXf.set(xf);
		scratchXf.translate(x - w * align.factor, y);
		scratchXf.scale(scale, scale);
		flush();
		emitText(text, scratchXf, c, shadow);
	}

	/** Text with extra spacing between characters, for small uppercase labels. */
	public void textTracked(String text, float x, float y, int color, float scale, float tracking, Align align) {
		if (text == null || text.isEmpty()) {
			return;
		}
		float total = 0f;
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			total += textWidth(ch) * scale + tracking;
			i += Character.charCount(cp);
		}
		total -= tracking;
		float cx = x - total * align.factor;
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			String ch = new String(Character.toChars(cp));
			text(ch, cx, y, color, scale, Align.LEFT, false);
			cx += textWidth(ch) * scale + tracking;
			i += Character.charCount(cp);
		}
	}

	public float textWidth(String text, float scale) {
		return textWidth(text) * crispScale(scale);
	}

	/**
	 * Rounds a text scale so that each font pixel covers a whole number of physical pixels; bitmap fonts look
	 * uneven otherwise.
	 */
	public float crispScale(float scale) {
		float sf = xf.scaleFactor();
		if (Math.abs(sf - 1f) > 0.01f) {
			// Inside an animated scale: keep motion smooth instead of stepping between pixel sizes.
			return scale;
		}
		float px = pixel();
		float steps = Math.max(1f, Math.round(scale / px));
		return steps * px;
	}

	/** Truncates {@code text} with an ellipsis so that it fits {@code maxWidth} at {@code scale}. */
	public String ellipsize(String text, float maxWidth, float scale) {
		if (text == null) {
			return "";
		}
		if (textWidth(text) * scale <= maxWidth) {
			return text;
		}
		String dots = "...";
		float dotsW = textWidth(dots) * scale;
		int end = text.length();
		while (end > 0 && textWidth(text.substring(0, end)) * scale + dotsW > maxWidth) {
			end--;
		}
		return text.substring(0, end) + dots;
	}

	public void image(ImageRef image, float x, float y, float w, float h) {
		image(image, x, y, w, h, 0f, 0f, 1f, 1f, Argb.WHITE);
	}

	public void image(ImageRef image, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tint) {
		if (image == null || !image.ready() || w <= 0f || h <= 0f) {
			return;
		}
		int c = Argb.mulAlpha(tint, alpha);
		if ((c >>> 24) == 0) {
			return;
		}
		scratchXf.set(xf);
		scratchXf.translate(x, y);
		flush();
		emitImage(image, scratchXf, w, h, u0, v0, u1, v1, c);
	}
}
