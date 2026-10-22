package io.github.alexwhitehk.gentleui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexwhitehk.gentleui.core.math.Rect;
import io.github.alexwhitehk.gentleui.core.render.Canvas;
import io.github.alexwhitehk.gentleui.core.render.Icon;
import io.github.alexwhitehk.gentleui.core.render.Paint;
import org.junit.jupiter.api.Test;

class CanvasTest {
	/** Minecraft's GUI pipeline culls back faces, so every emitted quad must share the front-facing winding. */
	private static void assertFrontFacing(RecordingCanvas c) {
		assertFalse(c.quads.isEmpty(), "nothing was drawn");
		for (float[] q : c.quads) {
			for (float v : q) {
				assertFalse(Float.isNaN(v) || Float.isInfinite(v), "non-finite vertex");
			}
			assertTrue(RecordingCanvas.shoelace(q) <= 1e-3f, "quad wound clockwise on screen");
		}
	}

	@Test
	void shapesAreFrontFacing() {
		RecordingCanvas c = new RecordingCanvas();
		c.fillRect(10, 10, 50, 20, 0xFFFFFFFF);
		c.fillRoundRect(10, 40, 80, 24, 12, 0xFF336699);
		c.strokeRoundRect(10, 70, 80, 24, 6, 1.5f, 0xFF000000);
		c.fillCircle(200, 100, 30, Paint.radial(200, 100, 30, 0xFFFFFFFF, 0x00FFFFFF));
		c.strokeArc(200, 100, 40, 0f, 3f, 2f, 0xFFFFFFFF, Canvas.Cap.ROUND);
		c.glow(100, 100, 60, 0x80FFFFFF);
		c.shadow(20, 150, 30, 12, 6, 14, 0, 0x55000000);
		c.line(0, 0, 100, 30, 0.3f, 0xFFFFFFFF, Canvas.Cap.BUTT);
		Icon.GEAR.draw(c, 300, 50, 16, 0xFFFFFFFF);
		Icon.CLOUD.draw(c, 330, 50, 16, 0xFFFFFFFF);
		assertFrontFacing(c);
	}

	@Test
	void stripsAreFrontFacing() {
		RecordingCanvas c = new RecordingCanvas();
		float[] left = {0, 0, 10, -5, 20, -3, 30, 0};
		float[] right = {0, 10, 10, 6, 20, 3, 30, 0};
		c.fillStrip(left, right, 4, null, 0xFFFFFFFF);
		assertFrontFacing(c);
	}

	@Test
	void smallShadowStaysInsideItsRing() {
		// A shadow far wider than the shape must not fold over itself (it used to draw an "X" through tiny cards).
		RecordingCanvas c = new RecordingCanvas();
		c.shadow(100, 100, 20, 8, 4, 30, 0, 0xFF000000);
		assertFrontFacing(c);
		float minX = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		for (float[] q : c.quads) {
			for (int i = 0; i < 4; i++) {
				minX = Math.min(minX, q[i * 2]);
				maxX = Math.max(maxX, q[i * 2]);
			}
		}
		assertTrue(minX >= 100 - 16 && maxX <= 120 + 16);
	}

	@Test
	void transparentShapesEmitNothing() {
		RecordingCanvas c = new RecordingCanvas();
		c.fillRoundRect(0, 0, 10, 10, 3, 0x00FFFFFF);
		c.pushAlpha(0f);
		c.fillCircle(5, 5, 5, 0xFFFFFFFF);
		c.text("hidden", 0, 0, 0xFFFFFFFF);
		c.popAlpha();
		assertTrue(c.quads.isEmpty());
		assertTrue(c.texts.isEmpty());
	}

	@Test
	void clipStackIntersects() {
		RecordingCanvas c = new RecordingCanvas();
		c.pushClip(0, 0, 100, 100);
		c.pushClip(50, 50, 100, 100);
		assertEquals(50f, c.clip.x);
		assertEquals(50f, c.clip.w);
		c.popClip();
		assertEquals(100f, c.clip.w);
		c.popClip();
		assertNull(c.clip);
	}

	@Test
	void ellipsizeFits() {
		RecordingCanvas c = new RecordingCanvas();
		String s = c.ellipsize("A fairly long label", 60f, 1f);
		assertTrue(c.textWidth(s) <= 60f);
		assertTrue(s.endsWith("..."));
		assertEquals("Short", c.ellipsize("Short", 60f, 1f));
	}

	@Test
	void textScaleSnapsToWholePixels() {
		RecordingCanvas c = new RecordingCanvas();
		c.pixel = 0.5f;
		assertEquals(1.5f, c.crispScale(1.4f), 1e-4f);
		assertEquals(0.5f, c.crispScale(0.1f), 1e-4f);
		c.push().scale(1.3f);
		assertEquals(1.4f, c.crispScale(1.4f), 1e-4f);
		c.pop();
	}

	@Test
	void transformsApplyToGeometry() {
		RecordingCanvas c = new RecordingCanvas();
		c.push().translate(100, 50).scale(2f);
		c.fillRect(0, 0, 10, 10, 0xFFFFFFFF);
		c.pop();
		Rect bounds = new Rect(Float.MAX_VALUE, Float.MAX_VALUE, 0, 0);
		float maxX = -Float.MAX_VALUE;
		for (float[] q : c.quads) {
			for (int i = 0; i < 4; i++) {
				bounds.x = Math.min(bounds.x, q[i * 2]);
				maxX = Math.max(maxX, q[i * 2]);
			}
		}
		assertEquals(100f, bounds.x, 0.6f);
		assertEquals(120f, maxX, 0.6f);
	}
}
