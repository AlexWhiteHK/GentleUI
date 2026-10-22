package io.github.alexwhitehk.gentleui.core.theme.liquid;

import io.github.alexwhitehk.gentleui.core.color.Argb;
import java.util.LinkedHashMap;
import java.util.Map;

/** Color schemes for the lake. Each one keeps the same structure so the scene code never branches on palette. */
public final class LiquidPalette {
	public final String id;
	public final boolean light;
	public final int bgTop;
	public final int bgMid;
	public final int bgBottom;
	public final int glowA;
	public final int glowB;
	public final int line;
	public final int accent;
	public final int mote;
	public final int text;
	public final int textMuted;
	public final int rim;
	public final int fill;
	public final int shadow;

	private static final Map<String, LiquidPalette> ALL = new LinkedHashMap<>();

	public static final LiquidPalette NIGHT = add(new LiquidPalette("night", false,
		0xFF060B16, 0xFF0A1727, 0xFF0E2536, 0xFF1D6E8A, 0xFF3A2D7C, 0xFFBFEFFF, 0xFF79E2F2, 0xFFE2FBFF,
		0xFFF2FBFF, 0xFF8FAABA, 0xFFE8FBFF, 0xFFFFFFFF, 0xFF000000));
	public static final LiquidPalette MIST = add(new LiquidPalette("mist", true,
		0xFFF1F5F8, 0xFFDFE9EF, 0xFFCADBE5, 0xFFFFFFFF, 0xFFAFD0EA, 0xFF4E7488, 0xFF2A7A9C, 0xFF6F9AB0,
		0xFF102230, 0xFF587080, 0xFF3D6B82, 0xFFFFFFFF, 0xFF27465A));
	public static final LiquidPalette ABYSS = add(new LiquidPalette("abyss", false,
		0xFF07051A, 0xFF0D0A29, 0xFF19113D, 0xFF4B2C9A, 0xFF1D4E8E, 0xFFD8CCFF, 0xFFB49DFF, 0xFFEDE6FF,
		0xFFF6F2FF, 0xFFA59CC4, 0xFFF0EAFF, 0xFFFFFFFF, 0xFF000000));
	public static final LiquidPalette JADE = add(new LiquidPalette("jade", false,
		0xFF03100E, 0xFF062220, 0xFF0B342F, 0xFF1A7E69, 0xFF2E6B4C, 0xFFBDFFEA, 0xFF6FE7C4, 0xFFE0FFF6,
		0xFFF0FFFA, 0xFF8DB5A9, 0xFFE6FFF7, 0xFFFFFFFF, 0xFF000000));

	private LiquidPalette(String id, boolean light, int bgTop, int bgMid, int bgBottom, int glowA, int glowB, int line,
			int accent, int mote, int text, int textMuted, int rim, int fill, int shadow) {
		this.id = id;
		this.light = light;
		this.bgTop = bgTop;
		this.bgMid = bgMid;
		this.bgBottom = bgBottom;
		this.glowA = glowA;
		this.glowB = glowB;
		this.line = line;
		this.accent = accent;
		this.mote = mote;
		this.text = text;
		this.textMuted = textMuted;
		this.rim = rim;
		this.fill = fill;
		this.shadow = shadow;
	}

	private static LiquidPalette add(LiquidPalette p) {
		ALL.put(p.id, p);
		return p;
	}

	public static LiquidPalette byId(String id) {
		LiquidPalette p = ALL.get(id);
		return p != null ? p : NIGHT;
	}

	public static Map<String, LiquidPalette> all() {
		return ALL;
	}

	/** Base color averaged from the gradient, for veils and solid fills. */
	public int base() {
		return Argb.lerp(bgTop, bgBottom, 0.5f);
	}
}
