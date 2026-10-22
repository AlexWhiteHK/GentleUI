package io.github.alexwhitehk.gentleui.core.render;

/**
 * Handle to a texture owned by the platform. The core only knows its id and pixel size; the platform stores
 * whatever native handle it needs in {@link #handle}.
 */
public final class ImageRef {
	public final String id;
	public int width;
	public int height;
	public Object handle;
	public boolean failed;

	public ImageRef(String id, int width, int height) {
		this.id = id;
		this.width = width;
		this.height = height;
	}

	public boolean ready() {
		return handle != null && !failed && width > 0 && height > 0;
	}

	public float aspect() {
		return height <= 0 ? 1f : (float) width / height;
	}
}
