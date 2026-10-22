package io.github.alexwhitehk.gentleui.core.scene.elements;

/** Nine reference points of the screen used to position elements so they survive window resizes. */
public enum Anchor {
	TOP_LEFT("top_left", 0f, 0f),
	TOP("top", 0.5f, 0f),
	TOP_RIGHT("top_right", 1f, 0f),
	LEFT("left", 0f, 0.5f),
	CENTER("center", 0.5f, 0.5f),
	RIGHT("right", 1f, 0.5f),
	BOTTOM_LEFT("bottom_left", 0f, 1f),
	BOTTOM("bottom", 0.5f, 1f),
	BOTTOM_RIGHT("bottom_right", 1f, 1f);

	public final String id;
	public final float fx;
	public final float fy;

	Anchor(String id, float fx, float fy) {
		this.id = id;
		this.fx = fx;
		this.fy = fy;
	}

	public static Anchor byId(String id) {
		for (Anchor a : values()) {
			if (a.id.equals(id)) {
				return a;
			}
		}
		return CENTER;
	}

	/** The anchor whose reference point is nearest to (x, y) on a w x h screen. */
	public static Anchor nearest(float x, float y, float w, float h) {
		int col = x < w / 3f ? 0 : (x < w * 2f / 3f ? 1 : 2);
		int row = y < h / 3f ? 0 : (y < h * 2f / 3f ? 1 : 2);
		return values()[row * 3 + col];
	}
}
