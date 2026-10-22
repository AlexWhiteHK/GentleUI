package io.github.alexwhitehk.gentleui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexwhitehk.gentleui.core.anim.Spring;
import io.github.alexwhitehk.gentleui.core.anim.Tween;
import io.github.alexwhitehk.gentleui.core.math.Easing;
import io.github.alexwhitehk.gentleui.core.math.MathUtil;
import org.junit.jupiter.api.Test;

class AnimationTest {
	@Test
	void criticallyDampedSpringDoesNotOvershoot() {
		Spring s = new Spring(0f, 200f, 1f).target(1f);
		float max = 0f;
		for (int i = 0; i < 240; i++) {
			max = Math.max(max, s.update(1f / 60f));
		}
		assertTrue(max <= 1.0001f);
		assertEquals(1f, s.value(), 1e-3f);
		assertTrue(s.settled());
	}

	@Test
	void bouncySpringOvershootsThenSettles() {
		Spring s = Spring.bouncy(0f).target(1f);
		float max = 0f;
		for (int i = 0; i < 600; i++) {
			max = Math.max(max, s.update(1f / 60f));
		}
		assertTrue(max > 1.01f);
		assertEquals(1f, s.value(), 1e-3f);
	}

	@Test
	void springIsFrameRateIndependent() {
		Spring fast = new Spring(0f, 150f, 0.8f).target(10f);
		Spring slow = new Spring(0f, 150f, 0.8f).target(10f);
		for (int i = 0; i < 144; i++) {
			fast.update(1f / 144f);
		}
		for (int i = 0; i < 30; i++) {
			slow.update(1f / 30f);
		}
		assertEquals(fast.value(), slow.value(), 0.05f);
	}

	@Test
	void easingsHitTheirEndpoints() {
		Easing[] all = {Easing.LINEAR, Easing.IN_QUAD, Easing.OUT_QUAD, Easing.IN_OUT_QUAD, Easing.OUT_CUBIC, Easing.IN_OUT_CUBIC,
			Easing.OUT_QUART, Easing.OUT_QUINT, Easing.IN_OUT_QUINT, Easing.OUT_EXPO, Easing.IN_OUT_EXPO, Easing.OUT_SINE, Easing.IN_OUT_SINE,
			Easing.OUT_BACK, Easing.OUT_BACK_SOFT, Easing.EMPHASIZED, Easing.bezier(0.2f, 0f, 0f, 1f)};
		for (Easing e : all) {
			assertEquals(0f, e.apply(0f), 1e-3f);
			assertEquals(1f, e.apply(1f), 1e-3f);
		}
	}

	@Test
	void bezierEasingIsMonotonicForStandardCurve() {
		Easing e = Easing.bezier(0.4f, 0f, 0.2f, 1f);
		float prev = 0f;
		for (int i = 1; i <= 100; i++) {
			float v = e.apply(i / 100f);
			assertTrue(v + 1e-4f >= prev);
			prev = v;
		}
	}

	@Test
	void tweenRunsForItsDuration() {
		Tween t = new Tween(0.5f, Easing.LINEAR).start(0.1f);
		t.update(0.1f);
		assertEquals(0f, t.value(), 1e-4f);
		t.update(0.25f);
		assertEquals(0.5f, t.value(), 1e-3f);
		t.update(0.3f);
		assertTrue(t.done());
		assertEquals(1f, t.value(), 1e-4f);
	}

	@Test
	void dampIsBoundedAndHalves() {
		assertEquals(0.5f, MathUtil.damp(0.2f, 0.2f), 1e-4f);
		assertEquals(1f, MathUtil.damp(0f, 0.016f), 1e-6f);
		assertEquals(0f, MathUtil.wrapAngle(MathUtil.TAU), 1e-5f);
	}
}
