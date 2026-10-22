package io.github.alexwhitehk.gentleui.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Loads, saves and broadcasts the configuration. The single shared instance lives in {@link #get()}. */
public final class ConfigStore {
	private static final Logger LOG = Logger.getLogger("GentleUI");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static ConfigStore instance;

	private final Path dir;
	private final Path file;
	private GentleConfig config = new GentleConfig();
	private final List<Consumer<GentleConfig>> listeners = new ArrayList<>();

	private ConfigStore(Path dir) {
		this.dir = dir;
		this.file = dir.resolve("gentleui.json");
	}

	public static ConfigStore init(Path dir) {
		instance = new ConfigStore(dir);
		instance.load();
		return instance;
	}

	public static ConfigStore get() {
		if (instance == null) {
			// Tests and early callers get an in-memory store.
			instance = new ConfigStore(Path.of("config", "gentleui"));
		}
		return instance;
	}

	public static GentleConfig config() {
		return get().config;
	}

	public Path directory() {
		return dir;
	}

	public Path imagesDirectory() {
		return dir.resolve("images");
	}

	public void load() {
		if (!Files.isRegularFile(file)) {
			config = new GentleConfig().sanitize();
			return;
		}
		try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			GentleConfig loaded = GSON.fromJson(r, GentleConfig.class);
			config = (loaded == null ? new GentleConfig() : loaded).sanitize();
		} catch (IOException | JsonParseException e) {
			LOG.log(Level.WARNING, "GentleUI: could not read " + file + ", using defaults", e);
			try {
				Files.copy(file, dir.resolve("gentleui.json.broken"), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException ignored) {
				// best effort backup
			}
			config = new GentleConfig().sanitize();
		}
	}

	public void save() {
		try {
			Files.createDirectories(dir);
			Files.createDirectories(imagesDirectory());
			Path tmp = dir.resolve("gentleui.json.tmp");
			try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(config, w);
			}
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (java.nio.file.AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "GentleUI: could not save " + file, e);
		}
	}

	/** Applies a change, saves it and notifies listeners. */
	public void update(Consumer<GentleConfig> change) {
		change.accept(config);
		config.sanitize();
		dirty = false;
		save();
		for (Consumer<GentleConfig> l : List.copyOf(listeners)) {
			l.accept(config);
		}
	}

	private boolean dirty;

	/** Applies a change and notifies listeners immediately, deferring the disk write to {@link #saveIfDirty()}. */
	public void edit(Consumer<GentleConfig> change) {
		change.accept(config);
		config.sanitize();
		dirty = true;
		for (Consumer<GentleConfig> l : List.copyOf(listeners)) {
			l.accept(config);
		}
	}

	public void saveIfDirty() {
		if (dirty) {
			dirty = false;
			save();
		}
	}

	public void addListener(Consumer<GentleConfig> listener) {
		listeners.add(listener);
	}

	public static String toJson(Object o) {
		return GSON.toJson(o);
	}

	public static <T> T fromJson(String json, Class<T> type) {
		return GSON.fromJson(json, type);
	}
}
