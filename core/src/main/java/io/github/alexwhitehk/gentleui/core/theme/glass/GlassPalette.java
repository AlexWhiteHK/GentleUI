package io.github.alexwhitehk.gentleui.core.theme.glass;

import java.util.LinkedHashMap;
import java.util.Map;

/** Background schemes for the Glass theme: a base gradient and five glowing orbs. */
public final class GlassPalette {
	public final String id;
	public final boolean light;
	public final int baseTop;
	public final int baseBottom;
	public final int[] orbs;
	public final int accent;
	public final int text;
	public final int textMuted;

	private static final Map<String, GlassPalette> ALL = new LinkedHashMap<>();

	public static final GlassPalette AURORA = add(new GlassPalette("aurora", false, 0xFF090E22, 0xFF130A2B,
		new int[] {0xFF7B5CFF, 0xFF22D3EE, 0xFFF472B6, 0xFF3B82F6, 0xFFA78BFA}, 0xFF7DD3FC, 0xFFF5F7FF, 0xFFB4BCD6));
	public static final GlassPalette SUNSET = add(new GlassPalette("sunset", false, 0xFF160A1B, 0xFF2A0E1E,
		new int[] {0xFFFB923C, 0xFFDB2777, 0xFF7C3AED, 0xFFFBBF24, 0xFFF43F5E}, 0xFFFDBA74, 0xFFFFF7F2, 0xFFD9B8C4));
	public static final GlassPalette OCEAN = add(new GlassPalette("ocean", false, 0xFF031424, 0xFF042A3D,
		new int[] {0xFF14B8A6, 0xFF0EA5E9, 0xFF6366F1, 0xFF34D399, 0xFF22D3EE}, 0xFF67E8F9, 0xFFF0FDFF, 0xFFA7C4CF));
	public static final GlassPalette FROST = add(new GlassPalette("frost", true, 0xFFF1F4FA, 0xFFE2E7F2,
		new int[] {0xFFA5B4FC, 0xFFF9A8D4, 0xFF93C5FD, 0xFF99F6E4, 0xFFFDE68A}, 0xFF4F46E5, 0xFF1B2030, 0xFF5B6478));

	private GlassPalette(String id, boolean light, int baseTop, int baseBottom, int[] orbs, int accent, int text, int textMuted) {
		this.id = id;
		this.light = light;
		this.baseTop = baseTop;
		this.baseBottom = baseBottom;
		this.orbs = orbs;
		this.accent = accent;
		this.text = text;
		this.textMuted = textMuted;
	}

	private static GlassPalette add(GlassPalette p) {
		ALL.put(p.id, p);
		return p;
	}

	public static GlassPalette byId(String id) {
		GlassPalette p = ALL.get(id);
		return p != null ? p : AURORA;
	}

	public static Map<String, GlassPalette> all() {
		return ALL;
	}
}
