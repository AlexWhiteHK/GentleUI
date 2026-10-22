package io.github.alexwhitehk.gentleui.fabric.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import io.github.alexwhitehk.gentleui.core.config.ConfigStore;
import io.github.alexwhitehk.gentleui.core.render.ImageRef;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resolves {@link ImageRef}s from resource ids or PNG files in {@code config/gentleui/images}. */
public final class Images {
	private static final Logger LOG = LoggerFactory.getLogger("GentleUI");
	private static final Map<String, ImageRef> CACHE = new HashMap<>();

	/** Native handle stored in {@link ImageRef#handle}. */
	public record Handle(Identifier id, boolean smooth) {
		@Nullable TextureSetup setup() {
			AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(id);
			if (tex == null) {
				return null;
			}
			try {
				var sampler = smooth ? RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR) : tex.getSampler();
				return TextureSetup.singleTexture(tex.getTextureView(), sampler);
			} catch (IllegalStateException notUploaded) {
				return null;
			}
		}
	}

	private Images() {
	}

	public static ImageRef get(String source) {
		return CACHE.computeIfAbsent(source, Images::load);
	}

	/** Drops cached file images so edited files are picked up again. */
	public static void clearFiles() {
		Minecraft mc = Minecraft.getInstance();
		CACHE.entrySet().removeIf(e -> {
			if (e.getKey().startsWith("file:") && e.getValue().handle instanceof Handle h) {
				mc.getTextureManager().release(h.id());
				return true;
			}
			return e.getKey().startsWith("file:");
		});
	}

	private static ImageRef load(String source) {
		ImageRef ref = new ImageRef(source, 0, 0);
		try {
			if (source.startsWith("file:")) {
				loadFile(ref, source.substring(5));
			} else {
				loadResource(ref, source);
			}
		} catch (Exception e) {
			LOG.warn("GentleUI: could not load image {}: {}", source, e.toString());
			ref.failed = true;
		}
		return ref;
	}

	private static void loadFile(ImageRef ref, String name) throws Exception {
		Path dir = ConfigStore.get().imagesDirectory().toAbsolutePath().normalize();
		Path file = dir.resolve(name).normalize();
		if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
			// Only files inside the images folder may be loaded.
			ref.failed = true;
			return;
		}
		try (InputStream in = Files.newInputStream(file)) {
			NativeImage image = NativeImage.read(in);
			ref.width = image.getWidth();
			ref.height = image.getHeight();
			String key = Integer.toHexString(name.toLowerCase(Locale.ROOT).hashCode());
			Identifier id = Identifier.fromNamespaceAndPath("gentleui", "dynamic/" + key);
			Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "gentleui:" + name, image));
			ref.handle = new Handle(id, true);
		}
	}

	private static void loadResource(ImageRef ref, String source) throws Exception {
		Identifier id = Identifier.parse(source);
		Minecraft mc = Minecraft.getInstance();
		var resource = mc.getResourceManager().getResource(id);
		if (resource.isEmpty()) {
			ref.failed = true;
			return;
		}
		try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
			ref.width = image.getWidth();
			ref.height = image.getHeight();
		}
		ref.handle = new Handle(id, true);
	}
}
