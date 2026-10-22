package io.github.alexwhitehk.gentleui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import io.github.alexwhitehk.gentleui.core.color.Oklab;
import io.github.alexwhitehk.gentleui.core.theme.Palette;
import io.github.alexwhitehk.gentleui.core.theme.TextColors;
import org.junit.jupiter.api.Test;

class ColorTest {
	@Test
	void parseAndFormat() {
		assertEquals(0xFF1A73E8, Argb.parse("#1A73E8", 0));
		assertEquals(0xFFFFFFFF, Argb.parse("#fff", 0));
		assertEquals(0x801A73E8, Argb.parse("#801A73E8", 0));
		assertEquals(7, Argb.parse("not a color", 7));
		assertEquals("#1A73E8", Argb.format(0xFF1A73E8));
		assertEquals("#801A73E8", Argb.format(0x801A73E8));
	}

	@Test
	void alphaHelpers() {
		assertEquals(0x80FFFFFF, Argb.withAlpha(0xFFFFFFFF, 128f / 255f));
		assertEquals(0x40FFFFFF, Argb.mulAlpha(0x80FFFFFF, 0.5f));
		assertEquals(0xFF000000, Argb.lerp(0xFF000000, 0xFFFFFFFF, 0f));
		assertEquals(0xFFFFFFFF, Argb.lerp(0xFF000000, 0xFFFFFFFF, 1f));
	}

	@Test
	void oklabRoundTrip() {
		int[] samples = {0xFF1A73E8, 0xFFEA4335, 0xFFFBBC04, 0xFF34A853, 0xFF000000, 0xFFFFFFFF, 0xFF808080};
		for (int c : samples) {
			float[] lab = Oklab.fromArgb(c);
			int back = Oklab.toArgb(lab[0], lab[1], lab[2], 1f);
			assertTrue(Math.abs(Argb.r(c) - Argb.r(back)) <= 1);
			assertTrue(Math.abs(Argb.g(c) - Argb.g(back)) <= 1);
			assertTrue(Math.abs(Argb.b(c) - Argb.b(back)) <= 1);
		}
	}

	private static Palette light() {
		Palette p = new Palette();
		p.light = true;
		p.text = 0xFF1F1F1F;
		p.textMuted = 0xFF5F6368;
		return p;
	}

	@Test
	void lightThemesDarkenVanillaText() {
		Palette p = light();
		assertEquals(0xFF1F1F1F, TextColors.remap(0xFFFFFFFF, p));
		assertEquals(0xFF5F6368, TextColors.remap(0xFFA0A0A0, p));
		int yellow = TextColors.remap(0xFFFFFF55, p);
		assertTrue(Argb.luminance(yellow) < 0.3f, "warning text must become readable on light surfaces");
		assertEquals(0xFF202020, TextColors.remap(0xFF202020, p));
	}

	@Test
	void darkThemesKeepVanillaText() {
		Palette p = light();
		p.light = false;
		assertEquals(0xFFFFFFFF, TextColors.remap(0xFFFFFFFF, p));
	}

	@Test
	void fadeTreatsZeroAlphaAsOpaque() {
		assertEquals(0x80FFFFFF, TextColors.fade(0x00FFFFFF, 128f / 255f));
		assertEquals(0xFFFFFFFF, TextColors.fade(0xFFFFFFFF, 1f));
	}
}
