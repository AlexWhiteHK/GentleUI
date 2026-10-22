package io.github.alexwhitehk.gentleui.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-theme title screen layout: adjustments to built-in elements plus user-added elements. */
public final class LayoutConfig {
	public Map<String, ElementOverride> overrides = new LinkedHashMap<>();
	public List<CustomElement> custom = new ArrayList<>();

	public ElementOverride override(String id) {
		return overrides.computeIfAbsent(id, k -> new ElementOverride());
	}

	public ElementOverride overrideOrNull(String id) {
		return overrides.get(id);
	}

	public static final class ElementOverride {
		/** Offset from the built-in position, in GUI units. */
		public float dx;
		public float dy;
		public float scale = 1f;
		public float opacity = 1f;
		public boolean hidden;

		public boolean isDefault() {
			return dx == 0f && dy == 0f && scale == 1f && opacity == 1f && !hidden;
		}
	}
}
