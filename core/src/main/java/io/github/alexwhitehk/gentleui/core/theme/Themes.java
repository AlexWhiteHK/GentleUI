package io.github.alexwhitehk.gentleui.core.theme;

import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Registry of themes and the currently active one. Themes are created lazily and kept for the session. */
public final class Themes {
	private static final Map<String, Supplier<Theme>> FACTORIES = new LinkedHashMap<>();
	private static final Map<String, Theme> INSTANCES = new LinkedHashMap<>();
	public static final String DEFAULT = "liquid";

	private Themes() {
	}

	public static void register(String id, Supplier<Theme> factory) {
		FACTORIES.put(id, factory);
	}

	public static List<String> ids() {
		return Collections.unmodifiableList(new ArrayList<>(FACTORIES.keySet()));
	}

	public static Theme get(String id) {
		Theme t = INSTANCES.get(id);
		if (t == null) {
			Supplier<Theme> f = FACTORIES.get(id);
			if (f == null) {
				f = FACTORIES.get(DEFAULT);
				if (f == null) {
					throw new IllegalStateException("No themes registered");
				}
				id = DEFAULT;
			}
			t = INSTANCES.get(id);
			if (t == null) {
				t = f.get();
				INSTANCES.put(id, t);
			}
		}
		return t;
	}

	public static Theme active() {
		return get(ConfigStore.config().theme);
	}

	public static void configChanged() {
		for (Theme t : INSTANCES.values()) {
			t.configChanged();
		}
	}
}
